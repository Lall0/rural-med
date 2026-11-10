CREATE TABLE municipio (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(120) NOT NULL,
    uf CHAR(2) NOT NULL,
    custo_medio_viagem_tfd NUMERIC(10,2) NOT NULL,
    CONSTRAINT pk_municipio PRIMARY KEY (id),
    CONSTRAINT ck_municipio_uf CHECK (REGEXP_LIKE(uf, '^[A-Z]{2}$', 'c')),
    CONSTRAINT ck_municipio_custo CHECK (custo_medio_viagem_tfd >= 0),
    CONSTRAINT uq_municipio UNIQUE (nome, uf)
);

CREATE TABLE ubs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    municipio_id BIGINT NOT NULL,
    nome VARCHAR(150) NOT NULL,
    cnes CHAR(7) NOT NULL UNIQUE,
    CONSTRAINT pk_ubs PRIMARY KEY (id),
    CONSTRAINT fk_ubs_municipio FOREIGN KEY (municipio_id) REFERENCES municipio(id),
    CONSTRAINT ck_ubs_cnes CHECK (REGEXP_LIKE(cnes, '^[0-9]{7}$', 'c'))
);

CREATE TABLE microarea (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ubs_id BIGINT NOT NULL,
    nome VARCHAR(80) NOT NULL,
    CONSTRAINT pk_microarea PRIMARY KEY (id),
    CONSTRAINT fk_microarea_ubs FOREIGN KEY (ubs_id) REFERENCES ubs(id),
    CONSTRAINT uq_microarea UNIQUE (ubs_id, nome)
);

CREATE TABLE especialidade (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(80) NOT NULL UNIQUE,
    CONSTRAINT pk_especialidade PRIMARY KEY (id)
);

CREATE TABLE clinica_parceira (
    id BIGINT NOT NULL AUTO_INCREMENT,
    municipio_id BIGINT NOT NULL,
    nome VARCHAR(150) NOT NULL,
    cnpj CHAR(14) NOT NULL UNIQUE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_clinica_parceira PRIMARY KEY (id),
    CONSTRAINT fk_clinica_municipio FOREIGN KEY (municipio_id) REFERENCES municipio(id),
    CONSTRAINT ck_clinica_cnpj CHECK (REGEXP_LIKE(cnpj, '^[0-9]{14}$', 'c'))
);

CREATE TABLE usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    cpf CHAR(11) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    perfil ENUM('ACS', 'UBS', 'MEDICO', 'GESTOR') NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT ck_usuario_cpf CHECK (REGEXP_LIKE(cpf, '^[0-9]{11}$', 'c'))
);

CREATE TABLE acs (
    usuario_id BIGINT NOT NULL,
    microarea_id BIGINT NOT NULL,
    CONSTRAINT pk_acs PRIMARY KEY (usuario_id),
    CONSTRAINT fk_acs_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_acs_microarea FOREIGN KEY (microarea_id) REFERENCES microarea(id)
);

CREATE TABLE medico (
    usuario_id BIGINT NOT NULL,
    especialidade_id BIGINT NOT NULL,
    clinica_id BIGINT,
    crm VARCHAR(10) NOT NULL,
    uf_crm CHAR(2) NOT NULL,
    crm_ativo BOOLEAN NOT NULL DEFAULT FALSE,
    valor_por_laudo NUMERIC(10,2) NOT NULL,
    CONSTRAINT pk_medico PRIMARY KEY (usuario_id),
    CONSTRAINT fk_medico_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_medico_especialidade FOREIGN KEY (especialidade_id) REFERENCES especialidade(id),
    CONSTRAINT fk_medico_clinica FOREIGN KEY (clinica_id) REFERENCES clinica_parceira(id),
    CONSTRAINT uq_medico_crm UNIQUE (crm, uf_crm),
    CONSTRAINT ck_medico_valor CHECK (valor_por_laudo >= 0)
);

CREATE TABLE paciente (
    id BIGINT NOT NULL AUTO_INCREMENT,
    microarea_id BIGINT NOT NULL,
    nome VARCHAR(150) NOT NULL,
    cpf CHAR(11) UNIQUE,
    cns CHAR(15) UNIQUE,
    data_nascimento DATE NOT NULL,
    telefone VARCHAR(20),
    consentimento_em DATETIME(6),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_paciente PRIMARY KEY (id),
    CONSTRAINT fk_paciente_microarea FOREIGN KEY (microarea_id) REFERENCES microarea(id),
    CONSTRAINT ck_paciente_documento CHECK (cpf IS NOT NULL OR cns IS NOT NULL)
);

