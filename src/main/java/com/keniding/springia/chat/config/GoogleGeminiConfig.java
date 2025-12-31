package com.keniding.springia.chat.config;

import com.google.genai.Client;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

@Slf4j
@Configuration
public class GoogleGeminiConfig {

    @Value("${spring.ai.google.genai.api-key}")
    private String apiKey;

    @Value("${spring.ai.google.genai.chat.options.model:gemini-2.5-flash}")
    private String model;

    @Value("${spring.ai.google.genai.chat.options.temperature:0.7}")
    private Double temperature;

    @Value("${spring.ai.google.genai.chat.options.max-output-tokens:2048}")
    private Integer maxOutputTokens;

    @Bean
    public Client googleGenAiClient() {
        log.info("Creando Google GenAI Client");
        log.info("   API Key configurada: {}", apiKey != null && !apiKey.isEmpty() ? "Sí" : "No");

        return Client.builder()
                .apiKey(apiKey)
                .build();
    }

    @Bean
    public ChatModel chatModel(Client googleGenAiClient) {
        log.info("Inicializando Google Gemini Chat Model");
        log.info("   Modelo: {}", model);
        log.info("   Temperature: {}", temperature);
        log.info("   Max Tokens: {}", maxOutputTokens);

        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .maxOutputTokens(maxOutputTokens)
                .build();

        return GoogleGenAiChatModel.builder()
                .genAiClient(googleGenAiClient)
                .defaultOptions(options)
                .retryTemplate(RetryTemplate.builder().build())
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
    }
}
