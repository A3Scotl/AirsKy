package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.ReviewRequest;
import iuh.fit.airsky.dto.response.ReviewResponse;
import iuh.fit.airsky.model.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(target = "reviewId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "flight", ignore = true)
    Review toEntity(ReviewRequest dto);

    ReviewResponse toResponseDTO(Review entity);
}