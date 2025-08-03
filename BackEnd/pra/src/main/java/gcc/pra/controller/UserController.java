package gcc.pra.controller;

import java.util.Map;

import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gcc.pra.Utils.JWTutil;
import gcc.pra.Utils.ThreadLocalUtil;
import gcc.pra.pojo.Result;
import gcc.pra.pojo.User;
import gcc.pra.service.UserService;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/user")
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    // http://localhost:8080/user/register?id=123&password=123456
    @PostMapping("/register")
    public Result register(@Pattern(regexp = "^\\S{5,16}$") String username,
            @Pattern(regexp = "^\\S{5,16}$") String password) {
        // 用户名是否重复
        if (userService.findUserByName(username) != null) {
            return Result.error("用户名重复");
        }
        userService.registerUser(username, password);
        return Result.success();
    }

    // http://localhost:8080/user/login?username=123&password=123456
    @PostMapping("/login")
    public Result<String> login(@Pattern(regexp = "^\\S{5,16}$") String username,
            @Pattern(regexp = "^\\S{5,16}$") String password) {
        User user = userService.findUserByName(username);
        if (user == null) {
            return Result.error("用户名不存在");
        }
        if (!password.equals(user.getPassword())) {
            return Result.error("密码错误");
        }
        Map<String, Object> claims = Map.of("username", user.getUsername(), "id", user.getId());
        String token = JWTutil.generateToken(claims);
        return Result.success(token);
    }

    // http://localhost:8080/user/info
    @GetMapping("/info")
    public Result<User> getInfo() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null) {
            return Result.error("请先登录");
        }
        String username = (String) claims.get("username");
        User user = userService.findUserByName(username);
        return Result.success(user);
    }

    // http://localhost:8080/user/update
    @PutMapping("/update")
    public Result updateInfo(@RequestBody @Validated User user) {
        userService.updateUserInfo(user);
        return Result.success();
    }

    // http://localhost:8080/user/updateAvatar
    @PatchMapping("/updateAvatar")
    public Result updateAvatar(@RequestParam @URL String avatarUrl) {
        userService.updateUserAvatar(avatarUrl);
        return Result.success();
    }

    // http://localhost:8080/user/updatePWD
    @PatchMapping("/updatePWD")
    public Result updatePWD(@RequestBody Map<String, String> params) {
        String oldPWD = params.get("oldPWD");
        String newPWD = params.get("newPWD");
        String renewPWD = params.get("renewPWD");
        if (oldPWD.length() == 0 || newPWD.length() == 0 || renewPWD.length() == 0) {
            return Result.error("密码不能为空");
        }
        if (!newPWD.equals(renewPWD)) {
            return Result.error("两次新密码输入不一致");
        }
        Map<String, Object> claims = ThreadLocalUtil.get();
        User user = userService.findUserByName(claims.get("username").toString());
        // md5密码解密
        String parseTokenpassword = user.getPassword();
        if (!oldPWD.equals(parseTokenpassword)) {
            return Result.error("旧密码错误");
        }
        userService.updateUserPWD(newPWD);
        return Result.success();
    }
}
