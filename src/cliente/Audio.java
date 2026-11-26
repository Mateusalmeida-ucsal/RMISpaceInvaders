package cliente;

import comum.Evento;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;

public final class Audio {

    static final double GANHO_MUSICA_DB = -9;
    static final double GANHO_TIRO_DB = -30;
    static final double GANHO_INVASOR_MORTO_DB = -16;
    static final double GANHO_EXPLOSAO_DB = -13;

    private static final long INTERVALO_MIN_MS = 50;

    private static final String PASTA_SONS = "sons";

    private static class Som {
        final Clip clip;
        long ultimaExecucao = 0;

        Som(Clip clip) {
            this.clip = clip;
        }
    }

    static class Pcm {
        final AudioFormat formato;
        final byte[] dados;

        Pcm(AudioFormat formato, byte[] dados) {
            this.formato = formato;
            this.dados = dados;
        }
    }

    private static Som musica;
    private static Som tiro;
    private static Som invasorMorto;
    private static Som explosao;

    private static boolean ativo = false;
    private static boolean mudo = false;
    private static boolean musicaDeveTocar = false;

    private Audio() { }

    public static synchronized void iniciar() {
        try {
            tiro = carregar("shoot.wav", GANHO_TIRO_DB);
            invasorMorto = carregar("invaderkilled.wav", GANHO_INVASOR_MORTO_DB);
            explosao = carregar("explosion.wav", GANHO_EXPLOSAO_DB);
            musica = carregar("soundtrack.wav", GANHO_MUSICA_DB);
            ativo = true;
        } catch (Throwable e) {
            ativo = false;
            System.err.println("[audio] Som desativado: " + e);
        }
    }

    public static synchronized void tocar(Evento evento) {
        if (!ativo || mudo) {
            return;
        }
        switch (evento) {
            case TIRO_JOGADOR:
            case TIRO_INVASOR:
                executar(tiro);
                break;
            case INVASOR_ELIMINADO:
                executar(invasorMorto);
                break;
            case JOGADOR_ATINGIDO:
                executar(explosao);
                break;
            default:
                break;
        }
    }

    public static synchronized void iniciarMusica() {
        musicaDeveTocar = true;
        if (!ativo || mudo) {
            return;
        }
        musica.clip.stop();
        musica.clip.setFramePosition(0);
        musica.clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public static synchronized void pararMusica() {
        musicaDeveTocar = false;
        if (ativo) {
            musica.clip.stop();
        }
    }

    /** Liga/desliga todo o som (tecla M). */
    public static synchronized void alternarMudo() {
        mudo = !mudo;
        if (!ativo) {
            return;
        }
        if (mudo) {
            musica.clip.stop();
            tiro.clip.stop();
            invasorMorto.clip.stop();
            explosao.clip.stop();
        } else if (musicaDeveTocar) {
            musica.clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public static synchronized boolean isMudo() {
        return mudo;
    }

    public static synchronized void fechar() {
        if (!ativo) {
            return;
        }
        ativo = false;
        for (Som s : new Som[]{musica, tiro, invasorMorto, explosao}) {
            if (s != null) {
                s.clip.stop();
                s.clip.close();
            }
        }
    }

    private static void executar(Som s) {
        long agora = System.currentTimeMillis();
        if (s == null || agora - s.ultimaExecucao < INTERVALO_MIN_MS) {
            return;
        }
        s.ultimaExecucao = agora;
        s.clip.stop();
        s.clip.setFramePosition(0);
        s.clip.start();
    }

    private static Som carregar(String nomeArquivo, double ganhoDb) throws Exception {
        File arquivo = new File(PASTA_SONS, nomeArquivo);
        if (!arquivo.isFile()) {
            throw new IllegalStateException("arquivo não encontrado: " + arquivo.getAbsolutePath()
                    + " (execute o jogo a partir da pasta do projeto)");
        }
        Pcm pcm = lerPcm(arquivo, ganhoDb);
        Clip clip = AudioSystem.getClip();
        clip.open(pcm.formato, pcm.dados, 0, pcm.dados.length);
        return new Som(clip);
    }

    static Pcm lerPcm(File arquivo, double ganhoDb) throws Exception {
        if (ganhoDb > 0) {
            ganhoDb = 0;
        }
        try (AudioInputStream original = AudioSystem.getAudioInputStream(arquivo)) {
            AudioFormat o = original.getFormat();
            AudioFormat alvo = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    o.getSampleRate(), 16, o.getChannels(), o.getChannels() * 2,
                    o.getSampleRate(), false);
            try (AudioInputStream convertido = AudioSystem.getAudioInputStream(alvo, original)) {
                byte[] dados = convertido.readAllBytes();
                aplicarGanho(dados, ganhoDb);
                return new Pcm(alvo, dados);
            }
        }
    }

    private static void aplicarGanho(byte[] dados, double ganhoDb) {
        double fator = Math.pow(10.0, ganhoDb / 20.0);
        for (int i = 0; i + 1 < dados.length; i += 2) {
            int amostra = (short) ((dados[i] & 0xFF) | (dados[i + 1] << 8));
            int nova = (int) Math.round(amostra * fator);
            nova = Math.max(-32768, Math.min(32767, nova));
            dados[i] = (byte) (nova & 0xFF);
            dados[i + 1] = (byte) ((nova >> 8) & 0xFF);
        }
    }
}
