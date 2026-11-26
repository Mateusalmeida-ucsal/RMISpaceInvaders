package comum;

import java.io.Serializable;

public class Barreira implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String[] FORMA = {
        "....XXXXXXXXXXXXXX....",
        "...XXXXXXXXXXXXXXXX...",
        "..XXXXXXXXXXXXXXXXXX..",
        ".XXXXXXXXXXXXXXXXXXXX.",
        "XXXXXXXXXXXXXXXXXXXXXX",
        "XXXXXXXXXXXXXXXXXXXXXX",
        "XXXXXXXXXXXXXXXXXXXXXX",
        "XXXXXXXXXXXXXXXXXXXXXX",
        "XXXXXXXXXXXXXXXXXXXXXX",
        "XXXXXXXXXXXXXXXXXXXXXX",
        "XXXXXXXX......XXXXXXXX",
        "XXXXXXX........XXXXXXX",
        "XXXXXX..........XXXXXX",
        "XXXXX............XXXXX",
        "XXXXX............XXXXX",
        "XXXXX............XXXXX"
    };

    public final int x;
    public final int y;
    public final boolean[] celulas = new boolean[Constantes.BARREIRA_COLUNAS * Constantes.BARREIRA_LINHAS];
    public Barreira(int x, int y) {
        this.x = x;
        this.y = y;
        for (int lin = 0; lin < Constantes.BARREIRA_LINHAS; lin++) {
            for (int col = 0; col < Constantes.BARREIRA_COLUNAS; col++) {
                celulas[lin * Constantes.BARREIRA_COLUNAS + col] = FORMA[lin].charAt(col) == 'X';
            }
        }
    }
    public Barreira(Barreira o) {
        this.x = o.x;
        this.y = o.y;
        System.arraycopy(o.celulas, 0, this.celulas, 0, o.celulas.length);
    }

    public boolean viva(int col, int lin) {
        if (col < 0 || col >= Constantes.BARREIRA_COLUNAS || lin < 0 || lin >= Constantes.BARREIRA_LINHAS) {
            return false;
        }
        return celulas[lin * Constantes.BARREIRA_COLUNAS + col];
    }

    public void destruir(int col, int lin) {
        if (col < 0 || col >= Constantes.BARREIRA_COLUNAS || lin < 0 || lin >= Constantes.BARREIRA_LINHAS) {
            return;
        }
        celulas[lin * Constantes.BARREIRA_COLUNAS + col] = false;
    }

    public int largura() {
        return Constantes.BARREIRA_COLUNAS * Constantes.BARREIRA_CELULA;
    }

    public int altura() {
        return Constantes.BARREIRA_LINHAS * Constantes.BARREIRA_CELULA;
    }
}
