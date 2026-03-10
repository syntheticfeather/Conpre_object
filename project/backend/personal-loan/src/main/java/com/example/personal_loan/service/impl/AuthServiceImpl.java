package com.example.personal_loan.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.personal_loan.config.FileStorageConfig;
import com.example.personal_loan.dto.GetCertResponse;
import com.example.personal_loan.entity.ImmovablesCert;
import com.example.personal_loan.entity.TriCert;
import com.example.personal_loan.entity.UserCert;
import com.example.personal_loan.entity.WorkCert;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.mapper.ImmovablesCertMapper;
import com.example.personal_loan.mapper.TriCertMapper;
import com.example.personal_loan.mapper.UserCertMapper;
import com.example.personal_loan.mapper.UserMapper;
import com.example.personal_loan.mapper.WorkCertMapper;
import com.example.personal_loan.service.AuthService;
import com.example.personal_loan.service.LocalFileStorageService;
import com.example.personal_loan.utils.BankCardUtils;
import com.example.personal_loan.utils.IdCardUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService{

    @Autowired
    private UserMapper userMapper; // 用户认证信息类

    @Autowired
    private UserCertMapper userCertMapper;

    @Autowired
    private WorkCertMapper workCertMapper;

    @Autowired
    private TriCertMapper triCertMapper;

    @Autowired
    private ImmovablesCertMapper immovablesCertMapper;

    @Autowired
    private LocalFileStorageService fileStorageService;

    @Autowired
    private FileStorageConfig fileStorageConfig;

    // 提交所有认证信息
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAllAuth(
            Long userId,
            String idCard,
            String bankCardId,
            MultipartFile propertyFile,
            MultipartFile carFile,
            MultipartFile employmentFile,
            MultipartFile salaryFile,
            MultipartFile socialSecurityFile,
            MultipartFile creditReportFile) {

        log.info("开始提交认证材料: userId={}, idCard={}, bankCardId={}", userId, idCard, bankCardId);

        // 1. 验证身份证号和银行卡号
        if (idCard.isBlank() || !IdCardUtils.isValid(idCard)) {
            throw new BusinessException(400, "身份证号不能为空或格式无效");
        }
        if (!bankCardId.isBlank() && BankCardUtils.isValid(bankCardId)) {
        } else {
            throw new BusinessException(400, "银行卡号不能为空或格式无效");
        }

        // 2. 获取用户认证主记录（user_certification）
        UserCert userCert = userCertMapper.selectByUserId(userId);

        // 3. 存储所有文件（任一失败则整体回滚）
        log.info("开始存储所有认证文件...");
        String propertyPath = storeRequiredFile(propertyFile, "property", userId, fileStorageConfig.getPaths().getPropertyProof());
        String carPath = storeRequiredFile(carFile, "car", userId, fileStorageConfig.getPaths().getCarProof());
        String empPath = storeRequiredFile(employmentFile, "employment", userId, fileStorageConfig.getPaths().getEmploymentProof());
        String salPath = storeRequiredFile(salaryFile, "salary", userId, fileStorageConfig.getPaths().getSalaryProof());
        String ssPath = storeRequiredFile(socialSecurityFile, "social", userId, fileStorageConfig.getPaths().getSocialSecurity());
        String crPath = storeRequiredFile(creditReportFile, "credit", userId, fileStorageConfig.getPaths().getCreditReport());
        log.info("所有文件存储完成: propertyPath={}, carPath={}, empPath={}, salPath={}, ssPath={}, crPath={}", 
            propertyPath, carPath, empPath, salPath, ssPath, crPath);

        // 4. 处理不动产认证
        handleImmovablesCert(userCert, propertyPath, carPath);
        log.info("不动产认证处理完成: immovableCertId={}", userCert.getImmovableCertId());

        // 5. 处理工作认证
        handleWorkCert(userCert, empPath, salPath);
        log.info("工作认证处理完成: workCertId={}", userCert.getWorkCertId());

        // 6. 处理第三方认证
        handleTriCert(userCert, ssPath, crPath);
        log.info("第三方认证处理完成: triCertId={}", userCert.getTriCertId());

        // 7. 更新主表中的文本字段
        userCert.setIdCard(idCard);
        userCert.setBankCardId(bankCardId);
        userCertMapper.update(userCert);
        
        log.info("认证材料提交成功: userId={}", userId);
    }

    // 计算贷款分数(125x6=750)
    @Override
    public int calScore(Long userId){ 
        UserCert userCert = userCertMapper.selectByUserId(userId);
        if (userCert == null) {
            return 0;
        }

        int score = 0;

        final int ITEM_SCORE = 125;

        // 1. 工作认证
        if (userCert.getWorkCertId() != null) {
            WorkCert workCert = workCertMapper.selectById(userCert.getWorkCertId());
            if (workCert != null) {
                if (workCert.getEmploymentCertPath() != null && !workCert.getEmploymentCertPath().isEmpty()) {
                    score += ITEM_SCORE;
                }
                if (workCert.getSalaryCertPath() != null && !workCert.getSalaryCertPath().isEmpty()) {
                    score += ITEM_SCORE;
                }
            }
        }

        // 2. 第三方认证
        if (userCert.getTriCertId() != null) {
            TriCert triCert = triCertMapper.selectById(userCert.getTriCertId());
            if (triCert != null) {
                if (triCert.getSocialSecurityPath() != null && !triCert.getSocialSecurityPath().isEmpty()) {
                    score += ITEM_SCORE;
                }
                if (triCert.getCreditReportPath() != null && !triCert.getCreditReportPath().isEmpty()) {
                    score += ITEM_SCORE;
                }
            }
        }

        // 3. 不动产认证
        if (userCert.getImmovableCertId() != null) {
            ImmovablesCert immoCert = immovablesCertMapper.selectById(userCert.getImmovableCertId());
            if (immoCert != null) {
                if (immoCert.getPropertyCertPath() != null && !immoCert.getPropertyCertPath().isEmpty()) {
                    score += ITEM_SCORE;
                }
                if (immoCert.getCarCertPath() != null && !immoCert.getCarCertPath().isEmpty()) {
                    score += ITEM_SCORE;
                }
            }
        }

        return score;
    }
    
    // 获取已经上传的认证信息
    @Override
    public GetCertResponse getCert(Long userId) {
        UserCert userCert = userCertMapper.selectByUserId(userId);
        
        WorkCert workCert = null;
        if (userCert.getWorkCertId() != null) {
            workCert = workCertMapper.selectById(userCert.getWorkCertId());
            // 转换路径为公开 URL
            if (workCert.getEmploymentCertPath() != null) {
                workCert.setEmploymentCertPath(buildPublicUrl(
                    fileStorageConfig.getPaths().getEmploymentProof(),
                    workCert.getEmploymentCertPath()
                ));
            }
            if (workCert.getSalaryCertPath() != null) {
                workCert.setSalaryCertPath(buildPublicUrl(
                    fileStorageConfig.getPaths().getSalaryProof(),
                    workCert.getSalaryCertPath()
                ));
            }
        }

        TriCert triCert = null;
        if (userCert.getTriCertId() != null) {
            triCert = triCertMapper.selectById(userCert.getTriCertId());
            if (triCert.getSocialSecurityPath() != null) {
                triCert.setSocialSecurityPath(buildPublicUrl(
                    fileStorageConfig.getPaths().getSocialSecurity(),
                    triCert.getSocialSecurityPath()
                ));
            }
            if (triCert.getCreditReportPath() != null) {
                triCert.setCreditReportPath(buildPublicUrl(
                    fileStorageConfig.getPaths().getCreditReport(),
                    triCert.getCreditReportPath()
                ));
            }
        }

        ImmovablesCert immovablesCert = null;
        if (userCert.getImmovableCertId() != null) {
            immovablesCert = immovablesCertMapper.selectById(userCert.getImmovableCertId());
            if (immovablesCert.getPropertyCertPath() != null) {
                immovablesCert.setPropertyCertPath(buildPublicUrl(
                    fileStorageConfig.getPaths().getPropertyProof(),
                    immovablesCert.getPropertyCertPath()
                ));
            }
            if (immovablesCert.getCarCertPath() != null) {
                immovablesCert.setCarCertPath(buildPublicUrl(
                    fileStorageConfig.getPaths().getCarProof(),
                    immovablesCert.getCarCertPath()
                ));
            }
        }
        userCert.setCreditScore(calScore(userId));
        return new GetCertResponse(userCert, workCert, triCert, immovablesCert);
    }

    // 个人征信认证
    // @Override
    // public void creditAuth(){ 

    // }


    // 工具方法

    private String storeRequiredFile(MultipartFile file, String type, Long userId, String basePath) {
        if (file == null || file.isEmpty()) {
            log.warn("文件为空，跳过存储: type={}, userId={}", type, userId);
            return null;
        }
        log.info("开始存储文件: type={}, userId={}, originalFilename={}, size={}", 
            type, userId, file.getOriginalFilename(), file.getSize());
        String storedPath = fileStorageService.storeFile(file, type, userId, basePath);
        log.info("文件存储成功: type={}, userId={}, storedPath={}", type, userId, storedPath);
        return storedPath;
    }

    private void handleImmovablesCert(UserCert userCert, String propertyPath, String carPath) {
        ImmovablesCert cert = new ImmovablesCert();
        cert.setPropertyCertPath(propertyPath);
        cert.setCarCertPath(carPath);
        // total_value 可后续计算，暂不设

        Integer existingId = userCert.getImmovableCertId();
        if (existingId == null) {
            immovablesCertMapper.insert(cert);
            userCert.setImmovableCertId(cert.getImmovableCertId());
        } else {
            cert.setImmovableCertId(existingId);
            immovablesCertMapper.update(cert);
        }
    }

    private void handleWorkCert(UserCert userCert, String empPath, String salPath) {
        WorkCert cert = new WorkCert();
        cert.setEmploymentCertPath(empPath);
        cert.setSalaryCertPath(salPath);

        Integer existingId = userCert.getWorkCertId();
        if (existingId == null) {
            workCertMapper.insert(cert);
            userCert.setWorkCertId(cert.getWorkCertId());
        } else {
            cert.setWorkCertId(existingId);
            workCertMapper.update(cert);
        }
    }

    private void handleTriCert(UserCert userCert, String ssPath, String crPath) {
        TriCert cert = new TriCert();
        cert.setSocialSecurityPath(ssPath);
        cert.setCreditReportPath(crPath);

        Integer existingId = userCert.getTriCertId();
        if (existingId == null) {
            triCertMapper.insert(cert);
            userCert.setTriCertId(cert.getTriCertId());
        } else {
            cert.setTriCertId(existingId);
            triCertMapper.update(cert);
        }
    }

    /**
     * 构建前端可直接访问的 URL：/uploads/{relativeBasePath}/{filename}
     */
    private String buildPublicUrl(String relativeBasePath, String filename) {
        // 确保路径不以 / 开头或结尾，避免双斜杠
        String cleanBase = relativeBasePath.replaceAll("/+$", "").replaceAll("^/+", "");
        String cleanFile = filename.replaceAll("^/+", "");
        return "/uploads/" + cleanBase + "/" + cleanFile;
    }
}
