
exigirPerfil('UBS');
document.getElementById('conexao').textContent = 'On-line';

let dossies = [];
let especialidades = [];

async function carregarDossies() {
    const status = document.getElementById('filtroStatus').value;
    dossies = await api('/api/dossies' + (status ? '?status=' + status : ''));
    document.getElementById('listaDossies').innerHTML =
        dossies.map(cartaoDossie).join('') || '<p class="vazio">Nenhum dossiê nesta situação.</p>';
}

document.getElementById('listaDossies').addEventListener('click', evento => {
    const cartao = evento.target.closest('.caso');
    if (cartao) abrirDossie(Number(cartao.dataset.id));
});

function abrirDossie(id) {
    const d = dossies.find(x => x.id === id);
    const podeEncaminhar = d.status !== 'LAUDO_EMITIDO';
    abrirModal('Dossiê', detalheDossie(d) + (podeEncaminhar ? `
        <form class="cartao" onsubmit="encaminhar(event, ${d.id})">
            <h3>Encaminhar para especialidade</h3>
            <div class="campo">
                <select id="especialidade" required onchange="mostrarMedicos()">
                    <option value="">Selecione</option>
                    ${especialidades.map(e => `<option value="${e.id}">${esc(e.nome)}</option>`).join('')}
                </select>
                <div class="ajuda" id="medicosDisponiveis"></div>
            </div>
            <button class="btn" type="submit">Encaminhar</button>
        </form>` : ''));
}

async function mostrarMedicos() {
    const id = document.getElementById('especialidade').value;
    if (!id) return;
    const medicos = await api('/api/medicos?especialidadeId=' + id);
    document.getElementById('medicosDisponiveis').textContent = medicos.length
        ? 'Médicos disponíveis: ' + medicos.map(m => m.usuario.nome).join(', ')
        : 'Nenhum médico disponível: o caso ficará aguardando.';
}

