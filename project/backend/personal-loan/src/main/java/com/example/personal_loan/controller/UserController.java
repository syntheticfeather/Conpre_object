package com.example.personal_loan.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.personal_loan.dto.AdminUserListResponse;
import com.example.personal_loan.dto.ApiResponse;
import com.example.personal_loan.dto.UserSearchDto;
import com.example.personal_loan.entity.User;
import com.example.personal_loan.service.OrderService;
import com.example.personal_loan.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<ApiResponse<List<AdminUserListResponse>>> getAllUsersWithStats() {
        List<AdminUserListResponse> userStatsList = userService.adminGetAllUsersWithStats();
        return ResponseEntity.ok(ApiResponse.success(userStatsList));
    }
    // @GetMapping("/search")
    // public List<User> searchUsers(@RequestParam(required = false) Long id,
    //                           @RequestParam(required = false) String name) {
    //     return userService.searchUsers(id, name);
    // }
    
    @GetMapping("/search-by-credit")
    public ResponseEntity<List<UserSearchDto>> searchByCreditScore(@RequestParam String expr) {
        return ResponseEntity.ok(userService.searchUsersByCreditScore(expr));
    }

    @PostMapping
    public ResponseEntity<User> addUser(@RequestBody @Valid User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addUser(user));
    }

    /*
     * 刷新token
     * 需zff检测     
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestBody Long id) {
        String newAccessToken = userService.refreshToken(id);
        return ResponseEntity.ok(Map.of("token", newAccessToken));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody @Valid User user) {
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
