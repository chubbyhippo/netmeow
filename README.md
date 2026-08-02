# netmeow

[meow](https://github.com/meow-edit/meow)-style modal editing for Apache
NetBeans, with native ports of [avy](https://github.com/abo-abo/avy),
[ace-window](https://github.com/abo-abo/ace-window) and Emacs'
`windmove`/`window.el`.

| | |
|---|---|
| Core engine — motions, things, selection, kill/save/yank, search, avy, grab, keypad, rc | 388 specs |
| NetBeans module (NBM) | 81 specs |
| Keymap | bundled `.netmeowrc`; user copy at `~/.netmeowrc` |

## Install

```sh
./setup.sh
```

| Flag | Effect |
|---|---|
| *(none)* | build and test the core, build the module, install into every detected userdir |
| `--core-only` | build and test the headless core only |
| `--build-only` | build the core and the module, install nothing |
| `--skip-build` | install the already-built module |
| `--list` | print the detected userdirs and exit |
| `--userdir DIR` | install into `DIR` instead of auto-detecting |
| `-h` | help |

Restart NetBeans afterwards — the userdir is read once, at boot.

| Userdir searched | Host |
|---|---|
| `~/Library/Application Support/NetBeans` | macOS |
| `~/.netbeans`, `~/.local/share/netbeans` | Linux |
| `%APPDATA%\NetBeans\<version>` | the Windows IDE, from WSL |

Userdirs from a NetBeans older than the release the module was built against
are reported and skipped.

## Build

JDK 21 and Maven, both pinned in `mise.toml`.

| Command | Runs |
|---|---|
| `mise exec -- mvn verify` | compile, spotless, SpotBugs, 469 specs |
| `mise exec -- mvn spotless:apply` | fix formatting |

| Gate on `verify` | Config |
|---|---|
| [spotless](https://github.com/diffplug/spotless) | google-java-format, AOSP style |
| [SpotBugs](https://spotbugs.github.io/) | no baselines, no suppression files |

## Configuration

| Layer | Where |
|---|---|
| Bundled defaults | `.netmeowrc`, a classpath resource |
| Your overrides | `~/.netmeowrc`, layered over it entry by entry |

```
nmap x meow-kill
map <leader>ff <action>(some.host.action.id)
cmap C-f forward-char
set overlay-color=#2ECC71
```

| Target | Means |
|---|---|
| `<action>(id)` | run an IDE action by id |
| a command name | a built-in meow command |
| anything else | replayed meow keys |

| `SPC i d` writes | |
|---|---|
| Every dispatchable id | NetBeans' global actions, its editor actions, and netmeow's own |
| Per id | its category, label and shortcut |
| Plus | any rc binding whose target the running IDE cannot reach |

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).
