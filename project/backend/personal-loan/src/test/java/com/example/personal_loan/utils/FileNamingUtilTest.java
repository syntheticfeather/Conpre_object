package com.example.personal_loan.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FileNamingUtilTest {

    @Test
    void testGenerateFileName_AllowsPdf() {
        String name = FileNamingUtil.generateFileName("credit", 1L, "report.pdf");

        assertTrue(name.startsWith("credit_1_"));
        assertTrue(name.endsWith(".pdf"));
    }

    @Test
    void testGenerateFileName_LowercasesExtension() {
        String name = FileNamingUtil.generateFileName("credit", 1L, "REPORT.PDF");

        assertTrue(name.endsWith(".pdf"));
    }

    @Test
    void testGenerateFileName_AllowsTxt() {
        String name = FileNamingUtil.generateFileName("note", 2L, "memo.txt");

        assertTrue(name.endsWith(".txt"));
    }
}
