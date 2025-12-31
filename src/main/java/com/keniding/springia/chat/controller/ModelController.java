package com.keniding.springia.chat.controller;

import com.keniding.springia.chat.service.ModelDiscoveryService;
import com.keniding.springia.chat.service.ModelDiscoveryService.ModelInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelDiscoveryService modelDiscoveryService;

    /**
     * Lista todos los modelos disponibles
     * GET /api/models
     */
    @GetMapping
    public ResponseEntity<List<ModelInfo>> listAllModels() {
        log.info("GET /api/models - Listando todos los modelos");
        try {
            List<ModelInfo> models = modelDiscoveryService.listAvailableModels();
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            log.error("Error al listar modelos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lista solo los modelos que soportan chat (generateContent)
     * GET /api/models/chat
     */
    @GetMapping("/chat")
    public ResponseEntity<List<ModelInfo>> listChatModels() {
        log.info("GET /api/models/chat - Listando modelos de chat");
        try {
            List<ModelInfo> chatModels = modelDiscoveryService.listChatModels();
            return ResponseEntity.ok(chatModels);
        } catch (Exception e) {
            log.error("Error al listar modelos de chat: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene información de un modelo específico
     * GET /api/models/{modelName}
     */
    @GetMapping("/{modelName}")
    public ResponseEntity<ModelInfo> getModelInfo(@PathVariable String modelName) {
        log.info("GET /api/models/{} - Obteniendo información del modelo", modelName);
        try {
            String fullModelName = modelName.startsWith("models/")
                    ? modelName
                    : "models/" + modelName;

            ModelInfo modelInfo = modelDiscoveryService.getModelInfo(fullModelName);
            return ResponseEntity.ok(modelInfo);
        } catch (ModelDiscoveryService.ModelDiscoveryException _) {
            log.error("Modelo no encontrado: {}", modelName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error al obtener información del modelo: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Verifica si un modelo está disponible
     * GET /api/models/check/{modelName}
     */
    @GetMapping("/check/{modelName}")
    public ResponseEntity<Map<String, Object>> checkModelAvailability(@PathVariable String modelName) {
        log.info("GET /api/models/check/{} - Verificando disponibilidad", modelName);
        try {
            boolean available = modelDiscoveryService.isModelAvailable(modelName);

            Map<String, Object> response = Map.of(
                    "modelName", modelName,
                    "available", available,
                    "message", available
                            ? "Modelo disponible"
                            : "Modelo no disponible o no encontrado"
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al verificar disponibilidad del modelo: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "modelName", modelName,
                            "available", false,
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * Obtiene estadísticas de los modelos disponibles
     * GET /api/models/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getModelsStats() {
        log.info("GET /api/models/stats - Obteniendo estadísticas de modelos");
        try {
            List<ModelInfo> allModels = modelDiscoveryService.listAvailableModels();
            List<ModelInfo> chatModels = modelDiscoveryService.listChatModels();

            Map<String, Object> stats = Map.of(
                    "totalModels", allModels.size(),
                    "chatModels", chatModels.size(),
                    "otherModels", allModels.size() - chatModels.size(),
                    "models", allModels.stream()
                            .map(ModelInfo::name)
                            .toList()
            );

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error al obtener estadísticas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Manejo global de excepciones para este controlador
     */
    @ExceptionHandler(ModelDiscoveryService.ModelDiscoveryException.class)
    public ResponseEntity<Map<String, String>> handleModelDiscoveryException(
            ModelDiscoveryService.ModelDiscoveryException e) {
        log.error("Error de descubrimiento de modelos: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Model Discovery Error",
                        "message", e.getMessage()
                ));
    }
}
