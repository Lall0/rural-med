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

    private String prescricao;

    private String assinaturaDigital;

    private OffsetDateTime assinadaEm;

    @Enumerated(EnumType.STRING)
    private StatusReceita status = StatusReceita.AGUARDANDO_ASSINATURA;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Laudo getLaudo() { return laudo; }
    public void setLaudo(Laudo laudo) { this.laudo = laudo; }

    public String getPrescricao() { return prescricao; }
    public void setPrescricao(String prescricao) { this.prescricao = prescricao; }

    public String getAssinaturaDigital() { return assinaturaDigital; }
    public void setAssinaturaDigital(String assinaturaDigital) { this.assinaturaDigital = assinaturaDigital; }

    public OffsetDateTime getAssinadaEm() { return assinadaEm; }
    public void setAssinadaEm(OffsetDateTime assinadaEm) { this.assinadaEm = assinadaEm; }

    public StatusReceita getStatus() { return status; }
    public void setStatus(StatusReceita status) { this.status = status; }
}
