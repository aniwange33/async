package com.amos.asynapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Service
public class ReportService {

    private final SimpMessageSendingOperations messagingTemplate;

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);


    public ReportService(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Async
    public CompletableFuture<String>  generateRecords(int numberOfRecords) {
        messagingTemplate.convertAndSend("/topic/progress", "total:" + numberOfRecords);
        for (int i = 0; i < numberOfRecords; i++) {
            try {
                processRecord(numberOfRecords, i);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Error occurred while generating records");
            }
        }
        return CompletableFuture.completedFuture("Records generation completed");
    }

    @Async
    public CompletableFuture<String> generateMtRecords(int numberOfRecords) {
        messagingTemplate.convertAndSend("/topic/progress", "total:" + numberOfRecords);
        // Create a fixed thread pool
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        logger.info("Available processors: {}", availableProcessors);
        ExecutorService executorService = Executors.newFixedThreadPool(availableProcessors);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfRecords; i++) {
            final int currentIndex = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    processRecord(numberOfRecords, currentIndex);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Error occurred while generating records");
                }
            }, executorService);

            futures.add(future);
        }

        // Wait for all tasks to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        return allFutures.thenApply(v -> {
            executorService.shutdown();
            return "Records generation completed";
        });
    }

    private void processRecord(int numberOfRecords, int currentIndex) throws InterruptedException {
        Thread.sleep(1000); // Simulate time taken to generate a record
        logger.info("current Thread: => : {}  ", Thread.currentThread().getName());
        int currentValue = currentIndex + 1;
        logger.info("Generated record {}/{}", currentValue, numberOfRecords);
        messagingTemplate.convertAndSend("/topic/progress", "currentValue:" + currentValue);
    }


}
