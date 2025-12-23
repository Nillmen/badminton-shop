package com.badmintonshop.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling file uploads
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.review-images:review-images}")
    private String reviewImagesDir;

    /**
     * Store review images and return their URLs
     */
    public List<String> storeReviewImages(List<MultipartFile> files) throws IOException {
        List<String> imageUrls = new ArrayList<>();
        
        if (files == null || files.isEmpty()) {
            return imageUrls;
        }

        // Create directory if not exists
        Path uploadPath = Paths.get(uploadDir, reviewImagesDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            
            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("Invalid file type: {}", contentType);
                continue;
            }

            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                log.warn("File too large: {} bytes", file.getSize());
                continue;
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Return URL path (relative for serving)
            String imageUrl = "/" + uploadDir + "/" + reviewImagesDir + "/" + newFilename;
            imageUrls.add(imageUrl);
            
            log.info("Saved review image: {}", imageUrl);
        }

        return imageUrls;
    }

    /**
     * Delete a file by its URL
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return;
        
        try {
            // Remove leading slash and convert to path
            String relativePath = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
            Path filePath = Paths.get(relativePath);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted file: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Error deleting file: {}", fileUrl, e);
        }
    }

    /**
     * Store exchange/warranty request images and return JSON array string
     */
    public String storeRequestImages(List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            return "[]";
        }

        // Create directory if not exists
        Path uploadPath = Paths.get(uploadDir, "request-images");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            
            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("Invalid file type: {}", contentType);
                continue;
            }

            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                log.warn("File too large: {} bytes", file.getSize());
                continue;
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Return URL path (relative for serving)
            String imageUrl = "/" + uploadDir + "/request-images/" + newFilename;
            imageUrls.add(imageUrl);
            
            log.info("Saved request image: {}", imageUrl);
        }

        // Convert to JSON array string
        if (imageUrls.isEmpty()) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < imageUrls.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(imageUrls.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
}
