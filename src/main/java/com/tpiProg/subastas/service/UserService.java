package com.tpiProg.subastas.service;

import com.tpiProg.subastas.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse getById(Long id);
    List<UserResponse> getAll();
    void blockUser(Long id);
    void unblockUser(Long id);
}