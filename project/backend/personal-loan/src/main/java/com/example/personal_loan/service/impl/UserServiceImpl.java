package com.example.personal_loan.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.personal_loan.config.FileStorageConfig;
import com.example.personal_loan.dto.BlackListDto;
import com.example.personal_loan.dto.LoginRequest;
import com.example.personal_loan.dto.LoginResponse;
import com.example.personal_loan.dto.RegisterRequest;
import com.example.personal_loan.dto.RegisterResponse;
import com.example.personal_loan.dto.UserDetailResponse;
import com.example.personal_loan.dto.UserListResponse;
import com.example.personal_loan.dto.UserSearchDto;
import com.example.personal_loan.dto.UserSelfResponse;
import com.example.personal_loan.entity.BlackUser;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.entity.User;
import com.example.personal_loan.entity.UserCert;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.exception.InvalidCredentialsException;
import com.example.personal_loan.mapper.BlackListMapper;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.mapper.UserCertMapper;
import com.example.personal_loan.mapper.UserMapper;
import com.example.personal_loan.service.LocalFileStorageService;
import com.example.personal_loan.service.UserService;
import com.example.personal_loan.utils.CalculateUtil;
import com.example.personal_loan.utils.JwtUtil;
import com.example.personal_loan.utils.RedisUtil;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BlackListMapper blackListMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserCertMapper userCertMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisUtil RedisUtil;

    @Autowired
    private FileStorageConfig fileStorageConfig;

    @Autowired
    private LocalFileStorageService fileStorageService;

    // 文件大小限制：5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /*
    * 用户认证（登录注册）
    */
    @Override
    public LoginResponse login(LoginRequest request) {

        User user = userMapper.findByPhone(request.getPhone());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "手机号或密码错误");
        }

        String token = jwtUtil.generateAccessToken(user.getPhone(), user.getId().toString());

        String refreshToken = jwtUtil.generateRefreshToken(user.getId().toString());
        // 存储在redis中，过期时间7天
        log.info("token:" + token);
        RedisUtil.set(RedisUtil.JWT_REFRESH_CACHE_TOKEN_PREFIX_STRING + user.getId(), refreshToken,7,TimeUnit.DAYS);

        return new LoginResponse(token, refreshToken);

    }

    /*
    * 刷新access token
    */
    @Override
    public Map<String, String> refreshToken(Map<String, String> refreshToken) {
        if(refreshToken == null|| refreshToken.isEmpty()) {
            log.info("refresh token is empty, please login again");
            throw new InvalidCredentialsException("refresh token 为空, 需重新登录");
        }
        if(!jwtUtil.validateToken(refreshToken.get("refreshToken"))) {
            log.info("refresh token is invalid, please login again");
            throw new InvalidCredentialsException("refresh token 无效, 需重新登录");
        }
        // 从refreshToken中提取用户ID
        Long id = jwtUtil.getUserIdFromRefreshToken(refreshToken.get("refreshToken"));
        String cachedToken = RedisUtil.get(RedisUtil.JWT_REFRESH_CACHE_TOKEN_PREFIX_STRING + id, String.class);
        if (cachedToken == null || !cachedToken.equals(refreshToken.get("refreshToken"))) {
            log.info("refresh token error, please login again");
            throw new InvalidCredentialsException("refresh token 无效, 需重新登录");
        }
        String phone = userMapper.findById(id).getPhone();
        String newAccessToken = jwtUtil.generateAccessToken(phone, id.toString());

        // 可选：检查用户是否被禁用、加入黑名单等
        log.info("Refresh access token successfully" );
        return Map.of(
            "token", newAccessToken,
            "refreshToken", cachedToken
        );
    }

    /*
    * 用户注册
    */
    @Override
    public RegisterResponse userRegister(RegisterRequest request) {
        //检验手机号是否已存在
        if (userMapper.findByPhone(request.getPhone()) != null) {
            throw new BusinessException(400, "手机号已存在");
        }
        request.setPassword(passwordEncoder.encode(request.getPassword()));
        User user = new User(request.getName(), request.getPassword(), request.getPhone());
        user.setRole(0);                      // 用户的注册，默认权限为0
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        // 创建认证记录
        UserCert cert = new UserCert();
        cert.setUserId(user.getId()); // 主键
        // 其他字段（idCard, bankCardId, workCertId...）留 null
        userCertMapper.insert(cert);

        return new RegisterResponse(user.getId(), user.getUserName(), user.getCreateTime());
    }

    /*
    * 删除用户
    */
    @Override
    public void deleteUser(Long id) {
        userMapper.delete(id);
    }

    /*
    * 批量删除用户
    */
    @Override
    @Transactional // 可选：根据业务决定是否加事务
    public void deleteUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            userMapper.delete(id);
        }
    }

    /*
    * 更新用户
    */
    @Override
    public User updateUser(Long id, User user) {

        User old = userMapper.findById(user.getId());

        // 设置id,更安全?
        user.setId(id);

        // 如果手机号被修改，校验唯一性
        if (user.getPhone() != null && !user.getPhone().equals(old.getPhone())) {
            if (userMapper.findByPhone(user.getPhone()) != null) {
                throw new BusinessException(400, "手机号已存在");
            } else {
                old.setPhone(user.getPhone());   // 更新手机号
            }
        }
        
        // 更新其他字段
        if (user.getPassword() != null) {
            old.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (user.getUserName() != null) {
            old.setUserName(user.getUserName());
        }
        if (user.getRole() != null) {
            old.setRole(user.getRole());
        }
        old.setUpdateTime(LocalDateTime.now());
        userMapper.update(old);
        return old;
    }

    /*
    * 根据ID查询用户
    */
    @Override
    public User getUserById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(404, "该用户不存在");
        }
        return user;
    }


    /*
    * 用户使用
    */

    // 查看个人信息
    @Override
    @Transactional
    public UserSelfResponse getUserSelfInfo(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 权限校验
        if(!user.getRole().equals(0)){
            throw new BusinessException(403,"无权查看个人信息");
        }

        return new UserSelfResponse(
                user.getId(),
                user.getUserName(),
                "/uploads/"+user.getAvatar()
        );
    }

    // 修改个人信息（仅昵称）
    @Override
    @Transactional
    public void updateUserSelfInfo(String newUserName, Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!id.equals(user.getId())) {
            throw new BusinessException(400, "只能更新自己的信息");
        }
        if(newUserName.length() > 20 || newUserName.length() < 2){
            throw new BusinessException(400,"用户名长度应在2-20之间");
        }
        // 仅更新允许的字段,用户名
        if (StringUtils.isBlank(newUserName)) {
            throw new BusinessException(400, "用户名不能为空或空格");
        }
        
        user.setUserName(newUserName.trim());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.update(user);     
    }

    // 设置头像
    @Override
    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file){
        // 1. 校验文件非空
        if (file.isEmpty()) {
            throw new BusinessException(400, "上传的头像为空");
        }
        // 2. 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "头像不能超过 5MB");
        }
        // 3. 校验 Content-Type（防止伪装图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(400, "仅支持图片格式");
        }

        // 4. 获取用户信息，旧头像路径
        User user = userMapper.findById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        String oldAvatarPath = user.getAvatar();

        // 5. 获取相对路径,存新头像
        String relativePath = fileStorageConfig.getPaths().getAvatar(); // e.g. "avatars"
        String newAvatarUrl = fileStorageService.storeFile(file,"avatar", userId, relativePath);

        // 6. 更新数据库
        user.setAvatar(newAvatarUrl);
        userMapper.update(user);
        if (oldAvatarPath != null && !oldAvatarPath.isEmpty() && !oldAvatarPath.equals(newAvatarUrl)) {
            try {
                // 调用你的文件服务删除旧文件
                fileStorageService.deleteFile(oldAvatarPath); 
            } catch (Exception e) {
                // 记录日志，但不要让异常抛出中断主流程
                log.error("Delete old avatar failed: {}", oldAvatarPath, e);
            }
        }

        log.info("User {} updates avatar to {} successfully", userId, newAvatarUrl);
        return "/uploads/"+newAvatarUrl;
    }

    /*
    * 管理员使用
     */
    @Override
    public List<UserSearchDto> searchUsersByCreditScore(String expr) {
        CreditExpr parsed = parseCreditExpression(expr.trim());
        if (parsed == null) {
            throw new BusinessException(400, "无效的搜索");
        }

        return userMapper.selectUsersByCreditScore(parsed.operator, parsed.value);
    }

    // 用户管理列表
    @Override
    public List<UserListResponse> adminGetAllUsersWithStats() {
        // 查询所有用户（带用户名）
        List<User> users = userMapper.findAll();

        // 遍历每个用户，计算统计信息
        return users.stream().map(user -> {
            List<Order> orders = orderMapper.selectAllByUserId(user.getId());
            UserCert userCert = userCertMapper.selectByUserId(user.getId());

            Integer transactionCount = CalculateUtil.getTotalTransactionCount(orders);
            BigDecimal totalLoanAmount = CalculateUtil.getTotalLoanAmount(orders);

            // 判断用户有无借贷, 逾期状态
            // 当成字段存储
            String loanStatus;
            if (orders.isEmpty()) {
                loanStatus = "无借贷";
            } else if (orders.stream().anyMatch(order -> 
                OrderStatus.已逾期.equals(order.getStatus()) && 
                order.getOverdueDays() != null && 
                order.getOverdueDays() > 0
            )) {
                loanStatus = "逾期";
            }else {
                loanStatus = "正常";
            }
            return new UserListResponse(
                    user.getId(),
                    user.getUserName(),
                    userCert.getCreditScore(),
                    loanStatus,
                    transactionCount,
                    totalLoanAmount
            );
        }).collect(Collectors.toList());
    }

    // 加入黑名单
    @Override
    public void addToBlackList(Long adminId, Long userId, int blackLevel) {
        // 权限校验
        User admin = userMapper.findById(adminId);
        if(!admin.getRole().equals(1)){
            throw new BusinessException(403,"权限不足");
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if(adminId.equals(userId)){
            throw new BusinessException(403,"无权添加到黑名单");
        }

        // 检查是否已在黑名单
        if (blackListMapper.selectActiveByUserId(userId) != null) {
            throw new BusinessException(400, "用户已在黑名单中");
        }

        // 检查level范围
        if (blackLevel < 1 || blackLevel > 3) {
            throw new BusinessException(400, "黑名单级别必须在1-3之间");
        }

        BlackUser blackUser = new BlackUser();
        blackUser.setUserId(userId);
        blackUser.setBlackLevel(blackLevel);
        blackUser.setCreateTime(LocalDateTime.now());
        blackUser.setUpdateTime(LocalDateTime.now());
        blackUser.setRemoveTime(null);
        blackListMapper.insert(blackUser);
    }

    // 移除黑名单
    @Override
    public void removeFromBlackList(Long adminId, Long userId){
        // 权限校验
        User admin = userMapper.findById(adminId);
        if(!admin.getRole().equals(1)){
            throw new BusinessException(403,"权限不足");
        }

        BlackUser blackUser = blackListMapper.selectActiveByUserId(userId);
        if (blackUser == null) {
            throw new BusinessException(404,"用户不在黑名单中");
        }
        blackUser.setRemoveTime(LocalDateTime.now());
        blackUser.setUpdateTime(LocalDateTime.now());
        blackListMapper.update(blackUser);
    }

    // 黑名单列表
    @Override
    public List<BlackListDto> getBlackList(Long adminId) {
        // 权限校验
        User admin = userMapper.findById(adminId);
        if(!admin.getRole().equals(1)){
            throw new BusinessException(403,"权限不足");
        }
        
        List<BlackListDto> blackUsers = blackListMapper.selectAll();
        return blackUsers;
    }

    // 用户管理详情
    @Override
    public UserDetailResponse adminGetUser(Long userId) {
        return userMapper.selectUserDetail(userId);
    }

    // 内部类
    private static class CreditExpr {

        private final String operator; // ">", ">=", "=", "<", "<="
        private final Integer value;

        public CreditExpr(String operator, Integer value) {
            this.operator = operator;
            this.value = value;
        }
    }

    private CreditExpr parseCreditExpression(String expr) {

        if (expr == null || expr.isEmpty()) {
            return null;
        }

        String[] ops = {">=", "<=", ">", "<", "="};
        for (String op : ops) {
            if (expr.startsWith(op)) {
                String numPart = expr.substring(op.length()).trim();
                try {
                    Integer val = Integer.valueOf(numPart);
                    // 可选：校验范围（根据实际业务）
                    return new CreditExpr(op, val);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