CREATE TABLE dossie (
    id BIGINT NOT NULL AUTO_INCREMENT,
    id_local CHAR(36) NOT NULL UNIQUE,
    paciente_id BIGINT NOT NULL,
    acs_id BIGINT NOT NULL,
    especialidade_id BIGINT,
    medico_id BIGINT,
    clinica_id BIGINT,
    dados_triagem JSON NOT NULL,
    relato TEXT,
    escore NUMERIC(6,2),
    prioritario BOOLEAN NOT NULL DEFAULT FALSE,
    fatores_escore JSON,
    status ENUM('SINCRONIZADO', 'AGUARDANDO_ENCAMINHAMENTO', 'ENCAMINHADO', 'LAUDO_EMITIDO') NOT NULL DEFAULT 'SINCRONIZADO',
    coletado_em DATETIME(6) NOT NULL,
    sincronizado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    encaminhado_em DATETIME(6),
    CONSTRAINT pk_dossie PRIMARY KEY (id),
    CONSTRAINT fk_dossie_paciente FOREIGN KEY (paciente_id) REFERENCES paciente(id),
    CONSTRAINT fk_dossie_acs FOREIGN KEY (acs_id) REFERENCES acs(usuario_id),
    CONSTRAINT fk_dossie_especialidade FOREIGN KEY (especialidade_id) REFERENCES especialidade(id),
    CONSTRAINT fk_dossie_medico FOREIGN KEY (medico_id) REFERENCES medico(usuario_id),
    CONSTRAINT fk_dossie_clinica FOREIGN KEY (clinica_id) REFERENCES clinica_parceira(id),
    CONSTRAINT ck_dossie_escore CHECK (escore IS NULL OR escore >= 0),
    CONSTRAINT ck_dossie_encaminhado CHECK (status NOT IN ('ENCAMINHADO','LAUDO_EMITIDO') OR (especialidade_id IS NOT NULL AND encaminhado_em IS NOT NULL))
);

CREATE TABLE midia (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dossie_id BIGINT NOT NULL,
    tipo ENUM('FOTO', 'AUDIO') NOT NULL,
    chave_objeto VARCHAR(255) NOT NULL UNIQUE,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_midia PRIMARY KEY (id),
    CONSTRAINT fk_midia_dossie FOREIGN KEY (dossie_id) REFERENCES dossie(id) ON DELETE CASCADE
);

CREATE TABLE cid (
    codigo VARCHAR(10) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    versao VARCHAR(6) NOT NULL,
    CONSTRAINT pk_cid PRIMARY KEY (codigo),
    CONSTRAINT ck_cid_versao CHECK (versao IN ('CID-10','CID-11'))
);

CREATE TABLE laudo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dossie_id BIGINT NOT NULL UNIQUE,
    medico_id BIGINT NOT NULL,
    cid_codigo VARCHAR(10) NOT NULL,
    parecer TEXT NOT NULL,
    pdf_chave VARCHAR(255),
    assinatura_digital TEXT,
    status ENUM('RASCUNHO', 'AGUARDANDO_ASSINATURA', 'EMITIDO') NOT NULL DEFAULT 'RASCUNHO',
    emitido_em DATETIME(6),
    CONSTRAINT pk_laudo PRIMARY KEY (id),
    CONSTRAINT fk_laudo_dossie FOREIGN KEY (dossie_id) REFERENCES dossie(id),
    CONSTRAINT fk_laudo_medico FOREIGN KEY (medico_id) REFERENCES medico(usuario_id),
    CONSTRAINT fk_laudo_cid FOREIGN KEY (cid_codigo) REFERENCES cid(codigo),
    CONSTRAINT ck_laudo_emitido CHECK (status <> 'EMITIDO' OR (assinatura_digital IS NOT NULL AND emitido_em IS NOT NULL))
);

CREATE TABLE receita (
    id BIGINT NOT NULL AUTO_INCREMENT,
    laudo_id BIGINT NOT NULL,
    prescricao TEXT NOT NULL,
    pdf_chave VARCHAR(255),
    assinatura_digital TEXT,
    assinada_em DATETIME(6),
    status ENUM('AGUARDANDO_ASSINATURA', 'ASSINADA') NOT NULL DEFAULT 'AGUARDANDO_ASSINATURA',
    CONSTRAINT pk_receita PRIMARY KEY (id),
    CONSTRAINT fk_receita_laudo FOREIGN KEY (laudo_id) REFERENCES laudo(id),
    CONSTRAINT ck_receita_assinada CHECK (status <> 'ASSINADA' OR (assinatura_digital IS NOT NULL AND assinada_em IS NOT NULL))
);

CREATE INDEX ix_ubs_municipio ON ubs (municipio_id);
CREATE INDEX ix_paciente_microarea ON paciente (microarea_id, ativo);
CREATE INDEX ix_acs_microarea ON acs (microarea_id);
CREATE INDEX ix_medico_especialidade ON medico (especialidade_id, crm_ativo);
-- Fila do médico (RF03/RN05): casos encaminhados por escore decrescente e ordem de chegada
CREATE INDEX ix_dossie_fila ON dossie (especialidade_id, status, escore DESC, encaminhado_em);
CREATE INDEX ix_dossie_paciente ON dossie (paciente_id, coletado_em);
CREATE INDEX ix_midia_dossie ON midia (dossie_id);
CREATE INDEX ix_cid_descricao ON cid (descricao);
CREATE INDEX ix_laudo_extrato ON laudo (medico_id, status, emitido_em);
CREATE INDEX ix_receita_laudo ON receita (laudo_id);
