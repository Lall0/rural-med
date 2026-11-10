
let pacientes = [];
let microareas = [];

async function carregarPacientes() {
    pacientes = await api('/api/pacientes');
    desenharPacientes();
}

function desenharPacientes() {
    const linhas = pacientes.map(p => `
        <tr>
            <td data-rotulo="Nome">${esc(p.nome)}</td>
            <td data-rotulo="Idade">${p.idade} anos</td>
            <td data-rotulo="Microárea">${esc(p.microarea.nome)}</td>
            <td data-rotulo="Consentimento">${p.consentimentoEm ? 'Sim' : '<span class="selo ALERTA">Pendente</span>'}</td>
            <td data-rotulo="Situação">${p.ativo ? 'Ativo' : '<span class="selo inativo">Inativo</span>'}</td>
            <td class="acoes">
                <button class="btn btn-sec btn-peq" type="button" onclick="verHistorico(${p.id})">Histórico</button>
                <button class="btn btn-sec btn-peq" type="button" onclick="formPaciente(${p.id})">Editar</button>
                ${p.ativo ? `<button class="btn btn-sec btn-peq" type="button" onclick="inativarPaciente(${p.id})">Inativar</button>` : ''}
            </td>
        </tr>`).join('');
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
            ${ehUbs ? `<div class="campo"><label for="pMicroarea">Microárea *</label>
                <select id="pMicroarea" required>
                    ${microareas.map(m => `<option value="${m.id}" ${p.microarea && p.microarea.id === m.id ? 'selected' : ''}>${esc(m.nome)}</option>`).join('')}
                </select></div>` : ''}
            <div class="campo"><label for="pNome">Nome *</label>
                <input id="pNome" value="${esc(p.nome)}" required></div>
            <div class="grade duas">
                <div class="campo"><label for="pCpf">CPF (só números)</label>
                    <input id="pCpf" value="${esc(p.cpf)}" pattern="\\d{11}" maxlength="11"></div>
                <div class="campo"><label for="pCns">Cartão do SUS (CNS)</label>
                    <input id="pCns" value="${esc(p.cns)}" pattern="\\d{15}" maxlength="15"></div>
                <div class="campo"><label for="pNascimento">Nascimento *</label>
                    <input id="pNascimento" type="date" value="${p.dataNascimento || ''}" required></div>
                <div class="campo"><label for="pTelefone">Telefone</label>
                    <input id="pTelefone" value="${esc(p.telefone)}"></div>
            </div>
            <div class="campo"><label><input id="pConsentimento" type="checkbox" ${p.consentimentoEm ? 'checked' : ''}>
                Paciente consentiu com o atendimento por telessaúde (Lei 14.510/2022)</label></div>
            <div class="campo"><label><input id="pAtivo" type="checkbox" ${p.ativo ? 'checked' : ''}> Ativo</label></div>
            <button class="btn" type="submit">Salvar</button>
        </form>`);
}

async function salvarPaciente(evento, id) {
    evento.preventDefault();
    const original = id ? pacientes.find(x => x.id === id) : {};
    const consentiu = document.getElementById('pConsentimento').checked;

    const paciente = {
        nome: document.getElementById('pNome').value,
        cpf: document.getElementById('pCpf').value,
        cns: document.getElementById('pCns').value,
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
        const dossies = await api(`/api/pacientes/${pacienteId}/historico`);
        abrirModal('Histórico do paciente', dossies.length === 0 ? '<p class="suave">Nenhum atendimento ainda.</p>' :
            '<div class="linha-tempo">' + dossies.map(d => `
                <div class="evento">
                    <div class="peq suave">${dataHora(d.coletadoEm)}</div>
                    <div>${selo(d.status)} <strong>${esc(d.dadosTriagem.queixaPrincipal)}</strong> · escore ${Math.round(d.escore)}</div>
                    ${d.laudo ? `<p class="peq">Laudo ${esc(d.laudo.cid.codigo)}: ${esc(d.laudo.parecer)}</p>` : ''}
                </div>`).join('') + '</div>');
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}
