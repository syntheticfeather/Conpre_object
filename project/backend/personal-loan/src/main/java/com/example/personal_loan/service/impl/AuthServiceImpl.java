package com.example.personal_loan.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.personal_loan.aop.RedisLocked;
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
     * @param idCard 身份证号 必须填写
     * @param realName 真实姓名 必须填写
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @RedisLocked(key = "'lock:auth:basic:' + #p0")
    public void submitBasicAuth(Long userId, String idCard, String realName) {
        UserCert userCert = userCertMapper.selectByUserId(userId);

        // 1. 参数校验
        if (idCard == null || idCard.trim().isEmpty()) {
            throw new BusinessException(400, "身份证号不能为空");
        }
        if (!IdCardUtils.isValid(idCard)) {
            throw new BusinessException(400, "身份证号格式无效");
        }
        
       
        if (realName == null || realName.trim().isEmpty()) {
            throw new BusinessException(400, "真实姓名不能为空");
        }
        if (!realName.matches("^[\\u4e00-\\u9fa5\\u3400-\\u4dbf]+$")) {
            throw new BusinessException(400, "请填写正确的姓名");
        }
        if (realName.length() < 2 || realName.length() > 4) {
            throw new BusinessException(400, "请填写正确的姓名");
        }
        // 校验姓名是否与身份证号匹配，需要接入第三方API，暂不实现
        
        // 2. 更新主表中的信息
        userCert.setIdCard(idCard);
        userCert.setRealName(realName);
        userCert.setCreditScore(calScore(userId));
        userCertMapper.update(userCert);
        
        log.info("Basic auth is submitted successfully: userId={}", userId);
    }

    /**
     * 提交或更新其他认证信息，包括银行卡及各类证明材料
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
    @RedisLocked(key = "'lock:auth:other:' + #p0")
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
        if (bankCardId != null && !BankCardUtils.isValid(bankCardId)) {
            throw new BusinessException(400, "银行卡号格式无效");
        }

        // 2. 存储所有文件（任一失败则整体回滚）
        log.info("Begin store all auth files:...");
        String propertyPath = fileStorageService.storeFile(propertyFile, "property", userId, fileStorageConfig.getPaths().getPropertyProof());
        String carPath = fileStorageService.storeFile(carFile, "car", userId, fileStorageConfig.getPaths().getCarProof());
        String empPath = fileStorageService.storeFile(employmentFile, "employment", userId, fileStorageConfig.getPaths().getEmploymentProof());
        String salPath = fileStorageService.storeFile(salaryFile, "salary", userId, fileStorageConfig.getPaths().getSalaryProof());
        String ssPath = fileStorageService.storeFile(socialSecurityFile, "social", userId, fileStorageConfig.getPaths().getSocialSecurity());
        String crPath = fileStorageService.storeFile(creditReportFile, "credit", userId, fileStorageConfig.getPaths().getCreditReport());
        log.info("all auth files are stored successfully: propertyPath={}, carPath={}, empPath={}, salPath={}, ssPath={}, crPath={}", 
            propertyPath, carPath, empPath, salPath, ssPath, crPath);

        // 3. 处理各类认证业务逻辑
        handleImmovablesCert(userCert, propertyPath, carPath);
        log.info("immovable cert is handled successfully: immovableCertId={}", userCert.getImmovableCertId());

        handleWorkCert(userCert, empPath, salPath);
        log.info("work cert is handled successfully: workCertId={}", userCert.getWorkCertId());

        handleTriCert(userCert, ssPath, crPath);
        log.info("tri cert is handled successfully: triCertId={}", userCert.getTriCertId());

        // 4. 更新主表中的银行卡号
        if(bankCardId != null){
            userCert.setBankCardId(bankCardId);
        }
        userCertMapper.update(userCert);
        // 计算信誉分
        userCert.setCreditScore(calScore(userId));
        userCertMapper.updateCreditScore(userId, userCert.getCreditScore());
        
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
        userCert.setBankCardId(userCert.getBankCardId());
        
        WorkCert workCert = null;
        if (userCert.getWorkCertId() != null) {
            workCert = workCertMapper.selectById(userCert.getWorkCertId());
            // 转换路径为公开 URL
            if (workCert.getEmploymentCertPath() != null) {
                workCert.setEmploymentCertPath("/uploads/"+workCert.getEmploymentCertPath());
            }
            if (workCert.getSalaryCertPath() != null) {
                workCert.setSalaryCertPath("/uploads/"+workCert.getSalaryCertPath());
            }
        }
        TriCert triCert = null;
        if (userCert.getTriCertId() != null) {
            triCert = triCertMapper.selectById(userCert.getTriCertId());
            if (triCert.getSocialSecurityPath() != null) {
                triCert.setSocialSecurityPath("/uploads/"+triCert.getSocialSecurityPath());
            }
            if (triCert.getCreditReportPath() != null) {
                triCert.setCreditReportPath("/uploads/"+triCert.getCreditReportPath());
            }
        }
        ImmovablesCert immovablesCert = null;
        if (userCert.getImmovableCertId() != null) {
            immovablesCert = immovablesCertMapper.selectById(userCert.getImmovableCertId());
            if (immovablesCert.getPropertyCertPath() != null) {
                immovablesCert.setPropertyCertPath("/uploads/"+immovablesCert.getPropertyCertPath());
            }
            if (immovablesCert.getCarCertPath() != null) {
                immovablesCert.setCarCertPath("/uploads/"+immovablesCert.getCarCertPath());
            }
        }
        return new GetCertResponse(userCert, workCert, triCert, immovablesCert);
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

    // 管理员根据 workCertId 查询工作认证信息
    @Override
    public WorkCert getWorkCertById(Integer workCertId) {
        WorkCert workCert = workCertMapper.selectById(workCertId);
        if (workCert != null) {
            // 转换路径为公开 URL
            if (workCert.getEmploymentCertPath() != null) {
                workCert.setEmploymentCertPath("/uploads/"+workCert.getEmploymentCertPath());
            }
            if (workCert.getSalaryCertPath() != null) {
                workCert.setSalaryCertPath("/uploads/"+workCert.getSalaryCertPath());
            }
        }
        return workCert;
    }

    // 管理员根据 triCertId 查询第三方认证信息
    @Override
    public TriCert getTriCertById(Integer triCertId) {
        TriCert triCert = triCertMapper.selectById(triCertId);
        if (triCert != null) {
            // 转换路径为公开 URL
            if (triCert.getSocialSecurityPath() != null) {
                triCert.setSocialSecurityPath("/uploads/"+triCert.getSocialSecurityPath());
            }
            if (triCert.getCreditReportPath() != null) {
                triCert.setCreditReportPath("/uploads/"+triCert.getCreditReportPath());
            }
        }
        return triCert;
    }

    // 管理员根据 immovableCertId 查询不动产认证信息
    @Override
    public ImmovablesCert getImmovablesCertById(Integer immovableCertId) {
        ImmovablesCert immovablesCert = immovablesCertMapper.selectById(immovableCertId);
        if (immovablesCert != null) {
            // 转换路径为公开 URL
            if (immovablesCert.getPropertyCertPath() != null) {
                immovablesCert.setPropertyCertPath("/uploads/"+immovablesCert.getPropertyCertPath());
            }
            if (immovablesCert.getCarCertPath() != null) {
                immovablesCert.setCarCertPath("/uploads/"+immovablesCert.getCarCertPath());
            }
        }
        return immovablesCert;
    }
}
