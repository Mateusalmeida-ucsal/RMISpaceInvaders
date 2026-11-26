# Space Invaders

## 1. Baixar o projeto

```
git clone https://github.com/Mateusalmeida-ucsal/RMISpaceInvaders.git
cd RMISpaceInvaders
```

## 2. Compilar

**CMD ou PowerShell**
```
mkdir out
javac -encoding UTF-8 -d out src\comum\*.java src\servidor\*.java src\cliente\*.java src\teste\*.java
```

Se não aparecer nenhuma mensagem de erro, deu certo.
Avisos amarelos não impedem a compilação. Só erros impedem.



## 3. Jogar em dois computadores 

Um computador será o servidor. O outro será só cliente.

### 1: Ligar os dois na mesma rede
Os dois computadores precisam estar na mesma rede.

### Passo 2: Descobrir o IP do computador servidor
No computador que vai rodar o servidor, abra o terminal e digite:

```
ipconfig
```
Procure o IPv4.

### Passo 3: Liberar o firewall (nos DOIS computadores)
O RMI usa a porta 1099 e também portas dinâmicas. O servidor chama o cliente de volt, então os dois lados precisam aceitar conexões.

Na primeira vez que você rodar o servidor ou o cliente, o Windows abre uma janela *"O Firewall do Windows bloqueou alguns recursos..."*. Marque Redes privadas e clique em Permitir acesso. Faça isso nos dois computadores.

**Se a janela não apareceu ou você clicou em "Cancelar" sem querer:** abra o **PowerShell como Administrador** e rode (ajuste o caminho do seu `java.exe`, descubra com `where java`):

```
netsh advfirewall firewall add rule name="Java RMI" dir=in action=allow program="C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot\bin\java.exe" enable=yes
```

### 4: Iniciar o servidor com o IP
No computador servidor, **informando o IP do passo 2**:

```
java -cp out servidor.ServidorMain (ip)
```

### 5: Iniciar os clientes
- **No computador do servidor** (jogador 1): `java -cp out cliente.ClienteMain` e digite o próprio IP (ou `localhost`).
- **No outro computador** (jogador 2): copie a pasta do projeto (ou clone de novo), compile (seção 3) e rode `java -cp out cliente.ClienteMain`. Na janela, digite o **IP do servidor** (ex.: `192.168.0.15`).

> O cliente descobre sozinho o IP dele e o informa ao RMI, então não precisa de configuração extra.

Quando os dois estiverem conectados, o jogo começa.

---

## 6: Controles

| Tecla | Ação |
|-------|------|
| `←` `→` ou `A` `D` | Mover a nave |
| `Espaço` | Atirar (há um pequeno intervalo entre tiros) |
| `Enter` | Jogar novamente, depois do GAME OVER (os dois jogadores precisam pedir) |
| `M` | Liga/desliga todo o som |

---

## 7. Jogar em um único computador (teste rápido)

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
