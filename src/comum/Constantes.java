package comum;

public final class Constantes {

    private Constantes() { }

    public static final String NOME_SERVICO = "SpaceInvadersService";
    public static final int PORTA_REGISTRY = 1099;

    public static final int LARGURA = 800;
    public static final int ALTURA = 600;
    public static final int TICK_MS = 33; // ~30 atualizações por segundo

    public static final int NAVE_LARGURA = 40;
    public static final int NAVE_ALTURA = 20;
    public static final int NAVE_Y = 540;
    public static final int NAVE_VELOCIDADE = 8;
    public static final int VIDAS_INICIAIS = 3;
    public static final int COOLDOWN_TIRO_MS = 400;
    public static final int INVULNERAVEL_TICKS = 60; // ~2s depois de ser atingido

    public static final int INV_COLUNAS = 11;
    public static final int INV_LINHAS = 5;
    public static final int INV_LARGURA = 32;
    public static final int INV_ALTURA = 24;
    public static final int INV_ESPACO_X = 48;
    public static final int INV_ESPACO_Y = 36;
    public static final int INV_PASSO_X = 10;
    public static final int INV_PASSO_Y = 20;
    public static final int INV_INICIO_X = 40;
    public static final int INV_INICIO_Y = 60;

    public static final int BARREIRA_QTD = 4;
    public static final int BARREIRA_COLUNAS = 22;
    public static final int BARREIRA_LINHAS = 16;
    public static final int BARREIRA_CELULA = 3;   // tamanho de cada "pixel" da barreira
    public static final int BARREIRA_Y = 450;

    public static final int TIRO_LARGURA = 4;
    public static final int TIRO_ALTURA = 12;
    public static final int TIRO_JOGADOR_VEL = 12;
    public static final int TIRO_INVASOR_VEL = 6;
    public static final int MAX_TIROS_INVASOR = 3;
}
