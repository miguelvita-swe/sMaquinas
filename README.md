# sMaquinas

**sMaquinas** é o plugin de máquinas da suite de plugins **"s"** para servidores Minecraft (Spigot/Paper **1.8 até 1.21**).

Máquinas são itens colocáveis no mundo que geram drops automaticamente ao consumir combustível — com upgrades, hologramas, economia, limites por jogador e muito mais.

---

## 🧩 Plugins da Suite

| Plugin | Descrição |
|---|---|
| **sMaquinas** | Máquinas que geram drops ao consumir combustível |

---

## ✨ Funcionalidades

- **Máquinas configuráveis** — tipo, drops, combustível, raio e upgrades via YAML
- **Combustível por máquina** — cada máquina consome combustível configurável
- **Stack de máquinas** — empilhe máquinas no inventário e no chão com merge inteligente
- **Hologramas automáticos** — exibe informações acima da máquina via DecentHolograms ou ArmorStand fallback
- **Sistema de upgrades** — velocidade, raio, drop multiplier e mais
- **Limites de compra e stack** — controle por permissão (`smaquinas.maquina_limite.<N>`)
- **Multiplicadores de compra** — compre múltiplas máquinas de uma vez
- **Loja configurável** — menu de compra de máquinas e combustíveis via YAML
- **Multi-economia** — Vault, PlayerPoints e mais via sCore
- **PlotSquared** — máquinas só podem ser colocadas em plots do dono
- **Top de drops** — ranking de drops por jogador
- **Amigos por máquina** — compartilhe máquinas com outros jogadores

---

## 📦 Instalação

1. Baixe o `sCore.jar` em [sCore Releases](https://github.com/miguelvita-swe/sCore/releases)
2. Baixe o `sMaquinas.jar` em [Releases](../../releases)
3. Coloque ambos em `plugins/` do servidor
4. Reinicie o servidor
5. Configure `plugins/sMaquinas/config.yml`

### Dependências

| Plugin | Tipo | Função |
|---|---|---|
| [sCore](https://github.com/miguelvita-swe/sCore) | **Obrigatório** | Núcleo da suite |
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | Opcional | Economia via Vault |
| [DecentHolograms](https://www.spigotmc.org/resources/decentholograms.96927/) | Opcional | Hologramas (prioridade) |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Opcional | Placeholders nos hologramas |
| [PlayerPoints](https://www.spigotmc.org/resources/playerpoints.80745/) | Opcional | Economia alternativa |
| [PlotSquared](https://www.spigotmc.org/resources/plotsquared-free.77506/) | Opcional | Proteção de plots |

---

## ⚙️ Configuração

```yaml
# plugins/sMaquinas/config.yml

Database:
  Tipo: SQLITE       # SQLITE ou MYSQL
  IP: localhost:3306
  DB: test
  User: admin
  Pass: ''

Opcoes:
  Holograma: true
  Raio: 5
  Plot: true
  Silk: true
  Multiplicador max: 10
  Compra chat: true
  CompactarCombustivel: true
```

---

## 🎮 Comandos

| Comando | Aliases | Descrição | Permissão |
|---|---|---|---|
| `/maquinas` | `maquina`, `machine`, `machines` | Abre a loja de máquinas | — |
| `/combustiveis` | `combustivel`, `fuel`, `fuels` | Abre a loja de combustíveis | — |
| `/limite` | `maquinalimite` | Gerencia limites de compra | `smaquinas.admin` |
| `/drops` | — | Visualiza drops das máquinas | — |

---

## 🔑 Permissões

| Permissão | Descrição |
|---|---|
| `smaquinas.admin` | Acesso a comandos administrativos |
| `smaquinas.maquina_limite.<N>` | Define o limite de máquinas que o jogador pode ter no chão |
| `smaquinas.infinito` | Máquinas ilimitadas no chão |

---

## 🏗️ Estrutura do Projeto

```
sMaquinas/
├── src/main/java/br/com/skyy/maquinas/
│   ├── SMaquinas.java               ← JavaPlugin principal
│   ├── api/EconomiaAPI.java         ← Integração com sCore Economy
│   ├── commands/                    ← Comandos do plugin
│   ├── database/                    ← Database (SQLite + MySQL)
│   ├── hooks/                       ← PlaceholderAPI, Hologramas
│   ├── listeners/                   ← Eventos do plugin
│   ├── managers/                    ← Managers (Maquina, Combustivel, etc.)
│   ├── menus/                       ← Menus de inventário
│   ├── models/                      ← Modelos de dados
│   └── utils/                       ← Utilitários
└── src/main/resources/
    ├── plugin.yml
    ├── config.yml
    ├── maquinas.yml                  ← Configuração das máquinas
    ├── combustiveis.yml              ← Configuração dos combustíveis
    ├── mensagens.yml                 ← Mensagens customizáveis
    └── shop/                         ← Configs da loja
```

---

## 🔧 Build

```bash
mvn clean package
```

Requer Java 8+. O JAR gerado em `target/sMaquinas-1.0.0-shaded.jar` é copiado para `plugins/`.

---

## 📋 Versões Suportadas

| Versão MC | NBT | Skull | Hologramas | Hex Colors |
|---|---|---|---|---|
| 1.8 – 1.12 | NMS Reflection | GameProfile | DH / HD | ❌ |
| 1.13 | NMS Reflection | GameProfile | DH / HD | ❌ |
| 1.14 – 1.15 | PDC | GameProfile | DH / HD | ❌ |
| 1.16 – 1.17 | PDC | GameProfile | DH / HD | ✅ |
| 1.18 – 1.21 | PDC | PlayerProfile | DH / HD | ✅ |

---

## 👤 Autor

**Skyy** —

---

## 📄 Licença

Este projeto é de uso privado. Todos os direitos reservados.
