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

    /**
     * 提交基本认证信息
     * @param userId 用户ID
     * @param idCard 身份证号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitBasicAuth(Long userId, String idCard) {
        UserCert userCert = userCertMapper.selectByUserId(userId);

        // 1. 参数校验
        if (userCert.getIdCard() == null) {
            if (idCard == null || idCard.trim().isEmpty()) {
                throw new BusinessException(400, "身份证号不能为空");
            }
            if (!IdCardUtils.isValid(idCard)) {
                throw new BusinessException(400, "身份证号格式无效");
            }
        }

        // 2. 更新主表中的身份证号
        userCert.setIdCard(idCard);
        userCert.setCreditScore(calScore(userId));
        userCertMapper.update(userCert);
        
        log.info("basic auth is submitted successfully: userId={}", userId);
    }

    /**
     * 提交其他认证信息，包括银行卡及各类证明材料
     * @param userId 用户ID
     * @param bankCardId 银行卡号
     * @param propertyFile 资产证明文件
     * @param carFile 车辆证明文件
     * @param employmentFile 工作证明文件
     * @param salaryFile 收入证明文件
     * @param socialSecurityFile 社保证明文件
     * @param creditReportFile 征信报告文件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitOtherAuth(
            Long userId,
            String bankCardId,
            MultipartFile propertyFile,
            MultipartFile carFile,
            MultipartFile employmentFile,
            MultipartFile salaryFile,
            MultipartFile socialSecurityFile,
            MultipartFile creditReportFile) {

        UserCert userCert = userCertMapper.selectByUserId(userId);

        // 1. 验证银行卡号
        if (userCert.getBankCardId() == null) {
            if (bankCardId == null || bankCardId.trim().isEmpty()) {
                throw new BusinessException(400, "银行卡号不能为空");
            }
            if (!BankCardUtils.isValid(bankCardId)) {
                throw new BusinessException(400, "银行卡号格式无效");
            }
        }

        // 2. 存储所有文件（任一失败则整体回滚）
        log.info("begin store all auth files:...");
        String propertyPath = storeRequiredFile(propertyFile, "property", userId, fileStorageConfig.getPaths().getPropertyProof());
        String carPath = storeRequiredFile(carFile, "car", userId, fileStorageConfig.getPaths().getCarProof());
        String empPath = storeRequiredFile(employmentFile, "employment", userId, fileStorageConfig.getPaths().getEmploymentProof());
        String salPath = storeRequiredFile(salaryFile, "salary", userId, fileStorageConfig.getPaths().getSalaryProof());
        String ssPath = storeRequiredFile(socialSecurityFile, "social", userId, fileStorageConfig.getPaths().getSocialSecurity());
        String crPath = storeRequiredFile(creditReportFile, "credit", userId, fileStorageConfig.getPaths().getCreditReport());
        log.info("all auth files are stored successfully: propertyPath={}, carPath={}, empPath={}, salPath={}, ssPath={}, crPath={}", 
            propertyPath, carPath, empPath, salPath, ssPath, crPath);

        // 3. 处理各类认证业务逻辑
        handleImmovablesCert(userCert, propertyPath, carPath);
        log.info("immovable cert is handled successfully: immovableCertId={}", userCert.getImmovableCertId());

        handleWorkCert(userCert, empPath, salPath);
        log.info("work cert is handled successfully: workCertId={}", userCert.getWorkCertId());

        handleTriCert(userCert, ssPath, crPath);
        log.info("tri cert is handled successfully: triCertId={}", userCert.getTriCertId());

        // 4. 更新主表中的银行卡号和信用分
        userCert.setBankCardId(bankCardId);
        userCert.setCreditScore(calScore(userId));
        userCertMapper.update(userCert);
        
        log.info("all other auth materials are submitted successfully: userId={}", userId);
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
        return new GetCertResponse(userCert, workCert, triCert, immovablesCert);
    }

    // 个人征信认证
    // @Override
    // public void creditAuth(){ 

    // }

    // 工具方法
    // 存储文件
    private String storeRequiredFile(MultipartFile file, String type, Long userId, String basePath) {
        if (file == null || file.isEmpty()) {
            log.warn("file is empty: type={}, userId={}", type, userId);
            return null;
        }
        log.info("begin store file: type={}, userId={}, originalFilename={}, size={}", 
            type, userId, file.getOriginalFilename(), file.getSize());
        String storedPath = fileStorageService.storeFile(file, type, userId, basePath);
        log.info("The file is stored successfully: type={}, userId={}, storedPath={}", type, userId, storedPath);
        return storedPath;
    }

    // 处理不动产认证
    private void handleImmovablesCert(UserCert userCert, String propertyPath, String carPath) {
        if (propertyPath == null && carPath == null) {
            return; // 无有效数据，跳过处理
        }
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

    // 处理工作认证
    private void handleWorkCert(UserCert userCert, String empPath, String salPath) {
        if (empPath == null && salPath == null) {
            return; // 无有效数据，跳过处理
        }

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

    // 处理第三方认证
    private void handleTriCert(UserCert userCert, String ssPath, String crPath) {
        if (ssPath == null && crPath == null) {
            return; // 无有效数据，跳过处理
        }

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
        // 检查filename是否已经是完整的URL路径（包含/uploads/）
        if (filename.startsWith("/uploads/")) {
            return filename;
        }
        // 确保路径不以 / 开头或结尾，避免双斜杠
        String cleanBase = relativeBasePath.replaceAll("/+$", "").replaceAll("^/+", "");
        String cleanFile = filename.replaceAll("^/+", "");
        return "/uploads/" + cleanBase + "/" + cleanFile;
    }

    // 根据 workCertId 查询工作认证信息
    @Override
    public WorkCert getWorkCertById(Integer workCertId) {
        WorkCert workCert = workCertMapper.selectById(workCertId);
        if (workCert != null) {
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
        return workCert;
    }

    // 根据 triCertId 查询第三方认证信息
    @Override
    public TriCert getTriCertById(Integer triCertId) {
        TriCert triCert = triCertMapper.selectById(triCertId);
        if (triCert != null) {
            // 转换路径为公开 URL
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
        return triCert;
    }

    // 根据 immovableCertId 查询不动产认证信息
    @Override
    public ImmovablesCert getImmovablesCertById(Integer immovableCertId) {
        ImmovablesCert immovablesCert = immovablesCertMapper.selectById(immovableCertId);
        if (immovablesCert != null) {
            // 转换路径为公开 URL
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
        return immovablesCert;
    }
}
