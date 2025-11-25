package net.ikwa.aiengineerpractice.controllers;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api")
public class UserEmailPrompt {

    private final ChatClient chatClient;

    public UserEmailPrompt(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    @Value("classpath:/promptTemplate/userEmail.st")
    Resource promptTemplate;
     @PostMapping("/email")
     public ResponseEntity<String> userEmailPrompt(@RequestBody String email,
                                                   String customermessage) {
         try {

             String message = chatClient.
                     prompt().
                     system(
                             "you are a professional customer service assistant which helps drafting" +
                                     "reponses to improve the productivity of the customer service team"

                     )
                     .user(promptUserSpec ->
                             promptUserSpec.text(promptTemplate))
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
