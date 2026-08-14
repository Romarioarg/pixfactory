package com.pixfactory.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contracts")
public class Contract {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Client client;
    private String tipo;
    private BigDecimal valorTotal = BigDecimal.ZERO;
    private BigDecimal valorPago = BigDecimal.ZERO;
    private Integer parcelasPagas = 0;
    private Integer parcelasTotais = 1;
    private LocalDate proximoPagamento;
    @Enumerated(EnumType.STRING)
    private ContractStatus status = ContractStatus.PENDENTE;
    private BigDecimal juros = BigDecimal.ZERO;
    private BigDecimal multa = BigDecimal.ZERO;
    private BigDecimal saldoDevedor = BigDecimal.ZERO;
    private BigDecimal jurosPendentes = BigDecimal.ZERO;
    private BigDecimal multaPendente = BigDecimal.ZERO;
    @Column(length = 4000)
    private String historicoJson = "[]";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public BigDecimal getValorPago() { return valorPago; }
    public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago; }
    public Integer getParcelasPagas() { return parcelasPagas; }
    public void setParcelasPagas(Integer parcelasPagas) { this.parcelasPagas = parcelasPagas; }
    public Integer getParcelasTotais() { return parcelasTotais; }
    public void setParcelasTotais(Integer parcelasTotais) { this.parcelasTotais = parcelasTotais; }
    public LocalDate getProximoPagamento() { return proximoPagamento; }
    public void setProximoPagamento(LocalDate proximoPagamento) { this.proximoPagamento = proximoPagamento; }
    public ContractStatus getStatus() { return status; }
    public void setStatus(ContractStatus status) { this.status = status; }
    public BigDecimal getJuros() { return juros; }
    public void setJuros(BigDecimal juros) { this.juros = juros; }
    public BigDecimal getMulta() { return multa; }
    public void setMulta(BigDecimal multa) { this.multa = multa; }
    public BigDecimal getSaldoDevedor() { return saldoDevedor; }
    public void setSaldoDevedor(BigDecimal saldoDevedor) { this.saldoDevedor = saldoDevedor; }
    public BigDecimal getJurosPendentes() { return jurosPendentes; }
    public void setJurosPendentes(BigDecimal jurosPendentes) { this.jurosPendentes = jurosPendentes; }
    public BigDecimal getMultaPendente() { return multaPendente; }
    public void setMultaPendente(BigDecimal multaPendente) { this.multaPendente = multaPendente; }
    public String getHistoricoJson() { return historicoJson; }
    public void setHistoricoJson(String historicoJson) { this.historicoJson = historicoJson; }
}
