package com.stockrecommendation.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.stockrecommendation.model.RecommendationRule;
import com.stockrecommendation.util.DBConnection;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.JdbcUtil;

/** SQL for the recommendation_rules table. */
public class RuleDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    public List<RecommendationRule> findAll() {
        String sql = "SELECT id, rule_key, name, description, weight, param_low, param_high, enabled "
                   + "FROM recommendation_rules ORDER BY id";
        List<RecommendationRule> rules = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                RecommendationRule rule = new RecommendationRule();
                rule.setId(resultSet.getInt("id"));
                rule.setRuleKey(resultSet.getString("rule_key"));
                rule.setName(resultSet.getString("name"));
                rule.setDescription(resultSet.getString("description"));
                rule.setWeight(resultSet.getBigDecimal("weight"));
                rule.setParamLow(resultSet.getBigDecimal("param_low"));
                rule.setParamHigh(resultSet.getBigDecimal("param_high"));
                rule.setEnabled(resultSet.getBoolean("enabled"));
                rules.add(rule);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the recommendation rules.", e);
        }
        return rules;
    }

    /** Saves the edited weights in ONE transaction, so the engine never sees half an update. */
    public void updateAll(List<RecommendationRule> rules) {
        String sql = "UPDATE recommendation_rules SET weight = ?, param_low = ?, param_high = ?, enabled = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (RecommendationRule rule : rules) {
                    statement.setBigDecimal(1, rule.getWeight());
                    JdbcUtil.setDecimal(statement, 2, rule.getParamLow());
                    JdbcUtil.setDecimal(statement, 3, rule.getParamHigh());
                    statement.setBoolean(4, rule.isEnabled());
                    statement.setInt(5, rule.getId());
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the recommendation rules.", e);
        }
    }
}
