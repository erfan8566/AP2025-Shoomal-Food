package com.aut.shoomal.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChangePasswordRequest
{
    @JsonProperty("user_id")
    private Long userId;
    private String password;
    public ChangePasswordRequest() {}

    public ChangePasswordRequest(Long userId, String password)
    {
        this.userId = userId;
        this.password = password;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }
}