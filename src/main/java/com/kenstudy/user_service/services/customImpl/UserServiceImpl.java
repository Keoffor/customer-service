package com.kenstudy.user_service.services.customImpl;





import com.kenstudy.account.AccountBalanceDTO;
import com.kenstudy.bank.BankAddress;
import com.kenstudy.customer.CustomerAccount;
import com.kenstudy.customer.CustomerRequestDTO;
import com.kenstudy.customer.CustomerResponseDTO;

import com.kenstudy.event.CustomerEvent;
import com.kenstudy.event.status.CustomerStatus;
import com.kenstudy.transaction.TransferRequestDTO;
import com.kenstudy.user_service.client.account.CustomersClient;
import com.kenstudy.user_service.event.BroadCastService;
import com.kenstudy.user_service.exception.ResourceNotFoundException;
import com.kenstudy.user_service.exception.TokenNotFoundException;
import com.kenstudy.user_service.exception.TransactionFailedException;
import com.kenstudy.user_service.exception.UserNotFoundException;
import com.kenstudy.user_service.model.TransferStatus;
import com.kenstudy.user_service.model.Users;
import com.kenstudy.user_service.model.VerificationToken;
import com.kenstudy.user_service.repository.TransferStatusRepo;
import com.kenstudy.user_service.repository.UserRepository;
import com.kenstudy.user_service.repository.VerificationTokenRepo;
import com.kenstudy.user_service.saga.CustomerHandler;
import com.kenstudy.user_service.saga.CustomerPublisher;
import com.kenstudy.user_service.services.UserService;
import com.kenstudy.user_service.util.CusTransResponseDto;
import com.kenstudy.user_service.util.UserHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepo verificationTokenRepo;
    private final CustomersClient customersClient;
    private final TransferStatusRepo transStatusRepo;
    private final BroadCastService broadCastService;
    private final CustomerPublisher customerPublisher;
    public static ConcurrentHashMap<Integer, String> catchEventId = new ConcurrentHashMap<>();
    @Autowired
    public UserServiceImpl(UserRepository userRepository, VerificationTokenRepo verificationTokenRepo,
                           CustomersClient customersClient, BroadCastService broadCastService,
                           CustomerPublisher customerPublisher, TransferStatusRepo transStatusRepo) {
        this.userRepository = userRepository;
        this.verificationTokenRepo = verificationTokenRepo;
        this.customersClient = customersClient;
        this.broadCastService = broadCastService;
        this.customerPublisher = customerPublisher;
        this.transStatusRepo = transStatusRepo;
    }

    @Override
    @Transactional
    public CustomerResponseDTO createCustomer(CustomerRequestDTO customer, HttpServletRequest request) {
        if(ObjectUtils.isEmpty(customer) || StringUtils.isEmpty(customer.getEmail())){
            log.info("Email must not be blank or empty  ::::::::::");
            throw new UserNotFoundException("email must not be blank or empty");
        }
        if(userRepository.existsByEmail(customer.getEmail())){
            log.info("Email address already exist  :::::::::: {}", customer.getEmail());
            throw new UserNotFoundException("email already exist");

        }
        Users save = userRepository.save(UserHelper.parseToUsers(customer));
        //publish customer email verification event
        String trimmed = request.getRequestURI();
        String apiUrl = trimmed.substring(0, trimmed.lastIndexOf("/"));
        try {
            broadCastService.broadCast(save,apiUrl);
        } catch (Exception e) {
            log.error("Error occurred Log::: ", e);
            throw new ResourceNotFoundException("Error has occurred "+ e.getMessage());
        }

        String cusId = String.valueOf(save.getId());
        AccountBalanceDTO client = customersClient.post(cusId);

        if(ObjectUtils.isEmpty(client)) {
            log.info("error occurred: unable to initialize customer account :::::::::: {}", client);
            throw new ResourceNotFoundException("error occurred: unable to initialize customer account");
        }
        return  mapCustomerAndBalanceResponse(save,client);

    }

    @Override
    public CustomerResponseDTO getUser(Integer userId) {
        if (ObjectUtils.isEmpty(userId)) {

            throw new UserNotFoundException("User ID must not be null or empty");
        }
        Users users = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        return mapCustomerResponse(users);
    }

    @Override
    @Transactional
    public CusTransResponseDto transferFund(TransferRequestDTO dto) {
        TransferStatus status = new TransferStatus();

        if (dto == null || dto.getSenderId() == null ||
                dto.getRecipientAcctId() == null || dto.getAmount() == null) {
            throw new ResourceNotFoundException("Transfer request or required fields must not be null");
        }

        List<Integer> ids = Arrays.asList(dto.getSenderId(), dto.getRecipientId());

        List<Users> users = userRepository.findSenderAndReceiver(ids);

        Map<Integer, Users> mapUsers = users.stream()
                .collect(Collectors.toConcurrentMap(Users::getId, Function.identity()));

        Users sender = mapUsers.get(dto.getSenderId());
        Users receiver = mapUsers.get(dto.getRecipientId());

        if (sender == null || receiver == null) {
            throw new UserNotFoundException("Sender or receiver not found.");
        }

        if (dto.getAmount() < 2 || dto.getAmount() > 10000) {
            throw new UserNotFoundException("Daily transfer must be from $2 and not exceed $10000");
        }

        //publish event
           dto.setStatus(CustomerStatus.PENDING.name());
           CustomerEvent cusEvent = new CustomerEvent(dto, false,CustomerStatus.PENDING,
                   "", false);

        //persist data
        String corrId = String.valueOf(cusEvent.getEventId()).trim();

        catchEventId.put(sender.getId(), corrId);

        status.setStatus(CustomerStatus.PENDING.name());
        status.setDateTime(LocalDateTime.now());
        status.setCorrelationId(corrId);
        transStatusRepo.save(status);

           //produce event
           customerPublisher.publishCustomerEvent(cusEvent);
           //return response
         return mapToResponse(dto,sender, receiver.getName(),cusEvent.getEventId());

    }

    @Override
    public void createVerificationToken(Users users, String token) {
      VerificationToken veriToken = new VerificationToken(users,token);
        verificationTokenRepo.save(veriToken);
    }

    @Override
    @Transactional
    public VerificationToken getVerifiedToken(String token) {
        return verificationTokenRepo.findByToken(token);
    }

    @Override
    public String confirmRegistration(VerificationToken verifier, HttpServletRequest request) {

        Users users = verifier.getUsers();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = verifier.getExpiredDate();

        if(ObjectUtils.isEmpty(verifier) ){
            throw new TokenNotFoundException("Invalid token");
        }else if(expiry.isBefore(now)) {
            throw new TokenNotFoundException("This link has expired");
        }
        else if(users.isEnabled()){
            throw new TokenNotFoundException("the link is no longer valid. Expired!");
        }else {

            users.setEnabled(true);
            users.setLocalDate(LocalDate.now());
            userRepository.save(users);
            return "confirmation was successful!";
        }
    }

    @Override
    public CusTransResponseDto getTransferStatus(String correlationId) {

        return transStatusRepo.findByCorrelationId(correlationId)
                .map(tranStatus -> {
                    CusTransResponseDto response = new CusTransResponseDto();
                    response.setTransactionType("Transfer");
                    response.setStatus(tranStatus.getStatus());
                    response.setTransactId(tranStatus.getTransactId());
                    response.setTransferEventId(tranStatus.getCorrelationId());
                    response.setCreatedDated(tranStatus.getDateTime());
                    response.setReason(tranStatus.getReason());
                    return response;
                }).orElseThrow(() -> new TransactionFailedException("correlation Id = " + correlationId + " not found"));
    }



    private CusTransResponseDto mapToResponse(TransferRequestDTO dto, Users sender, String receiverName, UUID transId) {
        CusTransResponseDto res = new CusTransResponseDto();
        res.setTransferBy(sender.getName());
        res.setAmount(dto.getAmount());
        res.setDescription(dto.getDescription());
        res.setCreatedDated(sender.getLocalDate().atStartOfDay());
        res.setReceiver(receiverName);
        res.setStatus(CustomerStatus.PENDING.name());
        res.setTransferEventId(String.valueOf(transId));
        res.setTransactionType("Transfer");
        return res;
    }


    private CustomerResponseDTO mapCustomerResponse(Users users){
        CustomerResponseDTO customerResponseDTO = new CustomerResponseDTO();
        customerResponseDTO.setId(users.getId());
        customerResponseDTO.setName(users.getName());
        customerResponseDTO.setEmail(users.getEmail());
        customerResponseDTO.setUsername(users.getUsername());
        customerResponseDTO.setBankAddress(mapBankAddress(users));
        return customerResponseDTO;
    }

    private CustomerResponseDTO mapCustomerAndBalanceResponse(Users users, AccountBalanceDTO balanceDTO){
        CustomerResponseDTO customerResponseDTO = new CustomerResponseDTO();
        customerResponseDTO.setId(users.getId());
        customerResponseDTO.setName(users.getName());
        customerResponseDTO.setEmail(users.getEmail());
        customerResponseDTO.setUsername(users.getUsername());
        customerResponseDTO.setBankAddress(mapBankAddress(users));
        customerResponseDTO.setCustomerAccounts(mapCustomerAcct(balanceDTO));
        customerResponseDTO.setLocalDate(users.getLocalDate());
        return customerResponseDTO;
    }

    private List<BankAddress> mapBankAddress(Users users) {
        if (ObjectUtils.isEmpty(users.getAddress())) {
            return Collections.emptyList();
        }
        return users.getAddress().stream().map(address -> {
            BankAddress bankAddress = new BankAddress();
            bankAddress.setId(address.getId());
            bankAddress.setZipCode(address.getZipCode());
            bankAddress.setState(address.getState());
            bankAddress.setStreet(address.getStreet());
            bankAddress.setCity(address.getCity());
            return bankAddress;
        }).collect(Collectors.toList());
    }

    private List<CustomerAccount> mapCustomerAcct(AccountBalanceDTO balanceDTO) {
        CustomerAccount account = new CustomerAccount();
        account.setAccountType(balanceDTO.getAccountType());
        account.setAccountNumber(balanceDTO.getAccountNumber());
        account.setBalance(balanceDTO.getBalance());
        return List.of(account);
    }

}


