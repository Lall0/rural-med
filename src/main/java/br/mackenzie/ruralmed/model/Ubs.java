package br.mackenzie.ruralmed.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ubs")
public class Ubs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "municipio_id")
    private Municipio municipio;

    private String nome;

    private String cnes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Municipio getMunicipio() { return municipio; }
    public void setMunicipio(Municipio municipio) { this.municipio = municipio; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCnes() { return cnes; }
    public void setCnes(String cnes) { this.cnes = cnes; }
}
