/*
 * @ (#) UserRespone.java 1.0 8/14/2025
 *
 * Copyright (c) 2025 IUH.All rights reserved
 */

package iuh.fit.airsky.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * @description
 * @author : Nguyen Truong An
 * @date : 8/14/2025
 * @version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRespone {
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
}
