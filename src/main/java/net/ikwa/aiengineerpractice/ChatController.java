package net.ikwa.aiengineerpractice;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api")
public class ChatController {
    private final ChatClient chatClient;


    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.defaultSystem(" " +
                "you must provide answers only about derive you" +
                "must serve only as a customer service based on derive" +
                "issues of trading hours, rates and trading instruments only" +
                "anything aside it kindly inform the user that " +
                "you can only assist with derive messages")

                .build();
    }
    @PostMapping("/chats")
    public ResponseEntity<String> userChats (@RequestBody String message) {
        try {
            String response = chatClient.prompt().system("address concerns only on banking")
                    .user(message)
                    .call().content();
            return ResponseEntity.ok(response);
        }
        catch (Exception e) {
            e.printStackTrace();

        }return ResponseEntity.status(500)
                .body("something went wrong");


    }
}