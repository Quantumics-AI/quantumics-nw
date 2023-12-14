package ai.quantumics.api.util;

public class QsRuleJobQueries {
    private static final String COMMON_PART = "SELECT NEW ai.quantumics.api.model.QsRuleJobResponse(qsj.jobId, qsj.ruleId, qsj.jobStatus, qsj.jobOutput, qsj.batchJobLog, qsj.batchJobId, qsj.userId, qsj.active, qsj.businessDate, qsj.jobSubmittedDate, qsj.jobFinishedDate, qsj.createdDate, qsj.modifiedDate, qsj.createdBy, qsj.modifiedBy, qsr.ruleName, qsr.ruleTypeName, qsr.levelName, qsr.status, qsr.sourceFeedName, qsr.targetFeedName) FROM QsRuleJob qsj JOIN QsRule qsr ON qsj.ruleId = qsr.ruleId ";
    public static final String GET_FILTERED_DATA = COMMON_PART + "WHERE (:feedName IS NULL OR qsr.sourceFeedName = :feedName OR qsr.targetFeedName = :feedName) "
            + "AND (qsj.businessDate BETWEEN :startDate AND :endDate) "
            + "AND ((:ruleTypeNames) IS NULL OR qsr.ruleTypeName IN (:ruleTypeNames)) "
            + "AND ((:ruleJobStatus) IS NULL OR qsj.jobStatus IN (:ruleJobStatus)) "
            + "ORDER BY qsj.modifiedDate DESC";
    public static final String GET_FILTERED_DATA_EXCLUDE_BUSINESS_DATE = COMMON_PART + "WHERE (:feedName IS NULL OR qsr.sourceFeedName = :feedName OR qsr.targetFeedName = :feedName) "
            + "AND ((:ruleTypeNames) IS NULL OR qsr.ruleTypeName IN (:ruleTypeNames)) "
            + "AND ((:ruleJobStatus) IS NULL OR qsj.jobStatus IS EMPTY OR qsj.jobStatus IN (:ruleJobStatus)) "
            + "ORDER BY qsj.modifiedDate DESC";
    public static final String GET_ALL_DATA = COMMON_PART + "WHERE qsj.active = true ORDER BY qsj.modifiedDate DESC";
}

