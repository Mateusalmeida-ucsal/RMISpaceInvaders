package cliente;

import comum.ClientCallback;
import comum.EstadoJogo;
import comum.EstadoPartida;
import comum.Evento;

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

    /**
     * Toca os efeitos dos eventos recebidos e controla a trilha: ela começa quando a
     * partida passa a EM_ANDAMENTO e para quando a partida deixa de estar em andamento.
     */
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
