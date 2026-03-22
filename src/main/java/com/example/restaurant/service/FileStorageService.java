package com.example.restaurant.service;

import com.example.restaurant.dto.UploadResponse;
import com.example.restaurant.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public UploadResponse storeMenuImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select an image");
        }

        String originalName = file.getOriginalFilename() == null ? "menu-item" : file.getOriginalFilename();
        String extension = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException("Only JPG, JPEG, PNG, or WEBP images are allowed");
        }

        try {
            Path menuDir = uploadRoot.resolve("menu");
            Files.createDirectories(menuDir);

            String fileName = UUID.randomUUID() + extension;
            Path targetPath = menuDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return new UploadResponse("/uploads/menu/" + fileName, fileName);
        } catch (IOException exception) {
            throw new BadRequestException("Could not store image: " + exception.getMessage());
        }
    }

    private String extractExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot == -1 ? "" : fileName.substring(lastDot);
    }
}
