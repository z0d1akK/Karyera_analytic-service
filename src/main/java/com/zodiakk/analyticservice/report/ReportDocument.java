package com.zodiakk.analyticservice.report;

import java.util.List;

public record ReportDocument(
        String title,
        String generatedAt,
        List<Section> sections
) {

    public record Section(
            String heading,
            List<String> paragraphs,
            Table table
    ) {
        public Section(String heading, List<String> paragraphs) {
            this(heading, paragraphs, null);
        }

        public Section(String heading, Table table) {
            this(heading, List.of(), table);
        }
    }

    public record Table(
            List<String> headers,
            List<List<String>> rows
    ) {
    }
}
