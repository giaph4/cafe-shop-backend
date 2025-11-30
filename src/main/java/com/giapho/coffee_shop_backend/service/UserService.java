package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.ChangePasswordRequestDTO;
import com.giapho.coffee_shop_backend.dto.RegisterRequest;
import com.giapho.coffee_shop_backend.dto.RoleDTO;
import com.giapho.coffee_shop_backend.dto.UserResponseDTO;
import com.giapho.coffee_shop_backend.dto.UserUpdateRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    UserResponseDTO registerUser(RegisterRequest registerRequest);
    
    List<RoleDTO> getAllRoles();

    Page<UserResponseDTO> getAllUsers(Pageable pageable);

    UserResponseDTO getUserById(Long id);

    UserResponseDTO updateUser(Long id, UserUpdateRequestDTO updateDTO);

    void changePassword(ChangePasswordRequestDTO request);

    UserResponseDTO getUserByUsername(String username);
    
    /**
     * Reset user's password to a secure temporary password and notify user via email
     * @param id The ID of the user whose password needs to be reset
     * @return The generated temporary password (for development/testing purposes)
     * @throws UserNotFoundException if user is not found
     */
    String resetPassword(Long id);
}
