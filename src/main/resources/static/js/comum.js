
function sessao() {
    return JSON.parse(localStorage.getItem('sessao'));
}

function urlComPreview(destino) {
    if (new URLSearchParams(location.search).get('visualizacaoPreview') !== '1') {
        return destino;
    }
    const url = new URL(destino, location.href);
    url.searchParams.set('visualizacaoPreview', '1');
    return url.pathname + url.search + url.hash;
}

async function api(url, metodo = 'GET', corpo = null) {
    const opcoes = { method: metodo, headers: { 'Content-Type': 'application/json' } };
    if (sessao()) {
        opcoes.headers['Authorization'] = sessao().token;
    }
    if (corpo !== null) {
        opcoes.body = JSON.stringify(corpo);
    }

    const resposta = await fetch(url, opcoes);
    const texto = await resposta.text();
    const dados = texto ? JSON.parse(texto) : null;

    if (!resposta.ok) {
        // sessão expirada (servidor reiniciado): volta para o login
        if (resposta.status === 403 && dados && dados.mensagem === 'Faça login para continuar') {
            localStorage.removeItem('sessao');
            location.href = urlComPreview('index.html');
        }
        throw new Error(dados && dados.mensagem ? dados.mensagem : 'Erro ' + resposta.status);
    }
    return dados;
}

function exigirPerfil(perfil) {
    const s = sessao();
    if (!s || s.usuario.perfil !== perfil) {
        location.href = urlComPreview('index.html');
        return null;
    }
    document.getElementById('usuarioNome').textContent = s.usuario.nome;
    return s;
}

async function sair() {
    try { await api('/api/sair', 'POST'); } catch (e) { /* já estava sem sessão */ }
    localStorage.removeItem('sessao');
    location.href = urlComPreview('index.html');
}

function mostrarAba(nome) {
    document.querySelectorAll('section[data-aba]').forEach(s => s.hidden = s.dataset.aba !== nome);
    document.querySelectorAll('nav .aba').forEach(b => b.setAttribute('aria-current', b.dataset.aba === nome ? 'page' : 'false'));
}

function avisar(mensagem, tipo = 'ok') {
    const aviso = document.createElement('div');
    aviso.className = 'aviso ' + (tipo === 'ok' ? '' : tipo);
    aviso.textContent = mensagem;
    document.getElementById('avisos').appendChild(aviso);
    setTimeout(() => aviso.remove(), 4500);
}

function abrirModal(titulo, html) {
    document.getElementById('modalTitulo').textContent = titulo;
    document.getElementById('modalCorpo').innerHTML = html;
    document.getElementById('modal').showModal();
}

function fecharModal() {
    document.getElementById('modal').close();
}

// Evita que um texto digitado vire HTML na tela (proteção contra XSS)
function esc(texto) {
    const div = document.createElement('div');
    div.textContent = texto ?? '';
    return div.innerHTML;
}

function dataHora(iso) {
    return iso ? new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' }) : '-';
}

