package com.eventflow.eventflow.service;

import com.eventflow.eventflow.dto.request.LoginRequest;
import com.eventflow.eventflow.dto.response.LoginResponse;
import com.eventflow.eventflow.exception.InvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }


    public LoginResponse login(LoginRequest request) {

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        } catch (AuthenticationException ex) {
            // Disabled/locked accounts or an internal failure while loading the user.
            // Without this, the exception escapes to the JWT entry point and the caller
            // sees a generic "Authentication required" with nothing in the logs.
            Throwable root = ex;
            while (root.getCause() != null) root = root.getCause();
            log.warn("Login failed for {}: {} ({}: {})", request.email(),
                    ex.getClass().getSimpleName(), root.getClass().getSimpleName(), root.getMessage());
            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        String token = jwtService.generateToken(userDetails);

        return new LoginResponse(token);
    }


}