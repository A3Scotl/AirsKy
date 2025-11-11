package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.response.DealUsageResponse;
import iuh.fit.airsky.model.DealUsage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DealUsageMapper {
    
    @Mapping(target = "dealId", source = "deal.dealId")
    @Mapping(target = "dealCode", source = "deal.dealCode")
    @Mapping(target = "dealTitle", source = "deal.title")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", ignore = true)
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "bookingId", source = "booking.bookingId")
    @Mapping(target = "bookingCode", ignore = true)
    DealUsageResponse toResponseDTO(DealUsage entity);
}
