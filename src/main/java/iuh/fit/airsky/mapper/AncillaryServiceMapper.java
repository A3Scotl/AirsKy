package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.AncillaryServiceRequest;
import iuh.fit.airsky.dto.response.AncillaryServiceResponse;
import iuh.fit.airsky.model.AncillaryService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AncillaryServiceMapper {
    
    AncillaryService toEntity(AncillaryServiceRequest request);
    
    @Mapping(target = "serviceTypeDisplayName", expression = "java(entity.getServiceType().getVietnameseName())")
    @Mapping(target = "isActive", source = "active")
    AncillaryServiceResponse toResponse(AncillaryService entity);
    
    void updateEntityFromRequest(AncillaryServiceRequest request, @MappingTarget AncillaryService entity);
}