package comum;

/**
 * Acontecimentos do jogo que geram som. O servidor sabe quando eles ocorrem
 * e os envia no snapshot; cada cliente decide como (e se) reproduzir o áudio.
 */
public enum Evento {
    TIRO_JOGADOR,
    TIRO_INVASOR,
    INVASOR_ELIMINADO,
    JOGADOR_ATINGIDO
}
