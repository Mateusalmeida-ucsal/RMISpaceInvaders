# Space Invaders Multijogador com Java RMI

Jogo Space Invaders para **2 jogadores em rede**, desenvolvido para a disciplina de Sistemas Distribuídos (UCSal). A comunicação entre clientes e servidor é feita com **Java RMI**. O servidor guarda todo o estado do jogo, valida as jogadas e envia o estado aos clientes por *callback*.

```
src/
├── comum/      Interfaces remotas e objetos trafegados (GameService, ClientCallback, EstadoJogo...)
├── servidor/   Servidor (GameServerImpl = lógica + game loop, ServidorMain = inicialização)
├── cliente/    Interface gráfica Swing e áudio (ClienteMain, TelaJogo, ClienteCallbackImpl, Audio)
└── teste/      Cliente de console com bots, para testar o servidor sem interface
sons/           Efeitos e trilha (.wav) - precisa estar na raiz do projeto, ao lado de src/
```

---

## 1. O que você precisa ter instalado

Apenas o **JDK (Java Development Kit) 11 ou superior**. O projeto foi testado com Java 21. Não usa nenhuma biblioteca externa.

**Como saber se já tem:** abra o terminal (Windows: tecla `Win`, digite `cmd`, Enter) e rode:

```
java -version
javac -version
```

Os **dois** comandos precisam mostrar uma versão. Se `javac` der *"não é reconhecido como comando"*, você tem só o JRE ou o Java não está no PATH. Instale o JDK:

1. Acesse **https://adoptium.net** e baixe o **Temurin JDK 21** (instalador `.msi` no Windows).
2. Na instalação, marque a opção **"Add to PATH"** (adicionar ao PATH) e a opção **"Set JAVA_HOME"**.
3. **Feche e abra o terminal de novo** e repita `java -version` e `javac -version`.

> Linux (Ubuntu/Debian): `sudo apt install default-jdk`  |  macOS: `brew install openjdk`

---

## 2. Baixar o projeto

**Opção A, com Git:**
```
git clone https://github.com/Mateusalmeida-ucsal/RMISpaceInvaders.git
cd RMISpaceInvaders
```

**Opção B, sem Git:** na página do repositório no GitHub, clique em **Code > Download ZIP**, extraia a pasta e abra o terminal dentro dela.

Confirme que está na pasta certa: ela deve conter as pastas `src` e `sons`.

> A pasta `sons` tem a trilha sonora (~13 MB) e os efeitos. O jogo carrega os sons a partir dela, então **execute sempre os comandos de dentro da pasta do projeto**. Se a pasta não for encontrada, o jogo abre normalmente, só que sem som.

---

## 3. Compilar

Dentro da pasta do projeto (a que contém `src`):

**Windows (CMD ou PowerShell):**
```
mkdir out
javac -encoding UTF-8 -d out src\comum\*.java src\servidor\*.java src\cliente\*.java src\teste\*.java
```

**Linux / macOS:**
```
mkdir -p out
javac -encoding UTF-8 -d out src/comum/*.java src/servidor/*.java src/cliente/*.java src/teste/*.java
```

Se não aparecer nenhuma mensagem de erro, deu certo. Os arquivos compilados ficam na pasta `out`.

> O `-encoding UTF-8` é importante: sem ele, os acentos do jogo (como "CONEXÃO") podem sair quebrados.
> Avisos amarelos (*warning*) não impedem a compilação. Só **erros** impedem.

---

## 4. Jogar em um único computador (teste rápido)

Abra **3 terminais**, todos dentro da pasta do projeto.

**Terminal 1, servidor:**
```
java -cp out servidor.ServidorMain
```
Deve aparecer: `'SpaceInvadersService' registrado. Aguardando jogadores...`

**Terminal 2, jogador 1:**
```
java -cp out cliente.ClienteMain
```
Na janela que abrir, deixe `localhost` e clique em **OK**.

**Terminal 3, jogador 2:** repita o mesmo comando do Terminal 2.

