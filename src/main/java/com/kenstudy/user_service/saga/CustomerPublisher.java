package com.kenstudy.user_service.saga;

import com.kenstudy.event.CustomerEvent;
import com.kenstudy.event.TransactEvent;
import com.kenstudy.event.status.CustomerStatus;
import com.kenstudy.transaction.TransferRequestDTO;
import com.kenstudy.user_service.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
@Slf4j
public class CustomerPublisher {
    @Autowired
    private Sinks.Many<CustomerEvent> usersSinks;

    public void publishCustomerEvent(CustomerEvent cusEvent){
        Sinks.EmitResult result = usersSinks.tryEmitNext(cusEvent);

        if (result.isFailure()) {
            log.error(" Failed to emit Event:::====:::::: {} ", result.name());
            throw new ResourceNotFoundException("Failed to emit CustomerEvent: " + result.name());
        }
    }

}
