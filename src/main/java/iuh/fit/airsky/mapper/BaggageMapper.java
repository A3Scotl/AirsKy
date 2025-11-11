package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.BaggageRequest;
import iuh.fit.airsky.dto.response.BaggageResponse;
import iuh.fit.airsky.model.Baggage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BaggageMapper {
    @Mapping(target = "baggageId", ignore = true)
    @Mapping(target = "checkIn", ignore = true) // Map riêng trong service
    @Mapping(target = "packagePrice", expression = "java(dto.getPurchasedPackage() != null ? dto.getPurchasedPackage().getPrice() : null)")
    @Mapping(target = "actualWeight", ignore = true)
    @Mapping(target = "excessWeight", ignore = true)
    @Mapping(target = "excessFee", ignore = true)
    Baggage toEntity(BaggageRequest dto);

    @Mapping(target = "checkinId", source = "checkIn.checkInId")
    BaggageResponse toResponseDTO(Baggage entity);
}