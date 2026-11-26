# Space Invaders RMI

## Compilar (na pasta do projeto)
Windows (CMD/PowerShell):
    mkdir out
    javac -encoding UTF-8 -d out src\comum\*.java src\servidor\*.java src\cliente\*.java src\teste\*.java

## Executar
1) Servidor (cria o RMI Registry na porta 1099 sozinho):
    java -cp out servidor.ServidorMain
    (em rede local: java -cp out servidor.ServidorMain IP_DA_MAQUINA_SERVIDORA)

2) Cada jogador, em sua maquina (ou em dois terminais na mesma maquina):
    java -cp out cliente.ClienteMain
    -> digite o IP do servidor (localhost se for a mesma maquina)

## Controles
    Setas ou A/D : mover      ESPACO : atirar      ENTER : jogar novamente (apos o Game Over)

## Teste sem interface (opcional)
    java -cp out teste.ClienteTeste localhost
