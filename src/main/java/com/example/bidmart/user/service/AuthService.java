package com.example.bidmart.user.service;

import com.example.bidmart.user.model.User;

public interface AuthService {
    User register(String username, String email, String displayName, String password);
}