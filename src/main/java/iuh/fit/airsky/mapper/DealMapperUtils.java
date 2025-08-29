package iuh.fit.airsky.mapper;

import iuh.fit.airsky.model.Deal;
import java.time.LocalDateTime;

public class DealMapperUtils {
    public static String calculateStatus(Deal entity) {
        LocalDateTime now = LocalDateTime.now();
        if (entity.getValidFrom() != null && entity.getValidFrom().isAfter(now)) {
            return "SẮP HOẠT ĐỘNG";
        }
        if (entity.getValidTo() != null && entity.getValidTo().isBefore(now)) {
            return "ĐÃ HẾT HẠN";
        }
        if (entity.getTotalUsageLimit() != null && entity.getUsedCount() != null && entity.getUsedCount() >= entity.getTotalUsageLimit()) {
            return "HẾT LƯỢT SỬ DỤNG";
        }
        if (entity.getIsActive() != null && entity.getIsActive()
                && entity.getValidFrom() != null && !entity.getValidFrom().isAfter(now)
                && entity.getValidTo() != null && !entity.getValidTo().isBefore(now)
                && (entity.getTotalUsageLimit() == null || entity.getUsedCount() == null || entity.getUsedCount() < entity.getTotalUsageLimit())) {
            return "ĐANG HOẠT ĐỘNG";
        }
        return "KHÔNG XÁC ĐỊNH";
    }
}

