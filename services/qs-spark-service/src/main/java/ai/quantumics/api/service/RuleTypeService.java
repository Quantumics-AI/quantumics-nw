/*
 * Copyright (c) 2020. Quantumspark.ai, http://quantumspark.ai.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package ai.quantumics.api.service;

import ai.quantumics.api.model.QsRuleType;
import ai.quantumics.api.req.RuleTypesDTO;

import java.util.List;

public interface RuleTypeService {

  List<QsRuleType> getActiveRuleTypes(boolean sourceOnly);
  List<QsRuleType> getFilteredRuleTypes(RuleTypesDTO ruleTypesDTO);
}
