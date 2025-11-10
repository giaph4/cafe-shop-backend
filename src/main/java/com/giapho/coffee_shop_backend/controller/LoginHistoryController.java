package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.LoginHistoryResponseDTO;
import com.giapho.coffee_shop_backend.service.LoginHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/login-history")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LoginHistoryController {

    private final LoginHistoryService loginHistoryService;

    /**
     * Lấy lịch sử login của người dùng
     * @param username Tên người dùng (tùy chọn)
     * @param success Trạng thái của lần login (tùy chọn)
     * @param startDate Ngày bắt đầu của khoảng thời gian (tùy chọn)
     * @param endDate Ngày kết thúc của khoảng thời gian (tùy chọn)
     * @param pageable Thông số trang và số bản ghi trên mỗi trang
     * @return Danh sách login history của người dùng
     */
    @GetMapping
    public ResponseEntity<Page<LoginHistoryResponseDTO>> getLoginHistory(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20, sort = "loginAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<LoginHistoryResponseDTO> historyPage = loginHistoryService.searchLoginHistory(
                username,
                success,
                startDate,
                endDate,
                pageable
        );
        return ResponseEntity.ok(historyPage);
    }
}
