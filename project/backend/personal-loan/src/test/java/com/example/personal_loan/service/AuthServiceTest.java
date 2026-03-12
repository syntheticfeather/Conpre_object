package com.example.personal_loan.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import com.example.personal_loan.service.impl.AuthServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserCertMapper userCertMapper;

    @Mock
    private WorkCertMapper workCertMapper;

    @Mock
    private TriCertMapper triCertMapper;

    @Mock
    private ImmovablesCertMapper immovablesCertMapper;

    @Mock
    private LocalFileStorageService fileStorageService;

    @Mock
    private FileStorageConfig fileStorageConfig;

    @InjectMocks
    private AuthServiceImpl authService;

    private UserCert userCert;
    private WorkCert workCert;
    private TriCert triCert;
    private ImmovablesCert immovablesCert;
    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        userCert = new UserCert();
        userCert.setUserId(1L);
        userCert.setCreditScore(0);

        workCert = new WorkCert();
        workCert.setWorkCertId(1);
        workCert.setEmploymentCertPath("employment.pdf");
        workCert.setSalaryCertPath("salary.pdf");

        triCert = new TriCert();
        triCert.setTriCertId(1);
        triCert.setSocialSecurityPath("social.pdf");
        triCert.setCreditReportPath("credit.pdf");

        immovablesCert = new ImmovablesCert();
        immovablesCert.setImmovableCertId(1);
        immovablesCert.setPropertyCertPath("property.pdf");
        immovablesCert.setCarCertPath("car.pdf");

        mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.pdf");
        when(mockFile.getSize()).thenReturn(1024L);

        FileStorageConfig.Paths paths = new FileStorageConfig.Paths();
        paths.setAvatar("avatars");
        paths.setContract("contracts");
        paths.setCarProof("car");
        paths.setPropertyProof("property");
        paths.setCreditReport("credit");
        paths.setSocialSecurity("social");
        paths.setEmploymentProof("employment");
        paths.setSalaryProof("salary");
        when(fileStorageConfig.getPaths()).thenReturn(paths);
    }

    @Test
    void testSubmitAllAuth_InvalidIdCard() {
        BusinessException exception = assertThrows(BusinessException.class, () -> 
            authService.submitAllAuth(
                1L, "", "6222021234567890",
                mockFile, mockFile, mockFile, mockFile, mockFile, mockFile
            )
        );
        assertEquals(400, exception.getCode());
    }

    @Test
    void testSubmitAllAuth_InvalidBankCard() {
        BusinessException exception = assertThrows(BusinessException.class, () -> 
            authService.submitAllAuth(
                1L, "123456789012345678", "",
                mockFile, mockFile, mockFile, mockFile, mockFile, mockFile
            )
        );
        assertEquals(400, exception.getCode());
    }

    @Test
    void testCalScore_AllCertsPresent() {
        userCert.setWorkCertId(1);
        userCert.setTriCertId(1);
        userCert.setImmovableCertId(1);

        when(userCertMapper.selectByUserId(1L)).thenReturn(userCert);
        when(workCertMapper.selectById(1)).thenReturn(workCert);
        when(triCertMapper.selectById(1)).thenReturn(triCert);
        when(immovablesCertMapper.selectById(1)).thenReturn(immovablesCert);

        int score = authService.calScore(1L);
        assertTrue(score > 0);
    }

    @Test
    void testCalScore_NoCertsPresent() {
        when(userCertMapper.selectByUserId(1L)).thenReturn(userCert);

        int score = authService.calScore(1L);
        assertEquals(0, score);
    }

    @Test
    void testCalScore_PartialCertsPresent() {
        userCert.setWorkCertId(1);
        workCert.setEmploymentCertPath("employment.pdf");
        workCert.setSalaryCertPath(null);

        when(userCertMapper.selectByUserId(1L)).thenReturn(userCert);
        when(workCertMapper.selectById(1)).thenReturn(workCert);

        int score = authService.calScore(1L);
        assertTrue(score >= 0);
    }

    @Test
    void testCalScore_UserCertNull() {
        when(userCertMapper.selectByUserId(1L)).thenReturn(null);

        int score = authService.calScore(1L);
        assertEquals(0, score);
    }

    @Test
    void testGetCert_NoCertsPresent() {
        when(userCertMapper.selectByUserId(1L)).thenReturn(userCert);

        var response = authService.getCert(1L);

        assertNotNull(response);
        assertNotNull(response.getUserCert());
        assertNull(response.getWorkCert());
        assertNull(response.getTriCert());
        assertNull(response.getImmovablesCert());
    }

    @Test
    void testGetWorkCertById() {
        when(workCertMapper.selectById(1)).thenReturn(workCert);

        WorkCert result = authService.getWorkCertById(1);

        assertNotNull(result);
        assertEquals(1, result.getWorkCertId());
    }

    @Test
    void testGetTriCertById() {
        when(triCertMapper.selectById(1)).thenReturn(triCert);

        TriCert result = authService.getTriCertById(1);

        assertNotNull(result);
        assertEquals(1, result.getTriCertId());
    }

    @Test
    void testGetImmovablesCertById() {
        when(immovablesCertMapper.selectById(1)).thenReturn(immovablesCert);

        ImmovablesCert result = authService.getImmovablesCertById(1);

        assertNotNull(result);
        assertEquals(1, result.getImmovableCertId());
    }

    @Test
    void testGetWorkCertById_Null() {
        when(workCertMapper.selectById(1)).thenReturn(null);

        WorkCert result = authService.getWorkCertById(1);

        assertNull(result);
    }

    @Test
    void testGetTriCertById_Null() {
        when(triCertMapper.selectById(1)).thenReturn(null);

        TriCert result = authService.getTriCertById(1);

        assertNull(result);
    }

    @Test
    void testGetImmovablesCertById_Null() {
        when(immovablesCertMapper.selectById(1)).thenReturn(null);

        ImmovablesCert result = authService.getImmovablesCertById(1);

        assertNull(result);
    }
}
