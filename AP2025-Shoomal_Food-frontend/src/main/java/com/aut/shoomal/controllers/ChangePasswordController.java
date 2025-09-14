package com.aut.shoomal.controllers;

import com.aut.shoomal.dto.request.ChangePasswordRequest;
import com.aut.shoomal.exceptions.FrontendServiceException;
import com.aut.shoomal.service.AuthService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class ChangePasswordController extends AbstractBaseController
{

    @FXML private TextField passwordField;
    @FXML private TextField confirmPasswordField;
    @FXML private Button confirmButton;
    @FXML private Button backButton;

    private AuthService authService;
    private Long id;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        super.initialize(url, resourceBundle);
        authService = new AuthService();
        addTextDirectionListener(passwordField);
        addTextDirectionListener(confirmPasswordField);
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    @FXML
    public void handleSavePassword(ActionEvent actionEvent)
    {
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        if (password == null || password.isEmpty() || !password.equals(confirmPassword))
        {
            showAlert("خطا", "رمز عبور جدید و تکرار آن باید یکسان باشند.", Alert.AlertType.ERROR, null);
            return;
        }

        ChangePasswordRequest request = new ChangePasswordRequest(id, password);
        authService.changePassword(request)
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.isSuccess())
                    {
                        showAlert("موفقیت", "رمز عبور شما با موفقیت تغییر یافت.", Alert.AlertType.INFORMATION, null);
                        navigateToSignInView((Node) actionEvent.getSource());
                    }
                    else
                        showAlert("خطا", "خطا در تغییر رمز عبور: " + response.getError(), Alert.AlertType.ERROR, null);
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        if (e.getCause() instanceof FrontendServiceException fsException)
                            showAlert(fsException);
                        else
                            showAlert("خطا", "خطای غیرمنتظره در تغییر رمز عبور: " + e.getMessage(), Alert.AlertType.ERROR, null);
                    });
                    return null;
                });
    }

    @FXML
    public void handleBack(ActionEvent actionEvent)
    {
        navigateTo(
                (Node) actionEvent.getSource(),
                "/com/aut/shoomal/views/ConfirmDataView.fxml",
                "/com/aut/shoomal/styles/MainView.css",
                TransitionType.SLIDE_LEFT
        );
    }
}