package com.kenstudy.user_service.util;


import com.kenstudy.customer.CustomerRequestDTO;
import com.kenstudy.user_service.model.Address;
import com.kenstudy.user_service.model.Users;

import java.time.LocalDate;

public class UserHelper {

    public static Users parseToUsers(CustomerRequestDTO customer){

        Users users = new Users();
                users.setName(customer.getName());
                users.setUsername(customer.getUsername());
                users.setEmail(customer.getEmail());
                users.setPassword(customer.getPassword());
                users.setLocalDate(LocalDate.now());
                users.setEnabled(false);
        customer.getBankAddress()
                .forEach(dto -> {
                    Address address = new Address();
                    address.setCity(dto.getCity());
                    address.setStreet(dto.getStreet());
                    address.setZipCode(dto.getZipCode());
                    address.setState(dto.getState());
                    users.addAddress(address);
                });

        return users;
    }
}
