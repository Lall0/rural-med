
exigirPerfil('MEDICO');
document.getElementById('conexao').textContent = 'On-line';

let fila = [];
let laudos = [];

async function carregarFila() {
    fila = await api('/api/fila');
    document.getElementById('listaFila').innerHTML =
        fila.map(cartaoDossie).join('') || '<p class="vazio">Nenhum caso esperando laudo.</p>';
}

document.getElementById('listaFila').addEventListener('click', evento => {
    const cartao = evento.target.closest('.caso');
    if (cartao) abrirCaso(Number(cartao.dataset.id));
});

function abrirCaso(id) {
    const d = fila.find(x => x.id === id);
    abrirModal('Atendimento', detalheDossie(d) + `
        <button class="btn btn-sec btn-peq" type="button" onclick="verHistorico(${d.paciente.id})">Ver histórico do paciente</button>
        <form class="cartao" onsubmit="emitirLaudo(event, ${d.id})" style="margin-top:1rem">
            <h3>Laudo e receita</h3>
            <div class="campo"><label for="parecer">Parecer *</label><textarea id="parecer" required></textarea></div>
            <div class="campo autocompletar"><label for="buscaCid">CID *</label>
                <input id="buscaCid" placeholder="Digite o código ou a doença" oninput="buscarCid()" autocomplete="off">
                <input id="cidCodigo" type="hidden">
                <ul class="sugestoes" id="sugestoesCid" hidden></ul></div>
            <div class="campo"><label for="prescricao">Prescrição (opcional)</label><textarea id="prescricao"></textarea></div>
            <button class="btn" type="submit">Emitir e assinar</button>
        </form>`);
}

async function buscarCid() {
    const texto = document.getElementById('buscaCid').value;
    document.getElementById('cidCodigo').value = '';
    const lista = document.getElementById('sugestoesCid');
    if (texto.length < 2) { lista.hidden = true; return; }

    const cids = await api('/api/cids?busca=' + encodeURIComponent(texto));
    lista.innerHTML = cids.map(c =>
        `<li onclick="escolherCid('${c.codigo}', '${esc(c.descricao).replace(/'/g, '')}')"><strong>${c.codigo}</strong> ${esc(c.descricao)}</li>`
    ).join('') || '<li>Nenhum código encontrado</li>';
    lista.hidden = false;
}

function escolherCid(codigo, descricao) {
    document.getElementById('cidCodigo').value = codigo;
    document.getElementById('buscaCid').value = codigo + ' - ' + descricao;
    document.getElementById('sugestoesCid').hidden = true;
}

async function emitirLaudo(evento, dossieId) {
    evento.preventDefault();
    if (!document.getElementById('cidCodigo').value) {
        avisar('Escolha um CID da lista', 'erro');
        return;
    }
    try {
        const laudo = await api(`/api/dossies/${dossieId}/laudo`, 'POST', {
            parecer: document.getElementById('parecer').value,
            cidCodigo: document.getElementById('cidCodigo').value,
            prescricao: document.getElementById('prescricao').value
        });
        avisar('Laudo emitido e assinado');
        mostrarLaudo(laudo);
        carregarFila();
        carregarLaudos();
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}

function mostrarLaudo(laudo) {
    abrirModal('Laudo', htmlLaudo(laudo, laudo.dossie.paciente.nome) +
        '<button class="btn" type="button" onclick="window.print()">Imprimir / salvar PDF</button>');
}

async function carregarLaudos() {
    laudos = await api('/api/laudos');
    document.getElementById('tabelaLaudos').innerHTML = laudos.map(l => `
        <tr>
            <td data-rotulo="Data">${dataHora(l.emitidoEm)}</td>
            <td data-rotulo="Paciente">${esc(l.dossie.paciente.nome)}</td>
            <td data-rotulo="CID">${esc(l.cid.codigo)}</td>
            <td class="acoes"><button class="btn btn-sec btn-peq" type="button"
                onclick="mostrarLaudo(laudos.find(x => x.id === ${l.id}))">Abrir</button></td>
        </tr>`).join('') || '<tr><td>Nenhum laudo emitido.</td></tr>';
}

async function verHistorico(pacienteId) {
    const dossies = await api(`/api/pacientes/${pacienteId}/historico`);
    abrirModal('Histórico do paciente', '<div class="linha-tempo">' + dossies.map(d => `
        <div class="evento">
            <div class="peq suave">${dataHora(d.coletadoEm)}</div>
            <div>${selo(d.status)} <strong>${esc(d.dadosTriagem.queixaPrincipal)}</strong></div>
            ${d.laudo ? `<p class="peq">Laudo ${esc(d.laudo.cid.codigo)}: ${esc(d.laudo.parecer)}</p>` : ''}
        </div>`).join('') + '</div>');
}

mostrarAba('fila');
carregarFila();
carregarLaudos();
