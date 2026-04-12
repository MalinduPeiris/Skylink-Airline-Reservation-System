package com.skylinkapplication.skylinkairlinereservationsystem.dto;

import java.util.Date;

public class FeedbackDTO {
    private Long id;
    private Integer rating;
    private String comments;
    private Date submittedDate;
    private Long userId;

    // Constructors
    public FeedbackDTO() {}
    public FeedbackDTO(Long id, Integer rating, String comments, Date submittedDate, Long userId) {
        this.id = id;
        this.rating = rating;
        this.comments = comments;
        this.submittedDate = submittedDate;
        this.userId = userId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public Date getSubmittedDate() { return submittedDate; }
    public void setSubmittedDate(Date submittedDate) { this.submittedDate = submittedDate; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}