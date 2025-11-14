package net.ikwa.aiengineerpractice;

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
            String response = chatClient
                    .prompt()
                    .user(message)        // user input
                    .call()
                    .content();           // AI output

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("Something went wrong");
        }
    }
}
