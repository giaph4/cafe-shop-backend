package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Role;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.repository.RoleRepository; // Import RoleRepository
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.dto.ChangePasswordRequestDTO;
import com.giapho.coffee_shop_backend.dto.UserResponseDTO;
import com.giapho.coffee_shop_backend.dto.UserUpdateRequestDTO;
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

import java.util.HashSet; // Import HashSet
import java.util.Set; // Import Set
import java.util.stream.Collectors; // Import Collectors

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository; // Cần để kiểm tra Role ID hợp lệ
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Lấy danh sách tất cả người dùng (phân trang)
     */
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        return userPage.map(userMapper::toUserResponseDto);
    }

    /**
     * Lấy chi tiết một người dùng theo ID
     */
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        return userMapper.toUserResponseDto(user);
    }

    /**
     * Cập nhật thông tin người dùng và quyền
     */
    @Transactional
    public UserResponseDTO updateUser(Long id, UserUpdateRequestDTO updateDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        // Kiểm tra SĐT mới (nếu thay đổi)
        if (!existingUser.getPhone().equals(updateDTO.getPhone()) &&
                userRepository.existsByPhone(updateDTO.getPhone())) {
            throw new IllegalArgumentException("Phone number already exists: " + updateDTO.getPhone());
        }
        // Kiểm tra Email mới (nếu thay đổi và không rỗng)
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().isEmpty() &&
                !updateDTO.getEmail().equals(existingUser.getEmail()) && // Chỉ kiểm tra nếu email thực sự thay đổi
                userRepository.existsByEmail(updateDTO.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + updateDTO.getEmail());
        }

        // **Kiểm tra và lấy các đối tượng Role từ DB**
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

        // Dùng mapper cập nhật thông tin cơ bản (mapper đã ignore roles)
        userMapper.updateUserFromDto(updateDTO, existingUser);

        // Gán lại Set<Role> đầy đủ từ DB vào User
        existingUser.setRoles(roles);

        // Lưu thay đổi
        User updatedUser = userRepository.save(existingUser);

        // Trả về DTO
        return userMapper.toUserResponseDto(updatedUser);
    }

    /**
     * Thay đổi mật khẩu cho người dùng đang đăng nhập
     */
    @Transactional
    public void changePassword(ChangePasswordRequestDTO request) {
        // 1. Lấy username của người dùng đang đăng nhập
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new IllegalStateException("User not authenticated");
        }
        String currentUsername = authentication.getName();

        // 2. Tìm User trong DB
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found in database"));

        // 3. Kiểm tra mật khẩu hiện tại có đúng không
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException("Incorrect current password");
        }

        // 4. Kiểm tra mật khẩu mới và mật khẩu xác nhận có khớp không
        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new IllegalArgumentException("New password and confirmation password do not match");
        }

        // 5. (Tùy chọn) Kiểm tra xem mật khẩu mới có giống mật khẩu cũ không
        if (passwordEncoder.matches(request.getNewPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as the old password");
        }

        // 6. Mã hóa mật khẩu mới
        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());

        // 7. Cập nhật mật khẩu mới cho user
        currentUser.setPassword(encodedNewPassword);

        // 8. Lưu thay đổi
        userRepository.save(currentUser);

        // (Tùy chọn nâng cao: Thu hồi tất cả token cũ của user này nếu dùng blacklist)
    }
}