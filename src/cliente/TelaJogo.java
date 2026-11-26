package cliente;

import comum.Acao;
import comum.Barreira;
import comum.Constantes;
import comum.EstadoJogo;
import comum.EstadoPartida;
import comum.GameService;
import comum.Invasor;
import comum.Jogador;
import comum.Projetil;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;

public class TelaJogo extends JPanel {

    private static final long serialVersionUID = 1L;

    // ---------- Sprites (pixel art do arcade original) ----------
    private static final String[][][] INVASOR_PIXELS = {
        { // tipo 0: lula (30 pts)
            {"...XX...", "..XXXX..", ".XXXXXX.", "XX.XX.XX", "XXXXXXXX", "..X..X..", ".X.XX.X.", "X.X..X.X"},
            {"...XX...", "..XXXX..", ".XXXXXX.", "XX.XX.XX", "XXXXXXXX", ".X.XX.X.", "X......X", ".X....X."}
        },
        { // tipo 1: caranguejo (20 pts)
            {"..X.....X..", "...X...X...", "..XXXXXXX..", ".XX.XXX.XX.", "XXXXXXXXXXX", "X.XXXXXXX.X", "X.X.....X.X", "...XX.XX..."},
            {"..X.....X..", "X..X...X..X", "X.XXXXXXX.X", "XXX.XXX.XXX", "XXXXXXXXXXX", ".XXXXXXXXX.", "..X.....X..", ".X.......X."}
        },
        { // tipo 2: polvo (10 pts)
            {"....XXXX....", ".XXXXXXXXXX.", "XXXXXXXXXXXX", "XXX..XX..XXX", "XXXXXXXXXXXX", "...XX..XX...", "..XX.XX.XX..", "XX........XX"},
            {"....XXXX....", ".XXXXXXXXXX.", "XXXXXXXXXXXX", "XXX..XX..XXX", "XXXXXXXXXXXX", "..XXX..XXX..", ".XX..XX..XX.", "..XX....XX.."}
        }
    };
    private static final String[] CANHAO_PIXELS = {
        "......X......", ".....XXX.....", ".....XXX.....", ".XXXXXXXXXXX.",
        "XXXXXXXXXXXXX", "XXXXXXXXXXXXX", "XXXXXXXXXXXXX", "XXXXXXXXXXXXX"
    };

    private static final Color COR_J1 = new Color(0, 255, 0);
    private static final Color COR_J2 = new Color(0, 220, 255);
    private static final Color COR_TEXTO = Color.WHITE;
    private static final Color COR_BARREIRA = Color.WHITE;

    private static final BufferedImage[][] INVASORES = new BufferedImage[3][2];
    private static final BufferedImage CANHAO_J1 = criarSprite(CANHAO_PIXELS, COR_J1);
    private static final BufferedImage CANHAO_J2 = criarSprite(CANHAO_PIXELS, COR_J2);

    static {
        for (int tipo = 0; tipo < 3; tipo++) {
            for (int frame = 0; frame < 2; frame++) {
                INVASORES[tipo][frame] = criarSprite(INVASOR_PIXELS[tipo][frame], Color.WHITE);
            }
        }
    }

    private static BufferedImage criarSprite(String[] linhas, Color cor) {
        BufferedImage img = new BufferedImage(linhas[0].length(), linhas.length, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < linhas.length; y++) {
            for (int x = 0; x < linhas[y].length(); x++) {
                if (linhas[y].charAt(x) == 'X') {
                    img.setRGB(x, y, cor.getRGB());
                }
            }
        }
        return img;
    }

    private final GameService servico;
    private final ClienteCallbackImpl callback;
    private final int meuId;

    private volatile boolean esquerda, direita, atirar, reiniciar;
    private volatile boolean conexaoPerdida = false;
    private volatile boolean rodando = true;

