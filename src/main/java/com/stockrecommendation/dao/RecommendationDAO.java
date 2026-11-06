package com.stockrecommendation.dao;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.stockrecommendation.model.Recommendation;
import com.stockrecommendation.util.JdbcUtil;

/** SQL for the recommendations table. */
@Repository
public class RecommendationDAO {

    private static final String SELECT_RECOMMENDATIONS =
            "SELECT r.id, r.stock_id, r.recommendation_type, r.recommendation_score, r.explanation, r.supporting_json, "
          + "r.data_source, r.generated_at, r.data_timestamp, s.symbol, s.company_name, s.exchange "
          + "FROM recommendations r JOIN stocks s ON s.id = r.stock_id ";

    private static final RowMapper<Recommendation> MAPPER = (rs, row) -> {
        Recommendation recommendation = new Recommendation();
        recommendation.setId(rs.getLong("id"));
        recommendation.setStockId(rs.getInt("stock_id"));
        recommendation.setRecommendationType(rs.getString("recommendation_type"));
        recommendation.setRecommendationScore(rs.getBigDecimal("recommendation_score"));
        recommendation.setExplanation(rs.getString("explanation"));
        recommendation.setSupportingJson(rs.getString("supporting_json"));
        recommendation.setDataSource(rs.getString("data_source"));
        recommendation.setGeneratedAt(JdbcUtil.getDateTime(rs, "generated_at"));
        recommendation.setDataTimestamp(JdbcUtil.getDateTime(rs, "data_timestamp"));
        recommendation.setSymbol(rs.getString("symbol"));
        recommendation.setCompanyName(rs.getString("company_name"));
        recommendation.setExchange(rs.getString("exchange"));
        return recommendation;
    };

    private final JdbcTemplate jdbc;

    public RecommendationDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The newest recommendation of every active stock (ids grow with time). */
    public List<Recommendation> findLatestPerStock() {
        return jdbc.query(SELECT_RECOMMENDATIONS
                + "WHERE s.is_active = TRUE AND r.id IN (SELECT MAX(id) FROM recommendations GROUP BY stock_id) "
                + "ORDER BY r.recommendation_score DESC, s.symbol", MAPPER);
    }

    public Recommendation findLatestForStock(int stockId) {
        return jdbc.query(SELECT_RECOMMENDATIONS + "WHERE r.stock_id = ? ORDER BY r.id DESC LIMIT 1", MAPPER, stockId)
                .stream().findFirst().orElse(null);
    }

    public List<Recommendation> findHistory(int stockId, int limit) {
        return jdbc.query(SELECT_RECOMMENDATIONS + "WHERE r.stock_id = ? ORDER BY r.id DESC LIMIT ?", MAPPER, stockId, limit);
    }

    /** Every recommendation, newest first - for the admin page. */
    public List<Recommendation> findRecent(int limit) {
        return jdbc.query(SELECT_RECOMMENDATIONS + "ORDER BY r.id DESC LIMIT ?", MAPPER, limit);
    }

    public void insert(Recommendation recommendation) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO recommendations (stock_id, recommendation_type, recommendation_score, explanation, "
                  + "supporting_json, data_source, data_timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setInt(1, recommendation.getStockId());
            statement.setString(2, recommendation.getRecommendationType());
            statement.setBigDecimal(3, recommendation.getRecommendationScore());
            statement.setString(4, recommendation.getExplanation());
            statement.setString(5, recommendation.getSupportingJson());
            statement.setString(6, recommendation.getDataSource());
            statement.setTimestamp(7, JdbcUtil.timestamp(recommendation.getDataTimestamp()));
            return statement;
        }, keys);
        recommendation.setId(keys.getKey().longValue());
    }

    public boolean delete(long recommendationId) {
        return jdbc.update("DELETE FROM recommendations WHERE id = ?", recommendationId) == 1;
    }

    public int count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM recommendations", Integer.class);
    }
}
