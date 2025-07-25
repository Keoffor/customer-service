package com.kenstudy.user_service.repository;

import com.kenstudy.user_service.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepo extends JpaRepository<Address, Integer> {
}
