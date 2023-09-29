/*
 * Copyright (c) 2020. Quantumspark.ai, http://quantumspark.ai.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package ai.quantumics.api.service;

import ai.quantumics.api.vo.RuleDetails;
import org.springframework.http.ResponseEntity;

public interface RuleService {
    ResponseEntity<Object> saveRule(RuleDetails ruleDetails, int userId, int projectId);
    ResponseEntity<Object> getRuleList(int userId, int projectId, String status, int page, int pageSize);
    ResponseEntity<Object> editRule(RuleDetails ruleDetails, int userId, int projectId);

}
