package com.lab.booking.model;

public class UserEntity {

    private Long userId;
    private String name;
    private String account;
    private String loginId;
    private String phone;
    private RoleCode roleCode;
    private UserStatus status;
    private Integer creditScore;
    private String password;
    private boolean firstLoginRequired;
    private Long managerId;
    private boolean deleted;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public RoleCode getRoleCode() { return roleCode; }
    public void setRoleCode(RoleCode roleCode) { this.roleCode = roleCode; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public Integer getCreditScore() { return creditScore; }
    public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isFirstLoginRequired() { return firstLoginRequired; }
    public void setFirstLoginRequired(boolean firstLoginRequired) { this.firstLoginRequired = firstLoginRequired; }
    public Long getManagerId() { return managerId; }
    public void setManagerId(Long managerId) { this.managerId = managerId; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
