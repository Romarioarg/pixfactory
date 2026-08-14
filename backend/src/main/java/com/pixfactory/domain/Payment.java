package com.pixfactory.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long contractId;
    @Column(nullable = false)
    private String method;
    @Column(nullable = false)
    private String txid;
    @Column(nullable = false)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;
    @Column(length = 2000)
    private String qrPayload;
    private String copyPaste;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private String provider = "DEMO";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getTxid() { return txid; }
    public void setTxid(String txid) { this.txid = txid; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getQrPayload() { return qrPayload; }
    public void setQrPayload(String qrPayload) { this.qrPayload = qrPayload; }
    public String getCopyPaste() { return copyPaste; }
    public void setCopyPaste(String copyPaste) { this.copyPaste = copyPaste; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
