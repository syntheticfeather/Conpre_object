package gcc.pra.service;

import gcc.pra.pojo.User;

public interface UserService {

    public User findUserByName(String username);

    public void registerUser(String id, String password);

    public void updateUserInfo(User user);

    public void updateUserAvatar(String avatarUrl);

    public void updateUserPWD(String newPassword);
}
