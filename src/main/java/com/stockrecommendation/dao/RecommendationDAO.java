package com.stockrecommendation.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.stockrecommendation.model.Recommendation;
import com.stockrecommendation.util.DBConnection;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.JdbcUtil;

/** SQL for the recommendations table. */
public class RecommendationDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String SELECT_RECOMMENDATIONS =
            "SELECT r.id, r.stock_id, r.recommendation_type, r.recommendation_score, r.explanation, r.supporting_json, "
          + "r.data_source, r.generated_at, r.data_timestamp, s.symbol, s.company_name, s.exchange "
          + "FROM recommendations r JOIN stocks s ON s.id = r.stock_id ";

    /**
     * The newest recommendation of every active stock.
     * The inner query finds the highest id per stock (ids grow with time).
     */
    public List<Recommendation> findLatestPerStock() {
        String sql = SELECT_RECOMMENDATIONS
                   + "WHERE s.is_active = TRUE AND r.id IN (SELECT MAX(id) FROM recommendations GROUP BY stock_id) "
                   + "ORDER BY r.recommendation_score DESC, s.symbol";
        return query(sql);
    }

    public Recommendation findLatestForStock(int stockId) {
        List<Recommendation> list = query(SELECT_RECOMMENDATIONS + "WHERE r.stock_id = ? ORDER BY r.id DESC LIMIT 1", stockId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Recommendation> findHistory(int stockId, int limit) {
        return query(SELECT_RECOMMENDATIONS + "WHERE r.stock_id = ? ORDER BY r.id DESC LIMIT ?", stockId, limit);
    }

    /** Every recommendation, newest first - for the admin page. */
    public List<Recommendation> findRecent(int limit) {
        return query(SELECT_RECOMMENDATIONS + "ORDER BY r.id DESC LIMIT ?", limit);
    }

    public void insert(Recommendation recommendation) {
        String sql = "INSERT INTO recommendations (stock_id, recommendation_type, recommendation_score, explanation, "
                   + "supporting_json, data_source, data_timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, recommendation.getStockId());
            statement.setString(2, recommendation.getRecommendationType());
            statement.setBigDecimal(3, recommendation.getRecommendationScore());
            statement.setString(4, recommendation.getExplanation());
            statement.setString(5, recommendation.getSupportingJson());
            statement.setString(6, recommendation.getDataSource());
            JdbcUtil.setDateTime(statement, 7, recommendation.getDataTimestamp());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    recommendation.setId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the recommendation.", e);
        }
    }

    public boolean delete(long recommendationId) {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM recommendations WHERE id = ?")) {
            statement.setLong(1, recommendationId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete the recommendation.", e);
        }
    }

    public int count() {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM recommendations");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Could not count the recommendations.", e);
        }
    }

    private List<Recommendation> query(String sql, Object... parameters) {
        List<Recommendation> list = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    list.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the recommendations.", e);
        }
        return list;
    }

    private Recommendation mapRow(ResultSet resultSet) throws SQLException {
        Recommendation recommendation = new Recommendation();
        recommendation.setId(resultSet.getLong("id"));
        recommendation.setStockId(resultSet.getInt("stock_id"));
        recommendation.setRecommendationType(resultSet.getString("recommendation_type"));
        recommendation.setRecommendationScore(resultSet.getBigDecimal("recommendation_score"));
        recommendation.setExplanation(resultSet.getString("explanation"));
        recommendation.setSupportingJson(resultSet.getString("supporting_json"));
        recommendation.setDataSource(resultSet.getString("data_source"));
        recommendation.setGeneratedAt(JdbcUtil.getDateTime(resultSet, "generated_at"));
        recommendation.setDataTimestamp(JdbcUtil.getDateTime(resultSet, "data_timestamp"));
        recommendation.setSymbol(resultSet.getString("symbol"));
        recommendation.setCompanyName(resultSet.getString("company_name"));
        recommendation.setExchange(resultSet.getString("exchange"));
        return recommendation;
    }
}
