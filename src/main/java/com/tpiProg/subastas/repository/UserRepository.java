package com.tpiProg.subastas.repository;

import com.tpiProg.subastas.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// JpaRepository ya nos da el Guardar, Borrar, BuscarPorId, etc.
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Spring crea automáticamente la consulta SQL leyendo el nombre del método
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}