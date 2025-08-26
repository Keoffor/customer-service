package com.kenstudy.user_service.repository;

import com.kenstudy.user_service.model.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransferStatusRepo extends JpaRepository<TransferStatus,Integer> {
    Optional<TransferStatus>findByCorrelationId(String correlationId);
}
