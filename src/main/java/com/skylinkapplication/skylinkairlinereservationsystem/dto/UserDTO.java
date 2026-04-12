package com.skylinkapplication.skylinkairlinereservationsystem.dto;

public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String password;
    private String phonenumber;
    private String address;
    private String role; // e.g., FREQUENT_TRAVELER, RESERVATION_MANAGER

    // Constructors
    public UserDTO() {}
    public UserDTO(Long id, String username, String email, String phonenumber, String address, String role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.phonenumber=phonenumber;
        this.address=address;
        this.role = role;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getName() { return username; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public void setPhonenumber(String phonenumber) { this.phonenumber = phonenumber; }
    public String getPhonenumber() { return phonenumber; }
    public void setAddress(String address) { this.address = address; }
    public String getAddress() { return address; }

}
