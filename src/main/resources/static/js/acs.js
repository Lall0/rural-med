
exigirPerfil('ACS');

// As triagens ficam guardadas no navegador (localStorage) até serem enviadas.

function filaOffline() {
    return JSON.parse(localStorage.getItem('filaTriagens') || '[]');
}

function guardarFila(fila) {
    localStorage.setItem('filaTriagens', JSON.stringify(fila));
}

function novoId() {
    if (crypto.randomUUID) return crypto.randomUUID();
    return '10000000-1000-4000-8000-100000000000'.replace(/[018]/g,
        c => (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16));
}

function salvarTriagem(evento) {
    evento.preventDefault();
    const select = document.getElementById('paciente');
    const comorbidades = [...document.querySelectorAll('input[name=comorbidade]:checked')].map(c => c.value);

    const triagem = {
        idLocal: novoId(),                       // identificador único gerado no celular (RNF02)
        pacienteId: Number(select.value),
        pacienteNome: select.options[select.selectedIndex].text,
        coletadoEm: new Date().toISOString(),
        relato: document.getElementById('relato').value,
        dados: {
            queixaPrincipal: document.getElementById('queixa').value,
            temperatura: Number(document.getElementById('temperatura').value),
            pressaoSistolica: Number(document.getElementById('pas').value),
            pressaoDiastolica: Number(document.getElementById('pad').value),
            frequenciaCardiaca: Number(document.getElementById('fc').value),
            saturacaoO2: Number(document.getElementById('sat').value),
            dor: Number(document.getElementById('dor').value),
            comorbidades: comorbidades
        }
    };

    const fila = filaOffline();
    fila.push(triagem);
    guardarFila(fila);

    document.getElementById('formTriagem').reset();
    avisar('Triagem salva no celular');
    sincronizar();
}

// RN01: só envia se houver conexão de verdade com o servidor
async function temConexao() {
    if (!navigator.onLine) return false;
    try {
        await api('/api/health');
        return true;
    } catch (erro) {
        return false;
    }
}

let enviando = false;

async function sincronizar() {
    const fila = filaOffline();
    if (fila.length === 0 || enviando) {
        atualizarFaixa();
        return;
    }
    enviando = true;
    try {
        if (!(await temConexao())) return;

        const resultados = await api('/api/triagens/sincronizar', 'POST', fila);

        // tira da fila o que o servidor recebeu (CRIADO ou JA_EXISTIA) ou recusou (REJEITADO)
        const resolvidas = resultados.map(r => r.idLocal);
        guardarFila(filaOffline().filter(t => !resolvidas.includes(t.idLocal)));

        const enviadas = resultados.filter(r => r.situacao !== 'REJEITADO').length;
        if (enviadas > 0) avisar(enviadas + ' triagem(ns) enviada(s) para a UBS');
        resultados.filter(r => r.situacao === 'REJEITADO').forEach(r => avisar('Recusada: ' + r.mensagem, 'erro'));
        carregarTriagens();
    } catch (erro) {
        avisar('Não foi possível enviar agora. A triagem continua salva.', 'alerta');
    } finally {
        enviando = false;
        atualizarFaixa();
    }
}

function atualizarFaixa() {
    const pendentes = filaOffline().length;
    document.getElementById('conexao').textContent = navigator.onLine ? 'On-line' : 'Off-line';
    document.getElementById('conexao').classList.toggle('off', !navigator.onLine);
    document.getElementById('faixa').innerHTML = pendentes === 0
        ? '<div class="faixa ok">Todas as triagens foram enviadas.</div>'
        : `<div class="faixa"><span><strong>${pendentes}</strong> triagem(ns) aguardando internet</span>
           <button class="btn btn-peq" type="button" onclick="sincronizar()">Enviar agora</button></div>`;
}

window.addEventListener('online', sincronizar);
window.addEventListener('offline', atualizarFaixa);

async function preencherSelectPacientes() {
    try {
        await carregarPacientes();
        localStorage.setItem('pacientesOffline', JSON.stringify(pacientes)); // cópia para usar sem internet
    } catch (erro) {
        pacientes = JSON.parse(localStorage.getItem('pacientesOffline') || '[]');
    }
    document.getElementById('paciente').innerHTML = '<option value="">Selecione</option>' +
        pacientes.filter(p => p.ativo).map(p =>
            `<option value="${p.id}" ${p.consentimentoEm ? '' : 'disabled'}>${esc(p.nome)}${p.consentimentoEm ? '' : ' (sem consentimento)'}</option>`
        ).join('');
}

let triagens = [];

async function carregarTriagens() {
    try {
        triagens = await api('/api/dossies');
        document.getElementById('listaTriagens').innerHTML =
            triagens.map(cartaoDossie).join('') || '<p class="vazio">Nenhuma triagem enviada.</p>';
    } catch (erro) { /* sem internet: mantém a lista anterior */ }
}

document.getElementById('listaTriagens').addEventListener('click', evento => {
    const cartao = evento.target.closest('.caso');
    if (!cartao) return;
    const d = triagens.find(x => x.id === Number(cartao.dataset.id));
    abrirModal('Triagem', detalheDossie(d) + (d.laudo
        ? `<div class="cartao">${htmlLaudo(d.laudo, d.paciente.nome)}
           <button class="btn btn-sec" type="button" onclick="ouvirReceita(${d.id})">🔊 Ouvir receita</button></div>`
        : ''));
});

function ditar() {
    const Reconhecimento = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!Reconhecimento) { avisar('Ditado não suportado neste navegador', 'alerta'); return; }
    const rec = new Reconhecimento();
    rec.lang = 'pt-BR';
    rec.onresult = e => { document.getElementById('relato').value += ' ' + e.results[0][0].transcript; };
    rec.start();
    avisar('Pode falar...');
}

function ouvirReceita(dossieId) {
    const d = triagens.find(x => x.id === dossieId);
    const texto = d.laudo.receitas.map(r => 'Receita: ' + r.prescricao).join('. ') || 'Sem receita.';
    const fala = new SpeechSynthesisUtterance(texto);
    fala.lang = 'pt-BR';
    speechSynthesis.speak(fala);
}

mostrarAba('triagem');
preencherSelectPacientes();
carregarTriagens();
sincronizar();
