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
    private BigDecimal lateFeePercent = new BigDecimal("2.0");
    private BigDecimal moraPercentPerDay = new BigDecimal("0.033");
    private BigDecimal earlyPayoffDiscountPercent = new BigDecimal("5.0");
    private Integer graceDays = 0;
    @Column(length = 500)
    private String cobrancaEscada = "antes,vencimento,1,7,30,60,90";

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
    public BigDecimal getLateFeePercent() { return lateFeePercent; }
    public void setLateFeePercent(BigDecimal lateFeePercent) { this.lateFeePercent = lateFeePercent; }
    public BigDecimal getMoraPercentPerDay() { return moraPercentPerDay; }
    public void setMoraPercentPerDay(BigDecimal moraPercentPerDay) { this.moraPercentPerDay = moraPercentPerDay; }
    public BigDecimal getEarlyPayoffDiscountPercent() { return earlyPayoffDiscountPercent; }
    public void setEarlyPayoffDiscountPercent(BigDecimal earlyPayoffDiscountPercent) { this.earlyPayoffDiscountPercent = earlyPayoffDiscountPercent; }
    public Integer getGraceDays() { return graceDays; }
    public void setGraceDays(Integer graceDays) { this.graceDays = graceDays; }
    public String getCobrancaEscada() { return cobrancaEscada; }
    public void setCobrancaEscada(String cobrancaEscada) { this.cobrancaEscada = cobrancaEscada; }
}
