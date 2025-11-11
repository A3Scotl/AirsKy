package iuh.fit.airsky.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class RoundTripFlightResponse {
    private List<RoundTripPair> roundTripPairs; // Danh sách các cặp khứ hồi hợp lệ

    @Data
    public static class RoundTripPair {
        private FlightResponse outbound;
        private FlightResponse inbound;

        public RoundTripPair(FlightResponse outbound, FlightResponse inbound) {
            this.outbound = outbound;
            this.inbound = inbound;
        }
    }
}
