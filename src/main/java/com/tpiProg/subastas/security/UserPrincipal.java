package com.tpiProg.subastas.security;

import com.tpiProg.subastas.domain.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.stream.Collectors;

public class UserPrincipal implements UserDetails {

    @Getter
    private final Long id;

    private final String email;
    private final String passwordHash;
    private final boolean blocked;
    private final OffsetDateTime lockedUntil;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserPrincipal(Long id, String email, String passwordHash, boolean blocked,
                          OffsetDateTime lockedUntil,
                          Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.blocked = blocked;
        this.lockedUntil = lockedUntil;
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
                user.getLockedUntil(),
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

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Bloqueado permanentemente por ADMIN
        if (blocked) return false;
        // Bloqueado temporalmente por intentos fallidos
        if (lockedUntil != null) {
            return OffsetDateTime.now(ZoneOffset.UTC).isAfter(lockedUntil);
        }
        return true;
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