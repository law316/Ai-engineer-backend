package net.ikwa.aiengineerpractice.controllers;

import net.ikwa.aiengineerpractice.dto.RagModelDTO;
import net.ikwa.aiengineerpractice.dto.RagSearchResponse;
import net.ikwa.aiengineerpractice.model.RagModel;
import net.ikwa.aiengineerpractice.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    @Autowired
    private RagService ragService;

    @PostMapping("/add")
    public ResponseEntity<?> ragAdd(@RequestBody RagModelDTO ragModelDTO) {
        try {
            ragService.createProduct(ragModelDTO);
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    // ✅ NEW: upload document endpoint
    @PostMapping("/upload-document")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            // Let the service handle text extraction + embedding + DB save
            var docResponse = ragService.ingestDocument(file);

            // You can define a proper DTO instead of Map if you prefer
            return ResponseEntity.ok(docResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestParam("query") String query) {
        try {
            List<RagModel> results = ragService.search(query);
            String aiResponse = ragService.makeNaturalResponse(query, results);

            RagSearchResponse response = new RagSearchResponse(results, aiResponse);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getRagStats() {
        try {
            long trainingDocuments = ragService.countTrainingDocuments();

            Map<String, Object> stats = new HashMap<>();
            stats.put("trainingDocuments", trainingDocuments);

            // you can later add more backend-side stats here if needed
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }

    }
    @GetMapping("/recent-uploads")
    public ResponseEntity<?> getRecentUploads() {
        try {
            var docs = ragService.getRecentTrainingDocs();

            var list = docs.stream().map(doc -> {
                Map<String, Object> m = new HashMap<>();
                m.put("file", doc.getProductName());
                // we don't have real timestamp in DB yet, so mark as "now"
                m.put("time", Instant.now().toString());
                m.put("status", "completed");
                return m;
            }).toList();

            return ResponseEntity.ok(list);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

}
