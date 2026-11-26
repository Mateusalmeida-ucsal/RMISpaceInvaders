package comum;

import java.io.Serializable;

/** Dados de um jogador. Vai dentro do snapshot enviado aos clientes. */
public class Jogador implements Serializable {
    private static final long serialVersionUID = 1L;

    public int id;
    public int x;
    public int y;
    public int vidas;
    public int pontos;
    public boolean viva;
    public boolean pediuReinicio;
    public int invulneravelTicks;

    public Jogador(int id, int x) {
        this.id = id;
        this.x = x;
        this.y = Constantes.NAVE_Y;
        this.vidas = Constantes.VIDAS_INICIAIS;
        this.pontos = 0;
        this.viva = true;
        this.pediuReinicio = false;
        this.invulneravelTicks = 0;
    }

    /** Construtor de cópia, usado para montar o snapshot. */
    public Jogador(Jogador o) {
        this.id = o.id;
        this.x = o.x;
        this.y = o.y;
        this.vidas = o.vidas;
        this.pontos = o.pontos;
        this.viva = o.viva;
        this.pediuReinicio = o.pediuReinicio;
        this.invulneravelTicks = o.invulneravelTicks;
    }

    public boolean isInvulneravel() {
        return invulneravelTicks > 0;
    }
}
