package com.example.personal_loan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.personal_loan.dao")
public class PersonalLoanApplication {

	public static void main(String[] args) {
		SpringApplication.run(PersonalLoanApplication.class, args);
	}

}
