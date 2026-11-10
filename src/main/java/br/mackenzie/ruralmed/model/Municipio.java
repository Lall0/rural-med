package br.mackenzie.ruralmed.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "municipio")
public class Municipio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String uf;

    private BigDecimal custoMedioViagemTfd;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }

    public BigDecimal getCustoMedioViagemTfd() { return custoMedioViagemTfd; }
    public void setCustoMedioViagemTfd(BigDecimal custoMedioViagemTfd) { this.custoMedioViagemTfd = custoMedioViagemTfd; }
}
