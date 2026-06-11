package com.example.personal_loan.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.entity.MlTrainingLog;

@Mapper
public interface MlTrainingLogMapper {

    @Insert(
        "INSERT INTO ml_training_log (user_id, application_id, features, model_version, model_score) " +
        "VALUES (#{userId}, #{applicationId}, #{features}, #{modelVersion}, #{modelScore})"
    )
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(MlTrainingLog log);

    @Update(
        "UPDATE ml_training_log SET actual_result = #{actualResult}, " +
        "result_at = #{resultAt}, updated_at = NOW() " +
        "WHERE application_id = #{applicationId} AND actual_result IS NULL"
    )
    int updateResult(@Param("applicationId") Long applicationId,
                     @Param("actualResult") int actualResult,
                     @Param("resultAt") LocalDateTime resultAt);

    @Select(
        "SELECT id, user_id AS userId, application_id AS applicationId, " +
        "features, model_version AS modelVersion, model_score AS modelScore, " +
        "actual_result AS actualResult, result_at AS resultAt, " +
        "created_at AS createdAt, updated_at AS updatedAt " +
        "FROM ml_training_log WHERE application_id = #{applicationId}"
    )
    MlTrainingLog selectByApplicationId(@Param("applicationId") Long applicationId);

    @Select(
        "SELECT id, user_id AS userId, application_id AS applicationId, " +
        "features, model_version AS modelVersion, model_score AS modelScore, " +
        "actual_result AS actualResult, result_at AS resultAt, " +
        "created_at AS createdAt, updated_at AS updatedAt " +
        "FROM ml_training_log WHERE actual_result IS NULL ORDER BY created_at DESC"
    )
    List<MlTrainingLog> selectPendingLabels();

    @Update(
        "UPDATE ml_training_log SET model_version = #{modelVersion}, " +
        "model_score = #{modelScore}, updated_at = NOW() " +
        "WHERE application_id = #{applicationId}"
    )
    int updateScore(@Param("applicationId") Long applicationId,
                    @Param("modelVersion") String modelVersion,
                    @Param("modelScore") java.math.BigDecimal modelScore);

    @Select(
        "SELECT COUNT(*) FROM ml_training_log WHERE actual_result IS NOT NULL"
    )
    int countLabeled();
}
