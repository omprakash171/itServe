package com.itserve.service;

import com.itserve.dto.CreateUserRequest;
import com.itserve.dto.UpdateUserRequest;
import com.itserve.dto.UserResponse;
import com.itserve.entity.Role;
import com.itserve.entity.User;
import com.itserve.exception.DuplicateUserException;
import com.itserve.exception.ForbiddenActionException;
import com.itserve.exception.ResourceNotFoundException;
import com.itserve.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list(Role roleFilter) {
        List<User> users = roleFilter == null ? userRepository.findAll() : userRepository.findByRole(roleFilter);
        return users.stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<com.itserve.dto.AgentSummaryResponse> listActiveAgents() {
        return userRepository.findByRoleAndActiveTrue(Role.SUPPORT_AGENT).stream()
                .sorted(java.util.Comparator.comparing(User::getFullName))
                .map(com.itserve.dto.AgentSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserResponse.from(find(id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateUserException("An account with email " + email + " already exists");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setDepartment(request.department());
        user.setSkills(request.skills() == null ? null : String.join(",", request.skills()));
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = find(id);
        String email = request.email().trim().toLowerCase();
        if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new DuplicateUserException("An account with email " + email + " already exists");
        }
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setDepartment(request.department());
        user.setSkills(request.skills() == null ? null : String.join(",", request.skills()));
        if (request.active() != null) {
            user.setActive(request.active());
        }
        return UserResponse.from(user);
    }

    /** Deactivates the account (soft delete) so ticket history stays intact. */
    @Transactional
    public void deactivate(Long id, User currentUser) {
        if (currentUser.getId().equals(id)) {
            throw new ForbiddenActionException("You cannot deactivate your own account");
        }
        User user = find(id);
        user.setActive(false);
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
