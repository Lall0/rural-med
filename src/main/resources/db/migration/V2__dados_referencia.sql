INSERT INTO municipio (nome, uf, custo_medio_viagem_tfd) VALUES
    ('Registro', 'SP', 850.00),
    ('Eldorado', 'SP', 1200.00);

INSERT INTO ubs (municipio_id, nome, cnes) VALUES
    ((SELECT id FROM municipio WHERE nome = 'Registro'), 'UBS Rural Norte', '1234567'),
    ((SELECT id FROM municipio WHERE nome = 'Eldorado'), 'UBS Vale do Ribeira', '7654321');

INSERT INTO microarea (ubs_id, nome) VALUES
    ((SELECT id FROM ubs WHERE cnes = '1234567'), 'MA-01 Bairro Alto'),
    ((SELECT id FROM ubs WHERE cnes = '1234567'), 'MA-02 Sítio Velho'),
    ((SELECT id FROM ubs WHERE cnes = '7654321'), 'MA-03 Quilombo Ivaporunduva');

INSERT INTO especialidade (nome) VALUES
    ('Cardiologia'), ('Dermatologia'), ('Endocrinologia'),
    ('Pediatria'), ('Clínica Médica'), ('Ortopedia');

INSERT INTO clinica_parceira (municipio_id, nome, cnpj, ativo) VALUES
    ((SELECT id FROM municipio WHERE nome = 'Registro'), 'Clínica Regional Vale Saúde', '12345678000190', TRUE);

INSERT INTO cid (codigo, descricao, versao) VALUES
    ('A09',   'Diarreia e gastroenterite de origem infecciosa presumível', 'CID-10'),
    ('B35.4', 'Tinha do corpo (tinea corporis)', 'CID-10'),
    ('B86',   'Escabiose', 'CID-10'),
    ('E10.9', 'Diabetes mellitus insulinodependente sem complicações', 'CID-10'),
    ('E11.9', 'Diabetes mellitus não insulinodependente sem complicações', 'CID-10'),
    ('E03.9', 'Hipotireoidismo não especificado', 'CID-10'),
    ('E66.9', 'Obesidade não especificada', 'CID-10'),
    ('I10',   'Hipertensão essencial (primária)', 'CID-10'),
    ('I20.9', 'Angina pectoris não especificada', 'CID-10'),
    ('I48',   'Flutter e fibrilação atrial', 'CID-10'),
    ('I50.9', 'Insuficiência cardíaca não especificada', 'CID-10'),
    ('J06.9', 'Infecção aguda das vias aéreas superiores não especificada', 'CID-10'),
    ('J18.9', 'Pneumonia não especificada', 'CID-10'),
    ('J45.9', 'Asma não especificada', 'CID-10'),
    ('J44.9', 'Doença pulmonar obstrutiva crônica não especificada', 'CID-10'),
    ('K29.7', 'Gastrite não especificada', 'CID-10'),
    ('L20.9', 'Dermatite atópica não especificada', 'CID-10'),
    ('L30.9', 'Dermatite não especificada', 'CID-10'),
    ('L40.0', 'Psoríase vulgar', 'CID-10'),
    ('L50.9', 'Urticária não especificada', 'CID-10'),
    ('L70.0', 'Acne vulgar', 'CID-10'),
    ('L89.9', 'Úlcera de decúbito não especificada', 'CID-10'),
    ('C44.9', 'Neoplasia maligna da pele não especificada', 'CID-10'),
    ('M54.5', 'Dor lombar baixa', 'CID-10'),
    ('M17.9', 'Gonartrose não especificada', 'CID-10'),
    ('M79.1', 'Mialgia', 'CID-10'),
    ('N39.0', 'Infecção do trato urinário de localização não especificada', 'CID-10'),
    ('R05',   'Tosse', 'CID-10'),
    ('R07.4', 'Dor torácica não especificada', 'CID-10'),
    ('R50.9', 'Febre não especificada', 'CID-10'),
    ('R51',   'Cefaleia', 'CID-10'),
    ('S82.9', 'Fratura da perna, parte não especificada', 'CID-10'),
    ('T14.1', 'Ferimento de região não especificada do corpo', 'CID-10'),
    ('Z00.0', 'Exame médico geral', 'CID-10'),
    ('Z34.9', 'Supervisão de gravidez normal não especificada', 'CID-10'),
    ('BA00',  'Hipertensão essencial', 'CID-11'),
    ('5A11',  'Diabetes mellitus tipo 2', 'CID-11'),
    ('EA90',  'Psoríase', 'CID-11');
