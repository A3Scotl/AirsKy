package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.DealRequest;
import iuh.fit.airsky.dto.response.DealResponse;
import iuh.fit.airsky.model.Deal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DealMapper {
    
    @Mapping(target = "dealId", ignore = true)
    @Mapping(target = "departureAirport", ignore = true)
    @Mapping(target = "arrivalAirport", ignore = true)
    @Mapping(target = "usedCount", ignore = true)
    @Mapping(target = "totalUsageLimit", expression = "java(dto.getTotalUsageLimit() != null ? dto.getTotalUsageLimit() : dto.getUsageLimit())")
    Deal toEntity(DealRequest dto);

    @Mapping(target = "departureAirportId", source = "departureAirport.airportId")
    @Mapping(target = "departureAirportName", source = "departureAirport.airportName")
    @Mapping(target = "departureAirportCode", source = "departureAirport.airportCode")
    @Mapping(target = "arrivalAirportId", source = "arrivalAirport.airportId")
    @Mapping(target = "arrivalAirportName", source = "arrivalAirport.airportName")
    @Mapping(target = "arrivalAirportCode", source = "arrivalAirport.airportCode")
    @Mapping(target = "remainingUsage", expression = "java(entity.getTotalUsageLimit() != null && entity.getUsedCount() != null ? entity.getTotalUsageLimit() - entity.getUsedCount() : 0)")
    @Mapping(target = "isExpired", expression = "java(entity.getValidTo().isBefore(java.time.LocalDateTime.now()))")
    @Mapping(target = "isAvailable", expression = "java(entity.getIsActive() != null && entity.getIsActive() && entity.getValidFrom().isBefore(java.time.LocalDateTime.now()) && entity.getValidTo().isAfter(java.time.LocalDateTime.now()) && (entity.getTotalUsageLimit() == null || entity.getUsedCount() == null || entity.getUsedCount() < entity.getTotalUsageLimit()))")
    DealResponse toResponseDTO(Deal entity);
}
