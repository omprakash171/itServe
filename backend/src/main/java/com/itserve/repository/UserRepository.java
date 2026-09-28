package com.itserve.repository;

import com.itserve.entity.Role;
import com.itserve.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRoleAndActiveTrue(Role role);

    List<User> findByRole(Role role);
}
