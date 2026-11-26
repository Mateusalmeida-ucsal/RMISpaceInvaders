package servidor;

import comum.Acao;
import comum.ClientCallback;
import comum.Constantes;
import comum.EstadoJogo;
import comum.EstadoPartida;
import comum.GameService;
import comum.Invasor;
import comum.Jogador;
import comum.Projetil;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Objeto remoto do jogo. Guarda TODO o estado da partida.
 *
 * Concorrência: o RMI atende cada chamada em uma thread diferente, e o game loop
 * roda em outra thread. Tudo que lê ou altera o estado fica dentro de
 * synchronized (trava). Chamadas REMOTAS (callbacks) nunca são feitas dentro
 * da região sincronizada, para um cliente lento não travar o jogo.
 */
public class GameServerImpl extends UnicastRemoteObject implements GameService {

    private static final long serialVersionUID = 1L;

    /** Dados de uma conexão: o jogador, o callback remoto e controle de envio. */
    private static class Sessao {
        final Jogador jogador;
        final ClientCallback callback;
        long ultimoTiro = 0;
        final AtomicBoolean enviando = new AtomicBoolean(false);

        Sessao(Jogador jogador, ClientCallback callback) {
            this.jogador = jogador;
            this.callback = callback;
        }
    }

    // ---- Estado compartilhado (protegido por "trava") ----
    private final Object trava = new Object();
    private final Map<Integer, Sessao> sessoes = new HashMap<>();
    private final List<Invasor> invasores = new ArrayList<>();
    private final List<Projetil> projeteis = new ArrayList<>();
    private EstadoPartida estado = EstadoPartida.AGUARDANDO_JOGADORES;
    private String mensagem = "Aguardando jogadores...";
    private int nivel = 1;
    private int direcaoInvasores = 1;   // 1 = direita, -1 = esquerda
    private int contadorPasso = 0;