Quando o segundo jogador entrar, a partida começa sozinha.

---

## 5. Jogar em dois computadores (cenário da sala de aula)

Um computador será o **servidor** (e também pode ter um jogador). O outro será só cliente.

### Passo 1: Ligar os dois na mesma rede
Os dois computadores precisam estar na **mesma rede** (mesmo Wi-Fi ou mesmo cabo/switch).

> **Atenção:** algumas redes (Wi-Fi da faculdade, por exemplo) bloqueiam a comunicação entre aparelhos ("isolamento de clientes"). Se nada conectar, use o **roteador próprio**, um cabo de rede entre os dois PCs, ou o **roteamento de internet (hotspot) do celular**, com os dois computadores conectados nele.

### Passo 2: Descobrir o IP do computador servidor
No computador que vai rodar o servidor, abra o terminal e digite:

```
ipconfig
```
Procure **Endereço IPv4** na rede em uso (Wi-Fi ou Ethernet). Será algo como `192.168.0.15` ou `192.168.1.23`. Anote.

> Linux/macOS: `ip a` ou `ifconfig`.

### Passo 3: Liberar o firewall (nos DOIS computadores)
O RMI usa a porta **1099** e também portas dinâmicas. O servidor chama o cliente de volta (*callback*), então **os dois lados precisam aceitar conexões**.

**O jeito mais fácil:** na primeira vez que você rodar o servidor ou o cliente, o Windows abre uma janela *"O Firewall do Windows bloqueou alguns recursos..."*. Marque **Redes privadas** e clique em **Permitir acesso**. Faça isso nos dois computadores.

**Se a janela não apareceu ou você clicou em "Cancelar" sem querer:** abra o **PowerShell como Administrador** e rode (ajuste o caminho do seu `java.exe`, descubra com `where java`):

```
netsh advfirewall firewall add rule name="Java RMI" dir=in action=allow program="C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot\bin\java.exe" enable=yes
```

**Último recurso, só para testar:** desative temporariamente o firewall do perfil de rede em uso (*Segurança do Windows > Firewall e proteção de rede*) e reative depois.

### Passo 4: Iniciar o servidor com o IP
No computador servidor, **informando o IP do passo 2**:

```
java -cp out servidor.ServidorMain 192.168.0.15
```
(troque pelo seu IP). Esse argumento é essencial: sem ele, o RMI pode anunciar um endereço que o outro computador não consegue acessar.

### Passo 5: Iniciar os clientes
- **No computador do servidor** (jogador 1): `java -cp out cliente.ClienteMain` e digite o próprio IP (ou `localhost`).
- **No outro computador** (jogador 2): copie a pasta do projeto (ou clone de novo), compile (seção 3) e rode `java -cp out cliente.ClienteMain`. Na janela, digite o **IP do servidor** (ex.: `192.168.0.15`).

> O cliente descobre sozinho o IP dele e o informa ao RMI, então não precisa de configuração extra.

Quando os dois estiverem conectados, o jogo começa.

---

## 6. Usando o IntelliJ IDEA

1. **File > Open** e escolha a pasta do projeto. Clique com o botão direito em `src` e escolha **Mark Directory as > Sources Root**.
2. Configure o **SDK**: **File > Project Structure > Project > SDK** (JDK 11 ou superior).
3. Abra `servidor/ServidorMain.java`, clique no ícone ▶ ao lado do `main` e escolha **Run**.
   - Para passar o IP (rede local): **Run > Edit Configurations**, selecione `ServidorMain` e preencha **Program arguments** com o IP, por exemplo `192.168.0.15`.
4. Abra `cliente/ClienteMain.java` e rode da mesma forma.
5. Para abrir **dois clientes na mesma máquina** pelo IntelliJ: em **Edit Configurations**, selecione `ClienteMain`, clique em **Modify options** e ative **Allow multiple instances**. Depois rode duas vezes.

---

## 7. Controles

