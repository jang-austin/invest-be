package com.invest.service;

import com.invest.domain.User;
import com.invest.repo.UserRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User loginOrCreateWithGoogle(String sub, String email, String name, String pictureUrl) {
        User user = userRepository.findById(sub).orElseGet(() -> {
            User u = new User(sub, BigDecimal.ZERO);
            return userRepository.save(u);
        });
        // 프로필 정보는 매 로그인마다 최신으로 갱신
        user.setEmail(email);
        user.setName(name);
        user.setPictureUrl(pictureUrl);
        return userRepository.save(user);
    }

    public User require(String userId) {
        return userRepository
                .findById(userId.trim())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
