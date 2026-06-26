package com.tpiProg.subastas.security;

import com.tpiProg.subastas.domain.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

public class UserPrincipal implements UserDetails {

    @Getter
    private final Long id;

    private final String email;
    private final String passwordHash;
    private final boolean blocked;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserPrincipal(Long id, String email, String passwordHash, boolean blocked,
                          Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.blocked = blocked;
        this.authorities = authorities;
    }

    public static UserPrincipal create(User user) {
        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .collect(Collectors.toList());

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isBlocked(),
                authorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    // Spring Security usa getUsername() como identificador; nosotros usamos email
    @Override
    public String getUsername() {
        return email;
    }

    // Si blocked == true, Spring lanza LockedException automaticamente al autenticar
    @Override
    public boolean isAccountNonLocked() {
        return !blocked;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}