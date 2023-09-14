package com.shier.shierusercenterbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shier.shierusercenterbackend.common.ErrorCode;
import com.shier.shierusercenterbackend.exception.BusinessException;
import com.shier.shierusercenterbackend.exception.ThrowUtils;
import com.shier.shierusercenterbackend.mapper.UserMapper;
import com.shier.shierusercenterbackend.model.domain.User;
import com.shier.shierusercenterbackend.model.request.UserAddRequest;
import com.shier.shierusercenterbackend.model.request.UserSearchRequest;
import com.shier.shierusercenterbackend.model.request.UserUpdatePasswordRequest;
import com.shier.shierusercenterbackend.model.request.UserUpdateRequest;
import com.shier.shierusercenterbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.shier.shierusercenterbackend.constant.UserConstant.SALT;
import static com.shier.shierusercenterbackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author Shier
 * 用户服务实现
 * @createDate 2023-05-05 16:48:11
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Resource
    private UserMapper userMapper;


    public static final int MIN_PASSWORD_LENGTH = 8;

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param userCode      用户编号
     * @return User ID
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String userCode) {

        // Check is blank
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, userCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Parameters are empty");
        }
        // 账号长度不小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The account length is less than 4 characters");
        }
        // 密码不小于8位
        if (userPassword.length() < MIN_PASSWORD_LENGTH || checkPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The password is less than 8 characters");
        }
        // 用户编号长度1~15位
        if (userCode.length() > 15) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The user ID is greater than 15 characters");
        }

        // Check special character
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        // 使用正则表达式进行校验
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The account contains special characters");
        }
        // 密码和校验密码是否相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The password not match the confirm password");
        }
        // 账户名称不能重复，查询数据库当中是否存在相同名称用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The account name already exists");
        }
        // 编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userCode", userCode);
        // count大于0，说明有重复了
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The user code already exists");
        }

        // 对密码进行加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 将数据插入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserCode(userCode);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Failed to save to the database");
        }
        return user.getId();
    }

    /**
     * 用户登录实现
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request request
     * @return User
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 非空校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Account and password cannot be empty");
        }
        // 账号长度不小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The account length is less than 4 characters");
        }
        // 密码不小于8位
        if (userPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The password is less than 8 characters");
        }

        // 账户不包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        // 使用正则表达式进行校验
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The account contains special characters");
        }
        // 对密码进行加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The account does not exist or the password is incorrect");
        }
        // 用户信息脱敏
        User safetyUser = getSafetyUser(user);
        // 用户登录成功,将登录态设置到Session当中
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        return safetyUser;
    }

    /**
     * 获取当前登录用户
     *
     * @param request request
     * @return currentUser
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "Not logged in");
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "Not logged in");
        }
        return currentUser;
    }

    /**
     * 分页查询
     * @param searchRequest request
     * @return queryWrapper
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserSearchRequest searchRequest)  {

        if (searchRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Request parameters are empty");
        }
        String username = searchRequest.getUsername();
        String userAccount = searchRequest.getUserAccount();
        String gender = searchRequest.getGender();
        String phone = searchRequest.getPhone();
        String email = searchRequest.getEmail();
        Integer userStatus = searchRequest.getUserStatus();
        String userRole = searchRequest.getUserRole();
        String userCode = searchRequest.getUserCode();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Date updateTime = searchRequest.getUpdateTime();
        Date createTime = searchRequest.getCreateTime();
        // username
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        // userAccount
        if (StringUtils.isNotBlank(userAccount)) {
            queryWrapper.like("userAccount", userAccount);
        }
        // gender
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.eq("gender", gender);
        }
        // phone
        if (StringUtils.isNotBlank(phone)) {
            queryWrapper.like("phone", phone);
        }
        // email
        if (StringUtils.isNotBlank(email)) {
            queryWrapper.like("email", email);
        }
        // userStatus
        if (userStatus != null) {
            queryWrapper.eq("userStatus", userStatus);
        }

        if (StringUtils.isNotBlank(userRole)) {
            queryWrapper.eq("userRole", userRole);
        }

        if (StringUtils.isNotBlank(userCode)) {
            queryWrapper.eq("userCode", userCode);
        }

        if (updateTime != null) {
            queryWrapper.like("updateTime", updateTime);
        }
        if (createTime != null) {
            queryWrapper.like("createTime", createTime);
        }
        return queryWrapper;
    }

    /**
     * 修改密码
     *
     * @param updatePasswordRequest request
     * @param request request
     * @return true
     */
    @Override
    public boolean updateUserPassword(UserUpdatePasswordRequest updatePasswordRequest, HttpServletRequest request) {
        if (updatePasswordRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // Check Current User
        User loginUser = getLoginUser(request);
        Long userId = loginUser.getId();
        if (userId < 0 || userId == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "The user does not exist");
        }
        // Check old password is match, to be able to change password.
        String oldPassword = DigestUtils.md5DigestAsHex((SALT + updatePasswordRequest.getUserPassword()).getBytes());
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", loginUser.getUserAccount());
        queryWrapper.eq("userPassword", oldPassword);
        User checkUser = userMapper.selectOne(queryWrapper);
        if (checkUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Old Password is incorrect");
        }
        // Install updated information
        User user = new User();
        BeanUtils.copyProperties(updatePasswordRequest, user);
        user.setId(loginUser.getId());

        // MD5 Password Encryption
        String encryptedPassword = DigestUtils.md5DigestAsHex((SALT + updatePasswordRequest.getNewPassword()).getBytes());
        user.setUserPassword(encryptedPassword);

        if (encryptedPassword.equals(updatePasswordRequest.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The new password cannot be the same as the old password");
        }

        boolean result = updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    /**
     * 管理员修改用户 Admin change other user
     *
     * @param userUpdateRequest
     * @param request
     * @return
     */
    @Override
    public boolean updateUser(UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        // Accept new password if request one, if null mean it doesn't need to change.
        if (userUpdateRequest.getUserPassword() != null) {
            // MD5 Password Encryption
            String encryptedPassword = DigestUtils.md5DigestAsHex((SALT + userUpdateRequest.getUserPassword()).getBytes());
            user.setUserPassword(encryptedPassword);
        }

        // Check special character
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        // 使用正则表达式进行校验
        Matcher matcher = Pattern.compile(validPattern).matcher(userUpdateRequest.getUserAccount());
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The account contains special characters");
        }

        boolean result = updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    /**
     * Admin add new user
     *
     * @param userAddRequest request
     * @param request request
     * @return long
     */
    @Override
    public long adduser(UserAddRequest userAddRequest, HttpServletRequest request) {
        // Check is blank
        if(StringUtils.isAnyBlank(userAddRequest.getUserAccount(), userAddRequest.getUserPassword(),
                userAddRequest.getUserRole(), userAddRequest.getUserStatus())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Missing key value");
        }

        // Check special character
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAddRequest.getUserAccount());
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The account contains special characters");
        }

        // Account names cannot be duplicated. Check if there are existing users with the same name in the database
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAddRequest.getUserAccount());
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The Account name already exists, change your Account!");
        }

        // Installing user info
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // MD5 password Encryption
        String encryptedPassword = DigestUtils.md5DigestAsHex((SALT + userAddRequest.getUserPassword()).getBytes());
        user.setUserPassword(encryptedPassword);

        boolean result = this.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return user.getId();
    }

    /**
     * User info desensitization
     *
     * @param originUser User
     * @return User
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserCode(originUser.getUserCode());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        return safetyUser;
    }

    /**
     * User Logout
     *
     * @param request request
     * @return 1 Success
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // Remove login status (cookies)
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

}




