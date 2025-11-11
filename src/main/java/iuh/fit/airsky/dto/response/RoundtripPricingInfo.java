// package iuh.fit.airsky.dto.response;

// import lombok.AllArgsConstructor;
// import lombok.Data;
// import lombok.NoArgsConstructor;

// import java.math.BigDecimal;

// @Data
// @NoArgsConstructor
// @AllArgsConstructor
// public class RoundtripPricingInfo {
//     private Long serviceId;
//     private String serviceName;
//     private String serviceType;
//     private BigDecimal unitPrice;
//     private Boolean isPerSegment;
//     private Integer segmentCount;
//     private BigDecimal totalPrice;
//     private String pricingExplanation;
    
//     public static RoundtripPricingInfo from(AncillaryServiceResponse service, int segmentCount, int quantity) {
//         RoundtripPricingInfo info = new RoundtripPricingInfo();
//         info.setServiceId(service.getServiceId());
//         info.setServiceName(service.getServiceName());
//         info.setServiceType(service.getServiceType().name());
//         info.setUnitPrice(service.getPrice());
//         info.setIsPerSegment(service.getIsPerSegment());
//         info.setSegmentCount(segmentCount);
        
//         // Calculate total price
//         BigDecimal baseAmount = service.getPrice().multiply(BigDecimal.valueOf(quantity));
//         BigDecimal totalPrice;
//         String explanation;
        
//         if (service.getIsPerSegment() && segmentCount > 1) {
//             totalPrice = baseAmount.multiply(BigDecimal.valueOf(segmentCount));
//             explanation = String.format("Per-segment service: %,.0f x %d segments = %,.0f VND", 
//                     baseAmount.doubleValue(), segmentCount, totalPrice.doubleValue());
//         } else {
//             totalPrice = baseAmount;
//             explanation = String.format("Per-booking service: %,.0f VND (covers entire journey)", 
//                     totalPrice.doubleValue());
//         }
        
//         info.setTotalPrice(totalPrice);
//         info.setPricingExplanation(explanation);
        
//         return info;
//     }
// }