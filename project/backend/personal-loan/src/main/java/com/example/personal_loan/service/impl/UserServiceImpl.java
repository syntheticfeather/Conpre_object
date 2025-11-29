package com.example.personal_loan.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.dto.AdminGetUserResponse;
import com.example.personal_loan.dto.AdminUserListResponse;
import com.example.personal_loan.dto.BlackListDto;
import com.example.personal_loan.dto.LoginRequest;
import com.example.personal_loan.dto.LoginResponse;
import com.example.personal_loan.dto.RegisterRequest;
import com.example.personal_loan.dto.RegisterResponse;
import com.example.personal_loan.dto.UserSearchDto;
import com.example.personal_loan.dto.UserSelfResponse;
import com.example.personal_loan.dto.UserUpdateRequest;
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
import com.example.personal_loan.service.UserService;
import com.example.personal_loan.utils.CalculateUtil;
import com.example.personal_loan.utils.JwtUtil;
import com.example.personal_loan.utils.RedisUtil;
import static com.example.personal_loan.utils.RedisUtil.JWT_REFRESH_CACHE_TOKEN_PREFIX_STRING;

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
        // 后期使用，存储在redis中?
        log.info("token:" + token);
        RedisUtil.set(RedisUtil.JWT_REFRESH_CACHE_TOKEN_PREFIX_STRING + user.getId(), refreshToken);

        return new LoginResponse(token);

    }

    @Override
    public String refreshToken(Long id) {
        String refreshToken = RedisUtil.get(JWT_REFRESH_CACHE_TOKEN_PREFIX_STRING + id, String.class);
        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            log.info("refresh token 无效, 需重新登录");
            throw new InvalidCredentialsException("refresh token 无效, 需重新登录");
        }
        String phone = userMapper.findById(id).getPhone();
        String newAccessToken = jwtUtil.generateAccessToken(phone, id.toString());

        // 可选：检查用户是否被禁用、加入黑名单等
        // 生成新的 access token（不发新的 refresh token）
        log.info("已刷新 access token" + newAccessToken);
        return newAccessToken;
    }

    @Override
    public RegisterResponse userRegister(RegisterRequest request) {
        request.setPassword(passwordEncoder.encode(request.getPassword()));
        User user = new User(request.getName(), request.getPassword(), null, request.getPhone());
        user.setRole(0);    // 用户的注册，默认权限为0
        User newUser = addUser(user);
        return new RegisterResponse(newUser.getId(), newUser.getUserName(), newUser.getCreateTime());
    }

    @Override
    public User addUser(User user) {

        if (userMapper.findByPhone(user.getPhone()) != null) {
            throw new BusinessException(400, "该手机号已被注册");
        }

        if (userMapper.findByIdCard(user.getIdCard()) != null) {
            throw new BusinessException(400, "身份证号已被注册");
        }

        userMapper.insert(user);
        user.setCreateTime(LocalDateTime.now());
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        userMapper.delete(id);
    }

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
        // 校验身份证号唯一性
        if (user.getIdCard() != null && !user.getIdCard().equals(old.getIdCard())) {
            if (userMapper.findByIdCardExcludeId(user.getIdCard(), user.getId()) != 0) {
                throw new BusinessException(400, "身份证号已存在");
            } else {
                old.setIdCard(user.getIdCard());  // 更新身份证号
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

        userMapper.update(old);
        return old;
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(404, "该用户不存在");
        }
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        return userMapper.findAll();
    }


    /*
    * 用户使用
     */
    @Override
    @Transactional
    public UserSelfResponse getUserSelfInfo(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        return new UserSelfResponse(
                user.getId(),
                user.getUserName(),
                user.getAvatar()
        );
    }

    @Override
    @Transactional
    public UserSelfResponse updateUserSelfInfo(UserUpdateRequest request, Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!id.equals(user.getId())) {
            throw new BusinessException(400, "只能更新自己的信息");
        }
        // 仅更新允许的字段,用户名和头像
        if (request.getUserName() != null) {
            user.setUserName(request.getUserName());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        user.setUpdateTime(LocalDateTime.now());

        userMapper.update(user);
        return new UserSelfResponse(
                id,
                user.getUserName(),
                user.getAvatar()
        );
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

    @Override
    public List<AdminUserListResponse> adminGetAllUsersWithStats() {
        // 查询所有用户（带用户名）
        List<User> users = userMapper.findAll();

        // 遍历每个用户，计算统计信息
        return users.stream().map(user -> {
            List<Order> orders = orderMapper.selectAllByUserId(user.getId());

            Integer transactionCount = CalculateUtil.getTotalTransactionCount(orders);
            BigDecimal totalLoanAmount = CalculateUtil.getTotalLoanAmount(orders);
            BigDecimal totalRepaidAmount = CalculateUtil.getTotalRepaidAmount(orders);

            // 判断用户有无借贷, 逾期状态
            String loanStatus;
            if (orders.isEmpty()) {
                loanStatus = "无借贷";
            } else if (orders.stream().anyMatch(order -> OrderStatus.OVERDUE.equals(order.getStatus()))) {
                loanStatus = "逾期";
            } else {
                loanStatus = "正常";
            }
            return new AdminUserListResponse(
                    user.getId(),
                    user.getUserName(),
                    loanStatus,
                    transactionCount,
                    totalLoanAmount,
                    totalRepaidAmount
            );
        }).collect(Collectors.toList());
    }

    @Override
    public void addToBlackList(Long userId, int blackLevel) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 检查是否已在黑名单
        if (blackListMapper.selectActiveByUserId(userId) != null) {
            throw new BusinessException(400, "用户已在黑名单中");
        }

        // 检查level范围 ？
        BlackUser blackUser = new BlackUser();
        blackUser.setUserId(userId);
        blackUser.setBlackLevel(blackLevel);
        blackUser.setCreateTime(LocalDateTime.now());
        blackUser.setUpdateTime(LocalDateTime.now());
        blackUser.setRemoveTime(null);
        blackListMapper.insert(blackUser);
    }

    @Override
    public void removeFromBlackList(Long userId){
        BlackUser blackUser = blackListMapper.selectByUserId(userId);
        if (blackUser == null) {
            throw new BusinessException(404,"用户不在黑名单中");
        }
        blackUser.setRemoveTime(LocalDateTime.now());
        blackUser.setUpdateTime(LocalDateTime.now());
        blackListMapper.update(blackUser);
    }

    @Override
    public List<BlackListDto> getBlackList() {
        List<BlackListDto> blackUsers = blackListMapper.selectAll();
        return blackUsers;
    }

    @Override
    public AdminGetUserResponse adminGetUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 查询认证信息（含 creditScore）
        UserCert cert = userCertMapper.selectByUserId(userId);
        Integer creditScore = (cert != null) ? cert.getCreditScore() : null;

        // 查询黑名单等级
        BlackUser blackUser = blackListMapper.selectByUserId(userId);
        int blackLevel = (blackUser != null) ? blackUser.getBlackLevel() : 0;

        return new AdminGetUserResponse(
                user.getId(),
                user.getUserName(),
                user.getAvatar(),
                user.getPhone(),
                user.getIdCard(),
                user.getRole(),
                creditScore,
                blackLevel,
                user.getCreateTime(),
                user.getUpdateTime()
        );
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
