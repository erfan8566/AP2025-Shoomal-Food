package com.aut.shoomal.utils;

import com.aut.shoomal.dto.response.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.prefs.Preferences;

public class PreferencesManager {

    private static final String PREF_NODE_NAME = "com.aut.shoomal";
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_USER_DATA = "user_data";

    private static Preferences getPrefs() {
        return Preferences.userRoot().node(PREF_NODE_NAME);
    }

    public static void saveAuthInfo(String token, UserResponse user) {
        Preferences prefs = getPrefs();
        prefs.put(KEY_JWT_TOKEN, token);
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            UserResponse userToSave = new UserResponse(
                    user.getId(),
                    user.getName(),
                    user.getPhoneNumber(),
                    user.getEmail(),
                    user.getRole(),
                    user.getAddress(),
                    null,
                    user.getBank()
            );

            String userDataJson = objectMapper.writeValueAsString(userToSave);
            prefs.put(KEY_USER_DATA, userDataJson);
        } catch (IOException e) {
            System.err.println("Error saving user data to preferences: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getJwtToken() {
        return getPrefs().get(KEY_JWT_TOKEN, null);
    }

    public static UserResponse getUserData() {
        Preferences prefs = getPrefs();
        String userDataJson = prefs.get(KEY_USER_DATA, null);
        if (userDataJson != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(userDataJson, UserResponse.class);
            } catch (IOException e) {
                System.err.println("Error loading user data from preferences: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void clearAuthInfo() {
        Preferences prefs = getPrefs();
        prefs.remove(KEY_JWT_TOKEN);
        prefs.remove(KEY_USER_DATA);
    }

    public static UserResponse attemptAutoLogin() {
        String storedToken = getJwtToken();
        UserResponse storedUser = getUserData();

        if (storedToken != null && !storedToken.isEmpty() && storedUser != null) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/auth/profile"))
                        .header("Authorization", "Bearer " + storedToken)
                        .header("Content-Type", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                client.close();

                if (response.statusCode() == 200) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    return objectMapper.readValue(response.body(), UserResponse.class);
                } else {
                    System.err.println("Auto-login failed: Token validation failed with status " + response.statusCode());
                    clearAuthInfo();
                    return null;
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Auto-login failed due to network or API error: " + e.getMessage());
                clearAuthInfo();
                return null;
            }
        }
        return null;
    }
}