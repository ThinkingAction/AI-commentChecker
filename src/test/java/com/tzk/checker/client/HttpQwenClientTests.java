package com.tzk.checker.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.tzk.checker.config.QwenProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpQwenClientTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> receivedBody = new AtomicReference<>();
    private HttpServer server;
    private QwenProperties properties;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();

        properties = new QwenProperties();
        properties.setApiKey("test-key");
        properties.setEndpoint("http://localhost:" + server.getAddress().getPort() + "/chat");
        properties.setModel("qwen-test");
        properties.setTemperature(0.2);
        properties.setMaxTokens(1024);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldSendExpectedRequestAndReturnContent() throws Exception {
        server.createContext("/chat", exchange -> {
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            assertEquals("Bearer test-key", exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"审核结果\"}}]}");
        });

        String result = createClient().chat("系统提示", "用户评论");

        assertEquals("审核结果", result);
        JsonNode request = objectMapper.readTree(receivedBody.get());
        assertEquals("qwen-test", request.path("model").asText());
        assertEquals(0.2, request.path("temperature").asDouble());
        assertEquals(1024, request.path("max_tokens").asInt());
        assertEquals("system", request.path("messages").get(0).path("role").asText());
        assertEquals("用户评论", request.path("messages").get(1).path("content").asText());
    }

    @Test
    void shouldRejectNon2xxResponse() {
        server.createContext("/chat", exchange -> respond(exchange, 401, "invalid api key"));

        QwenClientException exception = assertThrows(
                QwenClientException.class,
                () -> createClient().chat("系统提示", "用户评论"));

        assertTrue(exception.getMessage().contains("401"));
    }

    @Test
    void shouldRejectEmptyChoices() {
        server.createContext("/chat", exchange -> respond(exchange, 200, "{\"choices\":[]}"));

        QwenClientException exception = assertThrows(
                QwenClientException.class,
                () -> createClient().chat("系统提示", "用户评论"));

        assertTrue(exception.getMessage().contains("choices"));
    }

    @Test
    void shouldRejectBlankResponseBody() {
        server.createContext("/chat", exchange -> respond(exchange, 200, ""));

        QwenClientException exception = assertThrows(
                QwenClientException.class,
                () -> createClient().chat("系统提示", "用户评论"));

        assertTrue(exception.getMessage().contains("响应体为空"));
    }

    private HttpQwenClient createClient() {
        return new HttpQwenClient(properties, objectMapper, HttpClient.newHttpClient());
    }

    private static void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
