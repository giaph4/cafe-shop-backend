package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.Role;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.repository.RoleRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.dto.ChangePasswordRequestDTO;
import com.giapho.coffee_shop_backend.dto.RoleDTO;
import com.giapho.coffee_shop_backend.dto.UserResponseDTO;
import com.giapho.coffee_shop_backend.dto.UserUpdateRequestDTO;
import com.giapho.coffee_shop_backend.exception.role.RoleNotFoundException;
import com.giapho.coffee_shop_backend.exception.user.UserEmailAlreadyExistsException;
import com.giapho.coffee_shop_backend.exception.user.UserInvalidPasswordException;
import com.giapho.coffee_shop_backend.exception.user.UserNotAuthenticatedException;
import com.giapho.coffee_shop_backend.exception.user.UserNotFoundException;
import com.giapho.coffee_shop_backend.exception.user.UserPasswordConfirmationMismatchException;
import com.giapho.coffee_shop_backend.exception.user.UserPasswordUnchangedException;
import com.giapho.coffee_shop_backend.exception.user.UserPhoneAlreadyExistsException;
import com.giapho.coffee_shop_backend.exception.user.UserRequiresRoleException;
import com.giapho.coffee_shop_backend.exception.user.UserInvalidRequestException;
import com.giapho.coffee_shop_backend.mapper.RoleMapper;
import com.giapho.coffee_shop_backend.mapper.UserMapper;
import com.giapho.coffee_shop_backend.service.UserService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toUserResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        User user = getUserOrThrow(id);
        return userMapper.toUserResponseDto(user);
    }

    @Override
    public UserResponseDTO updateUser(Long id, UserUpdateRequestDTO updateDTO) {
        User existingUser = getUserOrThrow(id);
        sanitizeRequest(updateDTO);
        validateUniqueConstraints(id, updateDTO);

        Set<Role> roles = resolveRoles(updateDTO.getRoleIds());

        userMapper.updateUserFromDto(updateDTO, existingUser);
        updateAvatar(existingUser, updateDTO);
        updateAddress(existingUser, updateDTO);

        existingUser.setRoles(roles);

        User updatedUser = userRepository.save(existingUser);
        return userMapper.toUserResponseDto(updatedUser);
    }

    @Override
    public void changePassword(ChangePasswordRequestDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            throw new UserNotAuthenticatedException();
        }

        User currentUser = loadUserByUsername(authentication.getName());
        validatePasswordChange(request, currentUser);

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByUsername(String username) {
        User user = loadUserByUsername(username);
        return userMapper.toUserResponseDto(user);
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private User loadUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    private void validateUniqueConstraints(Long userId, UserUpdateRequestDTO updateDTO) {
        String phone = updateDTO.getPhone();
        if (userRepository.existsByPhoneAndIdNot(phone, userId)) {
            throw new UserPhoneAlreadyExistsException(phone);
        }

        String email = updateDTO.getEmail();
        if (StringUtils.hasText(email) && userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new UserEmailAlreadyExistsException(email);
        }
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            throw new UserRequiresRoleException();
        }

        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            Set<Long> foundIds = roles.stream()
                    .map(Role::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Long missingId = roleIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .orElse(null);
            throw new RoleNotFoundException(missingId);
        }

        return new HashSet<>(roles);
    }

    private void sanitizeRequest(UserUpdateRequestDTO updateDTO) {
        updateDTO.setFullName(normalizeRequired(updateDTO.getFullName(), "Full name"));
        updateDTO.setPhone(normalizeRequired(updateDTO.getPhone(), "Phone number"));
        updateDTO.setStatus(normalizeRequired(updateDTO.getStatus(), "Status"));
        updateDTO.setEmail(normalizeNullable(updateDTO.getEmail()));
        updateDTO.setAvatarUrl(normalizeNullable(updateDTO.getAvatarUrl()));
        updateDTO.setAddress(normalizeNullable(updateDTO.getAddress()));
    }

    private void updateAvatar(User user, UserUpdateRequestDTO updateDTO) {
        if (Boolean.TRUE.equals(updateDTO.getRemoveAvatar())) {
            user.setAvatarUrl(null);
            return;
        }
        user.setAvatarUrl(updateDTO.getAvatarUrl());
    }

    private void updateAddress(User user, UserUpdateRequestDTO updateDTO) {
        user.setAddress(updateDTO.getAddress());
    }

    private void validatePasswordChange(ChangePasswordRequestDTO request, User user) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UserInvalidPasswordException();
        }
        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new UserPasswordConfirmationMismatchException();
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new UserPasswordUnchangedException();
        }
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private String normalizeRequired(@NotNull String value, String fieldName) {
        String normalized = normalizeNullable(value);
        if (!StringUtils.hasText(normalized)) {
            throw new UserInvalidRequestException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
