// src/main/java/net/ikwa/aiengineerpractice/controllers/RateController.java
package net.ikwa.aiengineerpractice.controllers;

import net.ikwa.aiengineerpractice.model.RateModel;
import net.ikwa.aiengineerpractice.service.RateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rates")
@CrossOrigin(origins = "*")
public class RateController {

    private final RateService rateService;

    public RateController(RateService rateService) {
        this.rateService = rateService;
    }

    @GetMapping("/current")
    public ResponseEntity<RateModel> getCurrentRates() {
        return ResponseEntity.ok(rateService.getCurrentRates());
    }

    // ⚠️ In real life protect this with auth (admin only)
    @PostMapping("/admin/update")
    public ResponseEntity<RateModel> updateRates(@RequestBody RateModel model) {
        return ResponseEntity.ok(rateService.saveRates(model));
    }

    // optional: for dashboard history view (does NOT break existing logic)
    @GetMapping("/history")
    public ResponseEntity<List<RateModel>> getHistory() {
        return ResponseEntity.ok(rateService.getRateHistory());
    }
}
