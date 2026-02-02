# DonutSMPOrders

A high-performance, region-aware order system plugin built for modern Minecraft servers. Features a sleek **Small Caps GUI** and a robust **Modular Architecture** optimized for high-concurrency environments.

---

## 🚀 Key Features

*   **Folia Native Support**: Engineered for the Folia API (1.21.1) to ensure full compatibility with regional multi-threading.
*   **Modular Architecture**: Fully refactored into a decoupled handler-based system for high scalability and streamlined maintenance.
*   **Sleek Small Caps GUI**: Professional and aesthetic interface using small caps typography across all menus and buttons.
*   **"Plain Items Only" System**: Enforces standard vanilla items (Tools, Armor, Potions, Arrows) to maintain economy stability and prevent custom NBT exploits.
*   **Item Blacklist**: Fully configurable blacklist in `config.yml` to restrict admin or illegal items (e.g., Bedrock, Barrier, Command Blocks).
*   **Dynamic Order Limits**: Permission-based order limits via `donutorders.limit.<number>`. Highest value is automatically resolved and applied.
*   **Enchanted Book Support**: Specialized logic for Enchanted Books, allowing exactly one enchantment per order with instant selection.
*   **Standardized Potion Handling**: Support for all vanilla potion types and tipped arrows, strictly enforcing standard effects.
*   **Enterprise-Grade Storage**: Flexible data persistence using either local **H2** or remote **MySQL** engines.
*   **Vault Integration**: Seamless integration with the server economy via the Vault API.

---

## 📋 Commands & Permissions

### Player Commands
| Command | Aliases | Description | Permission |
| :--- | :--- | :--- | :--- |
| `/order [item]` | `/orders` | Open the main order dashboard. | `donutorders.use` |

### Administrative Commands
| Command | Description | Permission |
| :--- | :--- | :--- |
| `/donutordersadmin reload` | Reload configuration and localization files. | `donutorders.admin` |
| `/donutordersadmin removeorder <player> <id>` | Forcefully remove a specific active order. | `donutorders.admin` |

---

## 🛡️ Permissions Reference

| Node | Description | Default |
| :--- | :--- | :--- |
| `donutorders.use` | Grants access to basic order system features. | `true` |
| `donutorders.admin` | Full administrative access to the order system. | `op` |
| `donutorders.limit.<number>` | Sets maximum active order slots (e.g., `.limit.25`). | `10` |

---

## 🛠️ Configuration Highlights

### Item Blacklist
Restrict specific materials from entering the marketplace:
```yaml
orders:
  blacklist:
    - "BARRIER"
    - "BEDROCK"
    - "COMMAND_BLOCK"
    - "STRUCTURE_BLOCK"
```

### Dynamic Limits
Players gain additional order slots dynamically based on their permission nodes:
- **Default:** 10 slots (defined in `config.yml`)
- **Permission Bypass:** Grant `donutorders.limit.50` to allow 50 concurrent active orders.

---

## 🏗️ Requirements

*   **Java Runtime**: Version 21 or higher.
*   **Server Engine**: Folia or Paper-based derivatives.
*   **Dependencies**: **Vault** and a compatible Economy provider.

---

## 🔨 Development

To build the project from source:

```bash
mvn clean install
```

The compiled JAR will be available in the `target/` directory.

---
Developed by [vanes430](https://github.com/vanes430)
