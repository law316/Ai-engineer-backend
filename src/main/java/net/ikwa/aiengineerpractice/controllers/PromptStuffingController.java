package net.ikwa.aiengineerpractice.controllers;

import net.ikwa.aiengineerpractice.advisors.TokenUsageAuditAdvisor;
import net.ikwa.aiengineerpractice.model.ChatMessage;
import net.ikwa.aiengineerpractice.service.ChatMessageService;
import net.ikwa.aiengineerpractice.service.RateService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PromptStuffingController {

    private final ChatClient chatClient;
    private final ChatMessageService chatMessageService;
    private final RateService rateService; // ✅ NEW

    @Value("classpath:/promptTemplate/systemPromptTemplate.st")
    Resource systemPromptTemplate;

    public PromptStuffingController(ChatClient.Builder chatClientBuilder,
                                    ChatMessageService chatMessageService,
                                    RateService rateService) { // ✅ NEW ARG
        this.chatClient = chatClientBuilder.build();
        this.chatMessageService = chatMessageService;
        this.rateService = rateService; // ✅ ASSIGN
    }

    // 🔹 Small DTO so we can receive message + username + phoneNumber (JSON)
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

    // 🔍 Helper: check if management is currently the last non-user sender
    private boolean isManagementActive(String conversationId) {
        try {
            List<ChatMessage> history = chatMessageService.getMessagesForConversation(conversationId);
            if (history == null || history.isEmpty()) {
                return false;
            }

            ChatMessage lastNonUser = null;
            for (ChatMessage m : history) {
                String sender = m.getSender();
                if (sender == null) continue;
                if (!"user".equalsIgnoreCase(sender)) {
                    lastNonUser = m;
                }
            }

            if (lastNonUser == null) {
                return false;
            }

            // 🔒 lock if last non-user is management
            return "management".equalsIgnoreCase(lastNonUser.getSender());
        } catch (Exception e) {
            e.printStackTrace();
            // fail-open: if DB check fails, don't block AI
            return false;
        }
    }

    // ✅ Helper: detect questions about rates (very simple, can improve later)
    private boolean looksLikeRateQuestion(String msg) {
        if (msg == null) return false;
        String m = msg.toLowerCase();

        return m.contains("rate")
                || m.contains("rates")
                || m.contains("how much per")
                || m.contains("buy dollar")
                || m.contains("sell dollar")
                || m.contains("deriv rate")
                || m.contains("deriv rates")
                || m.contains("crypto rate")
                || m.contains("crypto rates")
                || m.contains("usd rate")
                || m.contains("today rate");
    }

    // ✅ 1) TEXT-ONLY CHAT (JSON)
    @PostMapping("/prompt")
    public ResponseEntity<String> userPromptSturfing(@RequestBody ChatRequest request) {
        try {
            if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
                return ResponseEntity.badRequest().body("Request message is empty");
            }

            if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
                return ResponseEntity.badRequest().body("Phone number is required");
            }

            String userMessage    = request.getMessage();
            String username       = request.getUsername();
            String phoneNumber    = request.getPhoneNumber();
            String conversationId = phoneNumber; // 🎯 group chats by phone for memory + DB

            // 📝 Save user message – don’t let DB failure kill the AI response
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

            // ⛔ Management lock: if last non-user message is from "management", AI must stay quiet
            boolean managementActive = isManagementActive(conversationId);
            if (managementActive) {
                String reply = "";
                try {
                    // 🔒 save as management, so lock stays active
                    chatMessageService.saveMessage(
                            "management",
                            reply,
                            conversationId,
                            phoneNumber,
                            "CheapNaira Management",
                            null
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return ResponseEntity.ok("");
            }

            // ✅ If it looks like a rate question → answer from DB, not model
            if (looksLikeRateQuestion(userMessage)) {
                String reply;
                try {
                    reply = rateService.buildRateMessage(); // 📊 pull from database
                } catch (Exception e) {
                    e.printStackTrace();
                    // fallback if rates fail: at least respond gracefully
                    reply = "I couldn't load the current rates right now. Please try again in a moment, or ask management to confirm for you.";
                }

                try {
                    chatMessageService.saveMessage(
                            "ai",
                            reply,
                            conversationId,
                            phoneNumber,
                            "CheapNaira AI",
                            null
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return ResponseEntity.ok(reply);
            }

            // 🔥 call model with:
            // - systemPromptTemplate (your big CheapNaira prompt)
            // - memory (via CONVERSATION_ID)
            // - TokenUsageAuditAdvisor for logging/usage
            String reply = chatClient
                    .prompt()
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
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

    // ✅ 1b) CHAT WITH IMAGE (multipart/form-data to /api/prompt)
    @PostMapping(
            value = "/prompt",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> userPromptWithImage(
            @RequestParam("message") String message,
            @RequestParam("file") MultipartFile file,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "username", required = false) String username
    ) {
        try {
            if (message == null || message.isBlank()) {
                return ResponseEntity.badRequest().body("Request message is empty");
            }
            if (phoneNumber == null || phoneNumber.isBlank()) {
                return ResponseEntity.badRequest().body("Phone number is required");
            }
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Image file is required");
            }

            String userMessage    = message;
            String conversationId = phoneNumber;

            // Determine mime type (fallback to JPEG)
            String contentType = file.getContentType();
            MimeType mimeType;
            if (contentType != null) {
                mimeType = MimeType.valueOf(contentType);
            } else {
                mimeType = MimeTypeUtils.IMAGE_JPEG;
            }

            // 🔥 Build base64 data URL for the *user* image
            byte[] bytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String dataUrl = "data:" + mimeType.toString() + ";base64," + base64;

            // 📝 Save user message + image in your own DB
            try {
                String combinedContent = userMessage + " ";
                chatMessageService.saveMessage(
                        "user",
                        combinedContent,
                        conversationId,
                        phoneNumber,
                        username,
                        dataUrl   // ✅ store image on user message
                );
            } catch (Exception e) {
                e.printStackTrace();
            }

            // ⛔ Management lock for image prompts too
            boolean managementActive = isManagementActive(conversationId);
            if (managementActive) {
                String reply = "";
                try {
                    // 🔒 save as management so lock stays
                    chatMessageService.saveMessage(
                            "management",
                            reply,
                            conversationId,
                            phoneNumber,
                            "CheapNaira Management",
                            null
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return ResponseEntity.ok(reply);
            }

            // 🔥 IMAGE-READING + TEXT – AI with memory + system prompt
            String reply = chatClient
                    .prompt()
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .advisors(new TokenUsageAuditAdvisor())
                    .system(systemPromptTemplate)
                    .user(userSpec -> userSpec
                            .text(userMessage)
                            .media(mimeType, file.getResource()))
                    .call()
                    .content();

            // Save AI reply (no imageUrl needed)
            try {
                chatMessageService.saveMessage(
                        "ai",
                        reply,
                        conversationId,
                        phoneNumber,
                        "CheapNaira AI",
                        null
                );
            } catch (Exception e) {
                e.printStackTrace();
            }

            return ResponseEntity.ok(reply);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("something went wrong (image prompt)");
        }
    }

    // ✅ 2) IMAGE + TEXT EVALUATION (receipt upload)
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

            String conversationId = (phoneNumber != null && !phoneNumber.isBlank())
                    ? phoneNumber
                    : "anonymous-image";

            // ⛔ If management is active on this conversation, don't let AI override them
            boolean managementActive = isManagementActive(conversationId);

            String description;
            if (managementActive) {
                description = "please wait..";
            } else {
                // 🔎 Let the model describe/check the image with your system prompt + memory (if phoneNumber present)
                description = chatClient
                        .prompt()
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .advisors(new TokenUsageAuditAdvisor())
                        .system(systemPromptTemplate)
                        .user(userSpec -> userSpec
                                .text(describe)
                                .media(mimeType, file.getResource()))
                        .call()
                        .content();
            }

            // 📝 Save as "receipt" message in history (your DB)
            if (phoneNumber != null && !phoneNumber.isBlank()) {
                byte[] bytes = file.getBytes();
                String base64 = Base64.getEncoder().encodeToString(bytes);
                String dataUrl = "data:" + mimeType.toString() + ";base64," + base64;

                String note = "📷 Receipt/check and: " + describe;

                try {
                    chatMessageService.saveMessage(
                            "user",
                            note,
                            conversationId,
                            phoneNumber,
                            username,
                            dataUrl
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    // Save the description too (label as ai when not locked, management when locked)
                    String sender = managementActive ? "management" : "ai";
                    String name   = managementActive ? "CheapNaira Management" : "CheapNaira AI";

                    chatMessageService.saveMessage(
                            sender,
                            description,
                            conversationId,
                            phoneNumber,
                            name,
                            null
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return ResponseEntity.ok(description);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("error describing image");
        }
    }



}
