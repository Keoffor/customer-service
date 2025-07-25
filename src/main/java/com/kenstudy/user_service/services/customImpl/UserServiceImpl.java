package com.kenstudy.user_service.services.customImpl;





import com.kenstudy.account.AccountBalanceDTO;
import com.kenstudy.bank.BankAddress;
import com.kenstudy.customer.CustomerAccount;
import com.kenstudy.customer.CustomerRequestDTO;
import com.kenstudy.customer.CustomerResponseDTO;
import com.kenstudy.transaction.TransactionResponseDTO;
import com.kenstudy.transaction.TransferRequestDTO;
import com.kenstudy.user_service.client.account.CustomersClient;
import com.kenstudy.user_service.event.BroadCastService;
import com.kenstudy.user_service.exception.ResourceNotFoundException;
import com.kenstudy.user_service.exception.TokenNotFoundException;
import com.kenstudy.user_service.exception.UserNotFoundException;
import com.kenstudy.user_service.model.Users;
import com.kenstudy.user_service.model.VerificationToken;
import com.kenstudy.user_service.repository.UserRepository;
import com.kenstudy.user_service.repository.VerificationTokenRepo;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepo verificationTokenRepo;
    private final CustomersClient customersClient;
    private final BroadCastService broadCastService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, VerificationTokenRepo verificationTokenRepo,
                   CustomersClient customersClient, BroadCastService broadCastService) {
        this.userRepository = userRepository;
        this.verificationTokenRepo = verificationTokenRepo;
        this.customersClient = customersClient;
        this.broadCastService = broadCastService;
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
        if (dto == null || dto.getCustomerId() == null ||
                dto.getRecipientId() == null || dto.getAmount() == null) {
            throw new ResourceNotFoundException("Transfer request or required fields must not be null");
        }

        Optional<Users> users = userRepository.findById(dto.getCustomerId());
        if (users.isEmpty()) {
            throw new UserNotFoundException("Customer with ID " + dto.getCustomerId() + " does not exist");
        }
        Users user = users.get();
        String sender = user.getName();

        CustomerResponseDTO receiver;
        try {
            String receiverId = String.valueOf(dto.getRecipientId());
            receiver = customersClient.get(receiverId);

        } catch (ResourceNotFoundException ex) {
            throw new ResourceNotFoundException("Failed to fetch recipient details: " + ex.getMessage());
        }

        if (receiver == null) {
            throw new UserNotFoundException("Receiver Account not found");
        }
        String receiverName = receiver.getName();

        return mapToResponse(customersClient.post(dto), sender, receiverName);
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


    private CusTransResponseDto mapToResponse(TransactionResponseDTO dto, String sender, String receiverName) {
        CusTransResponseDto res = new CusTransResponseDto();
        res.setTransferBy(sender);
        res.setAmount(dto.getAmount());
        res.setDescription(dto.getDescription());
        res.setCreatedDated(dto.getCreatedDated());
        res.setTransactionType(dto.getTransactionType());
        res.setTransactionId(dto.getTransactionId());
        res.setReceiver(receiverName);
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


