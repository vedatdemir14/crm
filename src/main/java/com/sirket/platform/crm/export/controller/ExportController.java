package com.sirket.platform.crm.export.controller;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.crm.export.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * FR-CRM-11. The response is streamed, so the export is not built up in memory before the first
 * byte reaches the client.
 */
@RestController
@RequestMapping("/api/crm")
@Tag(name = "CRM — Dışa Aktarma")
@PreAuthorize("hasRole('ADMIN')")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Belirtilen tarih aralığındaki kişi veya fırsat verilerini CSV olarak dışa aktarır")
    public ResponseEntity<StreamingResponseBody> export(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam String entity,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String delimiter) {

        if (from.isAfter(to)) {
            throw new ApiExceptions.BadRequest("Başlangıç tarihi bitiş tarihinden sonra olamaz");
        }
        if (!"csv".equalsIgnoreCase(format)) {
            // Excel opens the CSV directly; a native .xlsx writer would mean pulling in Apache POI.
            throw new ApiExceptions.BadRequest("Desteklenen format: csv");
        }

        char separator = resolveDelimiter(delimiter);
        String dataset = entity.toLowerCase();
        StreamingResponseBody body = switch (dataset) {
            case "contacts" -> out -> exportService.writeContacts(out, from, to, separator);
            case "opportunities" -> out -> exportService.writeOpportunities(out, from, to, separator);
            default -> throw new ApiExceptions.BadRequest(
                    "Geçersiz entity değeri: " + entity + " (contacts veya opportunities)");
        };

        String filename = "%s-%s_%s.csv".formatted(dataset, from, to);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }

    /**
     * Turkish Excel treats the semicolon as its list separator and drops a comma-delimited file
     * into a single column, so the default matches the users this export is for. A caller feeding
     * the file to a programmatic consumer can ask for the RFC 4180 comma instead.
     */
    private char resolveDelimiter(String delimiter) {
        if (delimiter == null || delimiter.isBlank()) {
            return ';';
        }
        if (delimiter.length() != 1) {
            throw new ApiExceptions.BadRequest("Ayırıcı tek karakter olmalıdır");
        }
        return delimiter.charAt(0);
    }
}
