package com.shier.shierusercenterbackend.service;

import com.shier.shierusercenterbackend.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * @author Shier
 * CreateTime 2023/5/5 16:53
 * 用户服务测试
 */
@SpringBootTest
public class UserServiceTest {

    @Resource
    private UserService userService;

    @Test
    void testInsertUser() {
        User user = new User();
//        user.setId(1L);
        user.setUsername("Shier");
        user.setUserAccount("123");
        user.setAvatarUrl("");
        user.setGender("Male");
        user.setUserPassword("123456789");
        user.setPhone("0122");
        user.setEmail("abc@sample.com");
        user.setUserCode("001");

        boolean result = userService.save(user);
        System.out.println("New User ID:" + user.getId());
        // 断言，判断一下是否符合预期结果，是否保存成功
        Assertions.assertTrue(result);
    }

    @Test
    void userRegister() {
        String userAccount = "xiaoshier";
        String userPassword = "123456789";
        String checkPassword = "123456789";
        String userCode = "002";
        long result = userService.userRegister(userAccount, userPassword, checkPassword, userCode);
        Assertions.assertEquals(2, result);
    }

    /**
     * 测试出错的情况
     */
    @Test
    void userRegisterLag() {
        // 测试非空
        String userAccount = "xiaoshier";
        String userPassword = "";
        String checkPassword = "12345678";
        String userCode = "001";
        long result = userService.userRegister(userAccount, userPassword, checkPassword, userCode);
        Assertions.assertEquals(-1, result);
        // 测试账户长度小于4
        userAccount = "shi";
        result = userService.userRegister(userAccount, userPassword, checkPassword, userCode);
        Assertions.assertEquals(-1, result);
        // 测试密码小于6位
        userAccount = "xiaoshier";
        userPassword = "1234";
        result = userService.userRegister(userAccount, userPassword, checkPassword, userCode);
        Assertions.assertEquals(-1, result);
        // 测试特殊字符
        userAccount = "shi@";
        userPassword = "12345678";
        result = userService.userRegister(userAccount, userPassword, checkPassword, userCode);
        Assertions.assertEquals(-1, result);
        // 测试密码和校验密码不相同
        userAccount = "xiaoshier";
        checkPassword = "123457899";
        result = userService.userRegister(userAccount, userPassword, checkPassword, userCode);
        Assertions.assertEquals(-1, result);

        // 测试账号不重复
        userAccount = "Shier02";
        checkPassword = "12345678";
        userCode = "001";
        result = userService.userRegister(userAccount, userPassword, checkPassword, userCode);
        Assertions.assertEquals(-1, result);

        // 测试账号不重复
        userAccount = "Shier";
        checkPassword = "12345678";
        result = userService.userRegister(userAccount, userPassword, checkPassword, userCode);
        Assertions.assertEquals(-1, result);

        //插入数据
        userAccount = "xiaoshier";
        userPassword = "123456789";
        checkPassword = "123456789";
        result = userService.userRegister(userAccount, userPassword, checkPassword, userCode);
        Assertions.assertEquals(-1, result);
    }
}