package com.kenstudy.user_service.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {

    private static final int EXPIRATION = 15;


    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Integer id;

    private String token;

    @OneToOne(targetEntity = Users.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonBackReference
    private Users users;

    private LocalDateTime createdDate;

    private LocalDateTime expiredDate;



    public VerificationToken(Users users, String token) {
        this.users = users;
        this.token = token;
        this.createdDate = LocalDateTime.now();
        this.expiredDate = calculateExpiryDateTime();
    }

    private LocalDateTime calculateExpiryDateTime() {
        return LocalDateTime.now().plusMinutes(EXPIRATION);
    }




}
