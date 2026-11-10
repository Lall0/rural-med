
exigirPerfil('GESTOR');
document.getElementById('conexao').textContent = 'On-line';

async function gerarRelatorio(evento) {
    if (evento) evento.preventDefault();
    const inicio = document.getElementById('inicio').value;
    const fim = document.getElementById('fim').value;
    try {
        const r = await api(`/api/relatorios/atendimentos?inicio=${inicio}&fim=${fim}`);
        document.getElementById('relatorio').innerHTML = `
            <div class="kpis" style="margin-bottom:1rem">
                <div class="kpi"><strong>${r.triagens}</strong><span>triagens</span></div>
                <div class="kpi"><strong>${r.prioritarias}</strong><span>casos prioritários</span></div>
                <div class="kpi"><strong>${r.laudosEmitidos}</strong><span>laudos emitidos</span></div>
                <div class="kpi"><strong>${r.viagensTfdEvitadas}</strong><span>viagens de TFD evitadas</span></div>
            </div>
            <div class="cartao">
                <h3>Economia estimada com transporte</h3>
                <p style="font-size:1.6rem;font-weight:800;margin:0">${moeda(r.economiaEstimada)}</p>
                <p class="suave peq">Cada laudo a distância conta como uma viagem de TFD evitada.</p>
            </div>`;
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}

async function verExtrato(evento) {
    if (evento) evento.preventDefault();
    const [ano, mes] = document.getElementById('mes').value.split('-');
    try {
        const linhas = await api(`/api/relatorios/repasses?ano=${ano}&mes=${Number(mes)}`);
        const total = linhas.reduce((soma, l) => soma + Number(l.total), 0);
        document.getElementById('extrato').innerHTML = `
            <div class="cartao">
                <h3>Repasses do mês</h3>
                <table class="tabela">
                    <thead><tr><th>Médico</th><th>Especialidade</th><th>Laudos</th><th>Valor por laudo</th><th>Total</th></tr></thead>
                    <tbody>${linhas.map(l => `
                        <tr>
                            <td data-rotulo="Médico">${esc(l.medico)}</td>
                            <td data-rotulo="Especialidade">${esc(l.especialidade)}</td>
                            <td data-rotulo="Laudos">${l.laudos}</td>
                            <td data-rotulo="Valor por laudo">${moeda(l.valorPorLaudo)}</td>
                            <td data-rotulo="Total"><strong>${moeda(l.total)}</strong></td>
                        </tr>`).join('') || '<tr><td>Nenhum laudo neste mês.</td></tr>'}
                    </tbody>
                </table>
                <p style="text-align:right"><strong>Total: ${moeda(total)}</strong></p>
            </div>`;
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}

const hoje = new Date().toISOString().substring(0, 10);
document.getElementById('inicio').value = hoje.substring(0, 8) + '01';
document.getElementById('fim').value = hoje;
document.getElementById('mes').value = hoje.substring(0, 7);
mostrarAba('relatorios');
gerarRelatorio();
verExtrato();
