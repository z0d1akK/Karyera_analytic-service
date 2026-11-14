package com.zodiakk.analyticservice.report;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public List<ReportDescriptor> list() {
        return reportService.available();
    }

    @GetMapping("/{type}/preview")
    public ReportDocument preview(
            @PathVariable AnalyticsReportType type,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) UUID assessmentId,
            @RequestParam(required = false) UUID reserveId,
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @RequestParam(required = false) Integer zoneOffsetMinutes
    ) {
        return reportService.preview(type, employeeId, assessmentId, reserveId, jobId, periodFrom, periodTo, zoneOffsetMinutes);
    }

    @GetMapping("/{type}")
    public ResponseEntity<byte[]> pdf(
            @PathVariable AnalyticsReportType type,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) UUID assessmentId,
            @RequestParam(required = false) UUID reserveId,
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @RequestParam(required = false) Integer zoneOffsetMinutes
    ) {
        byte[] pdf = reportService.pdf(type, employeeId, assessmentId, reserveId, jobId, periodFrom, periodTo, zoneOffsetMinutes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + type.name().toLowerCase() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
