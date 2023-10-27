package ai.quantumics.api.util;

import ai.quantumics.api.adapter.AwsAdapter;
import ai.quantumics.api.livy.LivyActions;
import ai.quantumics.api.model.QsRuleJob;
import ai.quantumics.api.vo.RuleDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Date;

import static ai.quantumics.api.constants.QsConstants.DQ_DATA_PROFILE_COLUMN_LEVEL_TEMPLATE_NAME;
import static ai.quantumics.api.constants.QsConstants.DQ_DATA_PROFILE_TABLE_LEVEL_TEMPLATE_NAME;
import static ai.quantumics.api.constants.QsConstants.DQ_DUPLICATE_VALUE_COLUMN_TEMPLATE_NAME;
import static ai.quantumics.api.constants.QsConstants.DQ_DUPLICATE_VALUE_ROW_TEMPLATE_NAME;
import static ai.quantumics.api.constants.QsConstants.DUPLICATE_COLUMN_VALUE;
import static ai.quantumics.api.constants.QsConstants.DUPLICATE_MULTI_COLUMN_VALUE;
import static ai.quantumics.api.constants.QsConstants.DUPLICATE_ROW;
import static ai.quantumics.api.constants.QsConstants.DUPLICATE_VALUE;
import static ai.quantumics.api.constants.QsConstants.RULE_OUTPUT_FOLDER;
import static ai.quantumics.api.constants.QsConstants.DATA_COMPLETENESS;
import static ai.quantumics.api.constants.QsConstants.ROW_COUNT;
import static ai.quantumics.api.constants.QsConstants.SUM_OF_COLUMN_VALUE;
import static ai.quantumics.api.constants.QsConstants.DATA_PROFILER;
import static ai.quantumics.api.constants.QsConstants.TABLE_LEVEL;
import static ai.quantumics.api.constants.QsConstants.COLUMN_LEVEL;
import static ai.quantumics.api.constants.QsConstants.DQ_ROW_COUNT_TEMPLATE_NAME;
import static ai.quantumics.api.constants.QsConstants.SOURCE_BUCKET;
import static ai.quantumics.api.constants.QsConstants.SOURCE_PATH;
import static ai.quantumics.api.constants.QsConstants.TARGET_BUCKET;
import static ai.quantumics.api.constants.QsConstants.TARGET_PATH;
import static ai.quantumics.api.constants.QsConstants.S3_OUTPUT_PATH;
import static ai.quantumics.api.constants.QsConstants.RULE_TYPE_NAME;
import static ai.quantumics.api.constants.QsConstants.LEVEL_NAME;
import static ai.quantumics.api.constants.QsConstants.DQ_COLUMN_SUM_TEMPLATE_NAME;
import static ai.quantumics.api.constants.QsConstants.DQ_NULL_VALUE_TEMPLATE_NAME;
import static ai.quantumics.api.constants.QsConstants.ACCEPTANCE_PER;
import static ai.quantumics.api.constants.QsConstants.COLUMNS_DETAILS;
import static ai.quantumics.api.constants.QsConstants.NULL_VALUE;

@Slf4j
@Component
public class RuleJobHelper {

    private final AwsAdapter awsAdapter;
    private final LivyActions livyActions;

    @Value("${qs.rule.etl.output}")
    private String qsEtlScriptBucket;

    @Value("${qs.rule.job.output}")
    private String qsRuleJobBucket;

    public RuleJobHelper(AwsAdapter awsAdapterCi, LivyActions livyActionsCi) {
        this.awsAdapter = awsAdapterCi;
        this.livyActions = livyActionsCi;
    }


