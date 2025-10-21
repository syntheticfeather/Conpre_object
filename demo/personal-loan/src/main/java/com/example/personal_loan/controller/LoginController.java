package com.example.personal_loan.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.example.personal_loan.service.UserService;

@RestController
public class LoginController {

    @Autowired
    private UserService userService;

}