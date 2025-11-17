package com.example.personal_loan.dao;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.entity.User;
import com.example.personal_loan.mapper.UserMapper;

@SpringBootTest
@Transactional // 保证每个测试方法结束后自动回滚，避免脏数据
@Rollback // 默认 true，显式声明更清晰
class UserMapperTest {
    
    @Autowired
    private UserMapper userMapper;

    // 创建用于测试的用户对象
    private User createTestUser() {
        User user = new User();
        user.setUserName("张一");
        user.setPassword("123456");
        user.setIdCard("110101199001011234");
        user.setPhone("13800138000");
        return user;
    }

    @Test
    void findById(){
        User user=userMapper.findById(1L);
        System.out.println(user);
    }

    @Test
    void testInsert() {
        User user = createTestUser();
        int result = userMapper.insert(user);
        assertThat(result).isEqualTo(1);      // 确保插入了数据
        assertThat(user.getId()).isNotNull(); // 确保 useGeneratedKeys 设置了id
        System.out.println("插入后ID: " + user.getId());
    }

    @Test
    void testFindByPhoneAndPassword() {
        User user = createTestUser();
        userMapper.insert(user);

        User found = userMapper.findByPhoneAndPassword("13800138000", "123456");
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(user.getId());
    }

    @Test
    void testUpdate() {
        User user = createTestUser();
        userMapper.insert(user);

        user.setPhone("13900139000");
        user.setPassword("654321");
        int result = userMapper.update(user);

        assertThat(result).isEqualTo(1);

        User updated = userMapper.findById(user.getId());
        assertThat(updated.getPhone()).isEqualTo("13900139000");
        assertThat(updated.getPassword()).isEqualTo("654321");
    }

    @Test
    void testDelete() {
        User user = createTestUser();
        userMapper.insert(user);

        int result = userMapper.delete(user.getId());
        assertThat(result).isEqualTo(1);

        User deleted = userMapper.findById(user.getId());
        assertThat(deleted).isNull();
    }

    @Test
    void testFindAll() {
        userMapper.insert(createTestUser());
        userMapper.insert(createTestUser());

        List<User> users = userMapper.findAll();
        assertThat(users).isNotEmpty();
        assertThat(users.size()).isGreaterThanOrEqualTo(2);
    }
}