async function encaminhar(evento, id) {
    evento.preventDefault();
    try {
        const d = await api(`/api/dossies/${id}/encaminhar`, 'POST',
            { especialidadeId: Number(document.getElementById('especialidade').value) });
        fecharModal();
        avisar(d.status === 'ENCAMINHADO' ? 'Encaminhado para ' + d.especialidade.nome : 'Sem médico disponível: caso aguardando',
               d.status === 'ENCAMINHADO' ? 'ok' : 'alerta');
        carregarDossies();
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}

let usuarios = [];

async function carregarUsuarios() {
    usuarios = await api('/api/usuarios');
    document.getElementById('tabelaUsuarios').innerHTML = usuarios.map(u => `
        <tr>
            <td data-rotulo="Nome">${esc(u.nome)}</td>
            <td data-rotulo="E-mail">${esc(u.email)}</td>
            <td data-rotulo="Perfil">${u.perfil}</td>
            <td data-rotulo="Situação">${u.ativo ? 'Ativo' : '<span class="selo inativo">Inativo</span>'}</td>
            <td class="acoes">${u.ativo
                ? `<button class="btn btn-sec btn-peq" type="button" onclick="inativarUsuario(${u.id})">Inativar</button>` : ''}</td>
        </tr>`).join('');
}

async function formUsuario() {
    const [listaMicroareas, listaClinicas] = await Promise.all([api('/api/microareas'), api('/api/clinicas')]);
    abrirModal('Novo usuário', `
        <form onsubmit="salvarUsuario(event)">
            <div class="campo"><label for="uNome">Nome *</label><input id="uNome" required></div>
            <div class="grade duas">
                <div class="campo"><label for="uEmail">E-mail *</label><input id="uEmail" type="email" required></div>
                <div class="campo"><label for="uCpf">CPF (só números) *</label><input id="uCpf" pattern="\\d{11}" maxlength="11" required></div>
                <div class="campo"><label for="uSenha">Senha *</label><input id="uSenha" type="password" minlength="6" required></div>
                <div class="campo"><label for="uPerfil">Perfil *</label>
                    <select id="uPerfil" onchange="mostrarCamposDoPerfil()">
                        <option value="ACS">ACS</option><option value="UBS">Equipe da UBS</option>
                        <option value="MEDICO">Médico</option><option value="GESTOR">Gestor</option>
                    </select></div>
            </div>
            <div id="camposAcs" class="campo"><label for="uMicroarea">Microárea do ACS</label>
                <select id="uMicroarea">${listaMicroareas.map(m => `<option value="${m.id}">${esc(m.nome)}</option>`).join('')}</select></div>
            <div id="camposMedico" hidden>
                <div class="grade duas">
                    <div class="campo"><label for="uEspecialidade">Especialidade</label>
                        <select id="uEspecialidade">${especialidades.map(e => `<option value="${e.id}">${esc(e.nome)}</option>`).join('')}</select></div>
                    <div class="campo"><label for="uClinica">Clínica parceira</label>
                        <select id="uClinica"><option value="">(nenhuma)</option>${listaClinicas.map(c => `<option value="${c.id}">${esc(c.nome)}</option>`).join('')}</select></div>
                    <div class="campo"><label for="uCrm">CRM</label><input id="uCrm" maxlength="10"></div>
                    <div class="campo"><label for="uUf">UF do CRM</label><input id="uUf" maxlength="2" value="SP"></div>
                    <div class="campo"><label for="uValor">Valor por laudo (R$)</label><input id="uValor" type="number" step="0.01" value="100"></div>
                </div>
                <label><input id="uCrmAtivo" type="checkbox"> CRM validado e ativo (RN04)</label>
            </div>
            <button class="btn" type="submit" style="margin-top:1rem">Salvar</button>
        </form>`);
}

function mostrarCamposDoPerfil() {
    const perfil = document.getElementById('uPerfil').value;
    document.getElementById('camposAcs').hidden = perfil !== 'ACS';
    document.getElementById('camposMedico').hidden = perfil !== 'MEDICO';
}

async function salvarUsuario(evento) {
    evento.preventDefault();
    const valor = id => document.getElementById(id).value;
    try {
        await api('/api/usuarios', 'POST', {
            nome: valor('uNome'), email: valor('uEmail'), cpf: valor('uCpf'), senha: valor('uSenha'),
            perfil: valor('uPerfil'),
            microareaId: Number(valor('uMicroarea')),
            especialidadeId: Number(valor('uEspecialidade')),
            clinicaId: valor('uClinica') ? Number(valor('uClinica')) : null,
            crm: valor('uCrm'), ufCrm: valor('uUf').toUpperCase(),
            crmAtivo: document.getElementById('uCrmAtivo').checked,
            valorPorLaudo: Number(valor('uValor'))
        });
        fecharModal();
        avisar('Usuário cadastrado');
        carregarUsuarios();
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}

async function inativarUsuario(id) {
    if (!confirm('Inativar este usuário?')) return;
    await api('/api/usuarios/' + id, 'DELETE');
    carregarUsuarios();
}

async function carregarEspecialidades() {
    especialidades = await api('/api/especialidades');
    document.getElementById('tabelaEspecialidades').innerHTML = especialidades.map(e => `
        <tr>
            <td data-rotulo="Nome">${esc(e.nome)}</td>
            <td class="acoes">
                <button class="btn btn-sec btn-peq" type="button" onclick="editarEspecialidade(${e.id})">Editar</button>
                <button class="btn btn-sec btn-peq" type="button" onclick="excluirEspecialidade(${e.id})">Excluir</button>
            </td>
        </tr>`).join('');
}

async function novaEspecialidade() {
    const nome = prompt('Nome da nova especialidade:');
    if (!nome) return;
    await api('/api/especialidades', 'POST', { nome: nome });
    carregarEspecialidades();
}

async function editarEspecialidade(id) {
    const atual = especialidades.find(e => e.id === id);
    const nome = prompt('Novo nome:', atual.nome);
    if (!nome) return;
    await api('/api/especialidades/' + id, 'PUT', { nome: nome });
    carregarEspecialidades();
}

async function excluirEspecialidade(id) {
    if (!confirm('Excluir esta especialidade?')) return;
    try {
        await api('/api/especialidades/' + id, 'DELETE');
        carregarEspecialidades();
    } catch (erro) {
        avisar('Não é possível excluir: a especialidade está em uso', 'erro');
    }
}

let clinicas = [];

async function carregarClinicas() {
    clinicas = await api('/api/clinicas');
    document.getElementById('tabelaClinicas').innerHTML = clinicas.map(c => `
        <tr>
            <td data-rotulo="Nome">${esc(c.nome)}</td>
            <td data-rotulo="CNPJ">${esc(c.cnpj)}</td>
            <td data-rotulo="Município">${esc(c.municipio.nome)}</td>
            <td data-rotulo="Situação">${c.ativo ? 'Ativa' : '<span class="selo inativo">Inativa</span>'}</td>
            <td class="acoes">
                <button class="btn btn-sec btn-peq" type="button" onclick="formClinica(${c.id})">Editar</button>
                ${c.ativo ? `<button class="btn btn-sec btn-peq" type="button" onclick="inativarClinica(${c.id})">Inativar</button>` : ''}
            </td>
        </tr>`).join('');
}

async function formClinica(id) {
    const c = id ? clinicas.find(x => x.id === id) : { ativo: true };
    const municipios = await api('/api/municipios');
    abrirModal(id ? 'Editar clínica' : 'Nova clínica', `
        <form onsubmit="salvarClinica(event, ${id})">
            <div class="campo"><label for="cNome">Nome *</label><input id="cNome" value="${esc(c.nome)}" required></div>
            <div class="campo"><label for="cCnpj">CNPJ (só números) *</label>
                <input id="cCnpj" value="${esc(c.cnpj)}" pattern="\\d{14}" maxlength="14" required></div>
            <div class="campo"><label for="cMunicipio">Município *</label>
                <select id="cMunicipio">${municipios.map(m => `<option value="${m.id}" ${c.municipio && c.municipio.id === m.id ? 'selected' : ''}>${esc(m.nome)}</option>`).join('')}</select></div>
            <div class="campo"><label><input id="cAtivo" type="checkbox" ${c.ativo ? 'checked' : ''}> Ativa</label></div>
            <button class="btn" type="submit">Salvar</button>
        </form>`);
}

async function salvarClinica(evento, id) {
    evento.preventDefault();
    const clinica = {
        nome: document.getElementById('cNome').value,
        cnpj: document.getElementById('cCnpj').value,
        municipio: { id: Number(document.getElementById('cMunicipio').value) },
        ativo: document.getElementById('cAtivo').checked
    };
    try {
        await api(id ? '/api/clinicas/' + id : '/api/clinicas', id ? 'PUT' : 'POST', clinica);
        fecharModal();
        avisar('Clínica salva');
        carregarClinicas();
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}

async function inativarClinica(id) {
    if (!confirm('Inativar esta clínica?')) return;
    await api('/api/clinicas/' + id, 'DELETE');
    carregarClinicas();
}

mostrarAba('dossies');
carregarEspecialidades();
carregarDossies();
carregarPacientes();
carregarUsuarios();
carregarClinicas();
