package br.mackenzie.ruralmed.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "laudo")
public class Laudo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "dossie_id")
    @JsonIgnoreProperties("laudo")
    private Dossie dossie;

    @ManyToOne
    @JoinColumn(name = "medico_id")
    private Medico medico;

    @ManyToOne
    @JoinColumn(name = "cid_codigo")
    private Cid cid;

    private String parecer;

    private String assinaturaDigital;

    @Enumerated(EnumType.STRING)
    private StatusLaudo status = StatusLaudo.RASCUNHO;

    private OffsetDateTime emitidoEm;

    @OneToMany(mappedBy = "laudo", fetch = FetchType.EAGER)
    private List<Receita> receitas = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Dossie getDossie() { return dossie; }
    public void setDossie(Dossie dossie) { this.dossie = dossie; }

    public Medico getMedico() { return medico; }
    public void setMedico(Medico medico) { this.medico = medico; }

    public Cid getCid() { return cid; }
    public void setCid(Cid cid) { this.cid = cid; }

    public String getParecer() { return parecer; }
    public void setParecer(String parecer) { this.parecer = parecer; }

    public String getAssinaturaDigital() { return assinaturaDigital; }
    public void setAssinaturaDigital(String assinaturaDigital) { this.assinaturaDigital = assinaturaDigital; }

    public StatusLaudo getStatus() { return status; }
    public void setStatus(StatusLaudo status) { this.status = status; }

    public OffsetDateTime getEmitidoEm() { return emitidoEm; }
    public void setEmitidoEm(OffsetDateTime emitidoEm) { this.emitidoEm = emitidoEm; }

    public List<Receita> getReceitas() { return receitas; }
    public void setReceitas(List<Receita> receitas) { this.receitas = receitas; }
}
