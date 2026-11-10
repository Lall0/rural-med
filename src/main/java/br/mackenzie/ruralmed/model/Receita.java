package br.mackenzie.ruralmed.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "receita")
public class Receita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "laudo_id")
    @JsonIgnore
    private Laudo laudo;

    @ManyToOne
    @JoinColumn(name = "dossie_id", nullable = false)
    @JsonIgnore
    private Dossie dossie;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false)
    @JsonIgnore
    private Medico medico;

    private String prescricao;

    @Column(name = "nome_medicamento")
    private String nomeMedicamento;

    private String apresentacao;

    private String dose;

    @Column(name = "via_administracao")
    private String viaAdministracao;

    private String frequencia;

    private String duracao;

    @Column(name = "orientacoes")
    private String orientacoes;

    private Boolean farmaciaPopular = false;

    private String assinaturaDigital;

    private OffsetDateTime assinadaEm;

    @Enumerated(EnumType.STRING)
    private StatusReceita status = StatusReceita.AGUARDANDO_ASSINATURA;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Laudo getLaudo() { return laudo; }
    public void setLaudo(Laudo laudo) { this.laudo = laudo; }

    public Dossie getDossie() { return dossie; }
    public void setDossie(Dossie dossie) { this.dossie = dossie; }

    public Medico getMedico() { return medico; }
    public void setMedico(Medico medico) { this.medico = medico; }

    public String getPrescricao() { return prescricao; }
    public void setPrescricao(String prescricao) { this.prescricao = prescricao; }

    public String getNomeMedicamento() { return nomeMedicamento; }
    public void setNomeMedicamento(String nomeMedicamento) { this.nomeMedicamento = nomeMedicamento; }

    public String getApresentacao() { return apresentacao; }
    public void setApresentacao(String apresentacao) { this.apresentacao = apresentacao; }

    public String getDose() { return dose; }
    public void setDose(String dose) { this.dose = dose; }

    public String getViaAdministracao() { return viaAdministracao; }
    public void setViaAdministracao(String viaAdministracao) { this.viaAdministracao = viaAdministracao; }

    public String getFrequencia() { return frequencia; }
    public void setFrequencia(String frequencia) { this.frequencia = frequencia; }

    public String getDuracao() { return duracao; }
    public void setDuracao(String duracao) { this.duracao = duracao; }

    public String getOrientacoes() { return orientacoes; }
    public void setOrientacoes(String orientacoes) { this.orientacoes = orientacoes; }

    public Boolean getFarmaciaPopular() { return farmaciaPopular; }
    public void setFarmaciaPopular(Boolean farmaciaPopular) { this.farmaciaPopular = farmaciaPopular; }

    public String getAssinaturaDigital() { return assinaturaDigital; }
    public void setAssinaturaDigital(String assinaturaDigital) { this.assinaturaDigital = assinaturaDigital; }

    public OffsetDateTime getAssinadaEm() { return assinadaEm; }
    public void setAssinadaEm(OffsetDateTime assinadaEm) { this.assinadaEm = assinadaEm; }

    public StatusReceita getStatus() { return status; }
    public void setStatus(StatusReceita status) { this.status = status; }
}
