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
            return response.getAccepted();
        } catch (Exception e) {
            log.error("Failed gRPC dispatch to {}:{}", hostname, port, e);
            return false;
        } finally {
            channel.shutdown();
        }
    }
}
