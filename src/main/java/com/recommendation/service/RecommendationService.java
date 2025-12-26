package com.recommendation.service;

import com.recommendation.dto.RecommendationDto;
import com.recommendation.dto.SearchResultDto;
import com.recommendation.repository.RecommendationRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {
    private final RecommendationRepository recommendationRepository;

    public RecommendationService(RecommendationRepository recommendationRepository) {
        this.recommendationRepository = recommendationRepository;
    }

    public List<SearchResultDto> search(String query, int limit) {
        return recommendationRepository.search(query, limit);
    }

    public List<RecommendationDto> recommendForPage(long itemId, int limit) {
        return recommendationRepository.recommendForPage(itemId, limit);
    }

    public List<RecommendationDto> recommendForUser(long userId, int limit) {
        return recommendationRepository.recommendForUser(userId, limit);
    }
}
