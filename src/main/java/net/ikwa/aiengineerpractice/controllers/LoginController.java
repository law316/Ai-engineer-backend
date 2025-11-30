package net.ikwa.aiengineerpractice.controllers;

import net.ikwa.aiengineerpractice.model.SignupModel;
import net.ikwa.aiengineerpractice.service.LoginService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/authenticate")
@CrossOrigin("*") // or restrict to your Vercel origin
public class LoginController {

    private final LoginService loginService;

    // ✅ Constructor injection so loginService is never null
    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody SignupModel userLogin) {
        try {
            SignupModel loggedInUser = loginService.authenticateUser(userLogin);
            return ResponseEntity.ok(loggedInUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Login failed: " + e.getMessage());
        }
    }
}
