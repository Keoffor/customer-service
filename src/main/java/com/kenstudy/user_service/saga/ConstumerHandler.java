package com.kenstudy.user_service.saga;

import com.kenstudy.event.CustomerEvent;
import com.kenstudy.event.TransactEvent;
import com.kenstudy.event.status.CustomerStatus;
import com.kenstudy.transaction.TransferRequestDTO;

import com.kenstudy.user_service.exception.UserNotFoundException;
import com.kenstudy.user_service.model.Users;
import com.kenstudy.user_service.repository.UserRepository;
import com.kenstudy.utils.CancelUtility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class ConstumerHandler {

    private final UserRepository customerRepo;

    @Autowired
    public ConstumerHandler(UserRepository customerRepo) {
        this.customerRepo = customerRepo;
    }


    public CustomerEvent updateTransactConsumeEvent(TransactEvent transactEvent) {
        CustomerEvent customerEvent = new CustomerEvent();
        TransferRequestDTO dto = transactEvent.getTransRequestDTO();
        if (ObjectUtils.isEmpty(dto)) {
            CancelUtility.cancelEventUtility(customerEvent, dto,
                    "TransactEvent is null or empty. Cannot proceed with transaction.",
                    CustomerStatus.TRANSFER_FAILED);
        }
        if (ObjectUtils.isEmpty(dto.getSenderId()) ||
                ObjectUtils.isEmpty(dto.getSenderAcctId()) ||
                ObjectUtils.isEmpty(dto.getRecipientId()) ||
                ObjectUtils.isEmpty(dto.getRecipientAcctId())) {
            log.error("Missing required account fields in DTO:::===::: {}", dto);
            return CancelUtility.cancelEventUtility(customerEvent, dto, "Missing required account fields",
                    CustomerStatus.TRANSFER_FAILED);
        }
        List<Integer> ids = Arrays.asList(dto.getSenderId(), dto.getRecipientId());
        ConcurrentMap<Integer, Users> senderAndReceiver = customerRepo.findSenderAndReceiver(ids)
                .stream()
                .collect(Collectors.toConcurrentMap(Users::getId, Function.identity()));
        if (ObjectUtils.isEmpty(senderAndReceiver)) {
            throw new UserNotFoundException("sender or receiver not found");
        }
        Users sender = senderAndReceiver.get(dto.getSenderId());
        Users receiver = senderAndReceiver.get(dto.getRecipientId());
        boolean verify = sender.getId().equals(dto.getSenderId()) && receiver.getId().equals(dto.getRecipientId());

        if (!verify) {
            throw new UserNotFoundException("senderId or receiverId does not match account");
        }
        sender.setStatus(CustomerStatus.TRANSFER_CREATED.name());
        sender.setLocalDate(LocalDate.now());
        receiver.setStatus(CustomerStatus.TRANSFER_CREATED.name());
        receiver.setLocalDate(LocalDate.now());
        customerRepo.saveAll(Arrays.asList(sender, receiver));
        dto.setStatus(CustomerStatus.TRANSFER_COMPLETED.name());
        customerEvent.setTransRequestDTO(dto);
        customerEvent .setStatus(CustomerStatus.TRANSFER_CREATED);
        customerEvent.setIsEventClosed(true);
        //produce event
        return customerEvent;
    }



}