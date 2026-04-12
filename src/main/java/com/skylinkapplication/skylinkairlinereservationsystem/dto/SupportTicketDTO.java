package com.skylinkapplication.skylinkairlinereservationsystem.dto;

import java.util.Date;

public class SupportTicketDTO {
    private Long id;
    private String issueTitle;
    private String issueDescription;
    private String status; // e.g., OPEN, RESOLVED
    private Date createdDate;
    private Long userId;

    // Constructors
    public SupportTicketDTO() {}
    public SupportTicketDTO(Long id, String issueTitle, String issueDescription, String status, Date createdDate, Long userId) {
        this.id = id;
        this.issueTitle = issueTitle;
        this.issueDescription = issueDescription;
        this.status = status;
        this.createdDate = createdDate;
        this.userId = userId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIssueTitle() { return issueTitle; }
    public void setIssueTitle(String issueTitle) { this.issueTitle = issueTitle; }
    public String getIssueDescription() { return issueDescription; }
    public void setIssueDescription(String issueDescription) { this.issueDescription = issueDescription; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}