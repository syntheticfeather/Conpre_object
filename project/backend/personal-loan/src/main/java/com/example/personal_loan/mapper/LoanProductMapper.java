package com.example.personal_loan.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.entity.LoanProduct;

@Mapper
public interface LoanProductMapper {

    @Insert("INSERT INTO loan_products (product_name, min_term, max_term, term_step, promotion_details) VALUES ( #{productName}, #{minTerm}, #{maxTerm}, #{termStep}, #{promotionDetails})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(LoanProduct loanProduct);

    @Delete("DELETE FROM loan_products WHERE id = #{id}")
    int delete(Long id);

    @Update("UPDATE loan_products SET product_name = #{productName}, min_term = #{minTerm}, max_term = #{maxTerm}, term_step = #{termStep}, promotion_details = #{promotionDetails} WHERE id = #{id}")
    int update(LoanProduct loanProduct);

    @Select("SELECT * FROM loan_products WHERE id = #{id}")
    LoanProduct findById(Long id);
    @Select("SELECT * FROM loan_products")
    List<LoanProduct> findAll();

}
