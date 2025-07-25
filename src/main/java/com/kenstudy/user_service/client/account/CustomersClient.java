package com.kenstudy.user_service.client.account;


import com.kenstudy.account.AccountBalanceDTO;
import com.kenstudy.customer.CustomerResponseDTO;
import com.kenstudy.transaction.TransactionResponseDTO;
import com.kenstudy.transaction.TransferRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
public class CustomersClient {


  private final GenericClientService genericClientService;

  @Autowired
    public CustomersClient(GenericClientService genericClientService) {
        this.genericClientService = genericClientService;
    }

    public AccountBalanceDTO post(String userId){
        return genericClientService.post(
                ServicesUrlConstant.ACCOUNT_CREATE_URL,
                AccountBalanceDTO.class
                ,
                Map.of("id",userId)
                );
    }

    public TransactionResponseDTO post(TransferRequestDTO transferRequestDTO) {
      return genericClientService.post(
              ServicesUrlConstant.TRANS_FUND_URL,
              transferRequestDTO,
              TransactionResponseDTO.class
      );
    }

    public CustomerResponseDTO get(String accountId){
      return genericClientService.get(
              ServicesUrlConstant.ACCOUNT_CHECK_CUSTOMER_URL,
              CustomerResponseDTO.class,
              Map.of("accountId", accountId)
      );
    }

}
