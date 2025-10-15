package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.ReviewRequest;
import iuh.fit.airsky.dto.response.ReviewResponse;
import iuh.fit.airsky.model.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "reviewId", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "flight", ignore = true)
    Review toEntity(ReviewRequest dto);

    @Mapping(target = "bookingId", source = "booking.bookingId")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", expression = "java(entity.getUser().getFirstName() + \" \" + entity.getUser().getLastName())")
    @Mapping(target = "userAvatar", source = "user.avatar")
    @Mapping(target = "flightId", source = "flight.flightId")
    @Mapping(target = "flightCode", source = "flight.flightNumber")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "sentAt", source = "sentAt")
    @Mapping(target = "retryCount", source = "retryCount")
    @Mapping(target = "lastError", source = "lastError")
    ReviewResponse toResponseDTO(Review entity);

    List<ReviewResponse> toResponseDTOList(List<Review> reviews);
}