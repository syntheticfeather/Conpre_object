package com.example.personal_loan.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.personal_loan.config.FileStorageConfig;
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


    // 不动产认证
    @Override
    @Transactional
    public void immovablesAuth(Long userId, MultipartFile propertyFile, MultipartFile carFile){

        UserCert cert = userCertMapper.selectByUserId(userId);

        // 房产证
        String propertyPath = null;
        if (propertyFile != null && !propertyFile.isEmpty()) {
            propertyPath = fileStorageService.storeFile(propertyFile,"property", userId, fileStorageConfig.getPaths().getPropertyProof());
        }else{
            throw new BusinessException(400,"上传的房产证图片为空");
        }

        // 车产证明
        String carPath = null;
        if (carFile != null && !carFile.isEmpty()) {
            carPath = fileStorageService.storeFile(carFile,"car", userId, fileStorageConfig.getPaths().getCarProof());
        }else{
            throw new BusinessException(400,"上传的车产证图片为空");
        }

        // 判断是插入还是更新
        Integer existingImmoCertId = cert.getImmovableCertId();

        if (existingImmoCertId == null) {
            // 插入新记录
            ImmovablesCert newImmoCert = new ImmovablesCert();
            newImmoCert.setPropertyCertPath(propertyPath);
            newImmoCert.setCarCertPath(carPath);

            // totalValue 后续计算，此处暂不设

            immovablesCertMapper.insert(newImmoCert); 

            // 回填 ID 到主表
            cert.setImmovableCertId(newImmoCert.getImmovableCertId());
            userCertMapper.update(cert); // 只更新该字段，更安全
        } else {
            // 更新已有记录
            ImmovablesCert updateImmoCert = new ImmovablesCert();
            updateImmoCert.setImmovableCertId(existingImmoCertId);
            updateImmoCert.setPropertyCertPath(propertyPath);
            updateImmoCert.setCarCertPath(carPath);

            immovablesCertMapper.update(updateImmoCert);
        }
    } 

    
    // 工作认证
    @Override
    @Transactional
    public void occupationAuth(Long userId, MultipartFile employmentFile, MultipartFile salaryFile){

        UserCert cert = userCertMapper.selectByUserId(userId);
        
        String empPath = fileStorageService.storeFile(employmentFile, "employment", userId, fileStorageConfig.getPaths().getEmploymentProof());
        String salPath = fileStorageService.storeFile(salaryFile, "salary", userId, fileStorageConfig.getPaths().getSalaryProof());

        Integer existingWorkCertId = cert.getWorkCertId();
        if (existingWorkCertId == null) {
            // 插入新工作证明
            WorkCert newWorkCert = new WorkCert();
            newWorkCert.setEmploymentCertPath(empPath);
            newWorkCert.setSalaryCertPath(salPath);
            workCertMapper.insert(newWorkCert); // ID 自动回填

            // 更新 work_cert_id
            cert.setWorkCertId(newWorkCert.getWorkCertId());
            userCertMapper.update(cert); 
        } else {
            // 更新已有记录
            WorkCert updateWorkCert = new WorkCert();
            updateWorkCert.setWorkCertId(existingWorkCertId);
            updateWorkCert.setEmploymentCertPath(empPath);
            updateWorkCert.setSalaryCertPath(salPath);
            workCertMapper.update(updateWorkCert);
        }
    }
    
    // 第三方信用分认证
    @Override
    @Transactional
    public void thirdPartyAuth(Long userId, MultipartFile socialSecurityFile, MultipartFile creditReportFile){
        UserCert cert = userCertMapper.selectByUserId(userId);

        String ssPath = fileStorageService.storeFile(socialSecurityFile, "social", userId, fileStorageConfig.getPaths().getSocialSecurity());
        String crPath = fileStorageService.storeFile(creditReportFile, "credit", userId, fileStorageConfig.getPaths().getCreditReport());

        TriCert triCert = new TriCert();
        triCert.setTriCertId(cert.getTriCertId());
        triCert.setSocialSecurityPath(ssPath);
        triCert.setCreditReportPath(crPath);

        if (triCert.getTriCertId() == null) {
            triCertMapper.insert(triCert);
            cert.setTriCertId(triCert.getTriCertId());
            userCertMapper.update(cert);
        } else {
            triCertMapper.update(triCert);
        }
    }

    // 银行卡
    @Override
    public void bankAccountAuth(Long userId, String bankCardId){ 
        UserCert cert = userCertMapper.selectByUserId(userId);
        cert.setBankCardId(bankCardId);
        userCertMapper.update(cert);
    }

    // 身份证
    @Override
    public void idCardAuth(Long userId, String idCard){
        UserCert cert = userCertMapper.selectByUserId(userId);
        cert.setIdCard(idCard);
        userCertMapper.update(cert);
    }

    // 计算贷款分数
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
    
    @Override
    public int getCert(Long userId){ 
        return 0;
    }

    // 个人征信认证
    // @Override
    // public void creditAuth(){ 

    // }
}
