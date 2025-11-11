package iuh.fit.airsky.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TripType {
    ROUND_TRIP("round_trip"),
    ONE_WAY("one_way"),
    MULTI_CITY("multi_city");

    private final String value;

    TripType(String value) {
        this.value = value;
    }

    @JsonCreator
    public static TripType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (TripType tripType : TripType.values()) {
            if (tripType.value.equalsIgnoreCase(value) || tripType.name().equalsIgnoreCase(value)) {
                return tripType;
            }
        }
        throw new IllegalArgumentException("Unknown TripType: " + value);
    }

    @JsonValue
    public String toValue() {
        return this.value;
    }
}
