package com.invest.web;

import com.invest.domain.User;
import com.invest.service.GoogleTokenVerifier;
import com.invest.service.GoogleTokenVerifier.GoogleUserInfo;
import com.invest.service.UserService;
import com.invest.web.dto.GoogleLoginRequest;
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
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthController(UserService userService, GoogleTokenVerifier googleTokenVerifier) {
        this.userService = userService;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @PostMapping("/google")
    public UserResponse googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        GoogleUserInfo info = googleTokenVerifier.verify(request.idToken());
        User user = userService.loginOrCreateWithGoogle(
                info.sub(), info.email(), info.name(), info.picture());
        return new UserResponse(user.getId(), user.getBalance(), user.getEmail(), user.getName(), user.getPictureUrl());
    }
}
