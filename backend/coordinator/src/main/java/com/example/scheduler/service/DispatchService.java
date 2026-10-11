package com.example.scheduler.service;

import com.example.scheduler.entity.ExecutionEvent;
import com.example.scheduler.entity.JobExecution;
import com.example.scheduler.entity.ScheduledJob;
import com.example.scheduler.entity.WorkerNode;
import com.example.scheduler.enums.ExecutionStatus;
import com.example.scheduler.enums.WorkerStatus;
import com.example.scheduler.grpc.GrpcDispatchClient;
import com.example.scheduler.repository.ExecutionEventRepository;
import com.example.scheduler.repository.JobExecutionRepository;
import com.example.scheduler.repository.WorkerNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchService {

    private final WorkerNodeRepository workerRepository;
    private final JobExecutionRepository executionRepository;
    private final ExecutionEventRepository eventRepository;
    private final GrpcDispatchClient grpcDispatchClient;

    @Transactional
    public boolean dispatchJobExecution(JobExecution execution, ScheduledJob job) {
        List<WorkerNode> availableWorkers = workerRepository.findByStatusIn(List.of(WorkerStatus.ONLINE, WorkerStatus.IDLE));

        Optional<WorkerNode> eligibleWorker = availableWorkers.stream()
                .filter(w -> w.getActiveTasksCount() < w.getMaxConcurrentTasks())
                .filter(w -> w.getSupportedTaskTypes() != null && w.getSupportedTaskTypes().contains(job.getTaskType()))
                .findFirst();

        if (eligibleWorker.isEmpty()) {
            // Fallback to any online worker
            eligibleWorker = availableWorkers.stream()
                    .filter(w -> w.getActiveTasksCount() < w.getMaxConcurrentTasks())
                    .findFirst();
        }

        if (eligibleWorker.isEmpty()) {
            log.warn("No available worker node for execution {}", execution.getId());
            // #region agent log
            try { java.nio.file.Files.writeString(java.nio.file.Path.of("C:\\Users\\YENNI RAHUL\\OneDrive\\Desktop\\distributed-task-scheduler\\debug-1fc9a9.log"), "{\"sessionId\":\"1fc9a9\",\"hypothesisId\":\"H2\",\"location\":\"DispatchService.java:dispatchJobExecution\",\"message\":\"no eligible worker\",\"data\":{\"onlineCount\":" + availableWorkers.size() + ",\"taskType\":\"" + job.getTaskType() + "\"},\"timestamp\":" + System.currentTimeMillis() + "}\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Exception ignored) {}
            // #endregion
            recordEvent(execution, "DISPATCH_FAILED", "No available worker with capacity for task type: " + job.getTaskType());
            return false;
        }

        WorkerNode worker = eligibleWorker.get();
        execution.setWorkerId(worker.getWorkerId());
        execution.setStatus(ExecutionStatus.DISPATCHED);
        execution.setStartedAt(LocalDateTime.now());
        executionRepository.save(execution);

        worker.setActiveTasksCount(worker.getActiveTasksCount() + 1);
        if (worker.getActiveTasksCount() >= worker.getMaxConcurrentTasks()) {
            worker.setStatus(WorkerStatus.BUSY);
        }
        workerRepository.save(worker);

        recordEvent(execution, "DISPATCHED", "Dispatched to worker: " + worker.getWorkerId());

        // Dispatch via gRPC
        boolean success = grpcDispatchClient.dispatchTask(worker.getHostname(), worker.getPort(), execution, job);
        if (!success) {
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage("Failed gRPC dispatch to worker");
            execution.setCompletedAt(LocalDateTime.now());
            executionRepository.save(execution);

            worker.setActiveTasksCount(Math.max(0, worker.getActiveTasksCount() - 1));
            workerRepository.save(worker);

            recordEvent(execution, "DISPATCH_ERROR", "gRPC invocation failed to worker " + worker.getWorkerId());
            return false;
        }

        return true;
    }

    public void recordEvent(JobExecution execution, String eventType, String message) {
        ExecutionEvent event = ExecutionEvent.builder()
                .execution(execution)
                .eventType(eventType)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        eventRepository.save(event);
    }
}