| Tecla | Ação |
|-------|------|
| `←` `→` ou `A` `D` | Mover a nave |
| `Espaço` | Atirar (há um pequeno intervalo entre tiros) |
| `Enter` | Jogar novamente, depois do GAME OVER (os dois jogadores precisam pedir) |
| `M` | Liga/desliga todo o som |

**Regras:** os dois jogadores cooperam contra os invasores, protegidos por 4 barreiras que se desgastam com os tiros (de qualquer lado) e são destruídas pelos invasores que encostam nelas. As barreiras voltam inteiras a cada nova onda. Cada jogador tem 3 vidas e pontuação própria (lula = 30, caranguejo = 20, polvo = 10). O jogo termina quando as duas naves são destruídas ou quando os invasores chegam ao chão. Ao eliminar todos os invasores, começa uma nova onda, mais rápida.

---

## 8. Som

- **Trilha:** começa quando a partida inicia, toca em loop e em volume baixo, e para no Game Over.
- **Efeitos:** tiro (de jogador ou de invasor), invasor eliminado e nave atingida. É o servidor que avisa quando cada um acontece, então os dois jogadores ouvem os mesmos sons.
- **Volume:** os efeitos originais estão gravados no volume máximo, por isso o jogo os reduz ao carregar. Para ajustar, edite as constantes `GANHO_*_DB` no início de `cliente/Audio.java` (em decibéis; valores negativos diminuem; o código nunca amplifica acima do original).
- Se o computador não tiver placa de som, o jogo funciona sem áudio e mostra apenas um aviso no terminal.

---

## 9. Problemas comuns

| Sintoma | Causa provável e solução |
|---------|--------------------------|
| `'javac' não é reconhecido como comando` | JDK não instalado ou fora do PATH. Veja a seção 1 e reabra o terminal. |
| Janela *"Não foi possível conectar ao servidor"* | Servidor não está rodando, IP digitado errado ou firewall bloqueando a porta 1099. |
| `Connection refused to host: 127.0.0.1` | O servidor foi iniciado **sem o IP** em uma rede com 2 computadores. Reinicie com `ServidorMain SEU_IP`. |
| Conecta, mas o jogo fica em "CONECTANDO..." ou não atualiza | O servidor não consegue chamar o cliente de volta. Libere o firewall **no computador do cliente** também (seção 5, passo 3). |
| `Port already in use` / porta 1099 em uso | Já existe um servidor aberto. Feche o terminal antigo ou encerre o processo `java`. |
| "Partida em andamento ou sem vagas" | Já há 2 jogadores ou uma partida rolando. Feche um cliente (ou reinicie o servidor) e tente de novo. |
| Aparece "CONEXÃO COM O SERVIDOR PERDIDA" | O servidor caiu ou a rede falhou. Feche a janela e conecte de novo. |
| Acentos quebrados no terminal do servidor (ex.: `conex?o`) | Só aparência do console do Windows. Não afeta o jogo. Pode usar `chcp 65001` antes de iniciar. |
| Sem som | Verifique se rodou os comandos de dentro da pasta do projeto (a pasta `sons` precisa estar ao lado de `src`), se o som do Windows está ligado e se não apertou `M`. Veja também o aviso `[audio]` no terminal. |
| Fechar um cliente no meio da partida | Comportamento esperado: a partida é encerrada e o outro jogador é avisado. |

---

## 10. Testar o servidor sem interface gráfica (opcional)

O pacote `teste` traz dois bots que jogam sozinhos por 15 segundos e imprimem o estado no console:

```
java -cp out servidor.ServidorMain
java -cp out teste.ClienteTeste localhost
java -cp out teste.ClienteTeste localhost desconectar
```

O terceiro comando também testa o tratamento de desconexão: o jogador 2 sai no meio da partida e o jogador 1 recebe o aviso.

---

## Autores

Mateus Almeida Bonfim e Lucas Teles de Andrade, Engenharia de Software, UCSal, disciplina de Sistemas Distribuídos.
