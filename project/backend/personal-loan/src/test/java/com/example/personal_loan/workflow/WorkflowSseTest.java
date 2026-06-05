package com.example.personal_loan.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Workflow SSE 内容验证（Spring Boot 集成测试，需要 Docker 服务运行）
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkflowSseTest {

    @Autowired
    private MockMvc mockMvc;

    // ================================================================
    // QUERY_STATUS
    // ================================================================

    @Test
    @Order(1)
    void queryStatus() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token-fake-123")
                        .content("""
                                {"message":"帮我查一下贷款申请进度","sessionId":"test-sse-1"}"""))
                .andExpect(request().asyncStarted())
                .andReturn();

        MockHttpServletResponse response = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        String body = response.getContentAsString(StandardCharsets.UTF_8);
        print("QUERY_STATUS", body);

        assertTrue(body.contains("type"), "应包含SSE事件");
    }

    // ================================================================
    // LIST_PRODUCTS
    // ================================================================

    @Test
    @Order(2)
    void listProducts() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token-fake-123")
                        .content("""
                                {"message":"有哪些贷款产品","sessionId":"test-sse-2"}"""))
                .andExpect(request().asyncStarted())
                .andReturn();

        MockHttpServletResponse response = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        String body = response.getContentAsString(StandardCharsets.UTF_8);
        print("LIST_PRODUCTS", body);

        assertTrue(body.contains("type"), "应返回SSE事件");
    }

    // ================================================================
    // CALCULATE
    // ================================================================

    @Test
    @Order(3)
    void calculate() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token-fake-123")
                        .content("""
                                {"message":"算算20万36期，利率4.5%，等额本息","sessionId":"test-sse-3"}"""))
                .andExpect(request().asyncStarted())
                .andReturn();

        MockHttpServletResponse response = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        String body = response.getContentAsString(StandardCharsets.UTF_8);
        print("CALCULATE", body);

        assertTrue(body.contains("type"), "应返回SSE结果");
    }

    // ================================================================
    // APPLY_LOAN
    // ================================================================

    @Test
    @Order(4)
    void applyLoan() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token-fake-123")
                        .content("""
                                {"message":"我要申请个人消费贷50万","sessionId":"test-sse-4"}"""))
                .andExpect(request().asyncStarted())
                .andReturn();

        MockHttpServletResponse response = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        String body = response.getContentAsString(StandardCharsets.UTF_8);
        print("APPLY_LOAN", body);

        assertTrue(body.contains("type"), "应返回SSE结果");
    }

    // ================================================================
    // AGENT 路由（验证复杂请求走 Agent 透传）
    // ================================================================

    @Test
    @Order(5)
    void agentRouting() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token-fake-123")
                        .content("""
                                {"message":"我不满意你们的利率","sessionId":"test-sse-5"}"""))
                .andExpect(request().asyncStarted())
                .andReturn();

        MockHttpServletResponse response = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        String body = response.getContentAsString(StandardCharsets.UTF_8);
        print("AGENT (COMPLAINT)", body);

        // 投诉走 Agent → 可能返回 "已转接" 或 Agent 实际回复
        assertTrue(body.length() > 0, "应收到响应");
    }

    private void print(String label, String body) {
        System.out.println("\n=== " + label + " ===");
        // 限长显示
        int maxLen = 600;
        String display = body.length() > maxLen ? body.substring(0, maxLen) + "..." : body;
        System.out.println(display);
    }
}
