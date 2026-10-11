package com.example.worker.heartbeat;

import com.example.worker.client.CoordinatorGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;

@Service
@RequiredArgsConstructor
@Slf4j
public class HeartbeatService implements CommandLineRunner {

    private final CoordinatorGrpcClient coordinatorClient;

    @Override
    public void run(String... args) throws Exception {
        log.info("Registering worker node with coordinator...");
        // #region agent log
        try { java.nio.file.Files.writeString(java.nio.file.Path.of("C:\\Users\\YENNI RAHUL\\OneDrive\\Desktop\\distributed-task-scheduler\\debug-1fc9a9.log"), "{\"sessionId\":\"1fc9a9\",\"hypothesisId\":\"H1\",\"location\":\"HeartbeatService.java:run\",\"message\":\"worker started; checking dispatch server presence\",\"data\":{\"hasDispatchGrpcServer\":false},\"timestamp\":" + System.currentTimeMillis() + "}\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Exception ignored) {}
        // #endregion
        coordinatorClient.registerWorker();
    }

    @Scheduled(fixedRate = 5000)
    public void sendHeartbeat() {
        double cpu = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        long freeMem = Runtime.getRuntime().freeMemory();
        long totalMem = Runtime.getRuntime().totalMemory();
        double usedMemoryMB = (totalMem - freeMem) / (1024.0 * 1024.0);

        coordinatorClient.sendHeartbeat(cpu > 0 ? cpu : 0.0, usedMemoryMB, 0);
    }
}
