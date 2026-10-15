package com.ims.authserver.service;

import com.ims.authserver.entity.UserEntity;
import com.ims.authserver.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserSessionUserResolver {

    private final UserRepository userRepository;

    public UserSessionUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UUID resolveUserId(String email) {

        if (email == null || email.isBlank()) {
            return null;
        }

        return userRepository.findByEmail(email)
                .map(UserEntity::getId)
                .orElse(null);
    }
}