package com.pixfactory.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "bank_connections")
public class BankConnection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nome;
    private String agencia;
    private String conta;
    private boolean connected = true;
    private BigDecimal saldo = BigDecimal.ZERO;
    private String institutionCode;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getAgencia() { return agencia; }
    public void setAgencia(String agencia) { this.agencia = agencia; }
    public String getConta() { return conta; }
    public void setConta(String conta) { this.conta = conta; }
    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }
    public String getInstitutionCode() { return institutionCode; }
    public void setInstitutionCode(String institutionCode) { this.institutionCode = institutionCode; }
}
