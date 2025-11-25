package net.ikwa.aiengineerpractice.controllers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;



    // ChatClient is injected automatically from AiConfig
    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/chats")
    public ResponseEntity<String> userChats(@RequestBody String message) {
        try {
            // Basic safety: avoid null / empty
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Empty request body");
            }

            String cleaned = message.trim();

            String response = chatClient
                    .prompt()
                    .user(cleaned)   // send user message to Spring AI / OpenAI
                    .call()
                    .content();      // AI reply

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace(); // full stacktrace in fly logs

            // ⚠ TEMPORARY: send error details back so we can see what's wrong
            String info = "ERROR: " + e.getClass().getSimpleName();
            if (e.getMessage() != null) {
                info += " - " + e.getMessage();
            }

            return ResponseEntity.status(500).body(info);
        }
    }

}
