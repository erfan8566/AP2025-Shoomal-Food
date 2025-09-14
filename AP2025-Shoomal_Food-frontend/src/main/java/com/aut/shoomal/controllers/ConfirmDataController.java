package com.aut.shoomal.controllers;

import com.aut.shoomal.dto.request.ConfirmDataRequest;
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

public class ConfirmDataController extends AbstractBaseController
{

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private Button confirmButton;
    @FXML private Button backButton;
    private AuthService authService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        super.initialize(url, resourceBundle);
        authService = new AuthService();
        addTextDirectionListener(nameField);
        addTextDirectionListener(phoneField);
    }

    @FXML
    public void handleSaveChanges(ActionEvent actionEvent)
    {
        String name = nameField.getText();
        String phoneNumber = phoneField.getText();
        if (name == null || name.isEmpty() || phoneNumber == null || phoneNumber.isEmpty())
        {
            showAlert("خطا", "لطفا نام کاربری و شماره تلفن را وارد کنید.", Alert.AlertType.ERROR, null);
            return;
        }

        ConfirmDataRequest request = new ConfirmDataRequest(name, phoneNumber);
        authService.confirmData(request)
                .thenAccept(id -> Platform.runLater(() -> {
                    if (id != null)
                    {
                        showAlert("تایید موفق", "اطلاعات با موفقیت تایید شد. اکنون رمز عبور جدید را وارد کنید.", Alert.AlertType.INFORMATION, null);
                        navigateToChangePassword(id, (Node) actionEvent.getSource());
                    }
                    else
                        showAlert("خطا", "اطلاعات وارد شده صحیح نمی‌باشد.", Alert.AlertType.ERROR, null);
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        if (e.getCause() instanceof FrontendServiceException fsException)
                            showAlert(fsException);
                        else
                            showAlert("خطا", "خطای غیرمنتظره در تایید اطلاعات: " + e.getMessage(), Alert.AlertType.ERROR, null);
                    });
                    return null;
                });
    }

    private void navigateToChangePassword(Long id, Node source)
    {
        navigateTo(
                source,
                "/com/aut/shoomal/views/ChangePasswordView.fxml",
                "/com/aut/shoomal/styles/MainView.css",
                TransitionType.SLIDE_LEFT,
                controller -> {
                    if (controller instanceof ChangePasswordController changePasswordController)
                        changePasswordController.setId(id);
                }
        );
    }

    @FXML
    public void handleBack(ActionEvent actionEvent)
    {
        navigateToSignInView((Node) actionEvent.getSource());
    }
}