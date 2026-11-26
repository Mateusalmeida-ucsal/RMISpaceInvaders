package comum;

import java.io.Serializable;

/**
 * Barreira (bunker). É uma grade de "pixels" (células) que pode ser destruída
 * aos poucos por tiros e pelos invasores, como no jogo original.
 * As células ficam em um vetor simples: índice = linha * COLUNAS + coluna.
 */
public class Barreira implements Serializable {
    private static final long serialVersionUID = 1L;

    // Formato do bunker: 22 colunas x 16 linhas ('X' = pedaço de barreira)
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

    /** Cria uma barreira inteira na posição (x, y). */
    public Barreira(int x, int y) {
        this.x = x;
        this.y = y;
        for (int lin = 0; lin < Constantes.BARREIRA_LINHAS; lin++) {
            for (int col = 0; col < Constantes.BARREIRA_COLUNAS; col++) {
                celulas[lin * Constantes.BARREIRA_COLUNAS + col] = FORMA[lin].charAt(col) == 'X';
            }
        }
    }

    /** Cópia, usada para montar o snapshot. */
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
