package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/v1/boarding-passes")
@RequiredArgsConstructor
@Slf4j
public class BoardingPassController {

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadBoardingPass(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get("boarding-passes", fileName);

            if (!Files.exists(filePath)) {
                log.warn("Boarding pass file not found: {}", fileName);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath);

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);

        } catch (Exception e) {
            log.error("Error downloading boarding pass {}: {}", fileName, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}