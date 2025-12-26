package net.ikwa.aiengineerpractice.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // adjust to your frontend origin for production (e.g. "https://cheapnaira-connect-2keu.vercel.app")
public class UploadController {

    // Max file size is controlled by Spring Boot properties (see below)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> handleFileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "username", required = false) String username
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded"));
        }

        try {
            // ensure uploads directory exists (relative to working dir)
            Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            // create a safe filename
            String original = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "file";
            String safe = System.currentTimeMillis() + "-" + original.replaceAll("[^a-zA-Z0-9._\\-]", "_");

            Path target = uploadDir.resolve(safe);

            // copy file stream to target location
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            // Build a public URL for the uploaded file. WebConfig (below) maps /uploads/** to this folder.
            String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(safe)
                    .toUriString();

            Map<String, Object> resp = Map.of(
                    "url", fileUrl,
                    "fileName", original,
                    "mimeType", file.getContentType(),
                    "size", file.getSize(),
                    "path", "/uploads/" + safe
            );

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Upload failed"));
        }
    }
}
