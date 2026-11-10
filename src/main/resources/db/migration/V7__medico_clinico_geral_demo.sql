INSERT INTO usuario (nome, email, cpf, senha_hash, perfil, ativo, criado_em) VALUES
    ('Dr. Rafael Martins (Clínico Geral)', 'clinico@ruralmed.dev', '10000000007',
     '$2a$10$RqVvcwxpr5Jn1yMMamWojOi/5bHfMpIBixX8H/sKl9zcbOcO1Twua', 'MEDICO', TRUE, now());

INSERT INTO medico (usuario_id, especialidade_id, clinica_id, crm, uf_crm, crm_ativo, valor_por_laudo) VALUES
    ((SELECT id FROM usuario WHERE email = 'clinico@ruralmed.dev'),
     (SELECT id FROM especialidade WHERE nome = 'Clínica Médica'),
     (SELECT id FROM clinica_parceira LIMIT 1), '789012', 'SP', TRUE, 100.00);
