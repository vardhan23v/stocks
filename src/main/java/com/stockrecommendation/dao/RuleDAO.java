package com.stockrecommendation.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.stockrecommendation.model.RecommendationRule;

/** SQL for the recommendation_rules table. */
@Repository
public class RuleDAO {

    private final JdbcTemplate jdbc;

    public RuleDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<RecommendationRule> findAll() {
        return jdbc.query("SELECT id, rule_key, name, description, weight, param_low, param_high, enabled "
                + "FROM recommendation_rules ORDER BY id", (rs, row) -> {
                    RecommendationRule rule = new RecommendationRule();
                    rule.setId(rs.getInt("id"));
                    rule.setRuleKey(rs.getString("rule_key"));
                    rule.setName(rs.getString("name"));
                    rule.setDescription(rs.getString("description"));
                    rule.setWeight(rs.getBigDecimal("weight"));
                    rule.setParamLow(rs.getBigDecimal("param_low"));
                    rule.setParamHigh(rs.getBigDecimal("param_high"));
                    rule.setEnabled(rs.getBoolean("enabled"));
                    return rule;
                });
    }

    /** Saves the edited rules in ONE transaction, so the engine never sees half an update. */
    @Transactional
    public void updateAll(List<RecommendationRule> rules) {
        List<Object[]> rows = new ArrayList<>();
        for (RecommendationRule rule : rules) {
            rows.add(new Object[] {rule.getWeight(), rule.getParamLow(), rule.getParamHigh(), rule.isEnabled(), rule.getId()});
        }
        jdbc.batchUpdate("UPDATE recommendation_rules SET weight = ?, param_low = ?, param_high = ?, enabled = ? WHERE id = ?", rows);
    }
}
