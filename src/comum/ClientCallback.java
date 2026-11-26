package comum;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCallback extends Remote {

    void atualizarEstado(EstadoJogo snapshot) throws RemoteException;
}
