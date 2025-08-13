/*
 * @ (#) CustomUserDetailsService.java 1.0 7/11/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved
 */
package iuh.fit.airsky.service;

import iuh.fit.airsky.model.User;
import iuh.fit.airsky.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/*
 * @description: Custom UserDetailsService for loading user details during authentication
 * @author: Nguyen Truong An
 * @date: 7/11/2025
 * @version: 1.0
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Tải thông tin người dùng với email: {}", email);

        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UsernameNotFoundException("Không tìm thấy người dùng với email: "+email));
//        if (user == null) {
//            logger.error("Không tìm thấy người dùng với email: {}", email);
//            throw new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email);
//        }

        // Chuyển đổi role thành GrantedAuthority
        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name());


        logger.debug("Người dùng được tải: {}, vai trò: {}", email, authority);
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(authority)
        );
    }
}