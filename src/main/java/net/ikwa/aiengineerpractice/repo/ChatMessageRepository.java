package net.ikwa.aiengineerpractice.repo;

import net.ikwa.aiengineerpractice.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // all messages in a conversation, oldest → newest
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    // latest N messages overall (we’ll group by conversationId in service)
    List<ChatMessage> findTop200ByOrderByCreatedAtDesc();

    // all messages for a given phone number, oldest → newest
    List<ChatMessage> findByPhoneNumberOrderByCreatedAtAsc(String phoneNumber);

}
