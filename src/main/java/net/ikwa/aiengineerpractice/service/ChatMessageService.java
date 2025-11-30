package net.ikwa.aiengineerpractice.service;

import net.ikwa.aiengineerpractice.model.ChatMessage;
import net.ikwa.aiengineerpractice.repo.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatMessageService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    /**
     * Existing simple save method used by your PromptStuffingController.
     * Here we assume:
     * - conversationId is the user's phone number (as we changed in the controller)
     * - username is not provided yet (null)
     * - no image (null)
     */
    public ChatMessage saveMessage(String sender, String content, String conversationId) {
        if (sender == null || sender.isBlank()
                || content == null || content.isBlank()
                || conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("sender, content and conversationId are required");
        }

        // conversationId is acting as phoneNumber
        String phoneNumber = conversationId;
        String username = null;
        String imageUrl = null;

        ChatMessage message = new ChatMessage(
                sender,
                content,
                imageUrl,
                conversationId,
                phoneNumber,
                username
        );
        message.setCreatedAt(LocalDateTime.now());
        return chatMessageRepository.save(message);
    }

    /**
     * Overloaded save method for when you want to explicitly pass
     * username / phoneNumber / imageUrl (e.g. for receipts later).
     */
    public ChatMessage saveMessage(
            String sender,
            String content,
            String conversationId,
            String phoneNumber,
            String username,
            String imageUrl
    ) {
        if (sender == null || sender.isBlank()
                || (content == null || content.isBlank()) && (imageUrl == null || imageUrl.isBlank())
                || conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("sender, (content or image), and conversationId are required");
        }

        ChatMessage message = new ChatMessage(
                sender,
                content,
                imageUrl,
                conversationId,
                phoneNumber,
                username
        );
        message.setCreatedAt(LocalDateTime.now());
        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getMessagesForConversation(String conversationId) {
        return chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    /**
     * Latest conversation summaries (one per conversationId/phoneNumber).
     */
    public List<Map<String, Object>> getRecentConversationSummaries(int limit) {
        List<ChatMessage> latestMessages =
                chatMessageRepository.findTop200ByOrderByCreatedAtDesc();

        // group by conversationId → keep newest
        Map<String, ChatMessage> latestByConversation = new HashMap<>();

        for (ChatMessage msg : latestMessages) {
            String convId = msg.getConversationId();
            ChatMessage existing = latestByConversation.get(convId);

            if (existing == null ||
                    msg.getCreatedAt().isAfter(existing.getCreatedAt())) {
                latestByConversation.put(convId, msg);
            }
        }

        // sort newest → oldest
        List<ChatMessage> summaries = new ArrayList<>(latestByConversation.values());
        summaries.sort(Comparator.comparing(ChatMessage::getCreatedAt).reversed());

        if (summaries.size() > limit) {
            summaries = summaries.subList(0, limit);
        }

        List<Map<String, Object>> dtoList = new ArrayList<>();
        for (ChatMessage m : summaries) {
            Map<String, Object> map = new HashMap<>();
            // id = conversationId (which is phoneNumber now)
            map.put("id", m.getConversationId());
            map.put("lastSender", m.getSender());
            map.put("lastMessage", m.getContent());
            map.put("updatedAt", m.getCreatedAt());
            map.put("type", "ai"); // for now all are AI chats

            // extra info for your dashboard if you want
            map.put("phoneNumber", m.getPhoneNumber());
            map.put("username", m.getUsername());

            dtoList.add(map);
        }

        return dtoList;
    }
}
