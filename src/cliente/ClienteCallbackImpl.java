package cliente;

import comum.ClientCallback;
import comum.EstadoJogo;

import javax.swing.JComponent;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Objeto remoto exportado pelo cliente. O servidor chama atualizarEstado()
 * ~30 vezes por segundo; aqui só guardamos o último snapshot e pedimos
 * para a tela redesenhar. Nenhuma regra do jogo roda no cliente.
 */
public class ClienteCallbackImpl extends UnicastRemoteObject implements ClientCallback {

    private static final long serialVersionUID = 1L;

    private volatile EstadoJogo ultimo;
    private volatile JComponent tela;

    public ClienteCallbackImpl() throws RemoteException {
        super();
    }

    @Override
    public void atualizarEstado(EstadoJogo snapshot) throws RemoteException {
        ultimo = snapshot;
        JComponent t = tela;
        if (t != null) {
            t.repaint(); // repaint() é seguro para chamar de qualquer thread
        }
    }

    public EstadoJogo getUltimo() {
        return ultimo;
    }

    public void setTela(JComponent tela) {
        this.tela = tela;
    }
}
