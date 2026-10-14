# MSH vs SF — player Android (FBNeo / CPS-2)

App Android próprio para jogar **Marvel Super Heroes vs. Street Fighter** a partir do **seu** romset
(`mvs.*`, `mvse.*`, `mvsex.*`: é o set de **arcade CPS-2** do MAME/FBNeo, não de SNES).

Kotlin + [LibretroDroid](https://github.com/Swordfish90/LibretroDroid) (vídeo GL, áudio, save states) +
núcleo libretro **FBNeo** (que já traz as chaves de decriptação do CPS-2).

## O que tem
| Recurso | Onde |
|---|---|
| Touch multitouch: direcional 8 vias flutuante, 6 botões (LP MP HP / LK MK HK), COIN/START | `input/TouchControlsView.kt` |
| Dedo entre dois botões = aperta os dois (chord com 1 dedo) | idem |
| Macros: 3P, 3K, QCF+HP, DP+HP, HYPER (respeitam o lado do P1 com o botão `P1 →`) | `input/Macros.kt` |
| START insere moeda sozinho (CPS-2 exige crédito) | `input/InputRouter.kt` |
| Rewind (segurar REW): snapshots a 10/s, ~20 s de histórico, deltas XOR comprimidos | `emu/RewindBuffer.kt` |
| Save states: 4 slots, quick save/load na tela, retomada automática | `emu/StateStore.kt`, `GameActivity.kt` |
| Avanço rápido (FF), shaders (padrão/CRT/LCD) | menu de pausa |
| Controle físico e teclado | `GameActivity.dispatchKeyEvent`, `input/Btn.kt` |
| Mods: patches IPS, cheats, romsets modificados, hiscore | `mods/` |

Controle: X/Y/RB = LP/MP/HP, A/B/RT = LK/MK/HK, Select = moeda, Start = start, Guide = menu,
LT (segurar) = rewind, R3 = avanço rápido. Teclado: U I O / J K L, WASD ou setas, `5` moeda, Enter start.

## Como compilar (no seu PC, com Android Studio)
1. Abra a pasta no **Android Studio** (Ladybug ou mais novo). Ele baixa o SDK e o Gradle.
2. Rode `./scripts/fetch_core.sh` (baixa o núcleo FBNeo arm64 do buildbot do libretro e instala em
   `app/src/main/jniLibs/arm64-v8a/libfbneo_libretro_android.so`). Ou baixe manualmente
   `fbneo_libretro_android.so.zip` de <https://buildbot.libretro.com/nightly/android/latest/arm64-v8a/>
   e renomeie para `libfbneo_libretro_android.so`.
3. Conecte o celular (depuração USB) e clique em **Run**.
4. No app: **Importar ROM** e escolha o seu `.zip`. O app reconhece o set (arquivos `mvs.*`) e salva como
   `mshvsf.zip`, o nome que o FBNeo espera. Depois, **Jogar**.

## Mods
Em **Mods**, importe os arquivos e marque **Ativar** no tipo desejado:
- **IPS**: `.dat`/`.ips` vão para `system/fbneo/ips/mshvsf/`.
- **Cheats**: arquivos do pacote *FBNeo-cheats* vão para `system/fbneo/cheats/`.
- **Romset modificado**: vai para `system/fbneo/patched/mshvsf.zip` (mesmos nomes/tamanhos de arquivo do original).
- **hiscore.dat**: vai para `system/fbneo/`.

Ao abrir o jogo, o app liga as opções correspondentes do núcleo e reinicia o jogo uma vez.
As chaves das opções variam entre versões do FBNeo, então o app as encontra por nome em tempo de execução
(`ModManager.applyCoreOptions`). Se algum mod não "pegar", veja *Core Options* no RetroArch para o nome atual.

## Pontos para conferir no primeiro teste (não consegui compilar/rodar aqui)
Este projeto foi escrito sem acesso ao Android SDK, então **nunca foi compilado nem testado em aparelho**.
Ao abrir, vale checar:
- **Versão do LibretroDroid** (`app/build.gradle.kts`, `0.13.2`) e nomes da API: `ShaderConfig.CRT/LCD`,
  `GLRetroView.ERROR_*`, `getGLRetroEvents()`. Se o compilador reclamar, ajuste uma linha.
- **Botões trocados**: o mapeamento está só em `input/Btn.kt`. LP/MP/HP e LK/MK/HK devem ser Y/X/L e B/A/R do RetroPad.
- **Pausa**: o menu usa `retroView.onPause()/onResume()` (o loop de emulação roda no `onDrawFrame`).
- **Rewind**: usa `serializeState()` a 10 Hz numa thread de fundo. Se o seu aparelho engasgar, reduza a frequência em
  `startRewindLoop` ou desative em Configurações.

## Não incluído: multiplayer online
Netplay não é um recurso do LibretroDroid; teria que ser implementado no frontend (sincronização de
save state + rollback), e o CPS-2 do FBNeo precisa de determinismo perfeito. Fica como etapa futura. Para jogar
online hoje, o caminho prático é RetroArch (que tem netplay) com o mesmo núcleo.

## Legal
- Use apenas ROMs que você tem direito de usar (backup do seu hardware). O app **não embute a ROM**; você a importa.
- O jogo é propriedade da Capcom/Marvel: não publique APK com a ROM nem distribua em loja.
- FBNeo tem licença não comercial; LibretroDroid é GPLv3. Um APK distribuído precisa respeitar ambas.
