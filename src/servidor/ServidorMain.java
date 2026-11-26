package servidor;

import comum.Constantes;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;

/**
 * Sobe o servidor.
 * Uso: java servidor.ServidorMain [ip-do-servidor]
 * Em rede local, passe o IP da máquina para os clientes conseguirem chamar de volta.
 */
public class ServidorMain {

    // referência estática para o Registry local não ser coletado pelo GC
    private static Registry registry;

    public static void main(String[] args) {
        try {
            if (args.length > 0) {
                System.setProperty("java.rmi.server.hostname", args[0]);
                System.out.println("[servidor] hostname RMI: " + args[0]);
            }
            // Timeouts nas chamadas de saída (callbacks): cliente travado não prende o servidor
            System.setProperty("sun.rmi.transport.tcp.readTimeout", "5000");
            System.setProperty("sun.rmi.transport.proxy.connectTimeout", "3000");

            try {
                registry = LocateRegistry.createRegistry(Constantes.PORTA_REGISTRY);
                System.out.println("[servidor] RMI Registry criado na porta " + Constantes.PORTA_REGISTRY);
            } catch (ExportException e) {
                registry = LocateRegistry.getRegistry(Constantes.PORTA_REGISTRY);
                System.out.println("[servidor] RMI Registry já existia; reutilizando.");
            }

            GameServerImpl servidor = new GameServerImpl();
            registry.rebind(Constantes.NOME_SERVICO, servidor);
            System.out.println("[servidor] '" + Constantes.NOME_SERVICO + "' registrado. Aguardando jogadores...");
        } catch (Exception e) {
            System.err.println("[servidor] Falha ao iniciar: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
