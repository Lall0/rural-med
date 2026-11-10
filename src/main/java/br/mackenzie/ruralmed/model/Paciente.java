package br.mackenzie.ruralmed.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;

@Entity
@Table(name = "paciente")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "microarea_id")
    private Microarea microarea;

    @NotBlank(message = "Informe o nome")
    private String nome;

    private String cpf;

    private String cns;

    @NotNull(message = "Informe a data de nascimento")
    private LocalDate dataNascimento;

    private String telefone;

    private OffsetDateTime consentimentoEm;

    private Boolean ativo = true;

    /** Idade calculada (não fica no banco, só aparece no JSON). */
    public int getIdade() {
        return dataNascimento == null ? 0 : Period.between(dataNascimento, LocalDate.now()).getYears();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Microarea getMicroarea() { return microarea; }
    public void setMicroarea(Microarea microarea) { this.microarea = microarea; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getCns() { return cns; }
    public void setCns(String cns) { this.cns = cns; }

    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public OffsetDateTime getConsentimentoEm() { return consentimentoEm; }
    public void setConsentimentoEm(OffsetDateTime consentimentoEm) { this.consentimentoEm = consentimentoEm; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
}
