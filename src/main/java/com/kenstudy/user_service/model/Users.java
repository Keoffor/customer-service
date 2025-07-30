package com.kenstudy.user_service.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer")
@Getter
@Setter
@ToString(exclude = {"address"})
@NoArgsConstructor
@AllArgsConstructor
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotNull
    private String name;
    @NotNull
    @Column(unique = true)
    private String username;
    @Email
    @Column(unique = true)
    private String email;
    private String password;
    private String status;
    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> address = new ArrayList<>();

    private boolean enabled;
    @NotNull
    private LocalDate localDate;

    public void addAddress(Address address){
        this.address.add(address);
        address.setUsers(this);
    }

}
