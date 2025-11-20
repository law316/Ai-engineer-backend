package net.ikwa.aiengineerpractice;

import net.ikwa.aiengineerpractice.dto.RagModelDTO;
import net.ikwa.aiengineerpractice.model.RagModel;
import net.ikwa.aiengineerpractice.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    @Autowired
    private RagService ragService;

    // ✅ ADD PRODUCT
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

    // ✅ SEARCH PRODUCTS
    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestParam String query) {
        try {
            List<RagModel> results = ragService.search(query);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }
}
