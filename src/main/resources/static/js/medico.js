
exigirPerfil('MEDICO');

let fila = [];
let laudos = [];
let medicamentosCatalogo = [];

async function buscarPacienteMedico(evento) {
    evento.preventDefault();
    const campo = document.getElementById('identificadorPaciente');
    const resultado = document.getElementById('resultadoPaciente');
    resultado.innerHTML = '<p class="suave">Localizando paciente…</p>';
    try {
        const paciente = await api('/api/pacientes/localizar?identificador=' + encodeURIComponent(campo.value));
        resultado.innerHTML = `
            <article class="resultado-paciente">
                <div><p class="rotulo-secao">Paciente localizado</p>
                    <h2>${esc(paciente.nome)}</h2>
                    <p class="suave">${paciente.idade} anos · ID ${esc(idRuralMed(paciente))}</p>
                </div>
                <button class="btn" type="button" onclick="verHistorico(${paciente.id})">Ver histórico completo</button>
            </article>`;
    } catch (erro) {
        resultado.innerHTML = `<p class="aviso erro">${esc(erro.message)}</p>`;
    }
}

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
        <button class="btn btn-sec btn-peq" type="button" onclick="verHistorico(${d.paciente.id})">Ver histórico completo do paciente</button>
        <section class="cartao bloco-atendimento">
            <h3>4. Parecer e CID</h3>
            <form onsubmit="emitirLaudo(event, ${d.id})">
                <div class="campo"><label for="parecer">Parecer *</label><textarea id="parecer" required></textarea></div>
                <div class="campo autocompletar"><label for="buscaCid">CID (opcional na consulta)</label>
                    <input id="buscaCid" placeholder="Se conhecido, busque por código ou doença" oninput="buscarCid()" autocomplete="off">
                    <input id="cidCodigo" type="hidden">
                    <ul class="sugestoes" id="sugestoesCid" hidden></ul></div>
                <button class="btn" type="submit">Emitir laudo</button>
            </form>
        </section>
        <section class="cartao bloco-atendimento">
            <h3>5. Prescrição médica</h3>
            <p class="ajuda">Catálogo de referência para busca. Não recomenda medicamentos por sintoma nem preenche doses; a escolha e a prescrição são responsabilidade do médico.</p>
            <div id="receitas-${d.id}" class="lista-exames"></div>
            <form onsubmit="emitirPrescricao(event, ${d.id})">
                <div class="campo autocompletar">
                    <label for="buscaMedicamento">Medicamento ou princípio ativo *</label>
                    <input id="buscaMedicamento" placeholder="Digite ao menos 2 caracteres" oninput="buscarMedicamentos()" autocomplete="off">
                    <ul class="sugestoes" id="sugestoesMedicamento" hidden></ul>
                    <input id="medicamentoSelecionado" type="hidden">
                    <input id="apresentacaoSelecionada" type="hidden">
                    <p id="medicamentoEscolhido" class="ajuda" aria-live="polite"></p>
                </div>
                <div class="grade duas">
                    <div class="campo"><label for="rxDose">Dose *</label><input id="rxDose" required maxlength="180"></div>
                    <div class="campo"><label for="rxVia">Via de administração *</label>
                        <select id="rxVia" required><option value="">Selecione</option><option>Oral</option><option>Tópica</option><option>Inalatória</option><option>Oftálmica</option><option>Otológica</option><option>Outra</option></select></div>
                    <div class="campo"><label for="rxFrequencia">Frequência *</label><input id="rxFrequencia" required maxlength="180"></div>
                    <div class="campo"><label for="rxDuracao">Duração *</label><input id="rxDuracao" required maxlength="180"></div>
                </div>
                <div class="campo"><label for="rxOrientacoes">Orientações ao paciente</label><textarea id="rxOrientacoes" maxlength="2000"></textarea></div>
                <label class="opcao-farmacia"><input id="rxFarmaciaPopular" type="checkbox">
                    Orientação sobre Farmácia Popular (disponibilidade não verificada)</label>
                <button class="btn" type="submit">Assinar prescrição</button>
            </form>
        </section>`);
    carregarExames(d.id);
    carregarReceitas(d.id);
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
    try {
        const laudo = await api(`/api/dossies/${dossieId}/laudo`, 'POST', {
            parecer: document.getElementById('parecer').value,
            cidCodigo: document.getElementById('cidCodigo').value || null,
            prescricao: null,
            farmaciaPopular: false
        });
        avisar('Laudo emitido e assinado');
        mostrarLaudo(laudo);
        carregarFila();
        carregarLaudos();
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}

async function buscarMedicamentos() {
    const busca = document.getElementById('buscaMedicamento');
    const lista = document.getElementById('sugestoesMedicamento');
    const termo = busca.value.trim();
    document.getElementById('medicamentoSelecionado').value = '';
    document.getElementById('apresentacaoSelecionada').value = '';
    document.getElementById('medicamentoEscolhido').textContent = '';
    if (termo.length < 2) {
        lista.hidden = true;
        return;
    }
    try {
        medicamentosCatalogo = await api('/api/medicamentos?busca=' + encodeURIComponent(termo));
        lista.innerHTML = medicamentosCatalogo.map((m, indice) =>
            `<li><button type="button" class="opcao-medicamento" onclick="selecionarMedicamento(${indice})">
                <strong>${esc(m.nome)}</strong> · ${esc(m.principioAtivo)}<br>
                <span class="peq suave">${esc(m.apresentacao)}</span>
            </button></li>`).join('') || '<li>Nenhum resultado no catálogo.</li>';
        lista.hidden = false;
    } catch (erro) {
        lista.hidden = true;
        avisar(erro.message, 'erro');
    }
}

function selecionarMedicamento(indice) {
    const medicamento = medicamentosCatalogo[indice];
    if (!medicamento) return;
    document.getElementById('medicamentoSelecionado').value = medicamento.nome;
    document.getElementById('apresentacaoSelecionada').value = medicamento.apresentacao;
    document.getElementById('buscaMedicamento').value = medicamento.nome;
    document.getElementById('medicamentoEscolhido').textContent =
        medicamento.principioAtivo + ' · ' + medicamento.apresentacao;
    document.getElementById('sugestoesMedicamento').hidden = true;
}

async function emitirPrescricao(evento, dossieId) {
    evento.preventDefault();
    if (!document.getElementById('medicamentoSelecionado').value) {
        avisar('Escolha um medicamento da lista de referência.', 'erro');
        return;
    }
    try {
        await api(`/api/dossies/${dossieId}/receitas`, 'POST', {
            medicamento: document.getElementById('medicamentoSelecionado').value,
            apresentacao: document.getElementById('apresentacaoSelecionada').value,
            dose: document.getElementById('rxDose').value,
            viaAdministracao: document.getElementById('rxVia').value,
            frequencia: document.getElementById('rxFrequencia').value,
            duracao: document.getElementById('rxDuracao').value,
            orientacoes: document.getElementById('rxOrientacoes').value,
            farmaciaPopular: document.getElementById('rxFarmaciaPopular').checked
        });
        evento.currentTarget.reset();
        document.getElementById('medicamentoSelecionado').value = '';
        document.getElementById('apresentacaoSelecionada').value = '';
        document.getElementById('medicamentoEscolhido').textContent = '';
        avisar('Prescrição assinada e salva no atendimento');
        carregarReceitas(dossieId);
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}

async function carregarReceitas(dossieId) {
    const alvo = document.getElementById('receitas-' + dossieId);
    if (!alvo) return;
    try {
        const receitas = await api(`/api/dossies/${dossieId}/receitas`);
        alvo.innerHTML = receitas.map(r => `<article class="receita-item">
            <h4>${r.medicamento ? `${esc(r.medicamento)} · ${esc(r.apresentacao)}` : 'Prescrição anterior'}</h4>
            ${r.dose ? `<p><strong>Dose:</strong> ${esc(r.dose)} · <strong>Via:</strong> ${esc(r.viaAdministracao)}</p>
                <p><strong>Frequência:</strong> ${esc(r.frequencia)} · <strong>Duração:</strong> ${esc(r.duracao)}</p>`
                : `<p>${esc(r.descricao)}</p>`}
            ${r.orientacoes ? `<p><strong>Orientações:</strong> ${esc(r.orientacoes)}</p>` : ''}
            ${r.farmaciaPopular ? '<span class="selo farmacia">Farmácia Popular: disponibilidade não verificada</span>' : ''}
            <p class="peq suave">Assinada em ${dataHora(r.assinadaEm)}</p>
        </article>`).join('') || '<p class="suave peq">Nenhuma prescrição emitida para este atendimento.</p>';
    } catch (erro) {
        alvo.innerHTML = `<p class="aviso erro">${esc(erro.message)}</p>`;
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
            <td data-rotulo="CID">${l.cid ? esc(l.cid.codigo) : 'Não informado'}</td>
            <td class="acoes"><button class="btn btn-sec btn-peq" type="button"
                onclick="mostrarLaudo(laudos.find(x => x.id === ${l.id}))">Abrir</button></td>
        </tr>`).join('') || '<tr><td>Nenhum laudo emitido.</td></tr>';
}

async function verHistorico(pacienteId) {
    try {
        await mostrarHistoricoPaciente(pacienteId);
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}

mostrarAba('fila');
carregarFila();
carregarLaudos();
