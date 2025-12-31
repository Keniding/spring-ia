package com.keniding.springia.chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {
    private String response;
    private String model;
    private Integer tokensUsed;
    private Integer promptTokens;
    private Integer completionTokens;
    private Long responseTimeMs;
    private String finishReason;
}
