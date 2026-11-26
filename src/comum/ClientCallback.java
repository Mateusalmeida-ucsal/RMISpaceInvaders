package comum;

import java.rmi.Remote;
import java.rmi.RemoteException;

/** Interface remota implementada pelo CLIENTE; o servidor chama para empurrar o estado. */
public interface ClientCallback extends Remote {

    void atualizarEstado(EstadoJogo snapshot) throws RemoteException;
}
