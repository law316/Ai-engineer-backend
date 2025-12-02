package net.ikwa.aiengineerpractice.configurations;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;


@Configuration
public class AiConfig {

    // 1. Repository where messages are stored in-memory
    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    // 2. ChatMemory wrapper that manages history retention
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(30)
                .build();
    }

    // 3. ChatClient with memory + advisor + defaults
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
                                 ChatMemory chatMemory) {
       ChatOptions options =  ChatOptions.builder().model("gpt-5.1").
               maxTokens(100).temperature(0.8).build();

        return chatClientBuilder
                .defaultAdvisors()
                .defaultOptions(options)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultSystem("Serve as an internal assistant")
                .defaultUser("How can you help me")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .build()) /// this line is red undellined red

                .build();
    }
}
