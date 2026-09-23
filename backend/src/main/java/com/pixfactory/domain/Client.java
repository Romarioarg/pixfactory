package com.pixfactory.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "clients")
public class Client {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nome;
    @Column(nullable = false, unique = true)
    private String cpf;
    private String telefone;
    private String email;
    private String endereco;
    private LocalDate dataNascimento;
    private String profissao;
    private BigDecimal rendaMensal = BigDecimal.ZERO;
    private String banco;
    private String classificacao;
    private String status = "ativo";
    private String fotoUrl;
    private LocalDate criadoEm = LocalDate.now();
    @Column(columnDefinition = "TEXT")
    private String historicoJson = "[]";
    @Column(columnDefinition = "TEXT")
    private String indicadorJson = "{}";
    @Column(columnDefinition = "TEXT")
    private String referenciasJson = "[]";
    @Column(columnDefinition = "TEXT")
    private String observacoes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getProfissao() { return profissao; }
    public void setProfissao(String profissao) { this.profissao = profissao; }
    public BigDecimal getRendaMensal() { return rendaMensal; }
    public void setRendaMensal(BigDecimal rendaMensal) { this.rendaMensal = rendaMensal; }
    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }
    public String getClassificacao() { return classificacao; }
    public void setClassificacao(String classificacao) { this.classificacao = classificacao; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public LocalDate getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDate criadoEm) { this.criadoEm = criadoEm; }
    public String getHistoricoJson() { return historicoJson; }
    public void setHistoricoJson(String historicoJson) { this.historicoJson = historicoJson; }
    public String getIndicadorJson() { return indicadorJson; }
    public void setIndicadorJson(String indicadorJson) { this.indicadorJson = indicadorJson; }
    public String getReferenciasJson() { return referenciasJson; }
    public void setReferenciasJson(String referenciasJson) { this.referenciasJson = referenciasJson; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
