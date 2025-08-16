package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.UserRequest;
import iuh.fit.airsky.dto.response.UserResponse;
import iuh.fit.airsky.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true) // Xử lý mã hóa riêng
    User toEntity(UserRequest dto);

    UserResponse toResponseDTO(User entity);
}