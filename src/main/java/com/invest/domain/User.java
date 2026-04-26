package com.invest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(length = 128)
    private String id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(length = 256)
    private String email;

    @Column(length = 128)
    private String name;

    @Column(name = "picture_url", length = 512)
    private String pictureUrl;

    protected User() {}

    public User(String id, BigDecimal balance) {
        this.id = id;
        this.balance = balance;
    }

    public String getId() { return id; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPictureUrl() { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }
}
