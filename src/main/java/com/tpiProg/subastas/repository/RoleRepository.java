package com.tpiProg.subastas.repository;

import com.tpiProg.subastas.domain.entity.Role;
import com.tpiProg.subastas.domain.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleType name);
}