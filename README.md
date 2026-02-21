**IVillager** simple plugin that uses the **vanilla villager trading UI** to create  multi configurable shops. Shops are defined in `config.yml`; players use `/ivillager` or `/ivl` to open them. 



## Use

- **Open default shop:** `/ivillager` or `/ivl`  
  Opens the shop set in `default_shop` in config. If none is set, only the command sender sees a short message.
- **Open a specific shop:** `/ivillager <shop name>` or `/ivl <shop name>`
- **List all shops:** `/ivillager list` or `/ivl list` — shows shop names (tab completion also lists them).
- **Reload config:** `/ivillager reload` (requires `ivillager.reload` or `ivillager.admin`)
- **Create a shop:** `/ivillager create <shop name>` (requires `ivillager.admin`)  
  Adds a new shop with one example trade (64 cobblestone → 1 diamond). Edit `config.yml` to add or change trades.
- **Delete a shop:** `/ivillager delete <shop name>` (requires `ivillager.admin` or `ivillager.delete`)



## Permissions

| Permission | Default | Description |
| `ivillager.use` | `true` | Allows opening all shops. |
| `ivillager.use.<shopname>` | `op` | Allows opening the named shop (e.g. `ivillager.use.default`). |
| `ivillager.admin` | `op` | Full admin: create, delete, reload, and bypass shop restrictions. |
| `ivillager.reload` | `op` | Allows reloading config and shop definitions. |
| `ivillager.delete` | `op` | Allows deleting shops from config. |

- With `ivillager.admin`, a player can open any shop and use create/delete/reload.
- With `ivillager.use`, a player can open all shops (unless you restrict with per-shop permissions).
- With only `ivillager.use.<shopname>`, a player can open that shop only.