package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.response.NotificationResponse;
import iuh.fit.airsky.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "isRead", target = "read")
    NotificationResponse toResponse(Notification notification);

}