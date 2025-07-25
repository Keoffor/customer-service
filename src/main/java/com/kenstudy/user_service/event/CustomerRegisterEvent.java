package com.kenstudy.user_service.event;

import com.kenstudy.user_service.model.Users;
import org.springframework.context.ApplicationEvent;


import java.util.Locale;


public class CustomerRegisterEvent extends ApplicationEvent {
    private String apiUrl;
    private Locale locale;
    private Users users;



    public CustomerRegisterEvent(Object source, Users users, String apiUrl, Locale locale) {
        super(source);
        this.apiUrl = apiUrl;
        this.locale = locale;
        this.users = users;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }
}
