package com.kenstudy.user_service.saga.saga_config;

import com.kenstudy.event.CustomerEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.function.Supplier;

@Configuration
public class CustomerPublisherConfig {

    @Bean
    public Sinks.Many<CustomerEvent> usersSinks(){

        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Supplier<Flux<CustomerEvent>> usersSupplier(Sinks.Many<CustomerEvent> sinks){

        return sinks::asFlux;
    }
}
