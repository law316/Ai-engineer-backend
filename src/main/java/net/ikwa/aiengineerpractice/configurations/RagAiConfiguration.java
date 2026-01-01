package net.ikwa.aiengineerpractice.configurations;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagAiConfiguration {

    // ✅ 1. Separate memory repository for RAG conversations
    @Bean
    @Qualifier("ragMemoryRepository")
    public ChatMemoryRepository ragMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    // ✅ 2. Separate ChatMemory for RAG with longer history
    @Bean
    @Qualifier("ragChatMemory")
    public ChatMemory ragChatMemory(@Qualifier("ragMemoryRepository") ChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(50)  // More messages for medical context
                .build();
    }

    // ✅ 3. RAG-specific ChatClient with enhanced options
    @Bean
    @Qualifier("ragChatClient")
    public ChatClient ragChatClient(
            ChatClient.Builder chatClientBuilder,
            @Qualifier("ragChatMemory") ChatMemory ragChatMemory) {

        OpenAiChatOptions ragOptions = OpenAiChatOptions.builder()
                .model("gpt-4o")      // ✅ Using gpt-4o (supports vision + chat)
                .maxTokens(2000)      // More tokens for detailed responses
                .temperature(0.7)     // Natural conversation
                .topP(0.9)
                .build();

        return chatClientBuilder
                .defaultOptions(ragOptions)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(ragChatMemory).build()
                )
                .build();
    }

    // ✅ 4. Vision-enabled ChatClient for image analysis
    @Bean
    @Qualifier("ragVisionChatClient")
    public ChatClient ragVisionChatClient(ChatClient.Builder chatClientBuilder) {

        OpenAiChatOptions visionOptions = OpenAiChatOptions.builder()
                .model("gpt-4o")      // ✅ gpt-4o handles both text and images
                .maxTokens(1500)
                .temperature(0.5)
                .build();

        return chatClientBuilder
                .defaultOptions(visionOptions)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
