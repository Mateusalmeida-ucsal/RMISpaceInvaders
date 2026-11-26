package comum;

import java.io.Serializable;

/** Um alienígena. tipo 0 = linha de cima (30 pts), 1 = 20 pts, 2 = 10 pts. */
public class Invasor implements Serializable {
    private static final long serialVersionUID = 1L;

    public int x;
    public int y;
    public int tipo;
    public int coluna;
    public int pontos;

    public Invasor(int x, int y, int tipo, int coluna) {
        this.x = x;
        this.y = y;
        this.tipo = tipo;
        this.coluna = coluna;
        this.pontos = (tipo == 0) ? 30 : (tipo == 1) ? 20 : 10;
    }

    public Invasor(Invasor o) {
        this.x = o.x;
        this.y = o.y;
        this.tipo = o.tipo;
        this.coluna = o.coluna;
        this.pontos = o.pontos;
    }
}
