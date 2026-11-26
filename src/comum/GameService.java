package comum;

import java.rmi.Remote;
import java.rmi.RemoteException;

/** Interface remota implementada pelo SERVIDOR; os clientes chamam via stub. */
public interface GameService extends Remote {

    /**
     * Registra o jogador e guarda o callback.
     * @return id do jogador (1 ou 2), ou -1 se não há vaga / partida em andamento.
     */
    int conectarJogador(ClientCallback callback) throws RemoteException;

    /** Envia uma ação. O servidor valida; ações inválidas são ignoradas. */
    void enviarJogada(int idJogador, Acao acao) throws RemoteException;

    /** Com a partida ENCERRADA, marca que o jogador quer jogar de novo. Reinicia quando os 2 pedirem. */
    void solicitarReinicio(int idJogador) throws RemoteException;

    /** Saída voluntária do jogador. */
    void desconectarJogador(int idJogador) throws RemoteException;
}
