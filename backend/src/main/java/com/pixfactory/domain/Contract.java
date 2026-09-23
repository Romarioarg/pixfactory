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
    @Column(columnDefinition = "TEXT")
    private String historicoJson = "[]";
    private String tipoOperacao = "emprestimo";
    private String sistemaAmortizacao = "price";
    private String modoPagamento = "parcela_cheia";
    private String periodicidade = "mensal";
    private Integer carenciaMeses = 0;
    private Long originalContractId;
    private Integer renegociacaoNumero = 0;
    @Column(columnDefinition = "TEXT")
    private String justificativaRenegociacao;
    private String baseCalculo = "saldo";
    private BigDecimal jurosFixo = BigDecimal.ZERO;
    private String ordemPagamento = "juros,multa,encargos,principal";
    private BigDecimal credito = BigDecimal.ZERO;
    @Column(columnDefinition = "TEXT")
    private String clausulas;
    @Column(columnDefinition = "TEXT")
    private String regrasSnapshotJson = "{}";
    @Column(columnDefinition = "TEXT")
    private String garantiasJson = "[]";
    @Column(columnDefinition = "TEXT")
    private String avalistaJson = "{}";
    @Column(columnDefinition = "TEXT")
    private String indicadoPorJson = "{}";
    private Integer periodicidadeDias = 0;

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
    public String getTipoOperacao() { return tipoOperacao; }
    public void setTipoOperacao(String tipoOperacao) { this.tipoOperacao = tipoOperacao; }
    public String getSistemaAmortizacao() { return sistemaAmortizacao; }
    public void setSistemaAmortizacao(String sistemaAmortizacao) { this.sistemaAmortizacao = sistemaAmortizacao; }
    public String getModoPagamento() { return modoPagamento; }
    public void setModoPagamento(String modoPagamento) { this.modoPagamento = modoPagamento; }
    public String getPeriodicidade() { return periodicidade; }
    public void setPeriodicidade(String periodicidade) { this.periodicidade = periodicidade; }
    public Integer getCarenciaMeses() { return carenciaMeses; }
    public void setCarenciaMeses(Integer carenciaMeses) { this.carenciaMeses = carenciaMeses; }
    public Long getOriginalContractId() { return originalContractId; }
    public void setOriginalContractId(Long originalContractId) { this.originalContractId = originalContractId; }
    public Integer getRenegociacaoNumero() { return renegociacaoNumero; }
    public void setRenegociacaoNumero(Integer renegociacaoNumero) { this.renegociacaoNumero = renegociacaoNumero; }
    public String getJustificativaRenegociacao() { return justificativaRenegociacao; }
    public void setJustificativaRenegociacao(String justificativaRenegociacao) { this.justificativaRenegociacao = justificativaRenegociacao; }
    public String getBaseCalculo() { return baseCalculo; }
    public void setBaseCalculo(String baseCalculo) { this.baseCalculo = baseCalculo; }
    public BigDecimal getJurosFixo() { return jurosFixo; }
    public void setJurosFixo(BigDecimal jurosFixo) { this.jurosFixo = jurosFixo; }
    public String getOrdemPagamento() { return ordemPagamento; }
    public void setOrdemPagamento(String ordemPagamento) { this.ordemPagamento = ordemPagamento; }
    public BigDecimal getCredito() { return credito; }
    public void setCredito(BigDecimal credito) { this.credito = credito; }
    public String getClausulas() { return clausulas; }
    public void setClausulas(String clausulas) { this.clausulas = clausulas; }
    public String getRegrasSnapshotJson() { return regrasSnapshotJson; }
    public void setRegrasSnapshotJson(String regrasSnapshotJson) { this.regrasSnapshotJson = regrasSnapshotJson; }
    public String getGarantiasJson() { return garantiasJson; }
    public void setGarantiasJson(String garantiasJson) { this.garantiasJson = garantiasJson; }
    public String getAvalistaJson() { return avalistaJson; }
    public void setAvalistaJson(String avalistaJson) { this.avalistaJson = avalistaJson; }
    public String getIndicadoPorJson() { return indicadoPorJson; }
    public void setIndicadoPorJson(String indicadoPorJson) { this.indicadoPorJson = indicadoPorJson; }
    public Integer getPeriodicidadeDias() { return periodicidadeDias; }
    public void setPeriodicidadeDias(Integer periodicidadeDias) { this.periodicidadeDias = periodicidadeDias; }
}
