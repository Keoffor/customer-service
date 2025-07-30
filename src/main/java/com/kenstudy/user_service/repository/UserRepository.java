package com.kenstudy.user_service.repository;

import com.kenstudy.user_service.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<Users, Integer> {

    boolean existsByEmail(String email);

    @Query("SELECT c FROM Customer c WHERE c.id IN (:ids)")
    List<Users> findSenderAndReceiver(@Param("ids") List<Integer> ids);

}
