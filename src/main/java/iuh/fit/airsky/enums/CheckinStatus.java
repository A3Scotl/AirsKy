package iuh.fit.airsky.enums;

public enum CheckinStatus {
    ELIGIBLE("Eligible for check-in"),
    ALREADY_CHECKED_IN("Already checked in"),
    PAYMENT_PENDING("Payment not completed"),
    BOOKING_NOT_CONFIRMED("Booking not confirmed"),
    NOT_AVAILABLE("Check-in not available"),
    BOOKING_CANCELLED("Booking has been cancelled"),
    FLIGHT_DEPARTED("Flight has already departed"),
    CHECKIN_NOT_OPEN("Check-in not open yet"),
    PREVIOUS_SEGMENT_NOT_CHECKED_IN("Previous flight segment not checked in"),

    // Entity statuses
    PENDING("Check-in record created, awaiting actual check-in"),
    COMPLETED("Check-in completed successfully"),
    CANCELLED("Check-in cancelled");

    private final String description;

    CheckinStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}