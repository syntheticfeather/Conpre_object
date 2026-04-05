package com.example.personal_loan.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.personal_loan.dto.ApiResult;
import com.example.personal_loan.dto.GetCertResponse;
import com.example.personal_loan.dto.LoginRequest;
import com.example.personal_loan.dto.LoginResponse;
import com.example.personal_loan.dto.RegisterRequest;
import com.example.personal_loan.dto.RegisterResponse;
import com.example.personal_loan.service.AuthService;
import com.example.personal_loan.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@Tag(name = "认证管理", description = "用户认证相关接口")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @PostMapping(value = "/login", produces = "application/json")
    @Operation(summary = "用户登录", description = "用户通过手机号和密码进行登录")
    public ResponseEntity<ApiResult<LoginResponse>> login(@Parameter(description = "登录请求参数") @RequestBody @Valid LoginRequest request) {
        log.info("login api success called");
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResult.success(response,"登录成功"));
    }

    @PostMapping(value = "/register", produces = "application/json")
    @Operation(summary = "用户注册", description = "用户注册新账号")
    public ResponseEntity<ApiResult<RegisterResponse>> userRegister(@Parameter(description = "注册请求参数") @RequestBody @Valid RegisterRequest request) {
        log.info("register api success called");
        return ResponseEntity.ok(ApiResult.success(userService.userRegister(request),"注册成功"));
    }

    /*
    * 刷新token
    */
   @PostMapping(value = "/refresh-token", produces = "application/json")
   @Operation(summary = "刷新token", description = "刷新用户的访问令牌")
   public ResponseEntity<ApiResult<Map<String,String>>> refreshToken(@Parameter(description = "刷新token") @RequestBody Map<String, String> refreshToken) {
       Map<String,String> newTokens = userService.refreshToken(refreshToken);
       return ResponseEntity.ok(ApiResult.success(newTokens,"刷新成功"));
    }

    /**
     * 提交基本认证信息
     */
    @PostMapping(value = "/submit-basic", produces = "application/json")
    @Operation(summary = "提交基本认证", description = "用户提交身份证信息进行基本认证")
    public ResponseEntity<ApiResult<Void>> submitBasicAuth(
            HttpServletRequest request,
            @Parameter(description = "身份证号", required = true) @RequestPart String idCard,
            @Parameter(description = "真实姓名", required = true) @RequestPart String realName) {

        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/auth/submit-basic called for user {} to authorize", userId);

        authService.submitBasicAuth(userId, idCard, realName);

        return ResponseEntity.ok(ApiResult.success(null, "基本认证提交成功"));
    }

    /**
     * 提交其他认证材料
     */
    @PostMapping(value = "/submit-other", produces = "application/json")
    @Operation(summary = "提交其他认证材料", description = "用户提交银行卡号、房产、车辆、工作、工资、社保及征信报告等证明材料")
    public ResponseEntity<ApiResult<Void>> submitOtherAuth(
            HttpServletRequest request,
            @Parameter(description = "银行卡号") @RequestPart(value = "bankCardId", required = false) String bankCardId,
            @Parameter(description = "房产证明文件") @RequestPart(value = "propertyFile", required = false) MultipartFile propertyFile,
            @Parameter(description = "车辆证明文件") @RequestPart(value = "carFile", required = false) MultipartFile carFile,
            @Parameter(description = "工作证明文件") @RequestPart(value = "employmentFile", required = false) MultipartFile employmentFile,
            @Parameter(description = "工资证明文件") @RequestPart(value = "salaryFile", required = false) MultipartFile salaryFile,
            @Parameter(description = "社保证明文件") @RequestPart(value = "socialSecurityFile", required = false) MultipartFile socialSecurityFile,
            @Parameter(description = "征信报告文件") @RequestPart(value = "creditReportFile", required = false) MultipartFile creditReportFile) {

        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/auth/submit-other called for user {} to authorize", userId);

        authService.submitOtherAuth(
                userId, bankCardId,
                propertyFile, carFile,
                employmentFile, salaryFile,
                socialSecurityFile, creditReportFile
        );

        return ResponseEntity.ok(ApiResult.success(null, "其他认证材料提交成功"));
    }

    /**
     * 获取已经上传的认证信息
     */
    @GetMapping(value = "/cert-info", produces = "application/json")
    @Operation(summary = "获取认证信息", description = "获取用户已上传的认证信息")
    public ResponseEntity<ApiResult<GetCertResponse>> getCertInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/auth/cert-info success called for user {} get cert information", userId);
        try {
            GetCertResponse certInfo = authService.getCert(userId);
            return ResponseEntity.ok(ApiResult.success(certInfo, "认证信息获取成功"));
        } catch (Exception e) {
            log.error("user {} get info failed", userId, e);
            return ResponseEntity.status(500).body(ApiResult.fail(500, "系统内部错误"));
        }
    }

    /**
     * 管理员根据 workCertId 查询工作认证信息
     */
    @GetMapping(value = "/work-cert/{workCertId}", produces = "application/json")
    @Operation(summary = "获取工作认证信息", description = "管理员根据工作认证ID查询工作认证详细信息")
    public ResponseEntity<ApiResult<?>> getWorkCert(@Parameter(description = "工作认证ID") @PathVariable("workCertId") Integer workCertId) {
        log.info("/api/auth/work-cert success called for workCertId {}", workCertId);
        try {
            Object workCert = authService.getWorkCertById(workCertId);
            return ResponseEntity.ok(ApiResult.success(workCert, "工作认证信息获取成功"));
        } catch (Exception e) {
            log.error("get work cert failed for workCertId {}", workCertId, e);
            return ResponseEntity.status(500).body(ApiResult.fail(500, "系统内部错误"));
        }
    }

    /**
     * 管理员根据 triCertId 查询第三方认证信息
     */
    @GetMapping(value = "/tri-cert/{triCertId}", produces = "application/json")
    @Operation(summary = "获取第三方认证信息", description = "管理员根据第三方认证ID查询第三方认证详细信息")
    public ResponseEntity<ApiResult<?>> getTriCert(@Parameter(description = "第三方认证ID") @PathVariable("triCertId") Integer triCertId) {
        log.info("/api/auth/tri-cert success called for triCertId {}", triCertId);
        try {
            Object triCert = authService.getTriCertById(triCertId);
            return ResponseEntity.ok(ApiResult.success(triCert, "第三方认证信息获取成功"));
        } catch (Exception e) {
            log.error("get tri cert failed for triCertId {}", triCertId, e);
            return ResponseEntity.status(500).body(ApiResult.fail(500, "系统内部错误"));
        }
    }

    /**
     * 管理员根据 immovableCertId 查询不动产认证信息
     */
    @GetMapping(value = "/immovables-cert/{immovableCertId}", produces = "application/json")
    @Operation(summary = "获取不动产认证信息", description = "管理员根据不动产认证ID查询不动产认证详细信息")
    public ResponseEntity<ApiResult<?>> getImmovablesCert(@Parameter(description = "不动产认证ID") @PathVariable("immovableCertId") Integer immovableCertId) {
        log.info("/api/auth/immovables-cert success called for immovableCertId {}", immovableCertId);
        try {
            Object immovablesCert = authService.getImmovablesCertById(immovableCertId);
            return ResponseEntity.ok(ApiResult.success(immovablesCert, "不动产认证信息获取成功"));
        } catch (Exception e) {
            log.error("get immovables cert failed for immovableCertId {}", immovableCertId, e);
            return ResponseEntity.status(500).body(ApiResult.fail(500, "系统内部错误"));
        }
    }
}
