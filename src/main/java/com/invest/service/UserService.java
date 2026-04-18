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
    public User loginOrCreate(String userId) {
        String id = userId.trim();
        return userRepository.findById(id).orElseGet(() -> userRepository.save(new User(id, BigDecimal.ZERO)));
    }

    public User require(String userId) {
        return userRepository
                .findById(userId.trim())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
