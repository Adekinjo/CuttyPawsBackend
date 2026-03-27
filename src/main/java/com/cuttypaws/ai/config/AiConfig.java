package com.cuttypaws.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CuttyPawsAiProperties.class)
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    public AiModelCatalog aiModelCatalog() {
        return new AiModelCatalog(
                "gpt-4.1-mini",
                "gpt-4.1",
                "text-embedding-3-small"
        );
    }

    public record AiModelCatalog(
            String defaultChatModel,
            String advancedReasoningModel,
            String embeddingModel
    ) {}
}