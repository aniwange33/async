package com.amos.asynapi.web;


import com.amos.asynapi.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("")
public class ReportController {

	private final SimpMessageSendingOperations messagingTemplate;
	private final ReportService reportService;
	private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    public ReportController(SimpMessageSendingOperations messagingTemplate, ReportService reportService) {
        this.messagingTemplate = messagingTemplate;
        this.reportService = reportService;
    }


	@GetMapping("/radet")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public CompletableFuture<ResponseEntity<String>> getRadet(
			@RequestParam("facilityId") Long facility,
			@RequestParam("startDate") LocalDate start,
			@RequestParam("endDate") LocalDate end) {
		messagingTemplate.convertAndSend("/topic/progress", "Starting radet report");
		messagingTemplate.convertAndSend("/topic/radet", "start");

		CompletableFuture<String> complete = reportService.generateMtRecords(1000);

		return complete.thenApply(result -> {
			messagingTemplate.convertAndSend("/topic/progress", "Radet report completed");
			messagingTemplate.convertAndSend("/topic/radet", "end");
			return ResponseEntity.ok(result);
		}).exceptionally(ex -> {
			messagingTemplate.convertAndSend("/topic/progress", "Radet report failed: " + ex.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Report generation failed.");
		});
	}


}
