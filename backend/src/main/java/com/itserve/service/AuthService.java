package com.itserve.service;

import com.itserve.dto.LoginRequest;
import com.itserve.dto.LoginResponse;
import com.itserve.dto.RegisterRequest;
import com.itserve.dto.UserResponse;
import com.itserve.entity.Role;
import com.itserve.entity.User;
import com.itserve.exception.DuplicateUserException;
import com.itserve.exception.ForbiddenActionException;
import com.itserve.repository.UserRepository;
import com.itserve.security.JwtService;
import com.itserve.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);
        return new LoginResponse(token, UserResponse.from(principal.getUser()));
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateUserException("An account with email " + request.email() + " already exists");
        }
        // Self-registration always creates an EMPLOYEE. Elevated roles are granted by admins only.
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.EMPLOYEE);
        user.setDepartment(request.department());
        userRepository.save(user);

        String token = jwtService.generateToken(new UserPrincipal(user));
        return new LoginResponse(token, UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ForbiddenActionException("Account no longer exists"));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse changePassword(UserPrincipal principal, String currentPassword, String newPassword) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ForbiddenActionException("Account no longer exists"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ForbiddenActionException("Current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        return UserResponse.from(user);
    }
}