    @Async("qsThreadPool")
    public void submitRuleJob(QsRuleJob ruleJob, RuleDetails ruleDetails, String modifiedBy, int projectId) throws Exception {
        final StringBuilder fileContents = new StringBuilder();
        final String jobName = getJobName(ruleJob);
        String ruleTypeName = ruleDetails.getRuleDetails().getRuleTypeName();
        String levelName = ruleDetails.getRuleDetails().getRuleLevel().getLevelName();
        String scriptStr = "";
        switch (ruleTypeName) {
            case DATA_COMPLETENESS:
                switch (levelName) {
                    case ROW_COUNT:
                        readLinesFromTemplate(fileContents, DQ_ROW_COUNT_TEMPLATE_NAME);
                        scriptStr = rowCountEtlScriptVarsInit(fileContents, ruleJob, ruleDetails, jobName);
                        break;
                    case SUM_OF_COLUMN_VALUE:
                        readLinesFromTemplate(fileContents, DQ_COLUMN_SUM_TEMPLATE_NAME);
                        scriptStr = sumColumnEtlScriptVarsInit(fileContents, ruleJob, ruleDetails, jobName);
                        break;
                }
                break;
            case NULL_VALUE:
                readLinesFromTemplate(fileContents, DQ_NULL_VALUE_TEMPLATE_NAME);
                scriptStr = nullValueEtlScriptVarsInit(fileContents, ruleJob, ruleDetails, jobName);
                break;
            case DATA_PROFILER:
                switch (levelName) {
                    case TABLE_LEVEL:
                        readLinesFromTemplate(fileContents, DQ_DATA_PROFILE_TABLE_LEVEL_TEMPLATE_NAME);
                        scriptStr = prepareDataProfileTableLevelEtlScriptVarsInit(fileContents, ruleJob, ruleDetails, jobName);
                        break;
                    case COLUMN_LEVEL:
                        readLinesFromTemplate(fileContents, DQ_DATA_PROFILE_COLUMN_LEVEL_TEMPLATE_NAME);
                        scriptStr = prepareDataProfileColumnLevelEtlScriptVarsInit(fileContents, ruleJob, ruleDetails, jobName);
                        break;
                }
                break;
            case DUPLICATE_VALUE:
                switch (levelName) {
                    case DUPLICATE_ROW:
                        readLinesFromTemplate(fileContents, DQ_DUPLICATE_VALUE_ROW_TEMPLATE_NAME);
                        scriptStr = prepareDuplicateRowValueEtlScriptVarsInit(fileContents, ruleJob, ruleDetails, jobName);
                        break;
                    case DUPLICATE_COLUMN_VALUE:
                    case DUPLICATE_MULTI_COLUMN_VALUE:
                        readLinesFromTemplate(fileContents, DQ_DUPLICATE_VALUE_COLUMN_TEMPLATE_NAME);
                        scriptStr = prepareDuplicateValueColumnEtlScriptVarsInit(fileContents, ruleJob, ruleDetails, jobName);
                        break;
                }
                break;
        }

        final URL s3ContentUpload = awsAdapter.s3ContentUploadV2(qsEtlScriptBucket, jobName, scriptStr);
        final String scriptFilePath = String.format("%s%s", qsEtlScriptBucket, jobName);

        log.info("uploaded to - s3 Full Path - {} and to path {}", s3ContentUpload, scriptFilePath);

        int batchId = livyActions.invokeRuleJobOperation(scriptFilePath, qsRuleJobBucket, jobName, ruleJob.getJobId(), modifiedBy, projectId);
        log.info("Batch Id received after the Livy Job submission is: {}", batchId);
    }

    private String getJobName(QsRuleJob qsRuleJob) {
        return String.format("%d-etl-%d.py", new Date().getTime(), qsRuleJob.getRuleId());
    }

    private void readLinesFromTemplate(StringBuilder fileContents, String templateName) throws Exception {
        /*URL url = null;
        File contentSource = null;
        url = getClass().getClassLoader().getResource(templateName);
        contentSource = new File(url.toURI());*/

        File contentSource = ResourceUtils.getFile("./" + templateName);
        log.info("File in classpath Found {} : ", contentSource.exists());
        fileContents.append(new String(Files.readAllBytes(contentSource.toPath())));
    }

    private String rowCountEtlScriptVarsInit(
            StringBuilder fileContents,
            QsRuleJob ruleJob,
            RuleDetails ruleDetails,
            String jobName) {

        String temp = prepareDataCompletenessEtlScriptVarsInit(fileContents, ruleJob, ruleDetails, jobName);
        return temp;
    }

    private String sumColumnEtlScriptVarsInit(
            StringBuilder fileContents,
            QsRuleJob ruleJob,
            RuleDetails ruleDetails,
            String jobName) {

        String temp = prepareDataCompletenessEtlScriptVarsInit(fileContents, ruleJob, ruleDetails, jobName);
        temp = temp.replace(COLUMNS_DETAILS, String.format("'%s'", String.join(",", ruleDetails.getRuleDetails().getRuleLevel().getColumns())));
        return temp;
    }

    private String prepareDataCompletenessEtlScriptVarsInit(StringBuilder fileContents, QsRuleJob ruleJob, RuleDetails ruleDetails, String jobName) {
        String temp;
        jobName = jobName.replace(".py", "");

        final String outputBucketName =
                String.format("s3://%s/%s/%s", qsRuleJobBucket, RULE_OUTPUT_FOLDER, jobName);

        temp = fileContents.toString().replace(SOURCE_BUCKET, String.format("'%s'", ruleDetails.getSourceData().getBucketName()));
        temp = temp.replace(SOURCE_PATH, String.format("'%s'", ruleDetails.getSourceData().getFilePath()));
        temp = temp.replace(TARGET_BUCKET, String.format("'%s'", ruleDetails.getTargetData().getBucketName()));
        temp = temp.replace(TARGET_PATH, String.format("'%s'", ruleDetails.getTargetData().getFilePath()));
        temp = temp.replace(S3_OUTPUT_PATH, String.format("'%s'", outputBucketName));
        temp = temp.replace(RULE_TYPE_NAME, String.format("'%s'", ruleDetails.getRuleDetails().getRuleTypeName()));
        temp = temp.replace(LEVEL_NAME, String.format("'%s'", ruleDetails.getRuleDetails().getRuleLevel().getLevelName()));
        temp = temp.replace(ACCEPTANCE_PER, String.format("'%s'", ruleDetails.getRuleDetails().getRuleLevel().getAcceptance()));
        return temp;
    }

