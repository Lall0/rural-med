
let pacientes = [];
let microareas = [];

async function carregarPacientes() {
    pacientes = await api('/api/pacientes');
    desenharPacientes();
}

function desenharPacientes() {
    const linhas = pacientes.map(p => {
        const identificador = idRuralMed(p);
        return `
        <tr>
            <td class="nome-paciente" data-rotulo="Nome">${esc(p.nome)}</td>
            <td data-rotulo="Identificação"><code class="id-paciente">${esc(identificador.slice(0, 19))}<wbr>${esc(identificador.slice(19))}</code></td>
            <td data-rotulo="Idade">${p.idade} anos</td>
            <td data-rotulo="Microárea">${esc(p.microarea.nome)}</td>
            <td data-rotulo="Consentimento">${p.consentimentoEm ? 'Sim' : '<span class="selo ALERTA">Pendente</span>'}</td>
            <td data-rotulo="Situação">${p.ativo ? 'Ativo' : '<span class="selo inativo">Inativo</span>'}</td>
            <td class="acoes">
                <button class="btn btn-sec btn-peq" type="button" onclick="verHistorico(${p.id})">Histórico</button>
                <button class="btn btn-sec btn-peq" type="button" onclick="formPaciente(${p.id})">Editar</button>
                ${p.ativo ? `<button class="btn btn-sec btn-peq" type="button" onclick="inativarPaciente(${p.id})">Inativar</button>` : ''}
            </td>
        </tr>`;
    }).join('');
    document.getElementById('tabelaPacientes').innerHTML = linhas || '<tr><td>Nenhum paciente cadastrado.</td></tr>';
}

async function formPaciente(id) {
    const p = id ? pacientes.find(x => x.id === id) : { ativo: true };
    const ehUbs = sessao().usuario.perfil === 'UBS';  // o ACS sempre usa a própria microárea (RN02)
    if (ehUbs && microareas.length === 0) {
        microareas = await api('/api/microareas');
    }

    abrirModal(id ? 'Editar paciente' : 'Novo paciente', `
        <form onsubmit="salvarPaciente(event, ${id})">
            <section class="form-secao">
                <h3>Identificação</h3>
                <p class="ajuda">CPF e Cartão SUS são números diferentes. Informe um deles se disponível; sem documentos, o RuralMed gera um identificador interno.</p>
            ${ehUbs ? `<div class="campo"><label for="pMicroarea">Microárea *</label>
                <select id="pMicroarea" required>
                    ${microareas.map(m => `<option value="${m.id}" ${p.microarea && p.microarea.id === m.id ? 'selected' : ''}>${esc(m.nome)}</option>`).join('')}
                </select></div>` : ''}
            <div class="campo"><label for="pNome">Nome *</label>
                <input id="pNome" value="${esc(p.nome)}" minlength="2" maxlength="120" required></div>
            <div class="grade duas">
                <div class="campo"><label for="pCpf">CPF (opcional)</label>
                    <input id="pCpf" inputmode="numeric" autocomplete="off" value="${esc(p.cpf)}" maxlength="14" placeholder="000.000.000-00">
                    <p class="ajuda">Validado pelos dígitos verificadores.</p></div>
                <div class="campo"><label for="pCns">Cartão SUS — CNS (opcional)</label>
                    <input id="pCns" inputmode="numeric" autocomplete="off" value="${esc(p.cns)}" maxlength="15" placeholder="15 números">
                    <p class="ajuda">O CNS não é igual ao CPF e tem numeração própria.</p></div>
                ${p.identificadorUnico ? `<div class="campo"><label>ID RuralMed</label><input value="${esc(idRuralMed(p))}" readonly></div>` : ''}
            </div>
            </section>
            <section class="form-secao">
                <h3>Contato e consentimento</h3>
                <div class="grade duas">
                <div class="campo"><label for="pNascimento">Nascimento *</label>
                    <input id="pNascimento" type="date" max="${new Date().toISOString().slice(0, 10)}"
                        value="${p.dataNascimento || ''}" required></div>
                <div class="campo"><label for="pTelefone">Telefone</label>
                    <input id="pTelefone" type="tel" value="${esc(p.telefone)}" maxlength="20"
                        placeholder="(00) 00000-0000"></div>
            <div class="campo"><label><input id="pConsentimento" type="checkbox" ${p.consentimentoEm ? 'checked' : ''}>
                Paciente consentiu com o atendimento por telessaúde (Lei 14.510/2022)</label></div>
            <div class="campo"><label><input id="pAtivo" type="checkbox" ${p.ativo ? 'checked' : ''}> Ativo</label></div>
                </div>
            </section>
            <button class="btn" type="submit">Salvar</button>
        </form>`);
}

function cpfValido(cpf) {
    const numero = (cpf || '').replace(/\D/g, '');
    if (!/^\d{11}$/.test(numero) || /^(\d)\1{10}$/.test(numero)) return false;
    const digito = tamanho => {
        let soma = 0;
        for (let i = 0; i < tamanho; i++) soma += Number(numero[i]) * (tamanho + 1 - i);
        const resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    };
    return digito(9) === Number(numero[9]) && digito(10) === Number(numero[10]);
}

async function salvarPaciente(evento, id) {
    evento.preventDefault();
    const original = id ? pacientes.find(x => x.id === id) : {};
    const consentiu = document.getElementById('pConsentimento').checked;
    const cpfDigitado = document.getElementById('pCpf').value.trim();
    const cnsDigitado = document.getElementById('pCns').value.trim();
    if (/[^0-9.\-\s]/.test(cpfDigitado) || /[^0-9\s]/.test(cnsDigitado)) {
        avisar('CPF e Cartão SUS devem conter apenas números (a formatação do CPF é opcional).', 'erro');
        return;
    }
    const cpf = cpfDigitado.replace(/[.\-\s]/g, '');
    const cns = cnsDigitado.replace(/\s/g, '');
    if (cpf && !cpfValido(cpf)) {
        avisar('Confira o CPF: os dígitos verificadores não são válidos.', 'erro');
        return;
    }
    if (cns && !/^\d{15}$/.test(cns)) {
        avisar('O Cartão SUS (CNS) deve conter 15 números.', 'erro');
        return;
    }

    const paciente = {
        nome: document.getElementById('pNome').value,
        cpf: cpf || null,
        cns: cns || null,
        dataNascimento: document.getElementById('pNascimento').value,
        telefone: document.getElementById('pTelefone').value,
        // guarda a data do consentimento (mantém a original se já existia)
        consentimentoEm: consentiu ? (original.consentimentoEm || new Date().toISOString()) : null,
        ativo: document.getElementById('pAtivo').checked
    };
    const campoMicroarea = document.getElementById('pMicroarea');
    if (campoMicroarea) {
        paciente.microarea = { id: Number(campoMicroarea.value) };
    }

    try {
        if (id) {
            await api('/api/pacientes/' + id, 'PUT', paciente);
        } else {
            await api('/api/pacientes', 'POST', paciente);
        }
        fecharModal();
        avisar('Paciente salvo');
        carregarPacientes();
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}

async function inativarPaciente(id) {
    if (!confirm('Inativar este paciente? O histórico dele continua guardado.')) return;
    try {
        await api('/api/pacientes/' + id, 'DELETE');
        avisar('Paciente inativado');
        carregarPacientes();
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}

async function verHistorico(pacienteId) {
    try {
        await mostrarHistoricoPaciente(pacienteId);
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}
