package net.ikwa.aiengineerpractice.controllers;

import net.ikwa.aiengineerpractice.model.ChatMessage;
import net.ikwa.aiengineerpractice.service.ChatMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminChatController {

    private final ChatMessageService chatMessageService;

    public AdminChatController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> listConversations() {
        try {
            List<Map<String, Object>> conversations =
                    chatMessageService.getRecentConversationSummaries(50);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("Error loading conversations: " + e.getMessage());
        }
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<?> getConversation(@PathVariable String conversationId) {
        try {
            List<ChatMessage> messages =
                    chatMessageService.getMessagesForConversation(conversationId);

            List<Map<String, Object>> dto = new ArrayList<>();
            for (ChatMessage m : messages) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", m.getId());
                map.put("sender", m.getSender());
                map.put("content", m.getContent());
                map.put("createdAt", m.getCreatedAt());

                // keep old key for safety
                map.put("phonenumber", m.getPhoneNumber());
                // add proper camelCase key your frontend expects
                map.put("phoneNumber", m.getPhoneNumber());

                map.put("username", m.getUsername());
                map.put("imageUrl", m.getImageUrl());  // ✅ include image URL

                dto.add(map);
            }

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("Error loading conversation " + conversationId + ": " + e.getMessage());
        }
    }

    public static class AdminReplyRequest {
        private String message;
        private String phoneNumber;   // conversationId in your design
        private String username;      // optional: admin name

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    @PostMapping("/conversations/{conversationId}/reply")
    public ResponseEntity<?> replyToConversation(
            @PathVariable String conversationId,
            @RequestBody AdminReplyRequest req
    ) {
        try {
            if (req.getMessage() == null || req.getMessage().isBlank()) {
                return ResponseEntity.badRequest().body("Message is required");
            }

            String phoneNumber = req.getPhoneNumber() != null
                    ? req.getPhoneNumber()
                    : conversationId; // since you use phoneNumber as conversationId

            String adminName = (req.getUsername() != null && !req.getUsername().isBlank())
                    ? req.getUsername()
                    : "CheapNaira Management";

            chatMessageService.saveMessage(
                    "management",              // 🔥 admin sender
                    req.getMessage(),
                    conversationId,
                    phoneNumber,
                    adminName,
                    null                      // imageUrl placeholder
            );

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("Error sending admin reply: " + e.getMessage());
        }
    }

    // ✅ mark conversation as read (no-op for now)
    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable String conversationId) {
        return ResponseEntity.ok().build();
    }

    // ✅ mark conversation as unread (no-op)
    @PostMapping("/conversations/{conversationId}/unread")
    public ResponseEntity<?> markAsUnread(@PathVariable String conversationId) {
        return ResponseEntity.ok().build();
    }

    // ✅ mark conversation as resolved (no-op)
    @PostMapping("/conversations/{conversationId}/resolve")
    public ResponseEntity<?> resolve(@PathVariable String conversationId) {
        return ResponseEntity.ok().build();
    }

    // ✅ return conversation back to AI → ACTUALLY UNLOCK
    @PostMapping("/conversations/{conversationId}/return-to-ai")
    public ResponseEntity<?> returnToAI(@PathVariable String conversationId) {
        try {
            // In your design, conversationId == phoneNumber
            String phoneNumber = conversationId;

            // Save a small control message from AI so the last non-user sender is NOT management anymore
            chatMessageService.saveMessage(
                    "ai",
                    "CheapNaira AI is now handling this chat again. You can continue normally.",
                    conversationId,
                    phoneNumber,
                    "CheapNaira AI",
                    null
            );

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("Error returning conversation to AI: " + e.getMessage());
        }
    }
}
