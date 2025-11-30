package net.ikwa.aiengineerpractice.controllers;

import net.ikwa.aiengineerpractice.advisors.TokenUsageAuditAdvisor;
import net.ikwa.aiengineerpractice.service.ChatMessageService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class PromptStuffingController {

    private final ChatClient chatClient;
    private final ChatMessageService chatMessageService;

    @Value("classpath:/promptTemplate/systemPromptTemplate.st")
    Resource systemPromptTemplate;

    public PromptStuffingController(ChatClient.Builder chatClientBuilder,
                                    ChatMessageService chatMessageService) {
        this.chatClient = chatClientBuilder.build();
        this.chatMessageService = chatMessageService;
    }

    // 🔹 Small DTO so we can receive message + username + phoneNumber
    public static class ChatRequest {
        private String message;
        private String username;
        private String phoneNumber;

        public String getMessage() {
            return message;
        }
        public void setMessage(String message) {
            this.message = message;
        }

        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }
        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }

    // ✅ 1) TEXT-ONLY CHAT (using phoneNumber as conversationId + saving username/phone)
    @PostMapping("/prompt")
    public ResponseEntity<String> userPromptSturfing(@RequestBody ChatRequest request) {
        try {
            if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
                return ResponseEntity.badRequest().body("Request message is empty");
            }

            if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
                return ResponseEntity.badRequest().body("Phone number is required");
            }

            String userMessage   = request.getMessage();
            String username      = request.getUsername();
            String phoneNumber   = request.getPhoneNumber();
            String conversationId = phoneNumber; // 🎯 group chats by phone

            // save user message – don’t let DB failure kill the AI response
            try {
                chatMessageService.saveMessage(
                        "user",
                        userMessage,
                        conversationId,
                        phoneNumber,
                        username,
                        null       // no image
                );
            } catch (Exception e) {
                e.printStackTrace();
            }

            String reply = chatClient
                    .prompt()
                    .advisors(new TokenUsageAuditAdvisor())
                    .system(systemPromptTemplate)
                    .user(userMessage)
                    .call()
                    .content();

            try {
                chatMessageService.saveMessage(
                        "ai",
                        reply,
                        conversationId,
                        phoneNumber,
                        "CheapNaira AI",
                        null        // no image
                );
            } catch (Exception e) {
                e.printStackTrace();
            }

            return ResponseEntity.ok(reply);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("something went wrong");
        }
    }

    // ✅ 2) IMAGE + TEXT EVALUATION (optionally log as history if phoneNumber provided)
    @PostMapping(
            value = "/imageupload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> describeImage(
            @RequestParam("describe") String describe,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "username", required = false) String username
    ) {
        try {
            // Determine mime type (fallback to JPEG)
            String contentType = file.getContentType();
            MimeType mimeType;
            if (contentType != null) {
                mimeType = MimeType.valueOf(contentType);
            } else {
                mimeType = MimeTypeUtils.IMAGE_JPEG;
            }

            String description = chatClient
                    .prompt()
                    .user(userSpec -> userSpec
                            .text(describe)
                            .media(mimeType, file.getResource()))
                    .call()
                    .content();

            // 📝 Optionally save this as a "receipt" message in history
            // We don't have a real URL here yet, so we log a text note for now.
            // Later, when you add Cloudinary/S3, you can replace imageUrl with real URL.
            if (phoneNumber != null && !phoneNumber.isBlank()) {
                String conversationId = phoneNumber;
                String note = "📷 Receipt/image uploaded: " + describe;

                try {
                    chatMessageService.saveMessage(
                            "user",
                            note,
                            conversationId,
                            phoneNumber,
                            username,
                            null  // imageUrl placeholder for future
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // 🔥 Return the actual description to the frontend
            return ResponseEntity.ok(description);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("error describing image");
        }
    }
}
