package comum;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EstadoJogo implements Serializable {
    private static final long serialVersionUID = 1L;

    public final EstadoPartida estadoPartida;
    public final int nivel;
    public final String mensagem;
    public final List<Jogador> jogadores = new ArrayList<>();
    public final List<Invasor> invasores = new ArrayList<>();
    public final List<Projetil> projeteis = new ArrayList<>();
    public final List<Barreira> barreiras = new ArrayList<>();
    public final List<Evento> eventos = new ArrayList<>();

    public EstadoJogo(EstadoPartida estadoPartida, int nivel, String mensagem) {
        this.estadoPartida = estadoPartida;
        this.nivel = nivel;
        this.mensagem = mensagem;
    }
}
