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
    @Mapping(target = "checkedAt", ignore = true)
    @Mapping(target = "checkInType", ignore = true)
    @Mapping(target = "boardingPassUrl", ignore = true)
    @Mapping(target = "baggage", ignore = true)
    CheckIn toEntity(CheckinRequest dto);

    @Mapping(target = "checkinId", source = "checkInId")
    @Mapping(target = "bookingId", source = "booking.bookingId")
    @Mapping(target = "passengerId", source = "passenger.passengerId")
    @Mapping(target = "passengerName", expression = "java(entity.getPassenger() != null ? entity.getPassenger().getFirstName() + \" \" + entity.getPassenger().getLastName() : null)")
    @Mapping(target = "seatNumber", source = "seatNumber")
    @Mapping(target = "seatType", expression = "java(entity.getPassenger() != null && entity.getPassenger().getSeat() != null ? entity.getPassenger().getSeat().getType().toString() : null)")
    @Mapping(target = "ticketPrice", source = "ticketPrice")
    @Mapping(target = "issueDate", source = "checkedAt")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "deleted", source = "deleted")
    @Mapping(target = "deletedAt", source = "deletedAt")
    @Mapping(target = "boardingPassUrl", source = "boardingPassUrl")
    CheckinResponse toResponseDTO(CheckIn entity);
}