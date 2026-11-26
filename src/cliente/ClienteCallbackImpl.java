package cliente;

import comum.ClientCallback;
import comum.EstadoJogo;
import comum.EstadoPartida;
import comum.Evento;

import javax.swing.JComponent;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClienteCallbackImpl extends UnicastRemoteObject implements ClientCallback {

    private static final long serialVersionUID = 1L;

    private volatile EstadoJogo ultimo;
    private volatile JComponent tela;
    private EstadoPartida estadoAnterior = null;

    public ClienteCallbackImpl() throws RemoteException {
        super();
    }

    @Override
    public void atualizarEstado(EstadoJogo snapshot) throws RemoteException {
        ultimo = snapshot;
        tratarAudio(snapshot);
        JComponent t = tela;
        if (t != null) {
            t.repaint(); // repaint() é seguro para chamar de qualquer thread
        }
    }

    private synchronized void tratarAudio(EstadoJogo snapshot) {
        for (Evento evento : snapshot.eventos) {
            Audio.tocar(evento);
        }
        boolean emAndamento = snapshot.estadoPartida == EstadoPartida.EM_ANDAMENTO;
        if (emAndamento && estadoAnterior != EstadoPartida.EM_ANDAMENTO) {
            Audio.iniciarMusica();
        } else if (!emAndamento && estadoAnterior == EstadoPartida.EM_ANDAMENTO) {
            Audio.pararMusica();
        }
        estadoAnterior = snapshot.estadoPartida;
    }

    public EstadoJogo getUltimo() {
        return ultimo;
    }

    public void setTela(JComponent tela) {
        this.tela = tela;
    }
}
