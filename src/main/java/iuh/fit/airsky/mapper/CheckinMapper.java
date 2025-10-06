package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.CheckinRequest;
import iuh.fit.airsky.dto.response.CheckinResponse;
import iuh.fit.airsky.model.CheckIn;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CheckinMapper {
    @Mapping(target = "checkInId", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "passenger", ignore = true)
    @Mapping(target = "checkedAt", source = "issueDate")
    @Mapping(target = "checkInType", ignore = true)
    @Mapping(target = "boardingPassUrl", ignore = true)
    @Mapping(target = "baggage", ignore = true)
    CheckIn toEntity(CheckinRequest dto);

    @Mapping(target = "checkinId", source = "checkInId")
    @Mapping(target = "bookingId", source = "booking.bookingId")
    @Mapping(target = "passengerId", source = "passenger.passengerId")
    @Mapping(target = "issueDate", source = "checkedAt")
    CheckinResponse toResponseDTO(CheckIn entity);
}