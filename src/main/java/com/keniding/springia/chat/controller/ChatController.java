package com.keniding.springia.chat.controller;

import com.keniding.springia.chat.dto.ChatRequest;
import com.keniding.springia.chat.dto.ChatResponse;
import com.keniding.springia.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @GetMapping
    public ChatResponse simpleChat(@RequestParam String message) {
        log.info("GET /api/chat - mensaje: {}", message);
        return chatService.simpleChat(message);
    }

    @PostMapping
    public ChatResponse customChat(@RequestBody ChatRequest request) {
        log.info("POST /api/chat - request: {}", request);
        return chatService.customChat(request);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam String message) {
        log.info("GET /api/chat/stream - mensaje: {}", message);
        return chatService.streamChat(message);
    }

    /**
     * Endpoint para obtener información detallada con análisis de tokens
     * GET /api/chat/analyze?message=Tu mensaje aquí
     */
    @GetMapping("/analyze")
    public Map<String, Object> analyzeChat(@RequestParam String message) {
        log.info("GET /api/chat/analyze - mensaje: {}", message);

        ChatResponse response = chatService.simpleChat(message);

        // Calcular costo estimado (ejemplo: $0.00025 por 1K tokens para Gemini)
        double costPer1KTokens = 0.00025;
        Double estimatedCost = null;

        if (response.getTokensUsed() != null) {
            estimatedCost = (response.getTokensUsed() / 1000.0) * costPer1KTokens;
        }

        return Map.of(
                "response", response.getResponse(),
                "model", response.getModel(),
                "usage", Map.of(
                        "totalTokens", response.getTokensUsed() != null ? response.getTokensUsed() : 0,
                        "promptTokens", response.getPromptTokens() != null ? response.getPromptTokens() : 0,
                        "completionTokens", response.getCompletionTokens() != null ? response.getCompletionTokens() : 0,
                        "estimatedCost", estimatedCost != null ? String.format("$%.6f", estimatedCost) : "N/A"
                ),
                "performance", Map.of(
                        "responseTimeMs", response.getResponseTimeMs(),
                        "tokensPerSecond", response.getTokensUsed() != null && response.getResponseTimeMs() > 0
                                ? (response.getTokensUsed() * 1000.0) / response.getResponseTimeMs()
                                : 0
                )
        );
    }
}
