package br.mackenzie.ruralmed.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "medico")
public class Medico {

    @Id
    private Long usuarioId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "especialidade_id")
    private Especialidade especialidade;

    @ManyToOne
    @JoinColumn(name = "clinica_id")
    private ClinicaParceira clinica;

    private String crm;

    private String ufCrm;

    private Boolean crmAtivo = false;

    private BigDecimal valorPorLaudo = BigDecimal.ZERO;

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Especialidade getEspecialidade() { return especialidade; }
    public void setEspecialidade(Especialidade especialidade) { this.especialidade = especialidade; }

    public ClinicaParceira getClinica() { return clinica; }
    public void setClinica(ClinicaParceira clinica) { this.clinica = clinica; }

    public String getCrm() { return crm; }
    public void setCrm(String crm) { this.crm = crm; }

    public String getUfCrm() { return ufCrm; }
    public void setUfCrm(String ufCrm) { this.ufCrm = ufCrm; }

    public Boolean getCrmAtivo() { return crmAtivo; }
    public void setCrmAtivo(Boolean crmAtivo) { this.crmAtivo = crmAtivo; }

    public BigDecimal getValorPorLaudo() { return valorPorLaudo; }
    public void setValorPorLaudo(BigDecimal valorPorLaudo) { this.valorPorLaudo = valorPorLaudo; }
}
