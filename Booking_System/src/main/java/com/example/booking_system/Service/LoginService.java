package com.example.booking_system.Service;
import com.example.booking_system.Model.LoggedInManager;
import com.example.booking_system.Model.User;
import com.example.booking_system.Persistence.UserDAO_Impl;
import com.example.booking_system.Persistence.dbConnection;
import com.example.booking_system.Controller.LoginScreenController;

import java.sql.Connection;

public class LoginService {

    private final Connection connection;

    private final UserDAO_Impl userDAO = new UserDAO_Impl();

    //private final LoginScreenController loginScreenController = new LoginScreenController();

    public LoginService(){
        connection = dbConnection.getInstance().getConnection();
    }

    public String hashPassword(String password) {
        return String.valueOf(password.hashCode());
    }

    public boolean validateLogin(String username, String password){
        User user = userDAO.readUsername(username);
        String hashedPassword = hashPassword(password);

        if (user != null && hashedPassword.equals(user.getPassword())){
            System.out.println("True!");
            LoggedInManager.getInstance().setCurrentUser(user);
            return true;

        }else {
            System.out.println("False!");
            return false;
        }

    }
}
