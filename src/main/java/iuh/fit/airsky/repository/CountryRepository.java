/*
 * @ (#) CountryRepository.java 1.0 8/17/2025
 *
 * Copyright (c) 2025 IUH.All rights reserved
 */
package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Country;
import org.springframework.data.jpa.repository.JpaRepository;

/*
 * @description
 * @author : Nguyen Truong An
 * @date : 8/17/2025
 * @version 1.0
 */
public interface CountryRepository extends JpaRepository<Country, Long> {
}
