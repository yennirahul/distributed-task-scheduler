package com.example.scheduler.grpc;

import com.example.scheduler.entity.JobExecution;
import com.example.scheduler.entity.ScheduledJob;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class GrpcDispatchClient {

    public boolean dispatchTask(String hostname, int port, JobExecution execution, ScheduledJob job) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(hostname, port)
                .usePlaintext()
                .build();

        try {
            WorkerGrpcServiceGrpc.WorkerGrpcServiceBlockingStub stub = WorkerGrpcServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(job.getTimeoutSeconds() != null ? job.getTimeoutSeconds() : 60, TimeUnit.SECONDS);

            TaskDispatchParam param = TaskDispatchParam.newBuilder()
                    .setExecutionId(execution.getId())
                    .setJobId(job.getId())
                    .setTaskType(job.getTaskType())
                    .setTaskData(job.getTaskData() != null ? job.getTaskData() : "")
                    .setTimeoutSeconds(job.getTimeoutSeconds() != null ? job.getTimeoutSeconds() : 60)
                    .setAttemptNumber(execution.getAttemptNumber())
                    .build();

            TaskDispatchResponse response = stub.dispatchTask(param);
            log.info("gRPC Task Dispatch response for {}: accepted={}", execution.getId(), response.getAccepted());
            // #region agent log
            try { java.nio.file.Files.writeString(java.nio.file.Path.of("C:\\Users\\YENNI RAHUL\\OneDrive\\Desktop\\distributed-task-scheduler\\debug-1fc9a9.log"), "{\"sessionId\":\"1fc9a9\",\"hypothesisId\":\"H5\",\"location\":\"GrpcDispatchClient.java:dispatchTask\",\"message\":\"dispatch rpc returned\",\"data\":{\"hostname\":\"" + hostname + "\",\"port\":" + port + ",\"accepted\":" + response.getAccepted() + ",\"status\":\"" + response.getStatus() + "\"},\"timestamp\":" + System.currentTimeMillis() + "}\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Exception ignored) {}
            // #endregion
            return response.getAccepted();
        } catch (Exception e) {
            log.error("Failed gRPC dispatch to {}:{}", hostname, port, e);
            // #region agent log
            try { java.nio.file.Files.writeString(java.nio.file.Path.of("C:\\Users\\YENNI RAHUL\\OneDrive\\Desktop\\distributed-task-scheduler\\debug-1fc9a9.log"), "{\"sessionId\":\"1fc9a9\",\"hypothesisId\":\"H1\",\"location\":\"GrpcDispatchClient.java:dispatchTask\",\"message\":\"dispatch rpc failed\",\"data\":{\"hostname\":\"" + hostname + "\",\"port\":" + port + ",\"errorType\":\"" + e.getClass().getSimpleName() + "\",\"error\":\"" + String.valueOf(e.getMessage()).replace("\"", "'") + "\"},\"timestamp\":" + System.currentTimeMillis() + "}\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Exception ignored) {}
            // #endregion
            return false;
        } finally {
            channel.shutdown();
        }
    }
}
