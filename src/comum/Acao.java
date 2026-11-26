package comum;

/**
 * Ações que um jogador pode enviar ao servidor.
 * Enum é serializável por padrão, então trafega pelo RMI sem configuração extra.
 */
public enum Acao {
    MOVER_ESQUERDA,
    MOVER_DIREITA,
    ATIRAR
}
