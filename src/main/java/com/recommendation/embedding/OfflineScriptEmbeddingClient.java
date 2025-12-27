package com.recommendation.embedding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OfflineScriptEmbeddingClient implements EmbeddingClient {

    private static final TypeReference<List<Double>> VECTOR_TYPE = new TypeReference<>() {};

    private final EmbeddingJobProperties properties;
    private final ObjectMapper objectMapper;

    public OfflineScriptEmbeddingClient(EmbeddingJobProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Double> embed(String text) {
        String scriptPath = properties.offlineScriptPath();
        if (scriptPath == null || scriptPath.isBlank()) {
            throw new IllegalStateException("embedding.job.offline-script-path must be configured for OFFLINE mode.");
        }
        Duration timeout = properties.offlineTimeout();
        Process process;
        try {
            process = new ProcessBuilder(scriptPath).start();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start offline embedding script: " + scriptPath, e);
        }
        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            process.destroyForcibly();
            throw new IllegalStateException("Failed to write input to offline embedding script.", e);
        }
        boolean finished;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IllegalStateException("Offline embedding script interrupted.", e);
        }
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("Offline embedding script timed out after " + timeout.toMillis() + "ms.");
        }
        if (process.exitValue() != 0) {
            String error = readOutput(process.getErrorStream());
            throw new IllegalStateException("Offline embedding script failed: " + error);
        }
        String output = readOutput(process.getInputStream());
        try {
            return objectMapper.readValue(output, VECTOR_TYPE);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse offline embedding output: " + output, e);
        }
    }

    private String readOutput(InputStream inputStream) {
        try {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read offline embedding output.", e);
        }
    }
}
