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
                map.put("phonenumber",m.getPhoneNumber());
                map.put("username", m.getUsername());
                dto.add(map);
            }

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("Error loading conversation " + conversationId + ": " + e.getMessage());
        }
    }
}
