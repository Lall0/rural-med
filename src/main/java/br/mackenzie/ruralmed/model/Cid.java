package br.mackenzie.ruralmed.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cid")
public class Cid {

    @Id
    private String codigo;

    private String descricao;

    private String versao;

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getVersao() { return versao; }
    public void setVersao(String versao) { this.versao = versao; }
}
