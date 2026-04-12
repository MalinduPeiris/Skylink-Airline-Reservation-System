package com.skylinkapplication.skylinkairlinereservationsystem.dto;

import java.util.Date;

public class PromotionDTO {
    private Long id;
    private Double discount;
    private Date validityStart;
    private Date validityEnd;
    private String targetCriteria; // e.g., travel history-based targeting
    private String promoCode;


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
}
