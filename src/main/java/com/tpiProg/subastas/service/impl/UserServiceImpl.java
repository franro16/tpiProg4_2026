package com.tpiProg.subastas.service.impl;

import com.tpiProg.subastas.domain.entity.User;
import com.tpiProg.subastas.dto.response.UserResponse;
import com.tpiProg.subastas.exception.ResourceNotFoundException;
import com.tpiProg.subastas.repository.UserRepository;
import com.tpiProg.subastas.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = findOrThrow(id);
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void blockUser(Long id) {
        User user = findOrThrow(id);
        if (user.isBlocked()) {
            throw new com.tpiProg.subastas.exception.BusinessException(
                    "El usuario ya está bloqueado");
        }
        user.setBlocked(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void unblockUser(Long id) {
        User user = findOrThrow(id);
        if (!user.isBlocked()) {
            throw new com.tpiProg.subastas.exception.BusinessException(
                    "El usuario no está bloqueado");
        }
        user.setBlocked(false);
        userRepository.save(user);
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isBlocked(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet())
        );
    }
}