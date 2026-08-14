package com.pixfactory.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "app_settings")
public class AppSettings {
    @Id
    private Long id = 1L;
    private String companyName = "PixFactory";
    private String slogan = "Gestão de empréstimos e cobranças";
    private BigDecimal defaultInterestRate = new BigDecimal("2.5");
    private BigDecimal referralReward = new BigDecimal("50");
    private boolean emailNotifications = true;
    private boolean paymentReminders = true;
    private String theme = "dark";
    private boolean hideValues;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getSlogan() { return slogan; }
    public void setSlogan(String slogan) { this.slogan = slogan; }
    public BigDecimal getDefaultInterestRate() { return defaultInterestRate; }
    public void setDefaultInterestRate(BigDecimal defaultInterestRate) { this.defaultInterestRate = defaultInterestRate; }
    public BigDecimal getReferralReward() { return referralReward; }
    public void setReferralReward(BigDecimal referralReward) { this.referralReward = referralReward; }
    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean emailNotifications) { this.emailNotifications = emailNotifications; }
    public boolean isPaymentReminders() { return paymentReminders; }
    public void setPaymentReminders(boolean paymentReminders) { this.paymentReminders = paymentReminders; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public boolean isHideValues() { return hideValues; }
    public void setHideValues(boolean hideValues) { this.hideValues = hideValues; }
}
