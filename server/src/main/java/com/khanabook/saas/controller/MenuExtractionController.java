package com.khanabook.saas.controller;

import com.khanabook.saas.entity.MenuExtractionJob;
import com.khanabook.saas.repository.MenuExtractionJobRepository;
import com.khanabook.saas.service.MenuExtractionWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/menus")
public class MenuExtractionController {

    @Autowired
    private MenuExtractionJobRepository jobRepository;

    @Autowired
    private MenuExtractionWorker menuExtractionWorker;

    // A directory to temporarily store uploaded files offline on the server
    private final String UPLOAD_DIR = "offline-storage/menus/";

    @PostMapping("/upload")
    public ResponseEntity<?> uploadMenu(@RequestParam("file") MultipartFile file,
                                        @RequestParam("restaurantId") Long restaurantId) {
        try {
            // 1. Create directory if not exists
            Path uploadPath = Paths.get(UPLOAD_DIR + "shop_" + restaurantId);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 2. Save file to disk
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            // 3. Create job in DB
            MenuExtractionJob job = new MenuExtractionJob();
            job.setRestaurantId(restaurantId);
            job.setFilePath(filePath.toAbsolutePath().toString());
            job = jobRepository.save(job);

            // 4. Trigger Async Processing (non-blocking)
            menuExtractionWorker.processMenuJob(job.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Menu uploaded successfully. Processing in background.");
            response.put("jobId", job.getId());
            response.put("status", job.getStatus());

            // 5. Return immediately
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<?> getJobStatus(@PathVariable Long jobId) {
        return jobRepository.findById(jobId)
                .map(job -> ResponseEntity.ok((Object) job))
                .orElse(ResponseEntity.notFound().build());
    }
}
