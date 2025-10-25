package com.giapho.coffee_shop_backend.security;

// SỬA LỖI Ở ĐÂY: Import đúng class User entity của chúng ta
import com.giapho.coffee_shop_backend.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service; // THÊM DÒNG NÀY

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service // THÊM DÒNG NÀY (Bạn bị thiếu @Service)
public class JwtService {

    // Lấy giá trị từ file application.properties
    @Value("${application.jwt.secretKey}")
    private String secretKey;

    @Value("${application.jwt.expirationMs}")
    private long expirationMs;

    /**
     * Trích xuất username từ Token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Trích xuất một "claim" (thông tin) cụ thể từ Token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Tạo Token chỉ từ UserDetails (không cần thêm claims)
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Tạo Token với các thông tin thêm (extra claims)
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        // Lấy các quyền (roles) của user để đưa vào token
        extraClaims.put("authorities", userDetails.getAuthorities());

        // Nếu userDetails là instance của User entity của chúng ta,
        // chúng ta có thể thêm các thông tin khác như fullName, email...
        if (userDetails instanceof User customUser) {
            extraClaims.put("userId", customUser.getId());
            extraClaims.put("fullName", customUser.getFullName());
        }

        return Jwts.builder()
                .setClaims(extraClaims) // Đặt các thông tin thêm
                .setSubject(userDetails.getUsername()) // Đặt "subject" là username
                .setIssuedAt(new Date(System.currentTimeMillis())) // Thời điểm phát hành
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs)) // Thời điểm hết hạn
                .signWith(getSignInKey(), SignatureAlgorithm.HS256) // Ký token
                .compact();
    }

    /**
     * Kiểm tra xem Token có hợp lệ không
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        // Token hợp lệ KHI VÀ CHỈ KHI:
        // 1. Username trong token == username trong userDetails
        // 2. Token chưa hết hạn
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Kiểm tra xem Token đã hết hạn chưa
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Lấy thời gian hết hạn từ Token
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Giải mã toàn bộ Token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Lấy "Khóa" (Key) dùng để ký và giải mã Token
     * Khóa này được tạo ra từ chuỗi secretKey trong file properties
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}