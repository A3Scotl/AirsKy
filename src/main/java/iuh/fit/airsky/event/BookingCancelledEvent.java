package iuh.fit.airsky.event;

import iuh.fit.airsky.model.Booking;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class BookingCancelledEvent extends ApplicationEvent {
    private final Booking booking;
    private final String reason;

    public BookingCancelledEvent(Object source, Booking booking, String reason) {
        super(source);
        this.booking = booking;
        this.reason = reason;
    }
}
