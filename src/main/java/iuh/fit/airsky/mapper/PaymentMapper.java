package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.PaymentRequest;
import iuh.fit.airsky.dto.response.PaymentResponse;
import iuh.fit.airsky.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(target = "paymentId", ignore = true)
    @Mapping(target = "booking", ignore = true)
    Payment toEntity(PaymentRequest dto);

    @Mapping(source = "booking.bookingId", target = "bookingId")
    PaymentResponse toResponseDTO(Payment entity);

}