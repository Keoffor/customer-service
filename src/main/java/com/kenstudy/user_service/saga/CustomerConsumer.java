package com.kenstudy.user_service.saga;

import com.kenstudy.event.CustomerEvent;
import com.kenstudy.event.TransactEvent;
import com.kenstudy.event.status.CustomerStatus;
import com.kenstudy.event.status.TransStatus;
import com.kenstudy.transaction.TransferRequestDTO;
import com.kenstudy.utils.CancelUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Consumer;

@Component
@Slf4j
public class CustomerConsumer {
    private final CustomerHandler handler;

    @Autowired
    public CustomerConsumer(CustomerHandler handler) {
        this.handler = handler;
    }


    @Bean
    public Consumer<TransactEvent> customerEventSuccess() {
        return transEvent -> {
            log.info("catch success event {} ",transEvent.getCustomerEventId());
            if (!transEvent.isError() || TransStatus.TRANSACTION_COMPLETED.equals(transEvent.getTransStatus())) {
                handleTransactSuccess(transEvent.getTransRequestDTO(), transEvent.getCustomerEventId());
            } else {
                // optional logging if a bad event sneaks in
                log.warn("Received unexpected event on transactEventSuccess: {}", transEvent);
            }
        };
    }
    @Bean
    public Consumer<TransactEvent> customerEventFailure() {
        return transEvent -> {
            log.info("catch failure event {} ",transEvent.getCustomerEventId());
            if (transEvent.isError() || TransStatus.TRANSACTION_FAILED.equals(transEvent.getTransStatus())) {
                handleFailure(transEvent);
            } else {

                log.warn("Received non-error event on transactEventFailure: {}", transEvent);
            }
        };
    }

    private void handleTransactSuccess(TransferRequestDTO dto, UUID cusId) {
        handler.updateTransactConsumeEvent(dto, cusId);
    }
    private void handleFailure(TransactEvent transactEvent) {
        handler.handleTransactFailure(transactEvent);
    }



}
