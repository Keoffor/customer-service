package com.kenstudy.user_service.controller;



import com.kenstudy.customer.CustomerRequestDTO;
import com.kenstudy.customer.CustomerResponseDTO;
import com.kenstudy.transaction.TransferRequestDTO;
import com.kenstudy.user_service.exception.TokenNotFoundException;
import com.kenstudy.user_service.model.Users;
import com.kenstudy.user_service.model.VerificationToken;
import com.kenstudy.user_service.saga.CustomerHandler;
import com.kenstudy.user_service.services.UserService;
import com.kenstudy.user_service.util.CusTransResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;

@RestController
@RequestMapping("/v1/bank")
@Slf4j
public class UserController {

    private final UserService userService;
    private final CustomerHandler customerHandler;

    @Autowired
    public UserController(UserService userService, CustomerHandler customerHandler) {
        this.userService = userService;
        this.customerHandler = customerHandler;
    }

    @PostMapping("/add")
    public ResponseEntity<CustomerResponseDTO>createUser(@Valid @RequestBody CustomerRequestDTO customerRequestDTO,
                     HttpServletRequest request){
        CustomerResponseDTO customer = userService.createCustomer(customerRequestDTO, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(customer);
    }

    @GetMapping("/confirm-registration")
    public ResponseEntity<String>confirmRegistration(HttpServletRequest request,
                                @RequestParam("code") String token){
        VerificationToken verifyToken = userService.getVerifiedToken(token);
        String result = userService.confirmRegistration(verifyToken, request);
        return ResponseEntity.status(HttpStatus.OK).body(result);

    }

    @GetMapping("/{userId}")
    public ResponseEntity<CustomerResponseDTO>getCustomer(@PathVariable Integer userId){
        CustomerResponseDTO user = userService.getUser(userId);
        return ResponseEntity.status(HttpStatus.OK).body(user);
    }

    @PostMapping("/fund-transfer")
    public ResponseEntity<CusTransResponseDto>transferFund(@RequestBody TransferRequestDTO requestDTO){
        CusTransResponseDto cusTransResponseDto = userService.transferFund(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(cusTransResponseDto);
    }

    @GetMapping("transfer/{correlationId}/status")
    public ResponseEntity<CusTransResponseDto>transferStatus(@PathVariable String correlationId){
        CusTransResponseDto cusTransResponseDto = userService.getTransferStatus(correlationId);
        return ResponseEntity.status(HttpStatus.OK).body(cusTransResponseDto);
    }


}
