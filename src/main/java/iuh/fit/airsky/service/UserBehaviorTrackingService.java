package iuh.fit.airsky.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class UserBehaviorTrackingService {

    // In-memory storage cho behavior data (có thể thay bằng Redis)
    private final Map<String, UserBehaviorData> behaviorCache = new ConcurrentHashMap<>();

    /**
     * Track user search behavior
     */
    public void trackSearch(Long userId, String sessionId, Map<String, Object> searchData) {
        String key = "search_" + userId + "_" + sessionId;

        UserBehaviorData data = behaviorCache.computeIfAbsent(key, k -> new UserBehaviorData());
        data.setUserId(userId);
        data.setSessionId(sessionId);
        data.setActionType("SEARCH");
        data.setSearchData(searchData);
        data.setTimestamp(LocalDateTime.now());

        // Log structured data for later analysis
        log.info("USER_SEARCH: userId={}, sessionId={}, departure={}, arrival={}, date={}, class={}, passengers={}",
                userId, sessionId,
                searchData.get("departureAirport"),
                searchData.get("arrivalAirport"),
                searchData.get("departureDate"),
                searchData.get("travelClass"),
                searchData.get("passengerCount"));
    }

    /**
     * Track flight view behavior
     */
    public void trackFlightView(Long userId, String sessionId, Map<String, Object> flightData) {
        String key = "view_" + userId + "_" + sessionId;

        UserBehaviorData data = behaviorCache.computeIfAbsent(key, k -> new UserBehaviorData());
        data.setUserId(userId);
        data.setSessionId(sessionId);
        data.setActionType("VIEW_FLIGHT");
        data.setFlightData(flightData);
        data.setTimestamp(LocalDateTime.now());

        log.info("USER_VIEW_FLIGHT: userId={}, sessionId={}, flightId={}, price={}, route={}",
                userId, sessionId,
                flightData.get("flightId"),
                flightData.get("price"),
                flightData.get("route"));
    }

    /**
     * Track deal application
     */
    public void trackDealApplication(Long userId, String sessionId, Map<String, Object> dealData) {
        String key = "deal_" + userId + "_" + sessionId;

        UserBehaviorData data = behaviorCache.computeIfAbsent(key, k -> new UserBehaviorData());
        data.setUserId(userId);
        data.setSessionId(sessionId);
        data.setActionType("APPLY_DEAL");
        data.setDealData(dealData);
        data.setTimestamp(LocalDateTime.now());

        log.info("USER_APPLY_DEAL: userId={}, sessionId={}, dealId={}, discount={}",
                userId, sessionId,
                dealData.get("dealId"),
                dealData.get("discountPercent"));
    }

    /**
     * Track booking creation
     */
    public void trackBooking(Long userId, String sessionId, Map<String, Object> bookingData) {
        String key = "booking_create_" + userId + "_" + sessionId;

        UserBehaviorData data = behaviorCache.computeIfAbsent(key, k -> new UserBehaviorData());
        data.setUserId(userId);
        data.setSessionId(sessionId);
        data.setActionType("BOOKING_START");
        data.setBookingData(bookingData);
        data.setTimestamp(LocalDateTime.now());

        log.info("USER_BOOKING_START: userId={}, sessionId={}, bookingId={}, totalAmount={}",
                userId, sessionId,
                bookingData.get("bookingId"),
                bookingData.get("totalAmount"));
    }
    public void trackBookingCompletion(Long userId, String sessionId, Map<String, Object> bookingData) {
        String key = "booking_" + userId + "_" + sessionId;

        UserBehaviorData data = behaviorCache.computeIfAbsent(key, k -> new UserBehaviorData());
        data.setUserId(userId);
        data.setSessionId(sessionId);
        data.setActionType("BOOKING_COMPLETE");
        data.setBookingData(bookingData);
        data.setTimestamp(LocalDateTime.now());

        log.info("USER_BOOKING_COMPLETE: userId={}, sessionId={}, bookingId={}, totalAmount={}, route={}",
                userId, sessionId,
                bookingData.get("bookingId"),
                bookingData.get("totalAmount"),
                bookingData.get("route"));
    }

    /**
     * Get user behavior summary for data mining
     */
    public Map<String, Object> getUserBehaviorSummary(Long userId) {
        // Aggregate behavior data từ cache và logs
        Map<String, Object> summary = new ConcurrentHashMap<>();

        // Đếm số lần search theo route
        Map<String, Integer> routeSearches = new ConcurrentHashMap<>();
        // Đếm số lần view theo price range
        Map<String, Integer> priceRanges = new ConcurrentHashMap<>();
        // Đếm deal applications
        int[] dealApplications = {0};
        // Đếm bookings completed
        int[] completedBookings = {0};

        // Process từ cache (có thể thay bằng query logs sau)
        behaviorCache.entrySet().stream()
            .filter(entry -> {
                UserBehaviorData data = entry.getValue();
                return data.getUserId() != null && data.getUserId().equals(userId);
            })
            .forEach(entry -> {
                UserBehaviorData data = entry.getValue();
                switch (data.getActionType()) {
                    case "SEARCH":
                        if (data.getSearchData() != null) {
                            String route = data.getSearchData().get("departureAirport") + "-" +
                                          data.getSearchData().get("arrivalAirport");
                            routeSearches.merge(route, 1, Integer::sum);
                        }
                        break;
                    case "VIEW_FLIGHT":
                        if (data.getFlightData() != null) {
                            Double price = (Double) data.getFlightData().get("price");
                            if (price != null) {
                                String range = getPriceRange(price);
                                priceRanges.merge(range, 1, Integer::sum);
                            }
                        }
                        break;
                    case "APPLY_DEAL":
                        dealApplications[0]++;
                        break;
                    case "BOOKING_COMPLETE":
                        completedBookings[0]++;
                        break;
                }
            });

        summary.put("routePreferences", routeSearches);
        summary.put("priceSensitivity", priceRanges);
        summary.put("dealApplications", dealApplications[0]);
        summary.put("completedBookings", completedBookings[0]);

        return summary;
    }

    /**
     * Get behavior cache for testing/debugging
     */
    public Map<String, UserBehaviorData> getBehaviorCache() {
        return behaviorCache;
    }
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    public void cleanupOldData() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        behaviorCache.entrySet().removeIf(entry -> {
            UserBehaviorData data = entry.getValue();
            return data.getTimestamp().isBefore(cutoff);
        });
        log.info("Cleaned up old behavior data, remaining: {}", behaviorCache.size());
    }

    private String getPriceRange(Double price) {
        if (price < 1000000) return "LOW";
        if (price < 2000000) return "MEDIUM";
        if (price < 5000000) return "HIGH";
        return "PREMIUM";
    }

    // Inner class cho behavior data
    public static class UserBehaviorData {
        private Long userId;
        private String sessionId;
        private String actionType;
        private LocalDateTime timestamp;
        private Map<String, Object> searchData;
        private Map<String, Object> flightData;
        private Map<String, Object> dealData;
        private Map<String, Object> bookingData;

        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public String getActionType() { return actionType; }
        public void setActionType(String actionType) { this.actionType = actionType; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public Map<String, Object> getSearchData() { return searchData; }
        public void setSearchData(Map<String, Object> searchData) { this.searchData = searchData; }

        public Map<String, Object> getFlightData() { return flightData; }
        public void setFlightData(Map<String, Object> flightData) { this.flightData = flightData; }

        public Map<String, Object> getDealData() { return dealData; }
        public void setDealData(Map<String, Object> dealData) { this.dealData = dealData; }

        public Map<String, Object> getBookingData() { return bookingData; }
        public void setBookingData(Map<String, Object> bookingData) { this.bookingData = bookingData; }
    }
}