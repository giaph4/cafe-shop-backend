package com.giapho.coffee_shop_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // OncePerRequestFilter: Đảm bảo bộ lọc này chỉ chạy 1 LẦN cho mỗi request

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Lấy header "Authorization" từ request
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 1. Kiểm tra xem header có tồn tại không, và có bắt đầu bằng "Bearer " không
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Nếu không có, cho phép request đi tiếp (filter tiếp theo)
            filterChain.doFilter(request, response);
            return; // Dừng thực thi
        }

        // 2. Lấy chuỗi Token (bỏ 7 ký tự "Bearer ")
        jwt = authHeader.substring(7);

        try {
            // 3. Giải mã token để lấy username
            username = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            // Nếu token bị lỗi (hết hạn, sai chữ ký...), gửi lỗi 401
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT Token");
            return;
        }


        // 4. Nếu có username và user chưa được xác thực (chưa đăng nhập)
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Tải thông tin UserDetails từ database (dùng UserDetailsService)
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // 5. Kiểm tra xem token có hợp lệ không (so với userDetails)
            if (jwtService.isTokenValid(jwt, userDetails)) {
                // Nếu token hợp lệ, tạo một đối tượng Authentication
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,    // Đối tượng user
                        null,           // Mật khẩu (không cần thiết sau khi đã xác thực)
                        userDetails.getAuthorities() // Quyền của user
                );

                // Gán thêm chi tiết của request (như IP, user-agent)
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 6. QUAN TRỌNG: "Đặt" user vào SecurityContextHolder
                // Từ giây phút này, Spring Security biết rằng user này đã được xác thực
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 7. Cho phép request đi tiếp
        filterChain.doFilter(request, response);
    }
}