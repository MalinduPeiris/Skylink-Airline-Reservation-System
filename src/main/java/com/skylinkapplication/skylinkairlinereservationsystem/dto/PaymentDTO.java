package com.skylinkapplication.skylinkairlinereservationsystem.dto;

import java.util.Date;

public class PaymentDTO {
    private Long id;
    private Double amount;
    private String status; // e.g., COMPLETED, REFUNDED, FAILED
    private String transactionId;
    private Date transactionDate;

    // Constructors
    public PaymentDTO() {}
    public PaymentDTO(Long id, Double amount, String status, String transactionId, Date transactionDate) {
        this.id = id;
        this.amount = amount;
        this.status = status;
        this.transactionId = transactionId;
        this.transactionDate = transactionDate;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public Date getTransactionDate() { return transactionDate; }
    public void setTransactionDate(Date transactionDate) { this.transactionDate = transactionDate; }
}