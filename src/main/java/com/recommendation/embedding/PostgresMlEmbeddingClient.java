package com.recommendation.embedding;

import java.util.List;

import org.springframework.ai.embedding.EmbeddingModel;
public class PostgresMlEmbeddingClient implements EmbeddingClient {

    private final EmbeddingModel embeddingModel;

    public PostgresMlEmbeddingClient(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public List<Double> embed(String text) {
        float[] vector = embeddingModel.embed(text);
        return toList(vector);
    }

    private List<Double> toList(float[] vector) {
        Double[] values = new Double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            values[i] = (double) vector[i];
        }
        return List.of(values);
    }
}
