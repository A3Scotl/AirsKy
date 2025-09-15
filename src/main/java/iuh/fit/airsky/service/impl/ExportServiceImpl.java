package iuh.fit.airsky.service.impl;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.resolver.font.DefaultFontProvider;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.layout.font.FontProvider;
import com.opencsv.CSVWriter;
import iuh.fit.airsky.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExportServiceImpl implements ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportServiceImpl.class);
    private static final String FONT_PATH = "fonts/NotoSerif-Regular.ttf";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Danh sách các trường kỹ thuật sẽ bị loại bỏ khi export
    private static final List<String> TECHNICAL_FIELDS = List.of("createdAt", "updatedAt", "deletedAt", "isActive", "delete", "deleted");

    private List<Field> getExportableFields(Object dto) {
        return getExportableFields(dto, null);
    }

private List<Field> getExportableFields(Object dto, List<String> fields) {
    Field[] allFields = dto.getClass().getDeclaredFields();
    List<Field> filteredFields = java.util.Arrays.stream(allFields)
            .filter(f -> !TECHNICAL_FIELDS.contains(f.getName()))
            .collect(Collectors.toList());

    if (fields != null && !fields.isEmpty()) {
        List<String> validFields = filteredFields.stream().map(Field::getName).collect(Collectors.toList());
        List<String> invalidFields = fields.stream()
                .filter(f -> !validFields.contains(f))
                .collect(Collectors.toList());
        if (!invalidFields.isEmpty()) {
            logger.warn("Invalid fields ignored for entity {}: {}", dto.getClass().getSimpleName(), invalidFields);
        }
        filteredFields = filteredFields.stream()
                .filter(f -> fields.contains(f.getName()))
                .collect(Collectors.toList());
    }
    return filteredFields.isEmpty() ? Collections.emptyList() : filteredFields;
}

    private String getExportValue(Object value) {
        if (value == null) return "";
        if (value instanceof String) return (String) value;
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof java.time.LocalDateTime) return ((java.time.LocalDateTime) value).format(DATE_FORMATTER);
        if (value instanceof java.util.Collection) {
            Collection<?> col = (Collection<?>) value;
            if (!col.isEmpty()) {
                Object first = col.iterator().next();
                if (first instanceof String) {
                    return String.join(", ", ((Collection<?>) value).stream()
                            .map(Object::toString)
                            .toArray(String[]::new));
                }
                if (first.getClass().getSimpleName().equals("GateResponse")) {
                    return col.stream()
                            .map(g -> {
                                try {
                                    java.lang.reflect.Field f = g.getClass().getDeclaredField("gateName");
                                    f.setAccessible(true);
                                    Object v = f.get(g);
                                    return v != null ? v.toString() : "";
                                } catch (Exception e) { return ""; }
                            })
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.joining(", "));
                }
                if (first.getClass().getSimpleName().equals("CategoryResponse")) {
                    return col.stream()
                            .map(c -> {
                                try {
                                    java.lang.reflect.Field f = c.getClass().getDeclaredField("name");
                                    f.setAccessible(true);
                                    Object v = f.get(c);
                                    return v != null ? v.toString() : "";
                                } catch (Exception e) { return ""; }
                            })
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.joining(", "));
                }
                return col.stream().map(this::getExportValue).collect(Collectors.joining(", "));
            }
            return "";
        }
        if (value.getClass().getSimpleName().equals("GateResponse")) {
            try {
                java.lang.reflect.Field f = value.getClass().getDeclaredField("gateName");
                f.setAccessible(true);
                Object v = f.get(value);
                return v != null ? v.toString() : "";
            } catch (Exception e) { return ""; }
        }
        if (value.getClass().getSimpleName().equals("CategoryResponse")) {
            try {
                java.lang.reflect.Field f = value.getClass().getDeclaredField("name");
                f.setAccessible(true);
                Object v = f.get(value);
                return v != null ? v.toString() : "";
            } catch (Exception e) { return ""; }
        }
        if (value.getClass().getSimpleName().endsWith("Response")) {
            return value.toString();
        }
        return "";
    }

    @Override
    public void exportToCsv(HttpServletResponse response, List<?> data, String entityName, List<String> fields) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + entityName + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv\"");
        response.setCharacterEncoding("UTF-8");

        try (PrintWriter writer = response.getWriter()) {
            writer.write("\uFEFF");
            CSVWriter csvWriter = new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);

            if (!data.isEmpty()) {
                Object firstItem = data.get(0);
                List<Field> exportFields = getExportableFields(firstItem, fields);
                String[] headers = exportFields.stream().map(Field::getName).toArray(String[]::new);
                csvWriter.writeNext(headers);

                for (Object item : data) {
                    String[] row = new String[exportFields.size()];
                    for (int i = 0; i < exportFields.size(); i++) {
                        try {
                            Field field = exportFields.get(i);
                            field.setAccessible(true);
                            row[i] = getExportValue(field.get(item));
                        } catch (IllegalAccessException e) {
                            logger.error("Error accessing field {} for entity {}", exportFields.get(i).getName(), entityName, e);
                            row[i] = "";
                        }
                    }
                    csvWriter.writeNext(row);
                }
            }
            csvWriter.close();
        } catch (IOException e) {
            logger.error("Error exporting CSV for entity {}", entityName, e);
            throw e;
        }
    }

    @Override
    public void exportToExcel(HttpServletResponse response, List<?> data, String entityName, List<String> fields) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + entityName + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx\"");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(entityName);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.MEDIUM);
            headerStyle.setBorderBottom(BorderStyle.MEDIUM);
            headerStyle.setBorderLeft(BorderStyle.MEDIUM);
            headerStyle.setBorderRight(BorderStyle.MEDIUM);

            CellStyle dataStyle = workbook.createCellStyle();
            Font dataFont = workbook.createFont();
            dataFont.setFontHeightInPoints((short) 10);
            dataStyle.setFont(dataFont);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
            dataStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle alternateDataStyle = workbook.createCellStyle();
            alternateDataStyle.cloneStyleFrom(dataStyle);
            alternateDataStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            alternateDataStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            if (!data.isEmpty()) {
                Object firstItem = data.get(0);
                List<Field> exportFields = getExportableFields(firstItem, fields);
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < exportFields.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(exportFields.get(i).getName());
                    cell.setCellStyle(headerStyle);
                }
                sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, data.size(), 0, exportFields.size() - 1));

                int rowNum = 1;
                for (Object item : data) {
                    Row row = sheet.createRow(rowNum);
                    CellStyle rowStyle = (rowNum % 2 == 0) ? alternateDataStyle : dataStyle;
                    for (int i = 0; i < exportFields.size(); i++) {
                        try {
                            Field field = exportFields.get(i);
                            field.setAccessible(true);
                            Object value = field.get(item);
                            Cell cell = row.createCell(i);
                            cell.setCellStyle(rowStyle);
                            cell.setCellValue(getExportValue(value));
                            if ("thumbnail".equals(field.getName())) {
                                sheet.setColumnWidth(i, 3000); // Giới hạn width thumbnail
                            }
                        } catch (IllegalAccessException e) {
                            logger.error("Error accessing field {} for entity {}", exportFields.get(i).getName(), entityName, e);
                            row.createCell(i).setCellStyle(rowStyle);
                        }
                    }
                    rowNum++;
                }
                for (int i = 0; i < exportFields.size(); i++) {
                    sheet.autoSizeColumn(i);
                    int width = sheet.getColumnWidth(i);
                    if (width > 15000) sheet.setColumnWidth(i, 15000);
                    else if (width < 2000) sheet.setColumnWidth(i, 2000);
                }
            }

            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            logger.error("Error exporting Excel for entity {}", entityName, e);
            throw e;
        }
    }

    @Override
    public void exportToPdf(HttpServletResponse response, List<?> data, String entityName, List<String> fields) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + entityName + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf\"");

        ClassPathResource resource = new ClassPathResource("pdf-templates/report_template.html");
        String template = new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));

        StringBuilder headerBuilder = new StringBuilder();
        StringBuilder bodyBuilder = new StringBuilder();
        Map<String, Integer> columnMaxLengths = null;
        if (!data.isEmpty()) {
            Object firstItem = data.get(0);
            List<Field> fieldsToExport = getExportableFields(firstItem, fields);

            // Tính độ dài tối đa của mỗi cột
            columnMaxLengths = fieldsToExport.stream().collect(Collectors.toMap(
                    Field::getName,
                    field -> {
                        int maxLength = field.getName().length();
                        for (Object item : data) {
                            try {
                                field.setAccessible(true);
                                String value = getExportValue(field.get(item));
                                maxLength = Math.max(maxLength, value.length());
                            } catch (Exception e) {
                                logger.error("Error accessing field {} for entity {}", field.getName(), entityName, e);
                            }
                        }
                        return maxLength;
                    }
            ));

            // Tính tổng độ dài và phân bổ tỷ lệ
            int totalMaxLength = columnMaxLengths.values().stream().mapToInt(Integer::intValue).sum();
            for (Field field : fieldsToExport) {
                String fieldName = field.getName();
                int maxLength = columnMaxLengths.get(fieldName);
                String widthClass = "col-auto"; // Mặc định
                if ("thumbnail".equals(fieldName)) {
                    widthClass = "col-thumbnail";
                } else {
                    double percentage = (double) maxLength / totalMaxLength * 100;
                    if (percentage < 10) widthClass = "col-narrow";
                    else if (percentage < 20) widthClass = "col-medium";
                    else widthClass = "col-wide";
                }
                headerBuilder.append("<th class=\"").append(widthClass).append("\">").append(fieldName).append("</th>");
            }

            for (Object item : data) {
                bodyBuilder.append("<tr>");
                for (Field field : fieldsToExport) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(item);
                        String displayValue = getExportValue(value);
                        String widthClass = "thumbnail".equals(field.getName()) ? "col-thumbnail" : "col-auto";
                        bodyBuilder.append("<td class=\"").append(widthClass).append("\">").append(displayValue).append("</td>");
                    } catch (Exception e) {
                        bodyBuilder.append("<td></td>");
                    }
                }
                bodyBuilder.append("</tr>");
            }
        }

        String html = template
                .replace("{{TITLE}}", "BÁO CÁO DANH SÁCH " + entityName.toUpperCase())
                .replace("{{EXPORT_DATE}}", DATE_FORMATTER.format(LocalDateTime.now()))
                .replace("{{RECORD_COUNT}}", String.valueOf(data.size()))
                .replace("{{DESCRIPTION}}", "Dữ liệu xuất từ hệ thống AirSky. Vui lòng kiểm tra kỹ thông tin trước khi sử dụng.")
                .replace("{{TABLE_HEADER}}", headerBuilder.toString())
                .replace("{{TABLE_BODY}}", bodyBuilder.toString())
                .replace("{{COMPANY_NAME}}", "AirSky Airline")
                .replace("{{REPORT_GENERATED_BY}}", "Hệ Thống Quản Lý Vé Máy Bay")
                .replace("{{CONTACT_INFO}}", "Email: support@airsky.com | Hotline: 1800-123-456")
                .replace("{{COPYRIGHT}}", "© 2025 AirSky. All rights reserved.");

        ConverterProperties converterProperties = new ConverterProperties();
        FontProvider fontProvider = new DefaultFontProvider(false, true, false);
        fontProvider.addFont(FONT_PATH, PdfEncodings.IDENTITY_H);
        converterProperties.setFontProvider(fontProvider);

        HtmlConverter.convertToPdf(html, response.getOutputStream(), converterProperties);
    }
}

