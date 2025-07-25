package com.kenstudy.user_service.repository;

import com.kenstudy.user_service.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationTokenRepo extends JpaRepository<VerificationToken, Integer> {

    VerificationToken findByToken(String token);
}
