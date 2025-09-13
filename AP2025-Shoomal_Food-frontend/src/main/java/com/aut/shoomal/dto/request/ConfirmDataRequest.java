package com.aut.shoomal.dto.request;

public class ConfirmDataRequest
{
    private String name, phone;
    public ConfirmDataRequest() {}

    public ConfirmDataRequest(String name, String phone)
    {
        this.name = name;
        this.phone = phone;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }
}