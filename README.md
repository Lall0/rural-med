# RuralMed

Sistema de telessaúde para triagem em áreas rurais, desenvolvido para a disciplina de Desenvolvimento de Sistemas I da Universidade Presbiteriana Mackenzie.

## Funcionalidades

- Cadastro de pacientes e registro de consentimento.
- Coleta de triagens com armazenamento local e sincronização quando a conexão retorna.
- Cálculo de escore de risco com indicação dos fatores considerados.
- Encaminhamento de casos por especialidade.
- Emissão de laudos e receitas com assinatura simulada.
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

## Limitações do MVP

- A assinatura digital é simulada, sem certificação ICP-Brasil.
- As sessões ficam na memória e são invalidadas ao reiniciar o servidor.
- O envio de fotos e áudios não está implementado.
- A fila local de triagens não torna todo o sistema disponível offline.
