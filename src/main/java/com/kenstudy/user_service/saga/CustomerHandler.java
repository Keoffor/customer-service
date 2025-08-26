package com.kenstudy.user_service.saga;

import com.kenstudy.event.CustomerEvent;
import com.kenstudy.event.TransactEvent;
import com.kenstudy.event.status.CustomerStatus;
import com.kenstudy.transaction.TransferRequestDTO;

import com.kenstudy.user_service.exception.ResourceNotFoundException;
import com.kenstudy.user_service.exception.TransactionFailedException;
import com.kenstudy.user_service.exception.UserNotFoundException;
import com.kenstudy.user_service.model.TransferStatus;
import com.kenstudy.user_service.model.Users;
import com.kenstudy.user_service.repository.TransferStatusRepo;
import com.kenstudy.user_service.repository.UserRepository;
import com.kenstudy.user_service.services.customImpl.UserServiceImpl;
import com.kenstudy.user_service.util.CusTransResponseDto;
import com.kenstudy.utils.CancelUtility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.stream.Streams;
import org.apache.kafka.shaded.com.google.protobuf.MapEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@Slf4j
public class CustomerHandler {

    private final UserRepository customerRepo;
    private final TransferStatusRepo transferStatusRepo;

    @Autowired
    public CustomerHandler(UserRepository customerRepo, TransferStatusRepo transferStatusRepo) {
        this.customerRepo = customerRepo;
        this.transferStatusRepo = transferStatusRepo;
    }

    public void updateTransactConsumeEvent(TransferRequestDTO dto, UUID custEventId) {
        String corrId = String.valueOf(custEventId).trim();
        if (ObjectUtils.isEmpty(dto) || ObjectUtils.isEmpty(custEventId)) {
            throw new ResourceNotFoundException("TransactEvent is null or empty. Cannot proceed with transaction.");
        }

        if (ObjectUtils.isEmpty(dto.getSenderId()) ||
                ObjectUtils.isEmpty(dto.getSenderAcctId()) ||
                ObjectUtils.isEmpty(dto.getRecipientId()) ||
                ObjectUtils.isEmpty(dto.getRecipientAcctId())) {
            log.error("Missing required account fields in DTO:::===::: {}", dto);
            throw new ResourceNotFoundException("Missing required account fields.");
        }
        ConcurrentHashMap<Integer, String> catchEventId = UserServiceImpl.catchEventId;
        String storedCorrId = catchEventId.get(dto.getSenderId());
        if (!corrId.equals(storedCorrId)) {
            // Clean up to avoid memory leaks
            catchEventId.remove(dto.getSenderId());
            throw new TransactionFailedException(
                    "Transfer request correlationId does not match for eventId=" + storedCorrId +
                            " (expected=" + storedCorrId + ", actual=" + corrId + ")"
            );
        }
        catchEventId.remove(dto.getSenderId(),storedCorrId);

    //Happy path - save and populate response to client
        transferStatusRepo.findByCorrelationId(String.valueOf(custEventId))
            .map(tranStatus -> {
                tranStatus.setStatus(CustomerStatus.SUCCESS.name());
                tranStatus.setDateTime(LocalDateTime.now());
                tranStatus.setTransactId(dto.getTransactId());
                tranStatus.setReason(null);
                log.info("TransactStatus was updated successfully {} ", custEventId);
                return transferStatusRepo.save(tranStatus);
            })
            .orElseThrow(() -> new TransactionFailedException("No transfer status found for correlationId: " + custEventId));
}



    public void handleTransactFailure(TransactEvent tranEvent) {
        String message = tranEvent.getErrorMessage()!= null ?
                tranEvent.getErrorMessage(): "transaction failed to process event";
        String corrId = String.valueOf(tranEvent.getCustomerEventId()).trim();

        transferStatusRepo.findByCorrelationId(corrId)
                .map(tranStatus -> {
                    tranStatus.setStatus(CustomerStatus.FAILURE.name());
                    tranStatus.setDateTime(LocalDateTime.now());
                    tranStatus.setTransactId(tranEvent.getTransRequestDTO().getTransactId());
                    tranStatus.setReason(message);
                    return transferStatusRepo.save(tranStatus);
                })
                .orElseThrow(() -> new TransactionFailedException("No transfer status found for correlationId: " +
                        tranEvent.getCustomerEventId()));
    }
}