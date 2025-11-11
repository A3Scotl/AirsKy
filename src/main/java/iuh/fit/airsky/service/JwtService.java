package iuh.fit.airsky.service;


/*
 * @description Defines the contract for JWT token generation, extraction, and validation.
 * @author : Nguyen Truong An
 * @date : 8/13/2025
 * @version 1.0
 */
public interface JwtService {

    String generateAccessToken(String email, String role);
    String generateAccessToken(Long userId, String email, String role); // Thêm hàm này để sinh token có id
    String generateRefreshToken(String email);
}