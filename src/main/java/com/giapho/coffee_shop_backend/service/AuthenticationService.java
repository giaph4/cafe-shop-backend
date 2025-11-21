package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.AuthenticationResponse;
import com.giapho.coffee_shop_backend.dto.LoginRequest;
import com.giapho.coffee_shop_backend.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthenticationService {

    AuthenticationResponse register(RegisterRequest request);

    AuthenticationResponse login(LoginRequest request, HttpServletRequest httpServletRequest);
}
