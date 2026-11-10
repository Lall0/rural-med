-- Dados para demonstração. Senha de todos os usuários: ruralmed123

INSERT INTO usuario (nome, email, cpf, senha_hash, perfil, ativo, criado_em) VALUES
    ('Equipe UBS Rural Norte',     'ubs@ruralmed.dev',     '10000000001', '$2a$10$RqVvcwxpr5Jn1yMMamWojOi/5bHfMpIBixX8H/sKl9zcbOcO1Twua', 'UBS',    TRUE, now()),
    ('Gestora Municipal de Saúde', 'gestor@ruralmed.dev',  '10000000002', '$2a$10$RqVvcwxpr5Jn1yMMamWojOi/5bHfMpIBixX8H/sKl9zcbOcO1Twua', 'GESTOR', TRUE, now()),
    ('Ana Souza (ACS)',            'acs@ruralmed.dev',     '10000000003', '$2a$10$RqVvcwxpr5Jn1yMMamWojOi/5bHfMpIBixX8H/sKl9zcbOcO1Twua', 'ACS',    TRUE, now()),
    ('Bruno Lima (ACS)',           'acs2@ruralmed.dev',    '10000000004', '$2a$10$RqVvcwxpr5Jn1yMMamWojOi/5bHfMpIBixX8H/sKl9zcbOcO1Twua', 'ACS',    TRUE, now()),
    ('Dra. Helena Prado',          'dermato@ruralmed.dev', '10000000005', '$2a$10$RqVvcwxpr5Jn1yMMamWojOi/5bHfMpIBixX8H/sKl9zcbOcO1Twua', 'MEDICO', TRUE, now()),
    ('Dr. Rui Carvalho',           'cardio@ruralmed.dev',  '10000000006', '$2a$10$RqVvcwxpr5Jn1yMMamWojOi/5bHfMpIBixX8H/sKl9zcbOcO1Twua', 'MEDICO', TRUE, now());

INSERT INTO acs (usuario_id, microarea_id) VALUES
    ((SELECT id FROM usuario WHERE email = 'acs@ruralmed.dev'),  (SELECT id FROM microarea WHERE nome LIKE 'MA-01%')),
    ((SELECT id FROM usuario WHERE email = 'acs2@ruralmed.dev'), (SELECT id FROM microarea WHERE nome LIKE 'MA-03%'));

INSERT INTO medico (usuario_id, especialidade_id, clinica_id, crm, uf_crm, crm_ativo, valor_por_laudo) VALUES
    ((SELECT id FROM usuario WHERE email = 'dermato@ruralmed.dev'), (SELECT id FROM especialidade WHERE nome = 'Dermatologia'),
     (SELECT id FROM clinica_parceira LIMIT 1), '123456', 'SP', TRUE, 120.00),
    ((SELECT id FROM usuario WHERE email = 'cardio@ruralmed.dev'),  (SELECT id FROM especialidade WHERE nome = 'Cardiologia'),
     (SELECT id FROM clinica_parceira LIMIT 1), '654321', 'SP', TRUE, 150.00);

INSERT INTO paciente (microarea_id, nome, cpf, data_nascimento, telefone, consentimento_em, ativo) VALUES
    ((SELECT id FROM microarea WHERE nome LIKE 'MA-01%'), 'José Ferreira',     '52998224725', '1952-04-12', '13999990001', now(), TRUE),
    ((SELECT id FROM microarea WHERE nome LIKE 'MA-01%'), 'Maria das Dores',   '11144477735', '1978-09-03', '13999990002', now(), TRUE),
    ((SELECT id FROM microarea WHERE nome LIKE 'MA-01%'), 'João Pedro Santos', '39053344705', '2016-01-20', '13999990003', now(), TRUE),
    ((SELECT id FROM microarea WHERE nome LIKE 'MA-01%'), 'Antônia Ribeiro',   '84434835006', '1990-06-30', '13999990004', NULL,  TRUE),
    ((SELECT id FROM microarea WHERE nome LIKE 'MA-03%'), 'Benedito Pereira',  '15350946056', '1947-11-05', '13999990005', now(), TRUE);

