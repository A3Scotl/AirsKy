/*
* @ (#) EmailService.java 1.0 8/13/2025
*
* Copyright (c) 2025 IUH.All rights reserved
*/
package iuh.fit.airsky.service;
/*
 * @description 
 * @author : Nguyen Truong An
 * @date : 8/13/2025
 * @version 1.0
*/
public interface EmailService {
    void sendVerificationEmail(String to, String subject, String body);
}
