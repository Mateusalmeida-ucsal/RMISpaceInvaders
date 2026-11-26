package comum;

import java.io.Serializable;

/** Tiro. dono = 0 é tiro de invasor; dono = 1 ou 2 é tiro do jogador correspondente. */
public class Projetil implements Serializable {
    private static final long serialVersionUID = 1L;

    public int dono;
    public int x;
    public int y;

    public Projetil(int dono, int x, int y) {
        this.dono = dono;
        this.x = x;
        this.y = y;
    }

    public Projetil(Projetil o) {
        this.dono = o.dono;
        this.x = o.x;
        this.y = o.y;
    }

    public boolean isDoJogador() {
        return dono != 0;
    }
}
