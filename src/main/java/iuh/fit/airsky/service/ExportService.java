package iuh.fit.airsky.service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface ExportService {

    void exportToCsv(HttpServletResponse response, List<?> data, String entityName, List<String> fields) throws IOException;

    void exportToExcel(HttpServletResponse response, List<?> data, String entityName, List<String> fields) throws IOException;

    void exportToPdf(HttpServletResponse response, List<?> data, String entityName, List<String> fields) throws IOException;
}