function moeda(valor) {
    return Number(valor || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

const NOMES_STATUS = {
    SINCRONIZADO: 'Recebido na UBS', AGUARDANDO_ENCAMINHAMENTO: 'Aguardando médico',
    ENCAMINHADO: 'Encaminhado', LAUDO_EMITIDO: 'Laudo emitido'
};

function selo(status) {
    return `<span class="selo ${status}">${NOMES_STATUS[status] || status}</span>`;
}

function seloPrazo(prazo) {
    if (!prazo || prazo.situacao === 'SEM_PRAZO') return '';
    const horas = Math.floor(Math.abs(prazo.minutosRestantes) / 60);
    const minutos = Math.abs(prazo.minutosRestantes) % 60;
    if (prazo.situacao === 'ATRASADO') return `<span class="selo ATRASADO">Prazo vencido</span>`;
    if (prazo.situacao === 'ALERTA') return `<span class="selo ALERTA">⚠ Restam ${horas}h${minutos}</span>`;
    return `<span class="selo">Restam ${horas}h</span>`;
}

function cartaoDossie(d) {
    return `
        <button class="caso" type="button" data-id="${d.id}">
            <span class="escore ${d.prioritario ? 'alto' : ''}">${Math.round(d.escore)}</span>
            <div>
                <h3>${esc(d.paciente.nome)} <span class="suave peq">${d.paciente.idade} anos</span></h3>
                <div>${esc(d.dadosTriagem.queixaPrincipal)}</div>
                <div class="meta">
                    ${selo(d.status)}
                    ${d.prioritario ? '<span class="selo prioritario">Prioritário</span>' : ''}
                    ${seloPrazo(d.prazo)}
                    ${d.especialidade ? `<span>${esc(d.especialidade.nome)}</span>` : ''}
                    <span>${dataHora(d.coletadoEm)}</span>
                </div>
            </div>
        </button>`;
}

function detalheDossie(d) {
    const t = d.dadosTriagem;
    return `
        <p><strong>${esc(d.paciente.nome)}</strong>, ${d.paciente.idade} anos · ${selo(d.status)}
           ${d.prioritario ? '<span class="selo prioritario">Prioritário</span>' : ''} ${seloPrazo(d.prazo)}</p>
        <p class="peq suave">Triagem feita por ${esc(d.acs.usuario.nome)} em ${dataHora(d.coletadoEm)}</p>
        <div class="cartao">
            <h3>Triagem</h3>
            <dl class="dados">
                <div><dt>Queixa</dt><dd>${esc(t.queixaPrincipal)}</dd></div>
                <div><dt>Temperatura</dt><dd>${t.temperatura} °C</dd></div>
                <div><dt>Pressão</dt><dd>${t.pressaoSistolica}x${t.pressaoDiastolica}</dd></div>
                <div><dt>Freq. cardíaca</dt><dd>${t.frequenciaCardiaca} bpm</dd></div>
                <div><dt>Saturação O₂</dt><dd>${t.saturacaoO2}%</dd></div>
                <div><dt>Dor</dt><dd>${t.dor}/10</dd></div>
                <div><dt>Comorbidades</dt><dd>${esc((t.comorbidades || []).join(', ') || 'nenhuma')}</dd></div>
            </dl>
            ${d.relato ? `<p><strong>Relato:</strong> ${esc(d.relato)}</p>` : ''}
        </div>
        <div class="cartao">
            <h3>Critérios aplicados ao escore ${Math.round(d.escore)}</h3>
            ${d.fatoresEscore.length
                ? '<ul class="fatores">' + d.fatoresEscore.map(f => `<li>${esc(f)}</li>`).join('') + '</ul>'
                : '<p class="suave">Nenhum sinal de risco.</p>'}
        </div>`;
}

function htmlLaudo(laudo, nomePaciente) {
    return `
        <div class="documento">
            <h2>LAUDO MÉDICO</h2>
            <p><strong>Paciente:</strong> ${esc(nomePaciente)}</p>
            <p><strong>CID:</strong> ${esc(laudo.cid.codigo)} - ${esc(laudo.cid.descricao)}</p>
            <p><strong>Parecer:</strong> ${esc(laudo.parecer)}</p>
            ${laudo.receitas.map(r => `<p><strong>Receita:</strong> ${esc(r.prescricao)}</p>`).join('')}
            <p>${esc(laudo.medico.usuario.nome)} · CRM ${esc(laudo.medico.crm)}/${esc(laudo.medico.ufCrm)}
               · emitido em ${dataHora(laudo.emitidoEm)}</p>
            <p class="assinatura">${esc(laudo.assinaturaDigital)}</p>
        </div>`;
}

function alternarContraste() {
    document.body.classList.toggle('alto-contraste');
    localStorage.setItem('contraste', document.body.classList.contains('alto-contraste'));
}
if (localStorage.getItem('contraste') === 'true') {
    document.body.classList.add('alto-contraste');
}

function adicionarVisualizador() {
    if (new URLSearchParams(location.search).get('visualizacaoPreview') === '1') return;

    const abrir = document.createElement('button');
    abrir.className = 'btn preview-trigger';
    abrir.type = 'button';
    abrir.textContent = 'Visualizar dispositivos';
    abrir.setAttribute('aria-haspopup', 'dialog');

    const dialogo = document.createElement('dialog');
    dialogo.className = 'preview-dialog';
    dialogo.setAttribute('aria-labelledby', 'previewTitulo');
    dialogo.innerHTML = `
        <div class="preview-cabecalho">
            <div>
                <h2 id="previewTitulo">Visualização responsiva</h2>
                <p class="suave peq">Confira o site em diferentes larguras de tela.</p>
            </div>
            <div class="preview-controles">
                <label for="previewDispositivo">Dispositivo</label>
                <select id="previewDispositivo">
                    <option value="390">Celular — 390 px</option>
                    <option value="768">Tablet — 768 px</option>
                    <option value="1280">Computador — 1280 px</option>
                </select>
                <button class="btn btn-sec btn-peq" type="button" id="fecharPreview">Fechar</button>
            </div>
        </div>
        <div class="preview-canvas">
            <iframe title="Pré-visualização do RuralMed no dispositivo selecionado"></iframe>
        </div>`;
    document.body.append(abrir, dialogo);

    const seletor = dialogo.querySelector('#previewDispositivo');
    const frame = dialogo.querySelector('iframe');
    const url = new URL(location.href);
    url.searchParams.set('visualizacaoPreview', '1');

    function atualizarTamanho() {
        frame.style.width = seletor.value + 'px';
        frame.style.height = Math.max(360, Math.min(window.innerHeight * 0.72, 900)) + 'px';
    }

    abrir.addEventListener('click', () => {
        atualizarTamanho();
        if (!frame.dataset.carregado) {
            frame.src = url.href;
            frame.dataset.carregado = 'true';
        }
        dialogo.showModal();
    });
    seletor.addEventListener('change', atualizarTamanho);
    dialogo.querySelector('#fecharPreview').addEventListener('click', () => dialogo.close());
    window.addEventListener('resize', atualizarTamanho);
}

document.addEventListener('DOMContentLoaded', adicionarVisualizador);
