package teste;

import comum.Acao;
import comum.ClientCallback;
import comum.Constantes;
import comum.EstadoJogo;
import comum.GameService;
import comum.Invasor;
import comum.Jogador;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
public class ClienteTeste {

    static class Bot extends UnicastRemoteObject implements ClientCallback {
        volatile EstadoJogo ultimo;
        int id;

        Bot() throws RemoteException {
            super();
        }

        @Override
        public void atualizarEstado(EstadoJogo snapshot) throws RemoteException {
            ultimo = snapshot;
        }

        Jogador eu() {
            EstadoJogo e = ultimo;
            if (e == null) return null;
            for (Jogador j : e.jogadores) {
                if (j.id == id) return j;
            }
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        boolean testarDesconexao = args.length > 1 && args[1].equals("desconectar");

        Registry reg = LocateRegistry.getRegistry(host, Constantes.PORTA_REGISTRY);
        GameService svc = (GameService) reg.lookup(Constantes.NOME_SERVICO);

        Bot b1 = new Bot();
        Bot b2 = new Bot();
        b1.id = svc.conectarJogador(b1);
        b2.id = svc.conectarJogador(b2);
        System.out.println("IDs recebidos: " + b1.id + " e " + b2.id);
        Bot b3 = new Bot();
        System.out.println("Terceiro jogador recebeu id: " + svc.conectarJogador(b3) + " (esperado -1)");

        long inicio = System.currentTimeMillis();
        long ultimoPrint = 0;
        boolean desconectou = false;
        while (System.currentTimeMillis() - inicio < 15000) {
            for (Bot b : new Bot[]{b1, b2}) {
                if (desconectou && b == b2) continue;
                jogar(svc, b);
            }
            long t = System.currentTimeMillis() - inicio;
            if (testarDesconexao && !desconectou && t > 6000) {
                svc.desconectarJogador(b2.id);
                desconectou = true;
                System.out.println(">>> Jogador 2 desconectou");
            }
            if (t - ultimoPrint >= 1000) {
                ultimoPrint = t;
                imprimir(b1.ultimo);
            }
            Thread.sleep(30);
        }
        imprimir(b1.ultimo);
        System.exit(0);
    }

    private static void jogar(GameService svc, Bot b) throws RemoteException {
        Jogador eu = b.eu();
        EstadoJogo e = b.ultimo;
        if (eu == null || e == null || e.invasores.isEmpty()) return;
        Invasor alvo = e.invasores.get(0);
        for (Invasor i : e.invasores) {
            if (i.y > alvo.y) alvo = i;
        }
        int centroAlvo = alvo.x + Constantes.INV_LARGURA / 2;
        int centroEu = eu.x + Constantes.NAVE_LARGURA / 2;
        if (centroAlvo < centroEu - 8) svc.enviarJogada(b.id, Acao.MOVER_ESQUERDA);
        else if (centroAlvo > centroEu + 8) svc.enviarJogada(b.id, Acao.MOVER_DIREITA);
        svc.enviarJogada(b.id, Acao.ATIRAR);
    }

    private static void imprimir(EstadoJogo e) {
        if (e == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append(e.estadoPartida).append(" nivel=").append(e.nivel)
          .append(" invasores=").append(e.invasores.size())
          .append(" tiros=").append(e.projeteis.size());
        for (Jogador j : e.jogadores) {
            sb.append(" | J").append(j.id).append(" x=").append(j.x)
              .append(" vidas=").append(j.vidas).append(" pts=").append(j.pontos);
        }
        if (!e.mensagem.isEmpty()) sb.append(" | ").append(e.mensagem);
        System.out.println(sb);
    }
}
