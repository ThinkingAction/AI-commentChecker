package com.tzk.checker.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tzk.checker.config.QwenProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class HttpQwenClient implements QwenClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final QwenProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public HttpQwenClient(QwenProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    HttpQwenClient(QwenProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public String chat(String systemMessage, String userMessage) {
        validateConfiguration();

        QwenChatRequest requestBody = new QwenChatRequest(
                properties.getModel(),
                List.of(new Message("system", systemMessage), new Message("user", userMessage)),
                properties.getTemperature(),
                properties.getMaxTokens()
        );

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getEndpoint()))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            return parseResponse(response);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new QwenClientException("Qwen 请求被中断", exception);
        } catch (QwenClientException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new QwenClientException("Qwen 调用失败", exception);
        }
    }

    private String parseResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        String responseBody = response.body();
        if (statusCode < 200 || statusCode >= 300) {
            log.error("Qwen 调用失败，HTTP 状态码：{}，响应内容：{}", statusCode, responseBody);
            throw new QwenClientException("Qwen 调用失败，HTTP 状态码：" + statusCode);
        }
        if (responseBody == null || responseBody.isBlank()) {
            throw new QwenClientException("Qwen 返回的响应体为空");
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new QwenClientException("Qwen 响应中 choices 为空");
            }
            JsonNode message = choices.get(0).path("message");
            if (message.isMissingNode() || message.isNull()) {
                throw new QwenClientException("Qwen 响应中 message 为空");
            }
            JsonNode content = message.path("content");
            if (!content.isTextual() || content.asText().isBlank()) {
                throw new QwenClientException("Qwen 响应中 content 为空");
            }
            return content.asText();
        } catch (QwenClientException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new QwenClientException("Qwen 响应 JSON 解析失败", exception);
        }
    }

    private void validateConfiguration() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new QwenClientException("未配置 Qwen API Key");
        }
        if (properties.getEndpoint() == null || properties.getEndpoint().isBlank()) {
            throw new QwenClientException("未配置 Qwen endpoint");
        }
        if (properties.getModel() == null || properties.getModel().isBlank()) {
            throw new QwenClientException("未配置 Qwen model");
        }
    }

    private record Message(String role, String content) {
    }

    private record QwenChatRequest(
            String model,
            List<Message> messages,
            double temperature,
            @JsonProperty("max_tokens") int maxTokens) {
    }
}
