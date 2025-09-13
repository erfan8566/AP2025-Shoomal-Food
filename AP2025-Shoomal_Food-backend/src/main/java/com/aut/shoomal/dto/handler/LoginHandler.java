package com.aut.shoomal.dto.handler;

import com.aut.shoomal.dto.request.ChangePasswordRequest;
import com.aut.shoomal.dto.request.ConfirmDataRequest;
import com.aut.shoomal.entity.user.User;
import com.aut.shoomal.auth.LoginManager;
import com.aut.shoomal.dto.request.UserLoginRequest;
import com.aut.shoomal.dto.response.ApiResponse;
import com.aut.shoomal.dto.response.BankInfoResponse;
import com.aut.shoomal.dto.response.UserLoginResponse;
import com.aut.shoomal.dto.response.UserResponse;
import com.aut.shoomal.entity.user.UserManager;
import com.aut.shoomal.exceptions.ConflictException;
import com.aut.shoomal.exceptions.InvalidInputException;
import com.aut.shoomal.exceptions.NotFoundException;
import com.aut.shoomal.util.JwtUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.regex.Pattern;

public class LoginHandler extends AbstractHttpHandler
{
    private final Pattern LOGIN_PATTERN = Pattern.compile("/auth/login/?");
    private final Pattern PASSWORD_PATTERN = Pattern.compile("/auth/forgot/change/?");
    private final Pattern CONFIRM_PATTERN = Pattern.compile("/auth/forgot/confirm/?");

    private final LoginManager loginManager;
    private final UserManager userManager;
    public LoginHandler(LoginManager loginManager, UserManager userManager)
    {
        this.loginManager = loginManager;
        this.userManager = userManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException
    {
        if (!checkHttpMethod(exchange, "POST"))
            return;
        if (!checkContentType(exchange))
            return;

        String path = exchange.getRequestURI().getPath();
        if (LOGIN_PATTERN.matcher(path).matches())
            loginUser(exchange);
        else if (CONFIRM_PATTERN.matcher(path).matches())
            confirmData(exchange);
        else if (PASSWORD_PATTERN.matcher(path).matches())
            changePassword(exchange);
        else
            sendResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, new ApiResponse(false, "404 Resource not found for POST."));
    }

    private void loginUser(HttpExchange exchange) throws IOException
    {
        UserLoginRequest requestBody;
        try {
            requestBody = parseRequestBody(exchange, UserLoginRequest.class);
            if (requestBody == null)
            {
                sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, new ApiResponse(false, "400 Invalid request: Request body is empty."));
                return;
            }
        } catch (IOException e) {
            System.err.println("Error parsing request body for /auth/login: " + e.getMessage());
            e.printStackTrace();
            sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, new ApiResponse(false, "400 Invalid input: Malformed JSON in request body."));
            return;
        }
        try {
            User user = loginManager.handleLogin(requestBody.getPassword(), requestBody.getPhone());
            System.out.println("User logged in successfully.");
            String token = JwtUtil.generateToken(user);
            UserLoginResponse loginResponse = getLoginResponse(user, token);
            sendRawJsonResponse(exchange, HttpURLConnection.HTTP_OK, loginResponse);
        } catch (InvalidInputException e) {
            System.err.println("Invalid input during login: " + e.getMessage());
            sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during auth/login: " + e.getMessage());
            e.printStackTrace();
            sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, new ApiResponse(false, "500 Internal Server Error: An unexpected error occurred."));
        }
    }

    private void confirmData(HttpExchange exchange) throws IOException
    {
        ConfirmDataRequest request;
        try {
            request = parseRequestBody(exchange, ConfirmDataRequest.class);
            if (request == null)
            {
                sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, new ApiResponse(false, "400 Invalid request: Request body is empty."));
                return;
            }
        } catch (IOException e) {
            System.err.println("Error parsing request body for /auth/forgot/confirm: " + e.getMessage());
            e.printStackTrace();
            sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, new ApiResponse(false, "400 Invalid input: Malformed JSON in request body."));
            return;
        }

        try {
            User user = loginManager.ConfirmToChangePassword(request.getName(), request.getPhone());
            if (user == null)
                throw new InvalidInputException("Invalid username or phone number.");
            sendRawJsonResponse(exchange, HttpURLConnection.HTTP_OK, user.getId());
        } catch (InvalidInputException e) {
            System.err.println("400 Invalid input: " + e.getMessage());
            sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, new ApiResponse(false, "400 Invalid input: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during /auth/forgot/confirm: " + e.getMessage());
            e.printStackTrace();
            sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, new ApiResponse(false, "500 Internal Server Error: " + e.getMessage()));
        }
    }

    private void changePassword(HttpExchange exchange) throws IOException
    {
        ChangePasswordRequest request;
        try {
            request = parseRequestBody(exchange, ChangePasswordRequest.class);
            if (request == null)
            {
                sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, new ApiResponse(false, "400 Invalid request: Request body is empty."));
                return;
            }
        } catch (IOException e) {
            System.err.println("Error parsing request body for /auth/forgot/change: " + e.getMessage());
            e.printStackTrace();
            sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, new ApiResponse(false, "400 Invalid input: Malformed JSON in request body."));
            return;
        }

        try {
            User user = userManager.getUserById(request.getUserId());
            if (user == null)
                throw new NotFoundException("User not found.");
            if (user.confirmPassword(request.getPassword()))
                throw new ConflictException("New password and old password are the same.");

            user.setPassword(request.getPassword());
            userManager.updateUser(user);
            sendResponse(exchange, HttpURLConnection.HTTP_OK, new ApiResponse(true, "200 Password changed successfully."));
        } catch (NotFoundException e) {
            System.err.println("User not found.");
            sendResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, new ApiResponse(false, "404 User not found."));
        } catch (ConflictException e) {
            System.err.println("409 Conflict: " + e.getMessage());
            sendResponse(exchange, HttpURLConnection.HTTP_CONFLICT, new ApiResponse(false, "409 Conflict: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during auth/forgot/change: " + e.getMessage());
            e.printStackTrace();
            sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, new ApiResponse(false, "500 Internal Server Error: " + e.getMessage()));
        }
    }

    private static UserLoginResponse getLoginResponse(User user, String token)
    {
        BankInfoResponse bankInfo = null;
        if (user.getBank() != null)
            bankInfo = new BankInfoResponse(user.getBank().getName(), user.getBank().getAccountNumber());
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getRole().getName(),
                user.getAddress(),
                user.getProfileImageBase64(),
                bankInfo
        );
        return new UserLoginResponse("200 User logged in successfully", token, userResponse);
    }
}