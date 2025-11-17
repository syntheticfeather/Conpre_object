package com.example.personal_loan.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.personal_loan.entity.Order;
import com.example.personal_loan.service.ApplicationService;
import com.example.personal_loan.service.AuthService;
import com.example.personal_loan.service.LoanProductService;
import com.example.personal_loan.service.UserService;

@Service
public class ApplicationServiceImpl implements ApplicationService{

    @Autowired
    private UserService userService;

    @Autowired
    private LoanProductService loanProductService;

    @Autowired
    private AuthService authService;

    @Override
    public void addApplication(Long userId, Long proId){

    }

    @Override
    public void deleteApplication(Long userId, Long proId){
    }

    @Override
    public void updateApplication(Long userId, Long proId){

    }

    public Order getOrder(Long userId, Long proId){
        return null;
    }

    public List<Order> getOrders(Long userId){
        return null;
    }
}