INSERT INTO dossie (id_local, paciente_id, acs_id, especialidade_id, dados_triagem, relato, escore, prioritario,
                    fatores_escore, status, coletado_em, sincronizado_em, encaminhado_em) VALUES
    (UUID(), (SELECT id FROM paciente WHERE cpf = '52998224725'), (SELECT id FROM usuario WHERE email = 'acs@ruralmed.dev'),
     (SELECT id FROM especialidade WHERE nome = 'Cardiologia'),
     '{"queixaPrincipal":"Dor no peito ao caminhar","temperatura":37.1,"pressaoSistolica":182,"pressaoDiastolica":110,"frequenciaCardiaca":118,"saturacaoO2":93,"dor":8,"comorbidades":["hipertensão","diabetes"]}',
     'Refere dor no peito e falta de ar ao subir a ladeira.', 94, TRUE,
     '["idade 65 anos ou mais (+10)","saturação de O2 baixa (+12)","pressão muito alta (+20)","dor intensa (+12)","comorbidade: hipertensão (+5)","comorbidade: diabetes (+5)","relato: dor no peito (+15)","relato: falta de ar (+15)"]',
     'ENCAMINHADO', now() - INTERVAL 22 HOUR, now() - INTERVAL 21 HOUR, now() - INTERVAL 21 HOUR),
    (UUID(), (SELECT id FROM paciente WHERE cpf = '11144477735'), (SELECT id FROM usuario WHERE email = 'acs@ruralmed.dev'),
     (SELECT id FROM especialidade WHERE nome = 'Dermatologia'),
     '{"queixaPrincipal":"Mancha escura no antebraço","temperatura":36.6,"pressaoSistolica":128,"pressaoDiastolica":82,"frequenciaCardiaca":76,"saturacaoO2":98,"dor":2,"comorbidades":[]}',
     'Mancha que cresce há três meses, com bordas irregulares.', 0, FALSE, '[]',
     'LAUDO_EMITIDO', now() - INTERVAL 2 DAY, now() - INTERVAL 2 DAY, now() - INTERVAL 40 HOUR),
    (UUID(), (SELECT id FROM paciente WHERE cpf = '39053344705'), (SELECT id FROM usuario WHERE email = 'acs@ruralmed.dev'),
     NULL,
     '{"queixaPrincipal":"Lesões com coceira no corpo","temperatura":37.9,"pressaoSistolica":105,"pressaoDiastolica":65,"frequenciaCardiaca":104,"saturacaoO2":97,"dor":4,"comorbidades":[]}',
     'Coceira intensa à noite; irmãos com o mesmo quadro.', 8, FALSE, '["febre (+8)"]',
     'SINCRONIZADO', now() - INTERVAL 3 HOUR, now() - INTERVAL 2 HOUR, NULL);

INSERT INTO laudo (dossie_id, medico_id, cid_codigo, parecer, assinatura_digital, status, emitido_em) VALUES
    ((SELECT id FROM dossie WHERE status = 'LAUDO_EMITIDO'), (SELECT id FROM usuario WHERE email = 'dermato@ruralmed.dev'), 'L30.9',
     'Lesão pigmentada com assimetria e bordas irregulares. Recomendo biópsia em serviço de referência e fotoproteção diária.',
     'ICP-Brasil (simulada) | CRM 123456/SP | demonstração', 'EMITIDO', now() - INTERVAL 1 DAY);

INSERT INTO receita (laudo_id, prescricao, assinatura_digital, assinada_em, status) VALUES
    ((SELECT id FROM laudo LIMIT 1), 'Protetor solar FPS 50, aplicar a cada 3 horas na área exposta.',
     'ICP-Brasil (simulada) | CRM 123456/SP | demonstração', now() - INTERVAL 1 DAY, 'ASSINADA');
