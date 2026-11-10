# RuralMed

Sistema de telessaúde para triagem em áreas rurais, desenvolvido para a disciplina de Desenvolvimento de Sistemas I da Universidade Presbiteriana Mackenzie.

## Funcionalidades

- Cadastro de pacientes e registro de consentimento.
- Coleta de triagens com armazenamento local e sincronização quando a conexão retorna.
- Cálculo de escore de risco com indicação dos fatores considerados.
- Histórico clínico longitudinal por paciente, compartilhado entre especialidades, com busca médica por CPF ou ID RuralMed; anexos de exames em PDF, JPG ou PNG.
- Validação de CPF/CNPJ, cadastro do paciente, CID selecionado e faixas/coerência dos sinais vitais no servidor (não apenas no navegador).
- Encaminhamento de casos por especialidade.
- Emissão independente de laudo e prescrição médica estruturada, com catálogo local de referência e assinatura simulada.
- Relatórios de atendimentos, economia estimada com transporte e repasses.

## Perfis de acesso

| Perfil | Responsabilidade |
|---|---|
| ACS | Cadastrar pacientes da sua microárea, registrar triagens e acompanhar resultados. |
| UBS | Gerenciar cadastros, receber triagens e encaminhar casos. |
| Médico | Analisar a fila da sua especialidade e emitir laudos e receitas. |
| Gestor | Consultar indicadores e extratos de repasses. |

## Tecnologias

Java, Spring Boot, Spring Data JPA, MySQL, Flyway, HTML, CSS e JavaScript.

## Testes

Testes unitários e integrados com JUnit, Mockito, Spring Boot, MockMvc e MySQL, cobrindo autenticação, permissões, regras de negócio e o fluxo de atendimento.

## Contas de demonstração

As contas de demonstração usam a senha `ruralmed123`. A tela de login inclui um atalho para o clínico geral (`clinico@ruralmed.dev`), vinculado à especialidade Clínica Médica.

## Limitações do MVP

- A assinatura digital é simulada, sem certificação ICP-Brasil.
- As sessões ficam na memória e são invalidadas ao reiniciar o servidor.
- O envio de áudios não está implementado; anexos de exames aceitam PDF, JPG ou PNG e ficam no banco.
- A orientação da Farmácia Popular é informativa; não há integração de catálogo, estoque ou localização de unidades.
- O catálogo de medicamentos é demonstrativo, não recomenda tratamentos e não substitui a validação clínica ou uma fonte farmacêutica atualizada.
- A fila local de triagens não torna todo o sistema disponível offline.
