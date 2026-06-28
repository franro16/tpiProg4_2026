package com.tpiProg.subastas.controller;

import com.tpiProg.subastas.dto.response.UserResponse;
import com.tpiProg.subastas.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Solo ADMIN puede ver todos los usuarios
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    // ADMIN puede ver cualquier usuario; USER y SELLER solo el propio (validado en service si hace falta)
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> block(@PathVariable Long id) {
        userService.blockUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unblock(@PathVariable Long id) {
        userService.unblockUser(id);
        return ResponseEntity.noContent().build();
    }
}