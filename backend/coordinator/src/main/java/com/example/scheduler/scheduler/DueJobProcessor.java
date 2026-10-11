package com.example.scheduler.scheduler;

import com.example.scheduler.entity.JobExecution;
import com.example.scheduler.entity.ScheduledJob;
import com.example.scheduler.enums.ExecutionStatus;
import com.example.scheduler.enums.JobStatus;
import com.example.scheduler.enums.ScheduleType;
import com.example.scheduler.repository.JobExecutionRepository;
import com.example.scheduler.repository.ScheduledJobRepository;
import com.example.scheduler.service.DispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DueJobProcessor {

    private final ScheduledJobRepository jobRepository;
    private final JobExecutionRepository executionRepository;
    private final DispatchService dispatchService;

    @Transactional
    public void processDueJobs() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledJob> dueJobs = jobRepository.findDueJobs(now);
        // #region agent log
        try { java.nio.file.Files.writeString(java.nio.file.Path.of("C:\\Users\\YENNI RAHUL\\OneDrive\\Desktop\\distributed-task-scheduler\\debug-1fc9a9.log"), "{\"sessionId\":\"1fc9a9\",\"hypothesisId\":\"H3\",\"location\":\"DueJobProcessor.java:processDueJobs\",\"message\":\"due jobs poll\",\"data\":{\"dueCount\":" + dueJobs.size() + "},\"timestamp\":" + System.currentTimeMillis() + "}\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Exception ignored) {}
        // #endregion

        for (ScheduledJob job : dueJobs) {
            log.info("Processing due job: {} ({})", job.getName(), job.getId());

            JobExecution execution = JobExecution.builder()
                    .job(job)
                    .status(ExecutionStatus.PENDING)
                    .attemptNumber(1)
                    .taskData(job.getTaskData())
                    .build();

            execution = executionRepository.save(execution);
            dispatchService.recordEvent(execution, "CREATED", "Execution record created");
            // #region agent log
            try { java.nio.file.Files.writeString(java.nio.file.Path.of("C:\\Users\\YENNI RAHUL\\OneDrive\\Desktop\\distributed-task-scheduler\\debug-1fc9a9.log"), "{\"sessionId\":\"1fc9a9\",\"runId\":\"pre-fix\",\"hypothesisId\":\"H3\",\"location\":\"DueJobProcessor.java:processDueJobs\",\"message\":\"due job execution created\",\"data\":{\"jobId\":\"" + job.getId() + "\",\"executionId\":\"" + execution.getId() + "\",\"taskType\":\"" + job.getTaskType() + "\"},\"timestamp\":" + System.currentTimeMillis() + "}\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Exception ignore) {}
            // #endregion

            dispatchService.dispatchJobExecution(execution, job);

            job.setLastRunAt(now);
            if (job.getScheduleType() == ScheduleType.FIXED_RATE && job.getFixedRateMs() != null) {
                job.setNextRunAt(now.plusNanos(job.getFixedRateMs() * 1_000_000));
            } else if (job.getScheduleType() == ScheduleType.ONE_TIME) {
                job.setNextRunAt(null);
                job.setStatus(JobStatus.COMPLETED);
            } else {
                job.setNextRunAt(now.plusMinutes(1)); // Default for demo CRON
            }
            jobRepository.save(job);
        }
    }
}
