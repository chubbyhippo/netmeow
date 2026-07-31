# netmeow

[meow](https://github.com/meow-edit/meow)-style modal editing for Apache
NetBeans: a selection-first grammar where you pick the region and then act on
it, plus native ports of [avy](https://github.com/abo-abo/avy),
[ace-window](https://github.com/abo-abo/ace-window), and Emacs'
`windmove`/`window.el` window navigation.

## Status

**Work in progress.** Installable via `./setup.sh`; the adapter now runs in a
live IDE, but only parts of it have been exercised from a keyboard.

| Part | State |
|---|---|
| `core` — the modal engine, motions, things, selection, kill/save/yank, search, avy, grab, keypad, rc | **complete, 381 specs green** |
| NetBeans adapter (`module`) | building and installing as an NBM, 75 specs green — **live but only partly keyboard-verified** |
| Bundled keymap `.netmeowrc` | present, with NetBeans action targets; `SPC i d` audits them against the running IDE |

Known gaps in the adapter: which-key renders to the status line rather than a
floating panel, only the first selection is applied (multi-caret is not wired),
and the caret shape is set through the mime type's editor preference rather
than per editor.

The core is headless and host-independent: it talks to the IDE through three
interfaces — `EditorPort` (11 methods), `UiPort` (16), `ClipboardPort` (2) —
and is fully tested against in-memory fakes, with no NetBeans classes on its
compile path.

## Install

```sh
./setup.sh              # build, test, and install into every NetBeans userdir
./setup.sh --list       # show the userdirs that would be used
./setup.sh --build-only # build only
./setup.sh -h           # all flags
```

Userdirs are found under `~/Library/Application Support/NetBeans`, `~/.netbeans`
and `~/.local/share/netbeans` — and, under WSL, the Windows IDE's
`%APPDATA%\NetBeans\<version>` on the mounted drive. Userdirs from a NetBeans
older than the release the module is built against are reported and skipped.

Restart NetBeans afterwards — the userdir is read once, at boot.

## Build

Requires JDK 21 and Maven; both are pinned in `mise.toml`.

```sh
mise exec -- mvn verify     # compile, format check, SpotBugs, 456 specs
mise exec -- mvn spotless:apply   # fix formatting
```

The build gates on
[spotless](https://github.com/diffplug/spotless) (google-java-format, AOSP
style) and [SpotBugs](https://spotbugs.github.io/) — no baselines, no
suppression files. A violation fails `verify`.

## Configuration

The entire keymap lives in an rc file, never in code. `.netmeowrc` is bundled
as a classpath resource with the defaults; a user copy at `~/.netmeowrc`
layers over it. Syntax is `.vimrc`-flavoured:

```
nmap x meow-kill
map <leader>ff <action>(some.host.action.id)
set overlay-color=#2ECC71
```

`<action>(id)` invokes an IDE action by id; everything else names a built-in
command. `SPC i d` writes every dispatchable id — NetBeans' global actions,
its editor actions, and netmeow's own commands — to a text file with each
one's category, label and shortcut, and lists any rc binding whose target the
running IDE cannot reach.

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).
