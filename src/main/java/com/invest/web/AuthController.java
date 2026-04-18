package com.invest.web;

import com.invest.service.UserService;
import com.invest.web.dto.LoginRequest;
import com.invest.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request) {
        var user = userService.loginOrCreate(request.userId());
        return new UserResponse(user.getId(), user.getBalance());
    }
}
