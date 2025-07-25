package com.kenstudy.user_service.repository;

import com.kenstudy.user_service.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Users, Integer> {

    boolean existsByEmail(String email);
}
