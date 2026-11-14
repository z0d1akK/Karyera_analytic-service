package com.zodiakk.analyticservice.report;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

@Component
public class PdfReportRenderer {

    private static final String FONT_RESOURCE = "/fonts/LiberationSans-Regular.ttf";

    public byte[] render(ReportDocument document) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(output);
            PdfDocument pdf = new PdfDocument(writer);
            Document layout = new Document(pdf, PageSize.A4);
            PdfFont font = loadFont();
            layout.setFont(font);

            layout.add(new Paragraph(safe(document.title())).setFontSize(16));
            if (document.generatedAt() != null && !document.generatedAt().isBlank()) {
                layout.add(new Paragraph(document.generatedAt()).setFontSize(10));
            }

            if (document.sections() != null) {
                for (ReportDocument.Section section : document.sections()) {
                    if (section.heading() != null && !section.heading().isBlank()) {
                        layout.add(new Paragraph(section.heading()).setFontSize(13).setMarginTop(12));
                    }
                    if (section.paragraphs() != null) {
                        for (String paragraph : section.paragraphs()) {
                            layout.add(new Paragraph(safe(paragraph)).setFontSize(11));
                        }
                    }
                    if (section.table() != null) {
                        layout.add(table(section.table(), font));
                    }
                }
            }

            layout.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Не удалось сформировать PDF", ex);
        }
    }

    private Table table(ReportDocument.Table source, PdfFont font) {
        List<String> headers = source.headers() == null || source.headers().isEmpty()
                ? List.of(" ")
                : source.headers();
        Table table = new Table(UnitValue.createPercentArray(headers.size())).useAllAvailableWidth();
        for (String header : headers) {
            table.addHeaderCell(cell(header, font, true));
        }
        if (source.rows() != null) {
            for (List<String> row : source.rows()) {
                for (int index = 0; index < headers.size(); index++) {
                    String value = row != null && index < row.size() ? row.get(index) : "—";
                    table.addCell(cell(value, font, false));
                }
            }
        }
        return table;
    }

    private Cell cell(String value, PdfFont font, boolean header) {
        Paragraph paragraph = new Paragraph(safe(value)).setFont(font).setFontSize(header ? 10 : 9);
        if (header) {
            paragraph.setBold();
        }
        return new Cell().add(paragraph);
    }

    private PdfFont loadFont() {
        try (InputStream input = getClass().getResourceAsStream(FONT_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Не найден шрифт отчёта");
            }
            return PdfFontFactory.createFont(input.readAllBytes(), PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
