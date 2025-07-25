package com.kenstudy.user_service.exception;

public class TokenNotFoundException extends RuntimeException{

    public TokenNotFoundException(String message){
        super(message);
    }
}
