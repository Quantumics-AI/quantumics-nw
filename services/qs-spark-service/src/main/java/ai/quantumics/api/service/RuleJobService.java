/*
 * Copyright (c) 2020. Quantumspark.ai, http://quantumspark.ai.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package ai.quantumics.api.service;

import ai.quantumics.api.req.CancelJobRequest;
import ai.quantumics.api.req.RuleJobDTO;
import ai.quantumics.api.req.RuleJobRequest;
import ai.quantumics.api.req.RunRuleJobRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface RuleJobService {
    ResponseEntity<Object> runRuleJob(RuleJobRequest ruleJobRequest, int userId, int projectId);
    ResponseEntity<Object> cancelRuleJobs(CancelJobRequest ruleJobRequest, int userId, int projectId);

    ResponseEntity<Object> fetchRuleJobList(int userId, int projectId, List<String> status);
    ResponseEntity<Object> getRowCount(String bucketName, String filePath, int userId, int projectId);

    ResponseEntity<Object> runBatchRuleJob(RunRuleJobRequest ruleJobRequest);
    ResponseEntity<Object> submitBatchRuleJob(RunRuleJobRequest ruleJobRequest);
    ResponseEntity<Object> getFilteredRuleJobs(int userId, int projectId, RuleJobDTO ruleJobDTO);
}
