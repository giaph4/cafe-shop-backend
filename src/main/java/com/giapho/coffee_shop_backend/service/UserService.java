package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Role;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.repository.RoleRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.dto.ChangePasswordRequestDTO;
import com.giapho.coffee_shop_backend.dto.RoleDTO;
import com.giapho.coffee_shop_backend.dto.UserResponseDTO;
import com.giapho.coffee_shop_backend.dto.UserUpdateRequestDTO;
import com.giapho.coffee_shop_backend.mapper.RoleMapper;
import com.giapho.coffee_shop_backend.mapper.UserMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleMapper roleMapper;

    @Transactional(readOnly = true)
    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        return userPage.map(userMapper::toUserResponseDto);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        return userMapper.toUserResponseDto(user);
    }

    @Transactional
    public UserResponseDTO updateUser(Long id, UserUpdateRequestDTO updateDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (!existingUser.getPhone().equals(updateDTO.getPhone())
                && userRepository.existsByPhone(updateDTO.getPhone())) {
            throw new IllegalArgumentException("Phone number already exists: " + updateDTO.getPhone());
        }

        if (updateDTO.getEmail() != null && !updateDTO.getEmail().isEmpty()
                && !updateDTO.getEmail().equals(existingUser.getEmail())
                && userRepository.existsByEmail(updateDTO.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + updateDTO.getEmail());
        }

        Set<Role> roles = new HashSet<>();
        if (updateDTO.getRoleIds() != null) {
            for (Long roleId : updateDTO.getRoleIds()) {
                Role role = roleRepository.findById(roleId)
                        .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + roleId));
                roles.add(role);
            }
        }

        if (roles.isEmpty()) {
            throw new IllegalArgumentException("User must have at least one role.");
        }

        userMapper.updateUserFromDto(updateDTO, existingUser);

        if (updateDTO.getAvatarUrl() != null) {
            String trimmedAvatarUrl = updateDTO.getAvatarUrl().trim();
            existingUser.setAvatarUrl(trimmedAvatarUrl.isEmpty() ? null : trimmedAvatarUrl);
        } else if (Boolean.TRUE.equals(updateDTO.getRemoveAvatar())) {
            existingUser.setAvatarUrl(null);
        }

        if (updateDTO.getAddress() != null) {
            String trimmedAddress = updateDTO.getAddress().trim();
            existingUser.setAddress(trimmedAddress.isEmpty() ? null : trimmedAddress);
        }

        existingUser.setRoles(roles);

        User updatedUser = userRepository.save(existingUser);
        return userMapper.toUserResponseDto(updatedUser);
    }

    @Transactional
    public void changePassword(ChangePasswordRequestDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            throw new IllegalStateException("User not authenticated");
        }

        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found in database"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException("Incorrect current password");
        }

        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new IllegalArgumentException("New password and confirmation password do not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as the old password");
        }

        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
        currentUser.setPassword(encodedNewPassword);
        userRepository.save(currentUser);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));
        return userMapper.toUserResponseDto(user);
    }
}
