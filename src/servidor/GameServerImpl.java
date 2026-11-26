package servidor;

import comum.Acao;
import comum.Barreira;
import comum.ClientCallback;
import comum.Constantes;
import comum.EstadoJogo;
import comum.Evento;
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
public class GameServerImpl extends UnicastRemoteObject implements GameService {

    private static final long serialVersionUID = 1L;
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
    private final Object trava = new Object();
    private final Map<Integer, Sessao> sessoes = new HashMap<>();
    private final List<Invasor> invasores = new ArrayList<>();
    private final List<Projetil> projeteis = new ArrayList<>();
    private final List<Barreira> barreiras = new ArrayList<>();
    private final List<Evento> eventos = new ArrayList<>(); // eventos desde o último snapshot
    private EstadoPartida estado = EstadoPartida.AGUARDANDO_JOGADORES;
    private String mensagem = "Aguardando jogadores...";
    private int nivel = 1;
    private int direcaoInvasores = 1;   // 1 = direita, -1 = esquerda
    private int contadorPasso = 0;
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
                        eventos.add(Evento.TIRO_JOGADOR);
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
        criarBarreiras();
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
    private void criarBarreiras() {
        barreiras.clear();
        int espaco = Constantes.LARGURA / Constantes.BARREIRA_QTD;
        int larguraBarreira = Constantes.BARREIRA_COLUNAS * Constantes.BARREIRA_CELULA;
        for (int i = 0; i < Constantes.BARREIRA_QTD; i++) {
            int x = i * espaco + (espaco - larguraBarreira) / 2;
            barreiras.add(new Barreira(x, Constantes.BARREIRA_Y));
        }
    }

    private void tratarDesconexao(int idJogador, String motivo) {
        synchronized (trava) {
            Sessao removida = sessoes.remove(idJogador);
            if (removida == null) {
                return;
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
                estado = EstadoPartida.AGUARDANDO_JOGADORES;
                mensagem = "Aguardando jogadores...";
                invasores.clear();
                projeteis.clear();
                barreiras.clear();
                nivel = 1;
            }
        }
    }
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
                eventos.clear();
            }

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
        for (Barreira b : barreiras) {
            snap.barreiras.add(new Barreira(b));
        }
        snap.eventos.addAll(eventos);
        return snap;
    }

    private void atualizarJogo() {
        for (Sessao s : sessoes.values()) {
            if (s.jogador.invulneravelTicks > 0) {
                s.jogador.invulneravelTicks--;
            }
        }
        moverProjeteis();
        invasoresAtiram();
        moverInvasores();
        invasoresDestroemBarreiras();
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
        eventos.add(Evento.TIRO_INVASOR);
    }

    private void tratarColisoes() {
        Iterator<Projetil> itP = projeteis.iterator();
        while (itP.hasNext()) {
            Projetil p = itP.next();
            if (tiroAtingiuBarreira(p)) {
                itP.remove();
                continue;
            }

            if (p.isDoJogador()) {
                Iterator<Invasor> itI = invasores.iterator();
                while (itI.hasNext()) {
                    Invasor i = itI.next();
                    if (colide(p.x, p.y, Constantes.TIRO_LARGURA, Constantes.TIRO_ALTURA,
                            i.x, i.y, Constantes.INV_LARGURA, Constantes.INV_ALTURA)) {
                        itI.remove();
                        itP.remove();
                        eventos.add(Evento.INVASOR_ELIMINADO);
                        Sessao dono = sessoes.get(p.dono);
                        if (dono != null) {
                            dono.jogador.pontos += i.pontos;
                        }
                        break;
                    }
                }
            } else {
                for (Sessao s : sessoes.values()) {
                    Jogador j = s.jogador;
                    if (!j.viva || j.isInvulneravel()) {
                        continue;
                    }
                    if (colide(p.x, p.y, Constantes.TIRO_LARGURA, Constantes.TIRO_ALTURA,
                            j.x, j.y, Constantes.NAVE_LARGURA, Constantes.NAVE_ALTURA)) {
                        itP.remove();
                        eventos.add(Evento.JOGADOR_ATINGIDO);
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
    private boolean tiroAtingiuBarreira(Projetil p) {
        final int cel = Constantes.BARREIRA_CELULA;
        for (Barreira b : barreiras) {
            if (!colide(p.x, p.y, Constantes.TIRO_LARGURA, Constantes.TIRO_ALTURA,
                    b.x, b.y, b.largura(), b.altura())) {
                continue;
            }
            int colIni = Math.max(0, Math.floorDiv(p.x - b.x, cel));
            int colFim = Math.min(Constantes.BARREIRA_COLUNAS - 1,
                    Math.floorDiv(p.x + Constantes.TIRO_LARGURA - 1 - b.x, cel));
            int linIni = Math.max(0, Math.floorDiv(p.y - b.y, cel));
            int linFim = Math.min(Constantes.BARREIRA_LINHAS - 1,
                    Math.floorDiv(p.y + Constantes.TIRO_ALTURA - 1 - b.y, cel));
            boolean sobe = p.isDoJogador();
            int linha = sobe ? linFim : linIni;
            while (sobe ? linha >= linIni : linha <= linFim) {
                for (int col = colIni; col <= colFim; col++) {
                    if (b.viva(col, linha)) {
                        erodirBarreira(b, col, linha);
                        return true;
                    }
                }
                linha += sobe ? -1 : 1;
            }
        }
        return false;
    }
    private void erodirBarreira(Barreira b, int colCentro, int linCentro) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                boolean perto = Math.abs(dx) + Math.abs(dy) <= 2;
                if (perto || random.nextBoolean()) {
                    b.destruir(colCentro + dx, linCentro + dy);
                }
            }
        }
    }
    private void invasoresDestroemBarreiras() {
        final int cel = Constantes.BARREIRA_CELULA;
        for (Invasor i : invasores) {
            for (Barreira b : barreiras) {
                if (!colide(i.x, i.y, Constantes.INV_LARGURA, Constantes.INV_ALTURA,
                        b.x, b.y, b.largura(), b.altura())) {
                    continue;
                }
                int colIni = Math.max(0, Math.floorDiv(i.x - b.x, cel));
                int colFim = Math.min(Constantes.BARREIRA_COLUNAS - 1,
                        Math.floorDiv(i.x + Constantes.INV_LARGURA - 1 - b.x, cel));
                int linIni = Math.max(0, Math.floorDiv(i.y - b.y, cel));
                int linFim = Math.min(Constantes.BARREIRA_LINHAS - 1,
                        Math.floorDiv(i.y + Constantes.INV_ALTURA - 1 - b.y, cel));
                for (int lin = linIni; lin <= linFim; lin++) {
                    for (int col = colIni; col <= colFim; col++) {
                        b.destruir(col, lin);
                    }
                }
            }
        }
    }

    private void verificarFimOuProximaOnda() {
        for (Invasor i : invasores) {
            if (i.y + Constantes.INV_ALTURA >= Constantes.NAVE_Y) {
                encerrarPartida("Os invasores chegaram à Terra!");
                return;
            }
        }
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
        if (invasores.isEmpty()) {
            nivel++;
            direcaoInvasores = 1;
            contadorPasso = 0;
            projeteis.clear();
            criarInvasores();
            criarBarreiras();
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
