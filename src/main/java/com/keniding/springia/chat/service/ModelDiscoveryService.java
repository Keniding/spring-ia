package com.keniding.springia.chat.service;

import com.google.genai.Client;
import com.google.genai.Pager;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.ListModelsConfig;
import com.google.genai.types.Model;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelDiscoveryService {

    private final Client googleGenAiClient;

    public List<ModelInfo> listAvailableModels() {
        try {
            log.info("Listando modelos disponibles...");

            ListModelsConfig config = ListModelsConfig.builder()
                    .pageSize(100)
                    .build();

            Pager<Model> pager = googleGenAiClient.models.list(config);

            List<ModelInfo> modelInfoList = new ArrayList<>();

            for (Model model : pager) {
                String name = model.name().orElse("unknown");
                String displayName = model.displayName().orElse("N/A");
                String description = model.description().orElse("N/A");

                List<String> supportedMethods = model.supportedActions()
                        .orElse(List.of());

                Integer inputTokenLimit = model.inputTokenLimit().orElse(0);
                Integer outputTokenLimit = model.outputTokenLimit().orElse(0);

                log.info("Modelo encontrado: {} - {}", name, displayName);

                ModelInfo modelInfo = new ModelInfo(
                        name,
                        displayName,
                        description,
                        supportedMethods,
                        inputTokenLimit,
                        outputTokenLimit
                );

                modelInfoList.add(modelInfo);
            }

            return modelInfoList;

        } catch (GenAiIOException e) {
            log.error("Error de Google GenAI al listar modelos: {}", e.getMessage(), e);
            throw new ModelDiscoveryException("Error al obtener modelos de Google GenAI", e);
        } catch (Exception e) {
            log.error("Error inesperado al listar modelos: {}", e.getMessage(), e);
            throw new ModelDiscoveryException("Error inesperado al obtener modelos disponibles", e);
        }
    }

    public List<ModelInfo> listChatModels() {
        return listAvailableModels().stream()
                .filter(model -> model.supportedMethods().contains("generateContent"))
                .toList();
    }

    public boolean isModelAvailable(String modelName) {
        try {
            List<ModelInfo> models = listAvailableModels();
            return models.stream()
                    .anyMatch(model -> model.name().contains(modelName));
        } catch (ModelDiscoveryException e) {
            log.error("Error verificando modelo: {}", e.getMessage(), e);
            return false;
        }
    }

    public ModelInfo getModelInfo(String modelName) {
        try {
            Model model = googleGenAiClient.models.get(modelName, null);

            return new ModelInfo(
                    model.name().orElse("unknown"),
                    model.displayName().orElse("N/A"),
                    model.description().orElse("N/A"),
                    model.supportedActions().orElse(List.of()),
                    model.inputTokenLimit().orElse(0),
                    model.outputTokenLimit().orElse(0)
            );
        } catch (GenAiIOException e) {
            log.error("Error de Google GenAI obteniendo modelo {}: {}", modelName, e.getMessage(), e);
            throw new ModelDiscoveryException("Modelo no encontrado: " + modelName, e);
        } catch (Exception e) {
            log.error("Error inesperado obteniendo modelo {}: {}", modelName, e.getMessage(), e);
            throw new ModelDiscoveryException("Error al obtener información del modelo: " + modelName, e);
        }
    }

    public record ModelInfo(
            String name,
            String displayName,
            String description,
            List<String> supportedMethods,
            Integer inputTokenLimit,
            Integer outputTokenLimit
    ) {}

    public static class ModelDiscoveryException extends RuntimeException {
        public ModelDiscoveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