    public TelaJogo(GameService servico, ClienteCallbackImpl callback, int meuId) {
        this.servico = servico;
        this.callback = callback;
        this.meuId = meuId;

        setPreferredSize(new Dimension(Constantes.LARGURA, Constantes.ALTURA));
        setBackground(Color.BLACK);
        setFocusable(true);
        callback.setTela(this);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                mudarTecla(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                mudarTecla(e.getKeyCode(), false);
            }
        });
    }

    private void mudarTecla(int codigo, boolean pressionada) {
        switch (codigo) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                esquerda = pressionada;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                direita = pressionada;
                break;
            case KeyEvent.VK_SPACE:
                atirar = pressionada;
                break;
            case KeyEvent.VK_ENTER:
                if (pressionada) {
                    reiniciar = true;
                }
                break;
            case KeyEvent.VK_M:
                if (pressionada) {
                    Audio.alternarMudo();
                    repaint();
                }
                break;
            default:
                break;
        }
    }

    public void iniciarEnvio() {
        Thread t = new Thread(() -> {
            while (rodando) {
                try {
                    if (esquerda) {
                        servico.enviarJogada(meuId, Acao.MOVER_ESQUERDA);
                    }
                    if (direita) {
                        servico.enviarJogada(meuId, Acao.MOVER_DIREITA);
                    }
                    if (atirar) {
                        servico.enviarJogada(meuId, Acao.ATIRAR);
                    }
                    if (reiniciar) {
                        reiniciar = false;
                        servico.solicitarReinicio(meuId);
                    }
                } catch (RemoteException e) {
                    conexaoPerdida = true;
                    Audio.pararMusica();
                    repaint();
                    return;
                }
                try {
                    Thread.sleep(Constantes.TICK_MS);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "envio-jogadas");
        t.setDaemon(true);
        t.start();
    }

    public void encerrar() {
        rodando = false;
        try {
            servico.desconectarJogador(meuId);
        } catch (RemoteException e) {
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        desenhar((Graphics2D) g, getWidth(), getHeight());
    }

    void desenhar(Graphics2D g, int largura, int altura) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, largura, altura);
        g.scale(largura / (double) Constantes.LARGURA, altura / (double) Constantes.ALTURA);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));

        if (conexaoPerdida) {
            textoCentral(g, "CONEXÃO COM O SERVIDOR PERDIDA", 300, 24, Color.RED);
            textoCentral(g, "Feche a janela e conecte novamente", 340, 16, COR_TEXTO);
            return;
        }
        EstadoJogo e = callback.getUltimo();
        if (e == null) {
            textoCentral(g, "CONECTANDO...", 300, 24, COR_TEXTO);
            return;
        }

        Jogador eu = null;
        for (Jogador j : e.jogadores) {
            if (j.id == meuId) {
                eu = j;
            }
        }

        desenharHud(g, e);
        desenharElementos(g, e);
        desenharMensagens(g, e, eu);
    }

    private void desenharHud(Graphics2D g, EstadoJogo e) {
        Jogador j1 = buscar(e, 1);
        Jogador j2 = buscar(e, 2);

        // topo: SCORE<1>  NIVEL  SCORE<2>
        g.setColor(COR_J1);
        g.drawString("SCORE<1>" + (meuId == 1 ? " *" : ""), 20, 26);
        g.setColor(COR_TEXTO);
        g.drawString(String.format("%04d", j1 == null ? 0 : j1.pontos), 20, 48);

        g.setColor(COR_TEXTO);
        textoCentral(g, "NIVEL " + e.nivel, 26, 18, COR_TEXTO);

        g.setColor(COR_J2);
        String rotulo2 = "SCORE<2>" + (meuId == 2 ? " *" : "");
        FontMetrics fm = g.getFontMetrics();
        g.drawString(rotulo2, Constantes.LARGURA - 20 - fm.stringWidth(rotulo2), 26);
        g.setColor(COR_TEXTO);
        String pts2 = String.format("%04d", j2 == null ? 0 : j2.pontos);
        g.drawString(pts2, Constantes.LARGURA - 20 - fm.stringWidth(pts2), 48);

        if (Audio.isMudo()) {
            textoCentral(g, "SOM DESLIGADO (M)", 590, 14, Color.GRAY);
        }

        g.setColor(COR_J1);
        g.fillRect(0, 565, Constantes.LARGURA, 2);
        if (j1 != null) {
            g.setColor(COR_TEXTO);
            g.drawString(String.valueOf(j1.vidas), 20, 590);
            for (int i = 0; i < j1.vidas; i++) {
                g.drawImage(CANHAO_J1, 45 + i * 34, 574, 26, 16, null);
            }
        }
        if (j2 != null) {
            g.setColor(COR_TEXTO);
            g.drawString(String.valueOf(j2.vidas), Constantes.LARGURA - 190, 590);
            for (int i = 0; i < j2.vidas; i++) {
                g.drawImage(CANHAO_J2, Constantes.LARGURA - 165 + i * 34, 574, 26, 16, null);
            }
        }
    }

    private void desenharBarreiras(Graphics2D g, EstadoJogo e) {
        final int cel = Constantes.BARREIRA_CELULA;
        g.setColor(COR_BARREIRA);
        for (Barreira b : e.barreiras) {
            for (int lin = 0; lin < Constantes.BARREIRA_LINHAS; lin++) {
                int col = 0;
                while (col < Constantes.BARREIRA_COLUNAS) {
                    if (!b.viva(col, lin)) {
                        col++;
                        continue;
                    }
                    int inicio = col;
                    while (col < Constantes.BARREIRA_COLUNAS && b.viva(col, lin)) {
                        col++;
                    }
                    g.fillRect(b.x + inicio * cel, b.y + lin * cel, (col - inicio) * cel, cel);
                }
            }
        }
    }

    private void desenharElementos(Graphics2D g, EstadoJogo e) {
        desenharBarreiras(g, e);
        for (Invasor i : e.invasores) {
            int frame = Math.abs(i.x / Constantes.INV_PASSO_X) % 2;
            g.drawImage(INVASORES[i.tipo][frame], i.x, i.y, Constantes.INV_LARGURA, Constantes.INV_ALTURA, null);
        }
        // Naves
        for (Jogador j : e.jogadores) {
            if (!j.viva) {
                continue;
            }
            if (j.isInvulneravel() && (j.invulneravelTicks / 4) % 2 == 0) {
                continue; // pisca depois de ser atingido
            }
            BufferedImage sprite = (j.id == 1) ? CANHAO_J1 : CANHAO_J2;
            g.drawImage(sprite, j.x, j.y, Constantes.NAVE_LARGURA, Constantes.NAVE_ALTURA, null);
        }
        // Tiros
        for (Projetil p : e.projeteis) {
            if (!p.isDoJogador()) {
                g.setColor(Color.WHITE);
            } else {
                g.setColor(p.dono == 1 ? COR_J1 : COR_J2);
            }
            g.fillRect(p.x, p.y, Constantes.TIRO_LARGURA, Constantes.TIRO_ALTURA);
        }
    }

    private void desenharMensagens(Graphics2D g, EstadoJogo e, Jogador eu) {
        if (e.estadoPartida == EstadoPartida.AGUARDANDO_JOGADORES) {
            textoCentral(g, "AGUARDANDO OPONENTE...", 330, 24, COR_TEXTO);
            textoCentral(g, "Você é o Jogador " + meuId, 365, 16, meuId == 1 ? COR_J1 : COR_J2);
            textoCentral(g, "SETAS ou A/D: mover   ESPAÇO: atirar", 400, 14, COR_TEXTO);
        } else if (e.estadoPartida == EstadoPartida.ENCERRADA) {
            // fundo escuro para o texto não se misturar com os sprites
            int linhas = e.mensagem.split(" \\| ").length;
            g.setColor(new Color(0, 0, 0, 220));
            g.fillRect(100, 200, Constantes.LARGURA - 200, 150 + linhas * 26);
            textoCentral(g, "GAME OVER", 260, 48, COR_TEXTO);
            int y = 305;
            for (String parte : e.mensagem.split(" \\| ")) {
                textoCentral(g, parte.replace("FIM DE JOGO - ", ""), y, 16, COR_TEXTO);
                y += 26;
            }
            String acao = (eu != null && eu.pediuReinicio)
                    ? "AGUARDANDO O OUTRO JOGADOR..."
                    : "ENTER PARA JOGAR NOVAMENTE";
            textoCentral(g, acao, y + 20, 18, COR_J1);
        } else if (eu != null && !eu.viva) {
            textoCentral(g, "SUA NAVE FOI DESTRUÍDA", 320, 20, Color.RED);
            textoCentral(g, "Torça pelo seu parceiro!", 348, 14, COR_TEXTO);
        }
    }

    private static Jogador buscar(EstadoJogo e, int id) {
        for (Jogador j : e.jogadores) {
            if (j.id == id) {
                return j;
            }
        }
        return null;
    }

    private static void textoCentral(Graphics2D g, String texto, int y, int tamanho, Color cor) {
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, tamanho));
        g.setColor(cor);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(texto, (Constantes.LARGURA - fm.stringWidth(texto)) / 2, y);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
    }
}
