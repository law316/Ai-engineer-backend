package net.ikwa.aiengineerpractice;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Controller
@RestController("/api")

public class PromptStuffingController {
    private final ChatClient chatClient;

    public PromptStuffingController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    @Value("classpath:/promptTemplate/systemPromptTemplate.st")
    Resource systemPromptTemplate;
    @PostMapping("/prompt")
    public ResponseEntity<String> userPromptSturfing(@RequestBody String userMessage
                                                  ) {
        try {

            String message = chatClient.
                    prompt().
                    system(systemPromptTemplate)
                    .user(userMessage)
                    .call().content();
            return ResponseEntity.ok(message);
        }
        catch (Exception e) {
            e.printStackTrace();

        }
        return ResponseEntity.status(500)
                .body("something went wrong");


    }
}
