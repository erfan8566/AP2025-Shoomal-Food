package com.aut.shoomal.controllers;

import com.aut.shoomal.exceptions.FrontendServiceException;
import com.aut.shoomal.service.AuthService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.layout.StackPane;
import javafx.animation.TranslateTransition;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import com.aut.shoomal.dto.request.UserLoginRequest;
import com.aut.shoomal.utils.PreferencesManager;

public class SignInController extends AbstractBaseController {
    @FXML private TextField phoneNumberField;
    @FXML private TextField passwordField;
    @FXML private Button enterButton;
    @FXML private Hyperlink signUpLink;
    @FXML private Hyperlink changePasswordLink;

    private AuthService authService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        super.initialize(url, resourceBundle);
        authService = new AuthService();

        if (phoneNumberField != null) addTextDirectionListener(phoneNumberField);
        if (passwordField != null) addTextDirectionListener(passwordField);
    }

    @FXML
    private void handleEnterButton() {
        String phoneNumber = phoneNumberField.getText();
        String password = passwordField.getText();

        System.out.println("Enter button clicked!");
        System.out.println("Phone Number: " + phoneNumber);

        if (phoneNumber.isEmpty() || password.isEmpty()) {
            showAlert("خطا", "شماره تلفن و رمز عبور الزامی هستند.");
            System.out.println("Phone number and password are required.");
        } else {
            authenticateUser(phoneNumber, password);
        }
    }

    private void authenticateUser(String phoneNumber, String password) {
        UserLoginRequest loginRequest = new UserLoginRequest(phoneNumber, password);

        authService.login(loginRequest)
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response != null)
                    {
                        PreferencesManager.saveAuthInfo(response.getToken(), response.getUser());
                        showAlert("موفقیت", "ورود با موفقیت انجام شد.", Alert.AlertType.INFORMATION, null);
                        navigateTo(
                                enterButton,
                                "/com/aut/shoomal/views/MainView.fxml",
                                "/com/aut/shoomal/styles/MainView.css",
                                TransitionType.SLIDE_LEFT,
                                controller -> {
                                    if (controller instanceof MainController mainController)
                                        mainController.setLoggedInUser(response.getUser());
                                }
                        );
                    }
                    else
                        showAlert("خطا", "خطا در ورود.", Alert.AlertType.ERROR, null);
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        if (e.getCause() instanceof FrontendServiceException fsException)
                            showAlert(fsException);
                        else
                            showAlert("خطا", "خطای غیرمنتظره در ورود: " + e.getMessage(), Alert.AlertType.ERROR, null);
                    });
                    return null;
                });
    }

    @FXML
    private void handleSignUpLink() {
        System.out.println("Sign up link clicked!");

        Stage stage = (Stage) signUpLink.getScene().getWindow();
        Parent currentRoot = signUpLink.getScene().getRoot();

        try {
            Parent signUpRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/aut/shoomal/views/SignUpView.fxml")));

            StackPane transitionContainer = new StackPane();
            transitionContainer.getChildren().addAll(currentRoot, signUpRoot);

            signUpRoot.setTranslateX(stage.getWidth());

            Scene newScene = new Scene(transitionContainer, stage.getWidth() - 15, stage.getHeight() - 38);
            newScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/aut/shoomal/styles/SignInUpStyles.css")).toExternalForm());
            stage.setScene(newScene);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(500), signUpRoot);
            slideIn.setFromX(stage.getWidth());
            slideIn.setToX(0);

            slideIn.setOnFinished(event -> transitionContainer.getChildren().remove(currentRoot));

            slideIn.play();

        } catch (IOException e) {
            System.err.println("Failed to load SignUpView.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleChangePasswordLink(ActionEvent actionEvent)
    {
        navigateTo(
                (Node) actionEvent.getSource(),
                "/com/aut/shoomal/views/ConfirmDataView.fxml",
                "/com/aut/shoomal/styles/MainView.css",
                TransitionType.SLIDE_LEFT
        );
    }
}