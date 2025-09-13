package com.aut.shoomal.auth;
import com.aut.shoomal.entity.user.User;
import com.aut.shoomal.entity.user.UserManager;
import com.aut.shoomal.exceptions.InvalidInputException;
import com.aut.shoomal.exceptions.NotFoundException;
import com.aut.shoomal.exceptions.ServiceUnavailableException;

public class LoginManager
{
    private final UserManager userManager;

    public LoginManager(UserManager userManager)
    {
        this.userManager = userManager;
    }

    public User handleLogin(String password, String phone)
    {
        if (password == null || password.trim().isEmpty()) {
            throw new InvalidInputException("400 Invalid input: 'password' is required.");
        }

        User user;
        if ("admin".equalsIgnoreCase(phone) && "admin".equals(password)) {
            try {
                user = userManager.getUserByName("admin");

                if (user == null || !user.getRole().getName().equalsIgnoreCase("Admin")) {
                    throw new InvalidInputException("400 Invalid credentials: Admin user 'admin' not found or role mismatch.");
                }

                if (!user.confirmPassword(password)) {
                    throw new InvalidInputException("400 Invalid credentials: 'password' is incorrect for admin.");
                }

                return user;
            } catch (NotFoundException e) {
                throw new InvalidInputException("400 Invalid credentials: Admin user 'admin' not provisioned in the system.");
            } catch (Exception e) {
                throw new ServiceUnavailableException("500 Internal Server Error: Failed to retrieve admin user for special login. " + e.getMessage());
            }
        }

        if (phone != null && !phone.trim().isEmpty()) {
            user = authenticateByPhoneNumber(phone);
        } else {
            throw new InvalidInputException("400 Invalid input: 'phone' is required.");
        }

        if (user == null) {
            throw new InvalidInputException("400 Invalid credentials: User not found.");
        }

        if (!user.confirmPassword(password)) {
            throw new InvalidInputException("400 Invalid credentials: 'password' is incorrect.");
        }

        return user;
    }

    private User authenticateByPhoneNumber(String phone) {
        try {
            return userManager.getUserByPhoneNumber(phone);
        } catch (NotFoundException e) {
            return null;
        } catch (Exception e) {
            throw new ServiceUnavailableException("500 Internal Server Error: Error during phone number authentication. " + e.getMessage());
        }
    }

    public User ConfirmToChangePassword(String name, String phone)
    {
        User user;
        try {
            user = userManager.getUserByPhoneNumber(phone);
        } catch (NotFoundException e) {
            return null;
        }

        if (!user.getName().equals(name))
            return null;
        return user;
    }
}