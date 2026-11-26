package comum;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameService extends Remote {
    int conectarJogador(ClientCallback callback) throws RemoteException;
    void enviarJogada(int idJogador, Acao acao) throws RemoteException;
    void solicitarReinicio(int idJogador) throws RemoteException;
    void desconectarJogador(int idJogador) throws RemoteException;
}
