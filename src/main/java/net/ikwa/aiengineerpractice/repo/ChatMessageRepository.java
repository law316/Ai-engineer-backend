package net.ikwa.aiengineerpractice.repo;

import net.ikwa.aiengineerpractice.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;


@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // all messages in a conversation, oldest → newest
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    // latest N messages overall (we’ll group by conversationId in service)
    List<ChatMessage> findTop200ByOrderByCreatedAtDesc();

    // all messages for a given phone number, oldest → newest
    List<ChatMessage> findByPhoneNumberOrderByCreatedAtAsc(String phoneNumber);

    @Query("""
SELECT m FROM ChatMessage m
WHERE m.conversationId = :cid
ORDER BY m.createdAt DESC
""")
    List<ChatMessage> findLatestMessages(
            @Param("cid") String conversationId,
            Pageable pageable
    );
    @Query("""
SELECT m FROM ChatMessage m
WHERE m.conversationId = :cid
ORDER BY m.createdAt DESC
""")
    ChatMessage findLatestMessageForConversation(
            @Param("cid") String conversationId
    );


}
