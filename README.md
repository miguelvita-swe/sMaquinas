# sMaquinas

Plugin de máquinas para Minecraft 1.8–1.21 — Spigot/Paper.

## Funcionalidades
- Máquinas com upgrades, hologramas, economia, menus
- Sistema de combustível por máquina
- Stack de máquinas no chão
- Hologramas compatíveis com DecentHolograms
- Sistema de limites de compra e stack
- Multiplicadores de compra
- Menu de loja configurável via YAML
- Suporte a múltiplas economias (Vault + PlayerPoints)
- Compatível com PlotSquared
- Top de drops e amigos por máquina

## Dependências
- [sCore](https://github.com/miguelvita-swe/sCore) — **obrigatório**
- Vault (opcional)
- DecentHolograms (opcional)
- PlaceholderAPI (opcional)
- PlayerPoints (opcional)
- PlotSquared (opcional)

## Instalação
1. Coloque `sCore.jar` em `plugins/`
2. Coloque `sMaquinas.jar` em `plugins/`
3. Reinicie o servidor
4. Configure `plugins/sMaquinas/config.yml`

## Build
```bash
mvn clean package
```
O jar gerado estará em `target/sMaquinas-1.0.0-shaded.jar`.

## Versões Suportadas
| Versão | Suporte |
|--------|---------|
| 1.8 – 1.12 | ✅ Legacy NMS |
| 1.13 – 1.20 | ✅ Completo |
| 1.21+ | ✅ Completo |

## Comandos
| Comando | Descrição |
|---------|-----------|
| `/maquinas` | Abre a loja de máquinas |
| `/combustiveis` | Abre a loja de combustíveis |
| `/limite` | Gerencia limites de compra |
| `/drops` | Visualiza drops das máquinas |

## Permissões
| Permissão | Descrição |
|-----------|-----------|
| `smaquinas.admin` | Acesso a comandos administrativos |
| `smaquinas.maquina_limite.<N>` | Define limite de máquinas no chão |

## Autor
Skyy — Suite de plugins "s" para Minecraft

## Licença
Uso privado. Todos os direitos reservados.
