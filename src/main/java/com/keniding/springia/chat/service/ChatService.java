package com.keniding.springia.chat.service;

import com.keniding.springia.chat.dto.ChatRequest;
import com.keniding.springia.chat.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private static final String ERROR_PREFIX = "Error: ";
    private static final String ERROR_UNKNOWN = "Error desconocido";

    private final ChatModel chatModel;

    @Value("${spring.ai.google.genai.chat.options.model:gemini-2.5-flash}")
    private String modelName;

    public ChatResponse simpleChat(String message) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Llamando a Gemini con mensaje: {}", message);
            Prompt prompt = new Prompt(message);
            org.springframework.ai.chat.model.ChatResponse aiResponse = chatModel.call(prompt);

            long endTime = System.currentTimeMillis();

            Integer totalTokens = null;
            Integer promptTokens = null;
            Integer completionTokens = null;

            if (aiResponse.getMetadata().getUsage() != null) {
                Usage usage = aiResponse.getMetadata().getUsage();
                totalTokens = usage.getTotalTokens();
                promptTokens = usage.getPromptTokens();
                completionTokens = usage.getCompletionTokens();

                log.info("Tokens usados - Prompt: {}, Completion: {}, Total: {}",
                        promptTokens, completionTokens, totalTokens);
            }

            String responseText = aiResponse.getResult().getOutput().getText();

            log.info("Respuesta recibida en {} ms", endTime - startTime);

            return ChatResponse.builder()
                    .response(responseText)
                    .model(modelName)
                    .tokensUsed(totalTokens)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .responseTimeMs(endTime - startTime)
                    .build();

        } catch (Exception e) {
            log.error("Error en simpleChat: ", e);

            String errorMessage = getString(e);

            return ChatResponse.builder()
                    .response(errorMessage)
                    .model(modelName)
                    .tokensUsed(0)
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private static @NonNull String getString(Exception e) {
        String errorMessage = "Error al comunicarse con Gemini: ";
        if (e.getMessage() != null) {
            if (e.getMessage().contains("429")) {
                errorMessage += "Límite de cuota excedido. Por favor, espera unos minutos.";
            } else if (e.getMessage().contains("404")) {
                errorMessage += "Modelo no encontrado. Verifica la configuración.";
            } else {
                errorMessage += e.getMessage();
            }
        }
        return errorMessage;
    }

    public ChatResponse customChat(ChatRequest chatRequest) {
        long startTime = System.currentTimeMillis();

        try {
            var options = GoogleGenAiChatOptions.builder()
                    .temperature(chatRequest.getTemperature() != null ?
                            chatRequest.getTemperature() : 0.7)
                    .maxOutputTokens(chatRequest.getMaxTokens() != null ?
                            chatRequest.getMaxTokens() : 2048)
                    .build();

            var prompt = new Prompt(chatRequest.getMessage(), options);
            var aiResponse = chatModel.call(prompt);

            long endTime = System.currentTimeMillis();

            Integer totalTokens = null;
            Integer promptTokens = null;
            Integer completionTokens = null;

            if (aiResponse.getMetadata().getUsage() != null) {
                Usage usage = aiResponse.getMetadata().getUsage();
                totalTokens = usage.getTotalTokens();
                promptTokens = usage.getPromptTokens();
                completionTokens = usage.getCompletionTokens();
            }

            return ChatResponse.builder()
                    .response(aiResponse.getResult().getOutput().getText())
                    .model(modelName)
                    .tokensUsed(totalTokens)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .responseTimeMs(endTime - startTime)
                    .build();

        } catch (Exception e) {
            log.error("Error en customChat: ", e);

            return ChatResponse.builder()
                    .response(ERROR_PREFIX + (e.getMessage() != null ? e.getMessage() : ERROR_UNKNOWN))
                    .model(modelName)
                    .tokensUsed(0)
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    public Flux<String> streamChat(String message) {
        try {
            var prompt = new Prompt(message);
            return chatModel.stream(prompt)
                    .mapNotNull(response -> response.getResult().getOutput().getText())
                    .onErrorResume(e -> {
                        log.error("Error en streamChat: ", e);
                        return Flux.just(ERROR_PREFIX + (e.getMessage() != null ? e.getMessage() : ERROR_UNKNOWN));
                    });
        } catch (Exception e) {
            log.error("Error al iniciar streamChat: ", e);
            return Flux.just(ERROR_PREFIX + (e.getMessage() != null ? e.getMessage() : ERROR_UNKNOWN));
        }
    }
}
