package com.shier.shierusercenterbackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shier.shierusercenterbackend.common.BaseResponse;
import com.shier.shierusercenterbackend.common.ErrorCode;
import com.shier.shierusercenterbackend.exception.BusinessException;
import com.shier.shierusercenterbackend.exception.ThrowUtils;
import com.shier.shierusercenterbackend.model.domain.User;
import com.shier.shierusercenterbackend.model.request.*;
import com.shier.shierusercenterbackend.service.UserService;
import com.shier.shierusercenterbackend.utils.ResultUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.shier.shierusercenterbackend.constant.UserConstant.ADMIN_ROLE;
import static com.shier.shierusercenterbackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author Shier
 * CreateTime 2023/5/6 22:16
 * 用户控制器接口
 */
@RestController
@RequestMapping("/user")
//@Api(tags = "用户管理")
//@CrossOrigin(origins = "http://user.kongshier.top/", allowCredentials = "true")
public class UserController {
    @Resource
    private UserService userService;

    /**
     * 用户注册请求
     *
     * @param userRegisterRequest request
     * @return success()
     */
    @PostMapping("/register")
//    @ApiOperation(value = "用户注册")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Request parameters are empty");
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String userCode = userRegisterRequest.getUserCode();

        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, userCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long userId = userService.userRegister(userAccount, userPassword, checkPassword, userCode);
        return ResultUtils.success(userId);
    }

    /**
     * 用户登录请求
     *
     * @param userLoginRequest request
     * @param request Request object
     * @return success()
     */
    @PostMapping("/login")
//    @ApiOperation(value = "用户登录")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Request parameters are empty");
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Request parameters are empty");
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    /**
     * 用户注销
     *
     * @param request request
     * @return success()
     */
    @PostMapping("/logout")
//    @ApiOperation(value = "退出登录")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Request parameters are empty");
        }
        int logoutResult = userService.userLogout(request);
        return ResultUtils.success(logoutResult);
    }

    /**
     * 当前登录用户请求
     *
     * @param request request
     * @return success()
     */
    @GetMapping("/current")
//    @ApiOperation(value = "当前用户")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        // 获取登录态
        User resultUser = userService.getLoginUser(request);
        return ResultUtils.success(resultUser);
    }

    /**
     * Create and add new user
     *
     * @param userAddRequest request
     * @param request request
     * @return success()
     */
    @PostMapping("/add")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "No permission");
        }
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Request parameters are empty");
        }
        long addUser = userService.adduser(userAddRequest, request);
        return ResultUtils.success(addUser);
    }

    /**
     * Search user database
     *
     * @param searchRequest request
     * @return success()
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(UserSearchRequest searchRequest, HttpServletRequest request) {
        // 管理员校验
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "No permission");
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
        List<User> userList = userService.list(queryWrapper);
        List<User> users = userList.stream().map(userService::getSafetyUser).collect(Collectors.toList());
        return ResultUtils.success(users);
    }

    /**
     * User logical deletion
     *
     * @param deleteRequest request
     * @return success()
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody UserDeleteRequest deleteRequest, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "No permission");
        }
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean removeUser = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(removeUser);
    }

    /**
     * Admin change other user
     *
     * @param userUpdateRequest request
     * @param request request
     * @return success()
     */
    @PostMapping("/update")
//    @ApiOperation(value = "更新用户")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "No permission");
        }
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//        User user = new User();
//        BeanUtils.copyProperties(userUpdateRequest, user);
//        boolean result = userService.updateById(user);
//        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        boolean updateUser = userService.updateUser(userUpdateRequest, request);
        if(updateUser){
            return ResultUtils.success(true);
        }else{
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
    }

    /**
     * User change their own information
     *
     * @param userUpdateMyRequest request
     * @param request request
     * @return success()
     */
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest,
                                              HttpServletRequest request) {
        if (userUpdateMyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Request is null");
        }
        User loginUser = userService.getLoginUser(request);
        User user = new User();
        BeanUtils.copyProperties(userUpdateMyRequest, user);
        user.setId(loginUser.getId());
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * User modify their own password
     *
     * @param updatePasswordRequest request
     * @param request request
     * @return success() / error()
     */
    @PostMapping("/update/password")
    public BaseResponse<Boolean> updateUserPassword(@RequestBody UserUpdatePasswordRequest updatePasswordRequest,
                                                    HttpServletRequest request) {
//        if (!isAdmin(request)) {
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "No permission");
//        }
        boolean updateUserPassword = userService.updateUserPassword(updatePasswordRequest, request);
        if (updateUserPassword) {
            return ResultUtils.success(true);
        } else {
            return ResultUtils.error(ErrorCode.INVALID_PASSWORD_ERROR);
        }
    }

    /**
     * Is admin
     *
     * @param request request
     * @return true / false
     */
    private boolean isAdmin(HttpServletRequest request) {
        // 管理员校验
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        //return user != null && user.getUserRole() == ADMIN_ROLE;
        if (user == null || !user.getUserRole().equals(ADMIN_ROLE)) {
            return false;
        }
        return true;
    }
}
