package com.kenstudy.user_service.services;



import com.kenstudy.customer.CustomerRequestDTO;
import com.kenstudy.customer.CustomerResponseDTO;
import com.kenstudy.transaction.TransferRequestDTO;
import com.kenstudy.user_service.model.Users;
import com.kenstudy.user_service.model.VerificationToken;
import com.kenstudy.user_service.util.CusTransResponseDto;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService {

    CustomerResponseDTO createCustomer(CustomerRequestDTO customer, HttpServletRequest request);

    CustomerResponseDTO getUser(Integer userId);

    CusTransResponseDto transferFund(TransferRequestDTO dto);

    void createVerificationToken(Users users, String token);

    VerificationToken getVerifiedToken (String token);

    String confirmRegistration(VerificationToken verifier, HttpServletRequest request);
}
