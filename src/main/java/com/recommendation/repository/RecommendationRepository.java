package com.recommendation.repository;

import com.recommendation.dto.RecommendationDto;
import com.recommendation.dto.SearchResultDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class RecommendationRepository {
    private static final String SEARCH_SQL = """
            select i.id, i.title
            from items i
            where i.title ilike ?
            order by i.title
            limit ?
            """;

    private static final String RECOMMEND_PAGE_SQL = """
            select i.id, i.title, r.score
            from item_recommendations r
            join items i on i.id = r.recommended_item_id
            where r.item_id = ?
            order by r.score desc
            limit ?
            """;

    private static final String RECOMMEND_USER_SQL = """
            select i.id, i.title, r.score
            from user_recommendations r
            join items i on i.id = r.item_id
            where r.user_id = ?
            order by r.score desc
            limit ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public RecommendationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SearchResultDto> search(String query, int limit) {
        String keyword = "%%%s%%".formatted(query);
        return jdbcTemplate.query(SEARCH_SQL, searchRowMapper(), keyword, limit);
    }

    public List<RecommendationDto> recommendForPage(long itemId, int limit) {
        return jdbcTemplate.query(RECOMMEND_PAGE_SQL, recommendationRowMapper(), itemId, limit);
    }

    public List<RecommendationDto> recommendForUser(long userId, int limit) {
        return jdbcTemplate.query(RECOMMEND_USER_SQL, recommendationRowMapper(), userId, limit);
    }

    private RowMapper<SearchResultDto> searchRowMapper() {
        return (rs, rowNum) -> new SearchResultDto(rs.getLong("id"), rs.getString("title"));
    }

    private RowMapper<RecommendationDto> recommendationRowMapper() {
        return new RecommendationRowMapper();
    }

    private static class RecommendationRowMapper implements RowMapper<RecommendationDto> {
        @Override
        public RecommendationDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RecommendationDto(rs.getLong("id"), rs.getString("title"), rs.getDouble("score"));
        }
    }
}
