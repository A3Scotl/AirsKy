/*
 * @ (#) CloudinaryService.java 1.0 8/12/2025
 *
 * Copyright (c) 2025 IUH.All rights reserved
 */

package iuh.fit.airsky.service;

/*
 * @description
 * @author : Nguyen Truong An
 * @date : 8/12/2025
 * @version 1.0
 */
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@Service
public class CloudinaryService {
    @Autowired
    private Cloudinary cloudinary;

    public String uploadFile(MultipartFile file) {
        try {
            // Upload file vào thư mục "booking"
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "airsky",
                    "resource_type", "image"
            ));
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to Cloudinary", e);
        }
    }

    public String uploadPdfFile(byte[] pdfBytes, String fileName) {
        try {
            Map uploadResult = cloudinary.uploader().upload(pdfBytes, ObjectUtils.asMap(
                    "folder", "airsky/boarding-passes",
                    "resource_type", "raw",
                    "public_id", fileName.replace(".pdf", ""),
                    "format", "pdf"
            ));
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload PDF to Cloudinary", e);
        }
    }

    public String uploadPdfFile(File pdfFile, String fileName) {
        try {
            byte[] fileBytes = Files.readAllBytes(pdfFile.toPath());
            return uploadPdfFile(fileBytes, fileName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read PDF file", e);
        }
    }
}
