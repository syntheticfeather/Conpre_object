package com.example.personal_loan.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.entity.RiskJob;

@Mapper
public interface RiskJobMapper {

    /**
     * 插入一条风控任务记录
     */
    @Insert("INSERT INTO risk_job " +
            "(application_id, user_id, job_version, status, stage, attempt, error_code, " +
            " score, feature_set, llm_feature_available, features_json, " +
            " model_version, prompt_version, converter_version, " +
            " document_count, converted_count, model, cached, reasoning, " +
            " started_at, finished_at) " +
            "VALUES " +
            "(#{applicationId}, #{userId}, #{jobVersion}, #{status}, #{stage}, #{attempt}, #{errorCode}, " +
            " #{score}, #{featureSet}, #{llmFeatureAvailable}, #{featuresJson}, " +
            " #{modelVersion}, #{promptVersion}, #{converterVersion}, " +
            " #{documentCount}, #{convertedCount}, #{model}, #{cached}, #{reasoning}, " +
            " #{startedAt}, #{finishedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "jobId", keyColumn = "job_id")
    int insert(RiskJob riskJob);

    /**
     * 按 application_id + job_version 查询（幂等用）
     */
    @Select("SELECT job_id, application_id, user_id, job_version, status, stage, attempt, error_code, " +
            "score, feature_set, llm_feature_available, features_json, " +
            "model_version, prompt_version, converter_version, " +
            "document_count, converted_count, model, cached, reasoning, " +
            "created_at, started_at, finished_at, updated_at " +
            "FROM risk_job WHERE application_id = #{applicationId} AND job_version = #{jobVersion}")
    RiskJob selectByAppVersion(@Param("applicationId") Long applicationId,
                               @Param("jobVersion") int jobVersion);

    /**
     * 按 application_id 查询最新一条
     */
    @Select("SELECT job_id, application_id, user_id, job_version, status, stage, attempt, error_code, " +
            "score, feature_set, llm_feature_available, features_json, " +
            "model_version, prompt_version, converter_version, " +
            "document_count, converted_count, model, cached, reasoning, " +
            "created_at, started_at, finished_at, updated_at " +
            "FROM risk_job WHERE application_id = #{applicationId} ORDER BY job_version DESC LIMIT 1")
    RiskJob selectLatestByApplicationId(@Param("applicationId") Long applicationId);

    /**
     * 按主键查询
     */
    @Select("SELECT job_id, application_id, user_id, job_version, status, stage, attempt, error_code, " +
            "score, feature_set, llm_feature_available, features_json, " +
            "model_version, prompt_version, converter_version, " +
            "document_count, converted_count, model, cached, reasoning, " +
            "created_at, started_at, finished_at, updated_at " +
            "FROM risk_job WHERE job_id = #{jobId}")
    RiskJob selectById(@Param("jobId") Long jobId);

    /**
     * 异步 Worker 回写完整结果（L7.2）
     */
    @Update("UPDATE risk_job SET status = #{status}, stage = #{stage}, " +
            "error_code = #{errorCode}, score = #{score}, features_json = #{featuresJson}, " +
            "llm_feature_available = #{llmFeatureAvailable}, " +
            "model_version = #{modelVersion}, prompt_version = #{promptVersion}, " +
            "converter_version = #{converterVersion}, document_count = #{documentCount}, " +
            "converted_count = #{convertedCount}, model = #{model}, cached = #{cached}, " +
            "reasoning = #{reasoning}, started_at = #{startedAt}, finished_at = #{finishedAt}, " +
            "updated_at = NOW() " +
            "WHERE job_id = #{jobId}")
    int updateCompleted(RiskJob riskJob);

    /**
     * 更新任务状态（L7.2 用，L7.1 同步版一般不需要）
     */
    @Update("UPDATE risk_job SET status = #{status}, stage = #{stage}, " +
            "error_code = #{errorCode}, score = #{score}, features_json = #{featuresJson}, " +
            "llm_feature_available = #{llmFeatureAvailable}, " +
            "finished_at = #{finishedAt}, updated_at = NOW() " +
            "WHERE job_id = #{jobId}")
    int updateResult(RiskJob riskJob);

    /**
     * 查询指定申请是否已有完成的风险打分（用于幂等判断）
     */
    @Select("SELECT COUNT(*) FROM risk_job WHERE application_id = #{applicationId} " +
            "AND status IN ('COMPLETED', 'DEGRADED')")
    int countCompletedByApplicationId(@Param("applicationId") Long applicationId);

    /**
     * 查询超时的 QUEUED 任务（L7.2 补偿扫描）
     */
    @Select("SELECT job_id, application_id, user_id, job_version, status, stage, attempt, error_code, " +
            "score, feature_set, llm_feature_available, features_json, " +
            "model_version, prompt_version, converter_version, " +
            "document_count, converted_count, model, cached, reasoning, " +
            "created_at, started_at, finished_at, updated_at " +
            "FROM risk_job WHERE status = 'QUEUED' " +
            "AND created_at < DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE) " +
            "ORDER BY created_at ASC")
    List<RiskJob> selectTimeoutQueued(@Param("timeoutMinutes") int timeoutMinutes);

    /**
     * 超时重投后更新 attempt / error_code（CAS：仅当仍是 QUEUED 时更新）
     */
    @Update("UPDATE risk_job SET attempt = attempt + 1, error_code = #{errorCode}, " +
            "updated_at = NOW() WHERE job_id = #{jobId} AND status = 'QUEUED'")
    int updateTimeoutRetry(@Param("jobId") Long jobId, @Param("errorCode") String errorCode);

    /**
     * 超时任务标记为 FAILED（CAS：仅当仍是 QUEUED 时更新）
     */
    @Update("UPDATE risk_job SET status = 'FAILED', stage = 'FAILED', " +
            "error_code = #{errorCode}, finished_at = #{finishedAt}, " +
            "updated_at = NOW() WHERE job_id = #{jobId} AND status = 'QUEUED'")
    int updateFailedByTimeout(@Param("jobId") Long jobId,
                              @Param("errorCode") String errorCode,
                              @Param("finishedAt") LocalDateTime finishedAt);
}
