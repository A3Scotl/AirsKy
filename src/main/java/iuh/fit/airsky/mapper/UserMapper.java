package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.UserRequest;
import iuh.fit.airsky.dto.response.UserResponse;
import iuh.fit.airsky.model.User;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true) // Xử lý mã hóa riêng
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "businessName", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "authProvider", ignore = true) // Không cho phép update từ request
    User toEntity(UserRequest dto);

    UserResponse toResponseDTO(User entity);

    List<UserResponse> toResponseDTOList(List<User> users);
}