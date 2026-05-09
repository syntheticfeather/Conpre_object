package com.example.personal_loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PersonalLoanApplication {

	public static void main(String[] args) {
		SpringApplication.run(PersonalLoanApplication.class, args);
	}

}
