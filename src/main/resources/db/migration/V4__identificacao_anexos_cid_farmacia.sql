ALTER TABLE paciente
    ADD COLUMN identificador_unico CHAR(36) NULL;

UPDATE paciente SET identificador_unico = UUID() WHERE identificador_unico IS NULL;

ALTER TABLE paciente
    MODIFY identificador_unico CHAR(36) NOT NULL,
    ADD CONSTRAINT uq_paciente_identificador_unico UNIQUE (identificador_unico),
    DROP CHECK ck_paciente_documento;

ALTER TABLE laudo
    MODIFY cid_codigo VARCHAR(10) NULL;

ALTER TABLE receita
    ADD COLUMN farmacia_popular BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE anexo_exame (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dossie_id BIGINT NOT NULL,
    nome_arquivo VARCHAR(255) NOT NULL,
    tipo_conteudo VARCHAR(100) NOT NULL,
    tamanho BIGINT NOT NULL,
    conteudo LONGBLOB NOT NULL,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_anexo_exame PRIMARY KEY (id),
    CONSTRAINT fk_anexo_exame_dossie FOREIGN KEY (dossie_id) REFERENCES dossie(id) ON DELETE CASCADE,
    CONSTRAINT ck_anexo_exame_tamanho CHECK (tamanho > 0 AND tamanho <= 10485760)
);

CREATE INDEX ix_anexo_exame_dossie ON anexo_exame (dossie_id, criado_em);
