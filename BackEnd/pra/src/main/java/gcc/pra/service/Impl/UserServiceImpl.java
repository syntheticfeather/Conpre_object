package gcc.pra.service.Impl;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gcc.pra.Utils.ThreadLocalUtil;
import gcc.pra.mapper.UserMapper;
import gcc.pra.pojo.User;
import gcc.pra.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User findUserByName(String username) {
        return userMapper.findUserByName(username);
    }

    @Override
    public void registerUser(String username, String password) {
        String md5Password = password;
        userMapper.addUser(username, md5Password);
    }

    @Override
    public void updateUserInfo(User user) {
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateUser(user);
    }

    @Override
    public void updateUserAvatar(String avatarUrl) {
        Integer id = ThreadLocalUtil.getId();
        userMapper.updateUserAvatar(avatarUrl, id);
    }

    @Override
    public void updateUserPWD(String PWD) {
        String md5PWD = PWD;
        Integer id = ThreadLocalUtil.getId();
        userMapper.updatePWD(md5PWD, id);
    }

}
