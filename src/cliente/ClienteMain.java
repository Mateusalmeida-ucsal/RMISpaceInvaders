package cliente;

import comum.Constantes;
import comum.GameService;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Inicia o cliente: pede o IP do servidor, localiza o serviço no RMI Registry
 * (lookup), registra o callback e abre a janela do jogo.
 * Uso: java cliente.ClienteMain [ip-do-servidor]
 */
public class ClienteMain {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // segue com o visual padrão
        }

        String padrao = args.length > 0 ? args[0] : "localhost";
        String host = JOptionPane.showInputDialog(null, "IP do servidor:", padrao);
        if (host == null || host.trim().isEmpty()) {
            return;
        }
        host = host.trim();

        ClienteCallbackImpl callback = null;
        try {
            // O servidor chama o cliente de volta (callback), então o cliente precisa
            // anunciar o IP certo da própria máquina. Isso deve vir ANTES de exportar o callback.
            System.setProperty("java.rmi.server.hostname", descobrirIpLocal(host));

            Registry registry = LocateRegistry.getRegistry(host, Constantes.PORTA_REGISTRY);
            GameService servico = (GameService) registry.lookup(Constantes.NOME_SERVICO);

            callback = new ClienteCallbackImpl();
            int meuId = servico.conectarJogador(callback);
            if (meuId == -1) {
                JOptionPane.showMessageDialog(null,
                        "Não foi possível entrar: partida em andamento ou sem vagas.",
                        "Space Invaders RMI", JOptionPane.WARNING_MESSAGE);
                UnicastRemoteObject.unexportObject(callback, true);
                return;
            }

            final ClienteCallbackImpl cb = callback;
            SwingUtilities.invokeLater(() -> abrirJanela(servico, cb, meuId));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Não foi possível conectar ao servidor em " + host + ":" + Constantes.PORTA_REGISTRY
                            + "\n\nVerifique se o servidor está rodando, se o IP está correto\n"
                            + "e se o firewall permite a porta 1099.\n\nDetalhe: " + e,
                    "Space Invaders RMI", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private static void abrirJanela(GameService servico, ClienteCallbackImpl callback, int meuId) {
        TelaJogo tela = new TelaJogo(servico, callback, meuId);

        JFrame janela = new JFrame("Space Invaders RMI - Jogador " + meuId
                + (meuId == 1 ? " (verde)" : " (ciano)"));
        janela.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        janela.add(tela);
        janela.pack();
        janela.setResizable(false);
        janela.setLocationRelativeTo(null);
        janela.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                tela.encerrar();
                try {
                    UnicastRemoteObject.unexportObject(callback, true);
                } catch (Exception ex) {
                    // ignorar
                }
                System.exit(0);
            }
        });
        janela.setVisible(true);
        tela.requestFocusInWindow();
        tela.iniciarEnvio();
    }

    /** Descobre qual IP local é usado para falar com o servidor (sem enviar nada na rede). */
    private static String descobrirIpLocal(String host) {
        try (DatagramSocket s = new DatagramSocket()) {
            s.connect(InetAddress.getByName(host), Constantes.PORTA_REGISTRY);
            return s.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
