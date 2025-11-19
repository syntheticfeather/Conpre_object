package com.example.personal_loan;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PersonalLoanApplicationTests {

    @Test
    void SimpleTest() {
        int b = 3 + 2;
        assertThat(b).isEqualTo(5);
    }
}