    private String nullValueEtlScriptVarsInit(
            StringBuilder fileContents,
            QsRuleJob ruleJob,
            RuleDetails ruleDetails,
            String jobName) {

        String temp;
        jobName = jobName.replace(".py", "");

        final String outputBucketName =
                String.format("s3://%s/%s/%s", qsRuleJobBucket, RULE_OUTPUT_FOLDER, jobName);

        temp = fileContents.toString().replace(SOURCE_BUCKET, String.format("'%s'", ruleDetails.getSourceData().getBucketName()));
        temp = temp.replace(SOURCE_PATH, String.format("'%s'", ruleDetails.getSourceData().getFilePath()));
        temp = temp.replace(S3_OUTPUT_PATH, String.format("'%s'", outputBucketName));
        temp = temp.replace(RULE_TYPE_NAME, String.format("'%s'", ruleDetails.getRuleDetails().getRuleTypeName()));
        temp = temp.replace(ACCEPTANCE_PER, String.format("'%s'", ruleDetails.getRuleDetails().getRuleLevel().getAcceptance()));
        temp = temp.replace(COLUMNS_DETAILS, String.format("'%s'", String.join(",", ruleDetails.getRuleDetails().getRuleLevel().getColumns())));
        return temp;
    }

    private String prepareDuplicateRowValueEtlScriptVarsInit(StringBuilder fileContents, QsRuleJob ruleJob, RuleDetails ruleDetails, String jobName) {
        String temp;
        jobName = jobName.replace(".py", "");

        final String outputBucketName =
                String.format("s3://%s/%s/%s", qsRuleJobBucket, RULE_OUTPUT_FOLDER, jobName);

        temp = fileContents.toString().replace(SOURCE_BUCKET, String.format("'%s'", ruleDetails.getSourceData().getBucketName()));
        temp = temp.replace(SOURCE_PATH, String.format("'%s'", ruleDetails.getSourceData().getFilePath()));
        temp = temp.replace(S3_OUTPUT_PATH, String.format("'%s'", outputBucketName));
        temp = temp.replace(RULE_TYPE_NAME, String.format("'%s'", ruleDetails.getRuleDetails().getRuleTypeName()));
        temp = temp.replace(LEVEL_NAME, String.format("'%s'", ruleDetails.getRuleDetails().getRuleLevel().getLevelName()));
        temp = temp.replace(ACCEPTANCE_PER, String.format("'%s'", ruleDetails.getRuleDetails().getRuleLevel().getAcceptance()));
        return temp;
    }

    private String prepareDuplicateValueColumnEtlScriptVarsInit(
            StringBuilder fileContents,
            QsRuleJob ruleJob,
            RuleDetails ruleDetails,
            String jobName) {

        String temp = prepareDuplicateRowValueEtlScriptVarsInit(fileContents, ruleJob, ruleDetails, jobName);
        temp = temp.replace(COLUMNS_DETAILS, String.format("'%s'", String.join(",", ruleDetails.getRuleDetails().getRuleLevel().getColumns())));
        return temp;
    }

    private String prepareDataProfileTableLevelEtlScriptVarsInit(StringBuilder fileContents, QsRuleJob ruleJob, RuleDetails ruleDetails, String jobName) {
        String temp;
        jobName = jobName.replace(".py", "");

        final String outputBucketName =
                String.format("s3://%s/%s/%s", qsRuleJobBucket, RULE_OUTPUT_FOLDER, jobName);

        temp = fileContents.toString().replace(SOURCE_BUCKET, String.format("'%s'", ruleDetails.getSourceData().getBucketName()));
        temp = temp.replace(SOURCE_PATH, String.format("'%s'", ruleDetails.getSourceData().getFilePath()));
        temp = temp.replace(TARGET_BUCKET, String.format("'%s'", ruleDetails.getTargetData().getBucketName()));
        temp = temp.replace(TARGET_PATH, String.format("'%s'", ruleDetails.getTargetData().getFilePath()));
        temp = temp.replace(S3_OUTPUT_PATH, String.format("'%s'", outputBucketName));
        temp = temp.replace(RULE_TYPE_NAME, String.format("'%s'", ruleDetails.getRuleDetails().getRuleTypeName()));
        temp = temp.replace(LEVEL_NAME, String.format("'%s'", ruleDetails.getRuleDetails().getRuleLevel().getLevelName()));
        return temp;
    }

    private String prepareDataProfileColumnLevelEtlScriptVarsInit(
            StringBuilder fileContents,
            QsRuleJob ruleJob,
            RuleDetails ruleDetails,
            String jobName) {

        String temp = prepareDataProfileTableLevelEtlScriptVarsInit(fileContents, ruleJob, ruleDetails, jobName);
        temp = temp.replace(COLUMNS_DETAILS, String.format("'%s'", String.join(",", ruleDetails.getRuleDetails().getRuleLevel().getColumns())));
        return temp;
    }

}