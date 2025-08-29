package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseFullSoftDeleteEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "airlines",
        indexes = {
                @Index(name = "idx_airline_code", columnList = "airline_code")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Airline extends BaseFullSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long airlineId;

    @Column(name = "airline_code", length = 5, unique = true)
    @NotNull
    @Size(min = 2, max = 5)
    private String airlineCode;

    @Column(name = "airline_name", length = 100)
    private String airlineName;

    @Column(length = 100)
    private String contact;


    
}
