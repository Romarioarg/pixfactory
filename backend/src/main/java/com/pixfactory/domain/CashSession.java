package com.pixfactory.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "cash_sessions")
public class CashSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate date = LocalDate.now();
    private String status = "aberto";
    private BigDecimal opening = BigDecimal.ZERO;
    private BigDecimal expected = BigDecimal.ZERO;
    private BigDecimal informed;
    private BigDecimal difference = BigDecimal.ZERO;
    private String operator;
    @Column(length = 2000)
    private String notes;
    private Instant openedAt = Instant.now();
    private Instant closedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getOpening() { return opening; }
    public void setOpening(BigDecimal opening) { this.opening = opening; }
    public BigDecimal getExpected() { return expected; }
    public void setExpected(BigDecimal expected) { this.expected = expected; }
    public BigDecimal getInformed() { return informed; }
    public void setInformed(BigDecimal informed) { this.informed = informed; }
    public BigDecimal getDifference() { return difference; }
    public void setDifference(BigDecimal difference) { this.difference = difference; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}
