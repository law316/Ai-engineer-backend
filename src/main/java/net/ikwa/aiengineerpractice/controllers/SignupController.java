package net.ikwa.aiengineerpractice.controllers;

import net.ikwa.aiengineerpractice.model.SignupModel;
import net.ikwa.aiengineerpractice.service.SignupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/signup")
public class SignupController {

    private final SignupService signupService;

    // ✅ Constructor injection (safer than field @Autowired)
    public SignupController(SignupService signupService) {
        this.signupService = signupService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> userSignup(@RequestBody SignupModel signupModel) {
        try {
            signupService.signupUser(signupModel); // your service returns void
            return ResponseEntity.ok("success");
        } catch (IllegalArgumentException e) {
            // from your validation: "all details must be filled"
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error registering user: " + e.getMessage());
        }
    }
}
