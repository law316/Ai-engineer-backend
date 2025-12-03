package net.ikwa.aiengineerpractice.configurations;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    // 1. Repository where messages are stored in-memory (for LLM context, NOT your DB history)
    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    // 2. ChatMemory wrapper that manages how many messages the model “remembers”
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(30)   // last 30 messages per conversationId
                .build();
    }

    // 3. ChatClient with memory + advisors + default options
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
                                 ChatMemory chatMemory) {

        ChatOptions options = ChatOptions.builder()
                .model("gpt-5.1")   // your chosen model (assuming your OpenAI setup supports it)
                .maxTokens(700)     // 100 was too small for your long answers, 400 is safer
                .temperature(0.2)   // slightly calmer / more consistent
                .build();

        return chatClientBuilder
                .defaultOptions(options)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
}
