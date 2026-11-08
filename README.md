# Nivek Utility (Fabric 1.21.1)

Mod de utilidades para Survival: Tree Capitator, Vein Miner, mineração 3x3, organizador de
inventário/baús, agricultura, waypoints, HUD, tochas, assistente de crafting e alertas de pesca/comida.

## Como compilar (GitHub Actions)
1. Crie um repositório no GitHub e envie TODOS os arquivos desta pasta (inclusive `.github`).
2. Abra a aba **Actions** > workflow **Build Nivek Utility** (roda sozinho a cada push; ou "Run workflow").
3. Quando terminar, baixe o artefato **nivek-utility-jar** (contém `nivek-utility-1.0.0.jar`).

Ou localmente, com Java 21 e Gradle 8.10+: `gradle build` -> `build/libs/nivek-utility-1.0.0.jar`.

## Como instalar
1. Instale o Fabric Loader para Minecraft 1.21.1.
2. Coloque o **Fabric API** (versão 1.21.1) e o `nivek-utility-1.0.0.jar` na pasta `mods`.
3. Para multiplayer, o mod precisa estar no servidor também (os recursos rodam no servidor).

## Teclas padrão (mude em Controles)
N abrir a tela | V Vein Miner (segurar) | B alternar 3x3 | G tocha | H comer | K crafting | J waypoints

## Configs
- Cliente: `config/nivekutility.json` (waypoints em `nivekutility-waypoints.json`)
- Servidor: `config/nivekutility-server.json` (limites máximos e permissão de cada recurso)
