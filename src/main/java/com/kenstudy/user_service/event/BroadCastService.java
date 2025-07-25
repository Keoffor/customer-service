package com.kenstudy.user_service.event;

import com.kenstudy.user_service.model.Users;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;


import java.util.Locale;
@Component
@Slf4j
public class BroadCastService {

    private final ApplicationEventPublisher publisher;

    public BroadCastService(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void broadCast(Users users, String apiUrl){
        CustomerRegisterEvent event = new CustomerRegisterEvent(
                this, users,apiUrl,Locale.US);
        publisher.publishEvent(event);


    }
}
