
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

    async function apiArquivo(url, arquivo) {
        const dados = new FormData();
        dados.append('arquivo', arquivo);
        const opcoes = { method: 'POST', headers: {}, body: dados };
        if (sessao()) opcoes.headers.Authorization = sessao().token;
        const resposta = await fetch(url, opcoes);
        const texto = await resposta.text();
        const resultado = texto ? JSON.parse(texto) : null;
        if (!resposta.ok) throw new Error(resultado?.mensagem || 'Erro ' + resposta.status);
        return resultado;
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

function abrirMeuPerfil() {
    const usuario = sessao().usuario;
    const perfis = {
        ACS: 'Agente Comunitário de Saúde',
        UBS: 'Equipe da UBS',
        MEDICO: 'Médico',
        GESTOR: 'Gestor'
    };
    abrirModal('Meu perfil', `
        <div class="perfil-resumo">
            <div class="perfil-avatar" aria-hidden="true">${esc(usuario.nome.trim().charAt(0).toUpperCase())}</div>
            <div><h3>${esc(usuario.nome)}</h3><p class="suave">${esc(perfis[usuario.perfil] || usuario.perfil)}</p></div>
        </div>
        <dl class="perfil-dados">
            <div><dt>E-mail</dt><dd>${esc(usuario.email || 'Não informado')}</dd></div>
            <div><dt>Perfil de acesso</dt><dd>${esc(perfis[usuario.perfil] || usuario.perfil)}</dd></div>
        </dl>
    `);
    document.querySelector('.menu-perfil').open = false;
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

function idRuralMed(paciente) {
    return 'RM-' + (paciente.identificadorUnico || '').replace(/-/g, '').toUpperCase();
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
    const risco = classeRisco(d);
    return `
        <button class="caso" type="button" data-id="${d.id}">
            <span class="escore ${risco.classe}">${Math.round(d.escore)}</span>
            <div>
                <h3>${esc(d.paciente.nome)} <span class="suave peq">${d.paciente.idade} anos</span></h3>
                <div>${esc(d.dadosTriagem.queixaPrincipal)}</div>
                <div class="meta">
                    <span class="selo risco-${risco.classe}">${risco.rotulo}</span>
                    ${selo(d.status)}
                    ${seloPrazo(d.prazo)}
                    ${d.especialidade ? `<span>${esc(d.especialidade.nome)}</span>` : ''}
                    <span>${dataHora(d.coletadoEm)}</span>
                </div>
            </div>
        </button>`;
}

function classeRisco(d) {
    if (d.prioritario) return { classe: 'alto', rotulo: 'Urgente' };
    if (Number(d.escore) >= 25) return { classe: 'medio', rotulo: 'Atenção' };
    return { classe: 'baixo', rotulo: 'Baixo risco' };
}

function detalheDossie(d) {
    const t = d.dadosTriagem;
    const risco = classeRisco(d);
    const fatores = d.fatoresEscore || [];
    return `
        <div class="resumo-atendimento">
            <div><p class="rotulo-secao">Paciente</p><h3>${esc(d.paciente.nome)}</h3>
                <p class="suave">${d.paciente.idade} anos · ${selo(d.status)} ${seloPrazo(d.prazo)}</p>
            </div>
            <div class="resumo-escore ${risco.classe}"><span>Escore de risco</span><strong>${Math.round(d.escore)}</strong><b>${risco.rotulo}</b></div>
        </div>
        <p class="peq suave">Triagem feita por ${esc(d.acs.usuario.nome)} em ${dataHora(d.coletadoEm)}</p>
        <section class="cartao bloco-atendimento">
            <h3>1. Dados da triagem</h3>
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
        </section>
        <section class="cartao bloco-atendimento">
            <h3>2. Critérios do escore</h3>
            ${fatores.length
                ? '<ul class="fatores">' + fatores.map(f => `<li>${esc(f)}</li>`).join('') + '</ul>'
                : '<p class="suave">Nenhum sinal de risco.</p>'}
            <p class="ajuda">O escore apoia a priorização e não substitui avaliação clínica.</p>
        </section>
        <section class="cartao bloco-atendimento">
            <h3>3. Exames e documentos</h3>
            <p class="ajuda">PDF, JPG ou PNG · até 10 MB por arquivo</p>
            <form class="linha-form-arquivo" onsubmit="enviarExame(event, ${d.id})">
                <input type="file" name="arquivo" accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png" required>
                <button class="btn btn-sec btn-peq" type="submit">Anexar exame</button>
            </form>
            <div id="exames-${d.id}" class="lista-exames"><span class="suave peq">Carregando exames…</span></div>
        </section>`;
}

function htmlLaudo(laudo, nomePaciente) {
    return `
        <div class="documento">
            <h2>LAUDO MÉDICO</h2>
            <p><strong>Paciente:</strong> ${esc(nomePaciente)}</p>
            ${laudo.cid ? `<p><strong>CID:</strong> ${esc(laudo.cid.codigo)} - ${esc(laudo.cid.descricao)}</p>` : '<p><strong>CID:</strong> Não informado nesta consulta</p>'}
            <p><strong>Parecer:</strong> ${esc(laudo.parecer)}</p>
            ${laudo.receitas.map(r => `<p><strong>Receita:</strong> ${esc(r.prescricao)}${r.farmaciaPopular ? '<br><span class="selo farmacia">Orientação: verificar disponibilidade no Farmácia Popular</span>' : ''}</p>`).join('')}
            <p>${esc(laudo.medico.usuario.nome)} · CRM ${esc(laudo.medico.crm)}/${esc(laudo.medico.ufCrm)}
               · emitido em ${dataHora(laudo.emitidoEm)}</p>
            <p class="assinatura">${esc(laudo.assinaturaDigital)}</p>
        </div>`;
}

async function carregarExames(dossieId) {
    const alvo = document.getElementById('exames-' + dossieId);
    if (!alvo) return;
    try {
        const anexos = await api(`/api/dossies/${dossieId}/exames`);
        alvo.innerHTML = anexos.map(a => `<div class="anexo-item">
            <span><strong>${esc(a.nomeArquivo)}</strong><small>${(a.tamanho / 1024).toFixed(0)} KB · ${dataHora(a.criadoEm)}</small></span>
            <button type="button" class="btn btn-sec btn-peq" onclick="baixarExame(${a.id})">Baixar</button>
        </div>`).join('') || '<p class="suave peq">Nenhum exame anexado a este atendimento.</p>';
    } catch (erro) {
        alvo.innerHTML = `<p class="aviso erro">${esc(erro.message)}</p>`;
    }
}

async function enviarExame(evento, dossieId) {
    evento.preventDefault();
    const arquivo = evento.currentTarget.querySelector('input[type=file]').files[0];
    if (!arquivo) return;
    try {
        await apiArquivo(`/api/dossies/${dossieId}/exames`, arquivo);
        evento.currentTarget.reset();
        avisar('Exame anexado ao histórico');
        carregarExames(dossieId);
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}

async function baixarExame(anexoId) {
    try {
        const resposta = await fetch(`/api/exames/${anexoId}`, {
            headers: { Authorization: sessao().token }
        });
        if (!resposta.ok) {
            const texto = await resposta.text();
            const erro = texto ? JSON.parse(texto) : null;
            throw new Error((erro && erro.mensagem) || 'Não foi possível baixar o exame');
        }
        const url = URL.createObjectURL(await resposta.blob());
        const link = document.createElement('a');
        link.href = url;
        const disposicao = resposta.headers.get('Content-Disposition') || '';
        const nomeUtf8 = disposicao.match(/filename\*=UTF-8''([^;]+)/i);
        const nomeSimples = disposicao.match(/filename="?([^";]+)"?/i);
        link.download = nomeUtf8 ? decodeURIComponent(nomeUtf8[1]) : nomeSimples ? nomeSimples[1] : 'exame';
        link.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}

async function mostrarHistoricoPaciente(pacienteId) {
    const dossies = await api(`/api/pacientes/${pacienteId}/historico`);
    const eventos = await Promise.all(dossies.map(async d => {
        const [exames, prescricoes] = await Promise.all([
            api(`/api/dossies/${d.id}/exames`),
            api(`/api/dossies/${d.id}/receitas`)
        ]);
        const risco = classeRisco(d);
        const triagem = d.dadosTriagem;
        return `<div class="evento">
            <div class="peq suave">${dataHora(d.coletadoEm)}</div>
            <div>${selo(d.status)} <strong>${esc(d.dadosTriagem.queixaPrincipal)}</strong>
                <span class="selo risco-${risco.classe}">${risco.rotulo} · ${Math.round(d.escore)} pontos</span>
            </div>
            <p class="peq suave">${d.especialidade ? `Especialidade: ${esc(d.especialidade)}` : 'Ainda sem encaminhamento'}
                ${d.medicoNome ? ` · Médico: ${esc(d.medicoNome)}` : ''}</p>
            <details class="historico-detalhes">
                <summary>Ver detalhes deste atendimento</summary>
                <dl class="dados">
                    <div><dt>Temperatura</dt><dd>${triagem.temperatura} °C</dd></div>
                    <div><dt>Pressão arterial</dt><dd>${triagem.pressaoSistolica} × ${triagem.pressaoDiastolica}</dd></div>
                    <div><dt>Frequência cardíaca</dt><dd>${triagem.frequenciaCardiaca} bpm</dd></div>
                    <div><dt>Saturação O₂</dt><dd>${triagem.saturacaoO2}%</dd></div>
                    <div><dt>Dor</dt><dd>${triagem.dor}/10</dd></div>
                    <div><dt>Comorbidades informadas</dt><dd>${esc((triagem.comorbidades || []).join(', ') || 'Nenhuma')}</dd></div>
                </dl>
                ${d.relato ? `<p><strong>Relato:</strong> ${esc(d.relato)}</p>` : ''}
                ${d.fatoresEscore?.length ? `<p><strong>Fatores do escore:</strong> ${esc(d.fatoresEscore.join('; '))}</p>` : ''}
                ${d.laudo ? `<div class="cartao"><h4>Laudo${d.laudo.cid ? ` · CID ${esc(d.laudo.cid.codigo)}` : ''}</h4>
                    <p>${esc(d.laudo.parecer)}</p>
                </div>` : '<p class="suave">Ainda não há laudo neste atendimento.</p>'}
            </details>
            ${prescricoes.length ? `<div class="lista-exames">${prescricoes.map(r => `<article class="receita-item">
                <h4>${r.medicamento ? `Prescrição: ${esc(r.medicamento)} · ${esc(r.apresentacao)}` : 'Prescrição anterior'}</h4>
                ${r.dose ? `<p><strong>Dose:</strong> ${esc(r.dose)} · <strong>Via:</strong> ${esc(r.viaAdministracao)}</p>
                    <p><strong>Frequência:</strong> ${esc(r.frequencia)} · <strong>Duração:</strong> ${esc(r.duracao)}</p>`
                    : `<p>${esc(r.descricao)}</p>`}
                ${r.orientacoes ? `<p><strong>Orientações:</strong> ${esc(r.orientacoes)}</p>` : ''}
                <p class="peq suave">Assinada em ${dataHora(r.assinadaEm)}</p>
            </article>`).join('')}</div>` : ''}
            ${exames.length ? `<div class="lista-exames">${exames.map(a => `<div class="anexo-item">
                <span><strong>${esc(a.nomeArquivo)}</strong><small>Exame · ${dataHora(a.criadoEm)}</small></span>
                <button type="button" class="btn btn-sec btn-peq" onclick="baixarExame(${a.id})">Baixar</button>
            </div>`).join('')}</div>` : ''}
        </div>`;
    }));
    abrirModal('Histórico do paciente', eventos.length
        ? `<div class="linha-tempo">${eventos.join('')}</div>`
        : '<p class="suave">Nenhum atendimento ainda.</p>');
}

function alternarContraste() {
    document.body.classList.toggle('alto-contraste');
    localStorage.setItem('contraste', document.body.classList.contains('alto-contraste'));
    atualizarPreferenciasAparencia();
}

function atualizarPreferenciasAparencia() {
    const contrasteAtivo = document.body.classList.contains('alto-contraste');
    const acaoContraste = document.getElementById('acaoContraste');
    if (acaoContraste) acaoContraste.setAttribute('aria-pressed', String(contrasteAtivo));
}

document.addEventListener('click', evento => {
    const menu = document.querySelector('.menu-perfil');
    if (menu && !menu.contains(evento.target)) menu.open = false;
});

if (localStorage.getItem('contraste') === 'true') {
    document.body.classList.add('alto-contraste');
}
atualizarPreferenciasAparencia();

function adicionarVisualizador() {
    if (new URLSearchParams(location.search).get('visualizacaoPreview') === '1') return;

    const dispositivos = {
        iphone: { nome: 'iPhone 16', largura: 393, altura: 852, moldura: 24, alturaExtra: 0 },
        samsung: { nome: 'Samsung Galaxy S24', largura: 360, altura: 800, moldura: 24, alturaExtra: 0 },
        tablet: { nome: 'iPad Air', largura: 820, altura: 1180, moldura: 28, alturaExtra: 0 },
        computador: { nome: 'Computador', largura: 1280, altura: 800, moldura: 24, alturaExtra: 64 }
    };

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
                    <option value="iphone">Apple · iPhone 16</option>
                    <option value="samsung">Samsung · Galaxy S24</option>
                    <option value="tablet">Tablet · iPad Air</option>
                    <option value="computador">Computador · 1280 px</option>
                </select>
                <button class="btn btn-sec btn-peq" type="button" id="fecharPreview">Fechar</button>
            </div>
        </div>
        <div class="preview-canvas">
            <div class="preview-stage">
                <div class="preview-hardware" id="previewHardware" data-model="iphone">
                    <span class="preview-camera" aria-hidden="true"></span>
                    <iframe title="Pré-visualização do RuralMed no dispositivo selecionado"></iframe>
                    <span class="preview-homebar" aria-hidden="true"></span>
                    <span class="preview-monitor-stand" aria-hidden="true"></span>
                </div>
            </div>
        </div>`;
    document.body.append(abrir, dialogo);

    const seletor = dialogo.querySelector('#previewDispositivo');
    seletor.value = localStorage.getItem('ruralmed-dispositivo-preview') || 'iphone';
    const hardware = dialogo.querySelector('#previewHardware');
    const frame = dialogo.querySelector('iframe');
    const canvas = dialogo.querySelector('.preview-canvas');
    const stage = dialogo.querySelector('.preview-stage');
    const url = new URL(location.href);
    url.searchParams.set('visualizacaoPreview', '1');

    function atualizarTamanho() {
        const dispositivo = dispositivos[seletor.value] || dispositivos.iphone;
        const larguraMoldura = dispositivo.largura + dispositivo.moldura * 2;
        const alturaMoldura = dispositivo.altura + dispositivo.moldura * 2 + dispositivo.alturaExtra;
        const escala = Math.min(1, (canvas.clientWidth - 32) / larguraMoldura,
            (canvas.clientHeight - 24) / alturaMoldura);

        hardware.dataset.model = seletor.value;
        hardware.style.left = '0';
        hardware.style.top = '0';
        hardware.style.width = larguraMoldura + 'px';
        hardware.style.height = alturaMoldura + 'px';
        hardware.style.padding = dispositivo.moldura + 'px';
        hardware.style.setProperty('--escala-dispositivo', Math.max(.25, escala));
        stage.style.width = larguraMoldura * escala + 'px';
        stage.style.height = alturaMoldura * escala + 'px';
        frame.style.width = dispositivo.largura + 'px';
        frame.style.height = dispositivo.altura + 'px';
        frame.title = 'Pré-visualização em ' + dispositivo.nome;
    }

    abrir.addEventListener('click', () => {
        if (!frame.dataset.carregado) {
            frame.src = url.href;
            frame.dataset.carregado = 'true';
        }
        dialogo.showModal();
        requestAnimationFrame(atualizarTamanho);
    });
    seletor.addEventListener('change', () => {
        localStorage.setItem('ruralmed-dispositivo-preview', seletor.value);
        atualizarTamanho();
    });
    dialogo.querySelector('#fecharPreview').addEventListener('click', () => dialogo.close());
    window.addEventListener('resize', () => {
        if (dialogo.open) atualizarTamanho();
    });
}

document.addEventListener('DOMContentLoaded', adicionarVisualizador);
