package com.kenstudy.user_service.saga;

import com.kenstudy.event.CustomerEvent;
import com.kenstudy.event.TransactEvent;
import com.kenstudy.event.status.CustomerStatus;
import com.kenstudy.event.status.TransStatus;
import com.kenstudy.transaction.TransferRequestDTO;
import com.kenstudy.utils.CancelUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
public class CustomerConsumer {
    private final ConstumerHandler handler;

    @Autowired
    public CustomerConsumer(ConstumerHandler handler) {
        this.handler = handler;
    }


    @Bean
    public Function<TransactEvent, CustomerEvent> updateTransactionAccount() {
        return this::processConsumeEventTransact;
    }
    private CustomerEvent processConsumeEventTransact(TransactEvent transactEvent) {
        if (TransStatus.TRANSACTION_COMPLETED.equals(transactEvent.getTransStatus())
                && transactEvent.isEventClosed()) {
            return this.handler.updateTransactConsumeEvent(transactEvent);
        } else {
            CustomerEvent customerEvent = new CustomerEvent();
            TransferRequestDTO dto = transactEvent.getTransRequestDTO();
            customerEvent.setTransRequestDTO(dto);
            return CancelUtility.cancelEventUtility(customerEvent,
                    dto,
                    customerEvent.getErrorMessage(),
                    CustomerStatus.TRANSFER_FAILED);
        }
    }

}
