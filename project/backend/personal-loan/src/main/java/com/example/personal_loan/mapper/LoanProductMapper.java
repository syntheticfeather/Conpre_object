package com.example.personal_loan.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.example.personal_loan.dto.ListProductResponse;
import com.example.personal_loan.entity.LoanProduct;

@Mapper
public interface LoanProductMapper {

    // 增加
    @Insert("INSERT INTO loan_products (" +
            "  product_name, description, loan_usage, status, " +
            "  min_term, max_term, term_step, " +
            "  min_amount, max_amount, " +
            "  promotion_details, create_time, update_time" +
            ") VALUES (" +
            "  #{productName}, #{description}, #{loanUsage}, #{status}, " +
            "  #{minTerm}, #{maxTerm}, #{termStep}, " +
            "  #{minAmount}, #{maxAmount}, " +
            "  #{promotionDetails}, #{createTime}, #{updateTime}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(LoanProduct loanProduct);

    //单个删除
    @Delete("DELETE FROM loan_products WHERE id = #{id}")
    int delete(Long id);

    //批量删除
    @Delete({
        "<script>",
        "DELETE FROM loan_products WHERE id IN",
        "<foreach collection='list' item='id' open='(' separator=',' close=')'>",
        "#{id}",
        "</foreach>",
        "</script>"
    })
    int batchDelete(List<Long> ids);

    // 修改
    void update(LoanProduct loanProduct);

    // 查询单个
    @Select("SELECT " +
            "id, " +
            "product_name as productName, " +
            "description, " +
            "loan_usage as loanUsage, " +
            "status as status, "+
            "min_term as minTerm, " +
            "max_term as maxTerm, " +
            "term_step as termStep, " +
            "min_amount AS minAmount, " +
            "max_amount AS maxAmount, " +
            "promotion_details as promotionDetails, " +
            "create_time as createTime, " +
            "update_time as updateTime " +
            "FROM loan_products" +
            " WHERE id = #{id}")
    LoanProduct findById(Long id);
    
    // 查询所有
    @Select("SELECT " +
            "id, " +
            "product_name as productName, " +
            "description, " +
            "loan_usage as loanUsage, " +
            "status as status, "+
            "min_term as minTerm, " +
            "max_term as maxTerm, " +
            "term_step as termStep, " +
            "min_amount AS minAmount, " +
            "max_amount AS maxAmount, " +
            "promotion_details as promotionDetails, " +
            "create_time as createTime, " +
            "update_time as updateTime " +
            "FROM loan_products "+
            "ORDER BY update_time DESC, create_time DESC")
    List<LoanProduct> findAll();

    // 查询上架产品
    @Select("SELECT " +
            "id, " +
            "product_name as productName, " +
            "description, " +
            "loan_usage as loanUsage, " +
            "min_term as minTerm, " +
            "max_term as maxTerm, " +
            "term_step as termStep, " +
            "min_amount AS minAmount, " +
            "max_amount AS maxAmount, " +
            "promotion_details as promotionDetails, " +
            "create_time as createTime, " +
            "update_time as updateTime " +
            "FROM loan_products "+
            "WHERE status = '上架中' "+
            "ORDER BY update_time DESC, create_time DESC")
    List<LoanProduct> findAllActive();

    // 用productName搜索查询
    @Select("SELECT " +
            "  id, " +
            "  product_name AS productName, " +
            "  description, " +
            "  loan_usage AS loanUsage, " +
            "  status, " +
            "  min_term AS minTerm, " +
            "  max_term AS maxTerm, " +
            "  term_step AS termStep, " +
            "  min_amount AS minAmount, " +
            "  max_amount AS maxAmount, " +
            "  promotion_details AS promotionDetails, " +
            "  create_time AS createTime, " +
            "  update_time AS updateTime " +
            "FROM loan_products " +
            "WHERE product_name LIKE CONCAT('%', #{keyword}, '%') " +
            "AND status = '上架中'")
    List<LoanProduct> findByProductNameLike(String productName);

    // 根据更新时间和创建时间的范围搜索产品
    List<ListProductResponse> searchByDate(
        @Param("createStartDate") LocalDate createStartDate,
        @Param("createEndDate") LocalDate createEndDate,
        @Param("updateStartDate") LocalDate updateStartDate,
        @Param("updateEndDate") LocalDate updateEndDate
    );
}
