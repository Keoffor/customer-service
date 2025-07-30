package com.kenstudy.user_service.saga;

import com.kenstudy.event.CustomerEvent;
import com.kenstudy.event.status.CustomerStatus;
import com.kenstudy.transaction.TransferRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

@Component
public class CustomerPublisher {
    @Autowired
    private Sinks.Many<CustomerEvent> usersSinks;

    public void publishCustomerEvent(TransferRequestDTO transDto, CustomerStatus cusStatus){
        CustomerEvent cusEvent = new CustomerEvent(transDto, cusStatus, "");
        usersSinks.tryEmitNext(cusEvent);
    }
}
