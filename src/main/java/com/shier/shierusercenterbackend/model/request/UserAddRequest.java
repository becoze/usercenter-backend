package com.shier.shierusercenterbackend.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户创建请求
 *
 */
@Data
public class UserAddRequest implements Serializable {

    private String username;

    private String userAccount;

    private String userPassword;

    private String avatarUrl;

    private String userCode;

    private String phone;

    private String userStatus;

    private String email;

    private String gender;

    /**
     * Role: user, admin
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}