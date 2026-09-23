package com.pixfactory.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "charges")
public class Charge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Client client;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Contract contract;

    private Integer numero = 1;
    private BigDecimal valor = BigDecimal.ZERO;
    private BigDecimal valorPago = BigDecimal.ZERO;
    private LocalDate vencimento;

    @Enumerated(EnumType.STRING)
    private ChargeStatus status = ChargeStatus.PENDENTE;

    private LocalDate promiseDate;
    private BigDecimal promiseAmount = BigDecimal.ZERO;
    private String promiseStatus;
    private String lastChannel;
    private String lastResult;
    private Instant lastContactAt;

    @Column(columnDefinition = "TEXT")
    private String notes;
    private BigDecimal valorPrincipal = BigDecimal.ZERO;
    private BigDecimal valorJuros = BigDecimal.ZERO;
    private String tipoParcela = "normal";
    private BigDecimal valorBase = BigDecimal.ZERO;
    private BigDecimal multaAplicada = BigDecimal.ZERO;
    private BigDecimal moraAplicada = BigDecimal.ZERO;
    private Instant lastPaidAt;
    private LocalDate nextFollowUp;
    private String formaPagamento;
    private String proximaAcao;
    private String promiseNote;
    private String promiseOwner;
    private BigDecimal pagoJuros = BigDecimal.ZERO;
    private BigDecimal pagoMulta = BigDecimal.ZERO;
    private BigDecimal pagoEncargos = BigDecimal.ZERO;
    private BigDecimal pagoPrincipal = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public BigDecimal getValorPago() { return valorPago; }
    public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago; }
    public LocalDate getVencimento() { return vencimento; }
    public void setVencimento(LocalDate vencimento) { this.vencimento = vencimento; }
    public ChargeStatus getStatus() { return status; }
    public void setStatus(ChargeStatus status) { this.status = status; }
    public LocalDate getPromiseDate() { return promiseDate; }
    public void setPromiseDate(LocalDate promiseDate) { this.promiseDate = promiseDate; }
    public BigDecimal getPromiseAmount() { return promiseAmount; }
    public void setPromiseAmount(BigDecimal promiseAmount) { this.promiseAmount = promiseAmount; }
    public String getPromiseStatus() { return promiseStatus; }
    public void setPromiseStatus(String promiseStatus) { this.promiseStatus = promiseStatus; }
    public String getLastChannel() { return lastChannel; }
    public void setLastChannel(String lastChannel) { this.lastChannel = lastChannel; }
    public String getLastResult() { return lastResult; }
    public void setLastResult(String lastResult) { this.lastResult = lastResult; }
    public Instant getLastContactAt() { return lastContactAt; }
    public void setLastContactAt(Instant lastContactAt) { this.lastContactAt = lastContactAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public BigDecimal getValorPrincipal() { return valorPrincipal; }
    public void setValorPrincipal(BigDecimal valorPrincipal) { this.valorPrincipal = valorPrincipal; }
    public BigDecimal getValorJuros() { return valorJuros; }
    public void setValorJuros(BigDecimal valorJuros) { this.valorJuros = valorJuros; }
    public String getTipoParcela() { return tipoParcela; }
    public void setTipoParcela(String tipoParcela) { this.tipoParcela = tipoParcela; }
    public BigDecimal getValorBase() { return valorBase; }
    public void setValorBase(BigDecimal valorBase) { this.valorBase = valorBase; }
    public BigDecimal getMultaAplicada() { return multaAplicada; }
    public void setMultaAplicada(BigDecimal multaAplicada) { this.multaAplicada = multaAplicada; }
    public BigDecimal getMoraAplicada() { return moraAplicada; }
    public void setMoraAplicada(BigDecimal moraAplicada) { this.moraAplicada = moraAplicada; }
    public Instant getLastPaidAt() { return lastPaidAt; }
    public void setLastPaidAt(Instant lastPaidAt) { this.lastPaidAt = lastPaidAt; }
    public LocalDate getNextFollowUp() { return nextFollowUp; }
    public void setNextFollowUp(LocalDate nextFollowUp) { this.nextFollowUp = nextFollowUp; }
    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }
    public String getProximaAcao() { return proximaAcao; }
    public void setProximaAcao(String proximaAcao) { this.proximaAcao = proximaAcao; }
    public String getPromiseNote() { return promiseNote; }
    public void setPromiseNote(String promiseNote) { this.promiseNote = promiseNote; }
    public String getPromiseOwner() { return promiseOwner; }
    public void setPromiseOwner(String promiseOwner) { this.promiseOwner = promiseOwner; }
    public BigDecimal getPagoJuros() { return pagoJuros; }
    public void setPagoJuros(BigDecimal pagoJuros) { this.pagoJuros = pagoJuros; }
    public BigDecimal getPagoMulta() { return pagoMulta; }
    public void setPagoMulta(BigDecimal pagoMulta) { this.pagoMulta = pagoMulta; }
    public BigDecimal getPagoEncargos() { return pagoEncargos; }
    public void setPagoEncargos(BigDecimal pagoEncargos) { this.pagoEncargos = pagoEncargos; }
    public BigDecimal getPagoPrincipal() { return pagoPrincipal; }
    public void setPagoPrincipal(BigDecimal pagoPrincipal) { this.pagoPrincipal = pagoPrincipal; }

    public BigDecimal remaining() {
        BigDecimal paid = valorPago == null ? BigDecimal.ZERO : valorPago;
        BigDecimal total = valor == null ? BigDecimal.ZERO : valor;
        return total.subtract(paid).max(BigDecimal.ZERO);
    }
}
