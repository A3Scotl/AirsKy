package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.PassengerRequest;
import iuh.fit.airsky.dto.response.PassengerResponse;
import iuh.fit.airsky.model.Passenger;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PassengerMapper {
    @Mapping(target = "passengerId", ignore = true)
    @Mapping(target = "reservation", ignore = true)
    Passenger toEntity(PassengerRequest dto);

    PassengerResponse toResponseDTO(Passenger entity);
}