    // ---- Fora do estado do jogo ----
    private final Random random = new Random();
    private final ExecutorService envio = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "envio-callback");
        t.setDaemon(true);
        return t;
    });

    public GameServerImpl() throws RemoteException {
        super();
        Thread loop = new Thread(this::gameLoop, "game-loop");
        loop.setDaemon(true);
        loop.start();
    }

    // =====================================================================
    // Métodos remotos
    // =====================================================================

    @Override
    public int conectarJogador(ClientCallback callback) throws RemoteException {
        if (callback == null) {
            return -1;
        }
        synchronized (trava) {
            if (estado == EstadoPartida.EM_ANDAMENTO) {
                System.out.println("[servidor] Conexão recusada: partida em andamento.");
                return -1;
            }
            int id = proximoIdLivre();
            if (id == -1) {
                System.out.println("[servidor] Conexão recusada: sem vagas.");
                return -1;
            }
            int xInicial = (id == 1) ? 200 : 560;
            sessoes.put(id, new Sessao(new Jogador(id, xInicial), callback));
            System.out.println("[servidor] Jogador " + id + " conectado.");

            if (estado == EstadoPartida.AGUARDANDO_JOGADORES) {
                if (sessoes.size() == 2) {
                    iniciarPartida();
                } else {
                    mensagem = "Aguardando oponente...";
                }
            } else {
                // ENCERRADA: o novo jogador entra na tela de fim e pode pedir reinício
                mensagem = "Partida encerrada. Peça um novo jogo.";
            }
            return id;
        }
    }

    @Override
    public void enviarJogada(int idJogador, Acao acao) throws RemoteException {
        if (acao == null) {
            return;
        }
        synchronized (trava) {
            Sessao s = sessoes.get(idJogador);
            // Validações: jogador existe, partida em andamento, nave viva
            if (s == null || estado != EstadoPartida.EM_ANDAMENTO || !s.jogador.viva) {
                return;
            }
            Jogador j = s.jogador;
            switch (acao) {
                case MOVER_ESQUERDA:
                    j.x = Math.max(0, j.x - Constantes.NAVE_VELOCIDADE);
                    break;
                case MOVER_DIREITA:
                    j.x = Math.min(Constantes.LARGURA - Constantes.NAVE_LARGURA,
                            j.x + Constantes.NAVE_VELOCIDADE);
                    break;
                case ATIRAR:
                    long agora = System.currentTimeMillis();
                    if (agora - s.ultimoTiro >= Constantes.COOLDOWN_TIRO_MS) {
                        int tx = j.x + Constantes.NAVE_LARGURA / 2 - Constantes.TIRO_LARGURA / 2;
                        projeteis.add(new Projetil(idJogador, tx, j.y - Constantes.TIRO_ALTURA));
                        s.ultimoTiro = agora;
                    }
                    break;
            }
        }
    }

    @Override
    public void solicitarReinicio(int idJogador) throws RemoteException {
        synchronized (trava) {
            Sessao s = sessoes.get(idJogador);
            if (s == null || estado != EstadoPartida.ENCERRADA) {
                return;
            }
            s.jogador.pediuReinicio = true;
            boolean todosQuerem = sessoes.size() == 2;
            for (Sessao outra : sessoes.values()) {
                if (!outra.jogador.pediuReinicio) {
                    todosQuerem = false;
                }
            }
            if (todosQuerem) {
                System.out.println("[servidor] Reiniciando partida.");
                iniciarPartida();
            }
        }
    }

    @Override
    public void desconectarJogador(int idJogador) throws RemoteException {
        tratarDesconexao(idJogador, "saiu da partida");
    }

    // =====================================================================
    // Controle de sessões / ciclo da partida (sempre chamados com a trava)
    // =====================================================================

    private int proximoIdLivre() {
        for (int id = 1; id <= 2; id++) {
            if (!sessoes.containsKey(id)) {
                return id;
            }
        }
        return -1;
    }

    private void iniciarPartida() {
        nivel = 1;
        direcaoInvasores = 1;
        contadorPasso = 0;
        projeteis.clear();
        criarInvasores();
        for (Sessao s : sessoes.values()) {
            int id = s.jogador.id;
            Jogador novo = new Jogador(id, id == 1 ? 200 : 560);
            s.jogador.x = novo.x;
            s.jogador.vidas = novo.vidas;
            s.jogador.pontos = 0;
            s.jogador.viva = true;
            s.jogador.pediuReinicio = false;
            s.jogador.invulneravelTicks = 0;
            s.ultimoTiro = 0;
        }
        estado = EstadoPartida.EM_ANDAMENTO;
        mensagem = "";
        System.out.println("[servidor] Partida iniciada.");
    }

    private void criarInvasores() {
        invasores.clear();
        int inicioY = Constantes.INV_INICIO_Y + Math.min(nivel - 1, 5) * 20;
        for (int linha = 0; linha < Constantes.INV_LINHAS; linha++) {
            int tipo = (linha == 0) ? 0 : (linha <= 2) ? 1 : 2;
            for (int col = 0; col < Constantes.INV_COLUNAS; col++) {
                int x = Constantes.INV_INICIO_X + col * Constantes.INV_ESPACO_X;
                int y = inicioY + linha * Constantes.INV_ESPACO_Y;
                invasores.add(new Invasor(x, y, tipo, col));
            }
        }
    }

    private void tratarDesconexao(int idJogador, String motivo) {
        synchronized (trava) {
            Sessao removida = sessoes.remove(idJogador);
            if (removida == null) {
                return; // já tinha sido removido (evita tratar duas vezes)
            }
            System.out.println("[servidor] Jogador " + idJogador + " " + motivo + ".");

            if (estado == EstadoPartida.EM_ANDAMENTO) {
                estado = EstadoPartida.ENCERRADA;
                mensagem = "Jogador " + idJogador + " desconectou. Partida encerrada.";
                for (Sessao s : sessoes.values()) {
                    s.jogador.pediuReinicio = false;
                }
            } else if (estado == EstadoPartida.AGUARDANDO_JOGADORES) {
                mensagem = "Aguardando oponente...";
            }

            if (sessoes.isEmpty()) {
                // ninguém sobrou: volta ao estado inicial
                estado = EstadoPartida.AGUARDANDO_JOGADORES;
                mensagem = "Aguardando jogadores...";
                invasores.clear();
                projeteis.clear();
                nivel = 1;
            }
        }
    }

    // =====================================================================
    // Game loop
    // =====================================================================

    private void gameLoop() {
        while (true) {
            long inicio = System.currentTimeMillis();
            EstadoJogo snapshot = null;
            List<Sessao> destinatarios = null;

            synchronized (trava) {
                if (estado == EstadoPartida.EM_ANDAMENTO) {
                    atualizarJogo();
                }
                if (!sessoes.isEmpty()) {
                    snapshot = criarSnapshot();
                    destinatarios = new ArrayList<>(sessoes.values());
                }
            }

            // Fora da região sincronizada: chamadas remotas
            if (snapshot != null) {
                for (Sessao s : destinatarios) {
                    notificar(s, snapshot);
                }
            }

            long gasto = System.currentTimeMillis() - inicio;
            try {
                Thread.sleep(Math.max(1, Constantes.TICK_MS - gasto));
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /**
     * Envia o snapshot em outra thread. Se o envio anterior a esse cliente ainda
     * não terminou, pula este quadro (cliente lento não acumula fila nem trava o loop).
     */
    private void notificar(Sessao s, EstadoJogo snapshot) {
        if (!s.enviando.compareAndSet(false, true)) {
            return;
        }
        envio.execute(() -> {
            try {
                s.callback.atualizarEstado(snapshot);
            } catch (RemoteException e) {
                tratarDesconexao(s.jogador.id, "perdeu a conexão (RemoteException)");
            } finally {
                s.enviando.set(false);
            }
        });
    }

    // Chamado dentro de synchronized (trava)
    private EstadoJogo criarSnapshot() {
        EstadoJogo snap = new EstadoJogo(estado, nivel, mensagem);
        for (Sessao s : sessoes.values()) {
            snap.jogadores.add(new Jogador(s.jogador));
        }
        for (Invasor i : invasores) {
            snap.invasores.add(new Invasor(i));
        }
        for (Projetil p : projeteis) {
            snap.projeteis.add(new Projetil(p));
        }
        return snap;
    }

    // =====================================================================
    // Regras do jogo (um tick) — chamados dentro de synchronized (trava)
    // =====================================================================

    private void atualizarJogo() {
        for (Sessao s : sessoes.values()) {
            if (s.jogador.invulneravelTicks > 0) {
                s.jogador.invulneravelTicks--;
            }
        }
        moverProjeteis();
        invasoresAtiram();
        moverInvasores();
        tratarColisoes();
        verificarFimOuProximaOnda();
    }

    private void moverProjeteis() {
        Iterator<Projetil> it = projeteis.iterator();
        while (it.hasNext()) {
            Projetil p = it.next();
            if (p.isDoJogador()) {
                p.y -= Constantes.TIRO_JOGADOR_VEL;
            } else {
                p.y += Constantes.TIRO_INVASOR_VEL;
            }
            if (p.y + Constantes.TIRO_ALTURA < 0 || p.y > Constantes.ALTURA) {
                it.remove();
            }
        }
    }

    /** Os invasores aceleram quando restam poucos e conforme o nível sobe. */
    private void moverInvasores() {
        contadorPasso++;
        int total = Constantes.INV_LINHAS * Constantes.INV_COLUNAS;
        int intervalo = Math.max(2, invasores.size() * 25 / total - (nivel - 1));
        if (contadorPasso < intervalo) {
            return;
        }
        contadorPasso = 0;

        boolean bateuNaBorda = false;
        for (Invasor i : invasores) {
            int nx = i.x + direcaoInvasores * Constantes.INV_PASSO_X;
            if (nx < 0 || nx + Constantes.INV_LARGURA > Constantes.LARGURA) {
                bateuNaBorda = true;
                break;
            }
        }
        if (bateuNaBorda) {
            direcaoInvasores = -direcaoInvasores;
            for (Invasor i : invasores) {
                i.y += Constantes.INV_PASSO_Y;
            }
        } else {
            for (Invasor i : invasores) {
                i.x += direcaoInvasores * Constantes.INV_PASSO_X;
            }
        }
    }

    /** Só o invasor mais baixo de cada coluna pode atirar, como no original. */
    private void invasoresAtiram() {
        int tirosInvasor = 0;
        for (Projetil p : projeteis) {
            if (!p.isDoJogador()) {
                tirosInvasor++;
            }
        }
        if (tirosInvasor >= Constantes.MAX_TIROS_INVASOR || invasores.isEmpty()) {
            return;
        }
        if (random.nextInt(100) >= 2 + nivel) {
            return;
        }
        Invasor[] maisBaixo = new Invasor[Constantes.INV_COLUNAS];
        for (Invasor i : invasores) {
            if (maisBaixo[i.coluna] == null || i.y > maisBaixo[i.coluna].y) {
                maisBaixo[i.coluna] = i;
            }
        }
        List<Invasor> candidatos = new ArrayList<>();
        for (Invasor i : maisBaixo) {
            if (i != null) {
                candidatos.add(i);
            }
        }
        Invasor atirador = candidatos.get(random.nextInt(candidatos.size()));
        projeteis.add(new Projetil(0,
                atirador.x + Constantes.INV_LARGURA / 2 - Constantes.TIRO_LARGURA / 2,
                atirador.y + Constantes.INV_ALTURA));
    }

    private void tratarColisoes() {
        Iterator<Projetil> itP = projeteis.iterator();
        while (itP.hasNext()) {
            Projetil p = itP.next();

            if (p.isDoJogador()) {
                // tiro do jogador x invasores
                Iterator<Invasor> itI = invasores.iterator();
                while (itI.hasNext()) {
                    Invasor i = itI.next();
                    if (colide(p.x, p.y, Constantes.TIRO_LARGURA, Constantes.TIRO_ALTURA,
                            i.x, i.y, Constantes.INV_LARGURA, Constantes.INV_ALTURA)) {
                        itI.remove();
                        itP.remove();
                        Sessao dono = sessoes.get(p.dono);
                        if (dono != null) {
                            dono.jogador.pontos += i.pontos;
                        }
                        break;
                    }
                }
            } else {
                // tiro do invasor x naves
                for (Sessao s : sessoes.values()) {
                    Jogador j = s.jogador;
                    if (!j.viva || j.isInvulneravel()) {
                        continue;
                    }
                    if (colide(p.x, p.y, Constantes.TIRO_LARGURA, Constantes.TIRO_ALTURA,
                            j.x, j.y, Constantes.NAVE_LARGURA, Constantes.NAVE_ALTURA)) {
                        itP.remove();
                        j.vidas--;
                        if (j.vidas <= 0) {
                            j.vidas = 0;
                            j.viva = false;
                        } else {
                            j.invulneravelTicks = Constantes.INVULNERAVEL_TICKS;
                        }
                        break;
                    }
                }
            }
        }
    }

    private void verificarFimOuProximaOnda() {
        // 1) invasores chegaram na altura das naves
        for (Invasor i : invasores) {
            if (i.y + Constantes.INV_ALTURA >= Constantes.NAVE_Y) {
                encerrarPartida("Os invasores chegaram à Terra!");
                return;
            }
        }
        // 2) todas as naves destruídas
        boolean algumaViva = false;
        for (Sessao s : sessoes.values()) {
            if (s.jogador.viva) {
                algumaViva = true;
            }
        }
        if (!algumaViva) {
            encerrarPartida("Todas as naves foram destruídas!");
            return;
        }
        // 3) onda derrotada: próximo nível
        if (invasores.isEmpty()) {
            nivel++;
            direcaoInvasores = 1;
            contadorPasso = 0;
            projeteis.clear();
            criarInvasores();
            System.out.println("[servidor] Nível " + nivel);
        }
    }

    private void encerrarPartida(String motivo) {
        estado = EstadoPartida.ENCERRADA;
        StringBuilder sb = new StringBuilder("FIM DE JOGO - " + motivo);
        Jogador melhor = null;
        boolean empate = false;
        for (Sessao s : sessoes.values()) {
            Jogador j = s.jogador;
            j.pediuReinicio = false;
            sb.append(" | Jogador ").append(j.id).append(": ").append(j.pontos).append(" pts");
            if (melhor == null || j.pontos > melhor.pontos) {
                melhor = j;
                empate = false;
            } else if (j.pontos == melhor.pontos) {
                empate = true;
            }
        }
        if (melhor != null && !empate) {
            sb.append(" | Maior pontuação: Jogador ").append(melhor.id);
        } else if (empate) {
            sb.append(" | Empate");
        }
        mensagem = sb.toString();
        System.out.println("[servidor] " + mensagem);
    }

    private static boolean colide(int ax, int ay, int aw, int ah, int bx, int by, int bw, int bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }
}
