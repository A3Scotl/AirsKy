package iuh.fit.airsky.enums;

public enum AncillaryServiceType {
    MEAL("Meal Service", "Dịch vụ ăn uống"),
    SEAT("Seat Selection", "Chọn chỗ ngồi"),
    ENTERTAINMENT("Entertainment", "Giải trí"),
    WIFI("WiFi", "Internet trên máy bay"),
    PRIORITY_BOARDING("Priority Boarding", "Lên máy bay ưu tiên"),
    LOUNGE_ACCESS("Lounge Access", "Phòng chờ VIP"),
    EXTRA_LEGROOM("Extra Legroom", "Chỗ ngồi rộng rãi"),
    PET_TRANSPORT("Pet Transport", "Vận chuyển thú cưng"),
    INFANT_MEAL("Infant Meal", "Suất ăn trẻ em"),
    SPECIAL_ASSISTANCE("Special Assistance", "Hỗ trợ đặc biệt"),
    TRAVEL_INSURANCE("Travel Insurance", "Bảo hiểm du lịch"),
    OTHER("Other", "Khác");

    private final String englishName;
    private final String vietnameseName;

    AncillaryServiceType(String englishName, String vietnameseName) {
        this.englishName = englishName;
        this.vietnameseName = vietnameseName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getVietnameseName() {
        return vietnameseName;
    }
}