package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.CountryRequest;
import iuh.fit.airsky.dto.response.CountryResponse;
import iuh.fit.airsky.model.Country;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CountryMapper {
    @Mapping(target = "countryId", ignore = true)
    @Mapping(target = "thumbnail", source = "thumbnail")
    Country toEntity(CountryRequest dto);

    @Mapping(target = "thumbnail", source = "thumbnail")
    CountryResponse toResponseDTO(Country entity);
}
