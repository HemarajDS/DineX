package com.example.restaurant.controller;

import com.example.restaurant.dto.UploadResponse;
import com.example.restaurant.service.AccessControlService;
import com.example.restaurant.service.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final FileStorageService fileStorageService;
    private final AccessControlService accessControlService;

    public UploadController(FileStorageService fileStorageService, AccessControlService accessControlService) {
        this.fileStorageService = fileStorageService;
        this.accessControlService = accessControlService;
    }

    @PostMapping("/menu-image")
    public ResponseEntity<UploadResponse> uploadMenuImage(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        accessControlService.requireAdmin(userId, role);
        return ResponseEntity.ok(fileStorageService.storeMenuImage(file));
    }
}
