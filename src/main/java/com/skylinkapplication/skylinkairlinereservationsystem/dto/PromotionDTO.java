package com.skylinkapplication.skylinkairlinereservationsystem.dto;

import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Promotion;
import java.time.ZoneId;
import java.time.LocalDate;

public class PromotionDTO {
    private Long id;
    private Double discount;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date validityStart;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date validityEnd;
    private String targetCriteria; // e.g., travel history-based targeting
    private String promoCode;
    private Promotion.Scope scope;
    private Boolean active;
    private java.util.List<Long> eligibleUserIds;


    // Constructors
    public PromotionDTO() {}
    public PromotionDTO(Long id, Double discount, Date validityStart, Date validityEnd, String targetCriteria, String promoCode) {
        this.id = id;
        this.discount = discount;
        this.validityStart = validityStart;
        this.validityEnd = validityEnd;
        this.targetCriteria = targetCriteria;
        this.promoCode = promoCode;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }
    public Date getValidityStart() { return validityStart; }
    public void setValidityStart(Date validityStart) { this.validityStart = validityStart; }
    public Date getValidityEnd() { return validityEnd; }
    public void setValidityEnd(Date validityEnd) { this.validityEnd = validityEnd; }
    public String getTargetCriteria() { return targetCriteria; }
    public void setTargetCriteria(String targetCriteria) { this.targetCriteria = targetCriteria; }
    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
    public Promotion.Scope getScope() { return scope; }
    public void setScope(Promotion.Scope scope) { this.scope = scope; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public java.util.List<Long> getEligibleUserIds() { return eligibleUserIds; }
    public void setEligibleUserIds(java.util.List<Long> eligibleUserIds) { this.eligibleUserIds = eligibleUserIds; }
    public String getValidityStartColombo() { return dateInColombo(validityStart); }
    public String getValidityEndColombo() { return dateInColombo(validityEnd); }
    public String getAvailabilityLabel() {
        if (scope == null) return "UNCLASSIFIED";
        if (!Boolean.TRUE.equals(active)) return "INACTIVE";
        if (discount == null || !Double.isFinite(discount) || discount <= 0 || discount > 100) return "INVALID DISCOUNT";
        if (validityStart == null || validityEnd == null) return "INVALID DATES";
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Colombo"));
        if (today.isBefore(validityStart.toInstant().atZone(ZoneId.of("Asia/Colombo")).toLocalDate())) return "SCHEDULED";
        if (today.isAfter(validityEnd.toInstant().atZone(ZoneId.of("Asia/Colombo")).toLocalDate())) return "EXPIRED";
        if (scope == Promotion.Scope.TARGETED && (eligibleUserIds == null || eligibleUserIds.isEmpty())) return "NO LINKED USERS";
        return "AVAILABLE";
    }
    private String dateInColombo(Date date) {
        return date == null ? "" : date.toInstant().atZone(ZoneId.of("Asia/Colombo")).toLocalDate().toString();
    }
}
