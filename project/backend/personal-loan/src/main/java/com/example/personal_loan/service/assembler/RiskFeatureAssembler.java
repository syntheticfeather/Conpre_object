package com.example.personal_loan.service.assembler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.personal_loan.dto.risk.RiskFeatureRequest;
import com.example.personal_loan.dto.risk.RiskFeatureRequest.ApplicationContext;
import com.example.personal_loan.dto.risk.RiskFeatureRequest.DocumentItem;
import com.example.personal_loan.entity.ImmovablesCert;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.TriCert;
import com.example.personal_loan.entity.UserCert;
import com.example.personal_loan.entity.WorkCert;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.mapper.ImmovablesCertMapper;
import com.example.personal_loan.mapper.TriCertMapper;
import com.example.personal_loan.mapper.UserCertMapper;
import com.example.personal_loan.mapper.WorkCertMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 风控特征请求组装器（L7.1）
 * <p>
 * 从 Java 主业务的数据源里拉取：
 * <ul>
 *   <li>申请上下文：金额 / 期限 / 收入</li>
 *   <li>材料路径：WorkCert / TriCert / ImmovablesCert 的各张认证表</li>
 * </ul>
 * 组装成 {@link RiskFeatureRequest}，供 {@code RiskFeatureClient} 调用 Python FastAPI。
 * <p>
 * 注意：
 * <ul>
 *   <li>{@code source_path} 是相对于 Python 侧 {@code RISK_DOC_BASE_DIR} 的路径，不是绝对路径</li>
 *   <li>如果 Java 的 {@code file-storage.base-dir} 和 Python 的 {@code RISK_DOC_BASE_DIR} 相同，
 *       直接把 Java DB 里存的相对路径传过去即可</li>
 *   <li>只有路径非空才会加入 documents 列表</li>
 * </ul>
 */
@Slf4j
@Component
public class RiskFeatureAssembler {

    @Autowired
    private UserCertMapper userCertMapper;

    @Autowired
    private WorkCertMapper workCertMapper;

    @Autowired
    private TriCertMapper triCertMapper;

    @Autowired
    private ImmovablesCertMapper immovablesCertMapper;

    @Autowired
    private ApplicationMapper applicationMapper;

    /** Python 侧文档目录的相对路径前缀（如果 Java DB 里存的是相对于 uploads 的路径） */
    @Value("${risk.service.doc-path-prefix:}")
    private String docPathPrefix;

    /**
     * 组装风控特征请求
     *
     * @param userId        用户 ID
     * @param applicationId 申请 ID
     * @return 组装好的请求体（保证 applicationId、application 上下文存在；documents 可能为空）
     */
    public RiskFeatureRequest buildRequest(Long userId, Long applicationId) {
        RiskFeatureRequest req = new RiskFeatureRequest();
        req.setApplicationId(applicationId.intValue());

        // 1. 申请上下文：金额、期限
        LoanApplication app = applicationMapper.selectById(applicationId);
        ApplicationContext ctx = new ApplicationContext();
        if (app != null) {
            ctx.setAmount(app.getLoanAmount());
            ctx.setTerm(app.getTerm() != null ? app.getTerm() : app.getLoanPeriod());
        }

        // 收入：从 WorkCert 拿（没有则给默认 0）
        BigDecimal income = loadIncome(userId);
        ctx.setIncome(income);
        req.setApplication(ctx);

        // 2. 材料列表
        List<DocumentItem> docs = new ArrayList<>();
        loadAndAddWorkCert(userId, docs);
        loadAndAddTriCert(userId, docs);
        loadAndAddImmovablesCert(userId, docs);
        req.setDocuments(docs);

        req.setApplicationNote("");  // 当前暂无可传备注

        log.info("[RiskFeatureAssembler] 组装请求 userId={} appId={} docCount={}",
                userId, applicationId, docs.size());
        return req;
    }

    // ─── 收入 ────────────────────────────────────────────
    private BigDecimal loadIncome(Long userId) {
        // WorkCert 里没有收入字段，这里给默认值 0，等后续 WorkCert 扩展
        return BigDecimal.ZERO;
    }

    // ─── 工作证明 ─────────────────────────────────────────
    private void loadAndAddWorkCert(Long userId, List<DocumentItem> docs) {
        UserCert userCert = userCertMapper.selectByUserId(userId);
        if (userCert == null || userCert.getWorkCertId() == null) return;
        WorkCert wc = workCertMapper.selectById(userCert.getWorkCertId());
        if (wc == null) return;

        addDocIfPresent(docs, "employment_cert", wc.getEmploymentCertPath());
        addDocIfPresent(docs, "salary_cert", wc.getSalaryCertPath());
    }

    // ─── 第三方认证 ───────────────────────────────────────
    private void loadAndAddTriCert(Long userId, List<DocumentItem> docs) {
        UserCert userCert = userCertMapper.selectByUserId(userId);
        if (userCert == null || userCert.getTriCertId() == null) return;
        TriCert tc = triCertMapper.selectById(userCert.getTriCertId());
        if (tc == null) return;

        addDocIfPresent(docs, "social_security", tc.getSocialSecurityPath());
        addDocIfPresent(docs, "credit_report", tc.getCreditReportPath());
    }

    // ─── 不动产认证 ───────────────────────────────────────
    private void loadAndAddImmovablesCert(Long userId, List<DocumentItem> docs) {
        UserCert userCert = userCertMapper.selectByUserId(userId);
        if (userCert == null || userCert.getImmovableCertId() == null) return;
        ImmovablesCert ic = immovablesCertMapper.selectById(userCert.getImmovableCertId());
        if (ic == null) return;

        addDocIfPresent(docs, "property_cert", ic.getPropertyCertPath());
        addDocIfPresent(docs, "car_cert", ic.getCarCertPath());
    }

    // ─── 工具：路径非空才加入 ──────────────────────────────
    private void addDocIfPresent(List<DocumentItem> docs, String type, String path) {
        if (path == null || path.isBlank()) return;
        DocumentItem doc = new DocumentItem();
        doc.setType(type);
        doc.setSourcePath(normalizePath(path));
        docs.add(doc);
    }

    /**
     * 规范化路径：去掉 Windows 盘符、统一分隔符、加上可选前缀。
     * 最终路径应该是相对于 Python RISK_DOC_BASE_DIR 的相对路径。
     */
    private String normalizePath(String path) {
        if (path == null) return null;
        // 统一斜杠
        String normalized = path.replace('\\', '/');
        // 去掉绝对路径前缀（如果有）
        int idx = normalized.indexOf("uploads/");
        if (idx >= 0) {
            normalized = normalized.substring(idx);
        }
        // 去掉开头的 ./ 或 /
        while (normalized.startsWith("./")) normalized = normalized.substring(2);
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (docPathPrefix != null && !docPathPrefix.isBlank() && !normalized.startsWith(docPathPrefix)) {
            normalized = docPathPrefix + "/" + normalized;
        }
        return normalized;
    }
}
