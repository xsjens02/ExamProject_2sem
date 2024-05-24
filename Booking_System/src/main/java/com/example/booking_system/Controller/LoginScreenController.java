package com.example.booking_system.Controller;

import com.example.booking_system.Model.LoggedInManager;
import com.example.booking_system.Model.User;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import com.example.booking_system.Service.LoginService;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class LoginScreenController {

    public TextField tfUsername;
    private final LoginService loginService = new LoginService();

    public PasswordField pfPassword;
    public Button loginButton;
    public Button cancelButton;
    public Button forgotButton;
    public Label failledLoginLabel;
    public Label forgotPasswordLink;


    public void onLoginButtonClick(ActionEvent actionEvent) {
        String username = tfUsername.getText();
        String password = pfPassword.getText();

        boolean isLoggedIn = loginService.validateLogin(username, password);

        if(isLoggedIn){
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.close();

            User loggedInUser = LoggedInManager.getInstance().getCurrentUser();
            System.out.println(loggedInUser.getUserID());


        }else{
            tfUsername.clear();
            pfPassword.clear();
            failledLoginLabel.setVisible(true);
        }

    }

    public void onForgotButtonClick(ActionEvent actionEvent) {
        String url = "https://moodle.easv.dk/login/forgot_password.php";

        if(Desktop.isDesktopSupported()){
            Desktop desktop = Desktop.getDesktop();
            try{
                desktop.browse(new URI(url));
            }catch (IOException | URISyntaxException e){
                e.printStackTrace();
            }
        }

    }

    public void onCancelButtonClick(ActionEvent actionEvent) {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

}