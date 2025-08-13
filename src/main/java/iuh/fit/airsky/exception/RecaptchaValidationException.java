/*
 * @ (#) RecaptchaValidationException.java 1.0 8/13/2025
 *
 * Copyright (c) 2025 IUH.All rights reserved
 */

package iuh.fit.airsky.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/*
 * @description
 * @author : Nguyen Truong An
 * @date : 8/13/2025
 * @version 1.0
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RecaptchaValidationException extends RuntimeException {
    public RecaptchaValidationException(String message) {
        super(message);
    }
}