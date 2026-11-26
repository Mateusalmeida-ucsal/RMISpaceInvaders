package comum;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot do estado da partida. O servidor monta uma CÓPIA do estado a cada tick
 * e envia aos clientes via callback. O cliente só lê, nunca altera o jogo.
 */
public class EstadoJogo implements Serializable {
    private static final long serialVersionUID = 1L;

    public final EstadoPartida estadoPartida;
    public final int nivel;
    public final String mensagem;
    public final List<Jogador> jogadores = new ArrayList<>();
    public final List<Invasor> invasores = new ArrayList<>();
    public final List<Projetil> projeteis = new ArrayList<>();
    public final List<Barreira> barreiras = new ArrayList<>();
    /** Eventos ocorridos desde o snapshot anterior (usados pelo cliente para tocar sons). */
    public final List<Evento> eventos = new ArrayList<>();

    public EstadoJogo(EstadoPartida estadoPartida, int nivel, String mensagem) {
        this.estadoPartida = estadoPartida;
        this.nivel = nivel;
        this.mensagem = mensagem;
    }
}
