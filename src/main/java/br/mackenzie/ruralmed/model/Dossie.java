package br.mackenzie.ruralmed.model;

import br.mackenzie.ruralmed.regras.RegrasDeNegocio;
import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "dossie")
public class Dossie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Gerado no aparelho do ACS; impede que um reenvio duplique a triagem (RNF02). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID idLocal;

    @ManyToOne
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "acs_id")
    private Acs acs;

    @ManyToOne
    @JoinColumn(name = "especialidade_id")
    private Especialidade especialidade;

    @ManyToOne
    @JoinColumn(name = "medico_id")
    private Medico medico;

    @ManyToOne
    @JoinColumn(name = "clinica_id")
    private ClinicaParceira clinica;

    @JdbcTypeCode(SqlTypes.JSON)
    @JsonRawValue
    private String dadosTriagem;

    private String relato;

    private BigDecimal escore;

    private Boolean prioritario = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @JsonRawValue
    private String fatoresEscore;

    @Enumerated(EnumType.STRING)
    private StatusDossie status = StatusDossie.SINCRONIZADO;

    private OffsetDateTime coletadoEm;

    private OffsetDateTime sincronizadoEm = OffsetDateTime.now();

    private OffsetDateTime encaminhadoEm;

    @OneToOne(mappedBy = "dossie")
    @JsonIgnoreProperties("dossie")
    private Laudo laudo;

    /** RN06: situação do prazo de 24 h (calculada na hora, não fica no banco). */
    public RegrasDeNegocio.Prazo getPrazo() {
        if (status != StatusDossie.ENCAMINHADO) {
            return RegrasDeNegocio.calcularPrazo(false, null, null);
        }
        return RegrasDeNegocio.calcularPrazo(Boolean.TRUE.equals(prioritario), encaminhadoEm, OffsetDateTime.now());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getIdLocal() { return idLocal; }
    public void setIdLocal(UUID idLocal) { this.idLocal = idLocal; }

    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }

    public Acs getAcs() { return acs; }
    public void setAcs(Acs acs) { this.acs = acs; }

    public Especialidade getEspecialidade() { return especialidade; }
    public void setEspecialidade(Especialidade especialidade) { this.especialidade = especialidade; }

    public Medico getMedico() { return medico; }
    public void setMedico(Medico medico) { this.medico = medico; }

    public ClinicaParceira getClinica() { return clinica; }
    public void setClinica(ClinicaParceira clinica) { this.clinica = clinica; }

    public String getDadosTriagem() { return dadosTriagem; }
    public void setDadosTriagem(String dadosTriagem) { this.dadosTriagem = dadosTriagem; }

    public String getRelato() { return relato; }
    public void setRelato(String relato) { this.relato = relato; }

    public BigDecimal getEscore() { return escore; }
    public void setEscore(BigDecimal escore) { this.escore = escore; }

    public Boolean getPrioritario() { return prioritario; }
    public void setPrioritario(Boolean prioritario) { this.prioritario = prioritario; }

    public String getFatoresEscore() { return fatoresEscore; }
    public void setFatoresEscore(String fatoresEscore) { this.fatoresEscore = fatoresEscore; }

    public StatusDossie getStatus() { return status; }
    public void setStatus(StatusDossie status) { this.status = status; }

    public OffsetDateTime getColetadoEm() { return coletadoEm; }
    public void setColetadoEm(OffsetDateTime coletadoEm) { this.coletadoEm = coletadoEm; }

    public OffsetDateTime getSincronizadoEm() { return sincronizadoEm; }
    public void setSincronizadoEm(OffsetDateTime sincronizadoEm) { this.sincronizadoEm = sincronizadoEm; }

    public OffsetDateTime getEncaminhadoEm() { return encaminhadoEm; }
    public void setEncaminhadoEm(OffsetDateTime encaminhadoEm) { this.encaminhadoEm = encaminhadoEm; }

    public Laudo getLaudo() { return laudo; }
    public void setLaudo(Laudo laudo) { this.laudo = laudo; }
}
