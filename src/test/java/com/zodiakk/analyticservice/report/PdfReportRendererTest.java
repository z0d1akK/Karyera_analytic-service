package com.zodiakk.analyticservice.report;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfReportRendererTest {

    @Test
    void embedsCyrillicInPdf() {
        ReportDocument document = new ReportDocument(
                "Профиль сотрудника",
                "13.11.2026 12:00",
                List.of(new ReportDocument.Section(
                        "Сотрудник",
                        List.of("ФИО: Иван Петров"),
                        new ReportDocument.Table(
                                List.of("Навык", "Уровень"),
                                List.of(List.of("Java", "3"))
                        )
                ))
        );

        byte[] pdf = new PdfReportRenderer().render(document);
        String raw = new String(pdf, StandardCharsets.ISO_8859_1);

        assertThat(pdf.length).isGreaterThan(100);
        assertThat(raw).startsWith("%PDF");
        assertThat(raw).contains("Identity-H");
        assertThat(raw).doesNotContain("/BaseFont /Helvetica");
        assertThat(pdf).contains("Иван".getBytes(StandardCharsets.UTF_16BE));
    }
}
