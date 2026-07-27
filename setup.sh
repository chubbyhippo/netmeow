#!/bin/sh
# setup.sh — build and install the netmeow NetBeans module. POSIX sh.
#
#   ./setup.sh                build + test the core, build the module, then
#                             install it into every detected NetBeans userdir
#   ./setup.sh --core-only    build + test only the headless core
#   ./setup.sh --build-only   build the core and the module, install nothing
#   ./setup.sh --skip-build   install the already-built module
#   ./setup.sh --list         show detected NetBeans userdirs and exit
#   ./setup.sh --userdir DIR  install into DIR instead of auto-detecting
#   ./setup.sh -h             show this help and exit
#
# The Plugins dialog cannot be scripted, so the module is installed the way
# the IDE reads modules at start-up: the built cluster
# (module/target/nbm/clusters/extra) is copied into the userdir, which gives
# modules/, modules/ext/, config/Modules/ and update_tracking/ in one step.
# NetBeans must be RESTARTED afterwards — it reads the userdir once, at boot.
# Under WSL the Windows IDE's userdir (%APPDATA%\NetBeans\<version>) counts too.

set -eu

here=$(cd "$(dirname "$0")" && pwd)
cluster="$here/module/target/nbm/clusters/extra"

do_build=1 do_install=1 core_only=0 list_only=0
userdir=""

while [ $# -gt 0 ]; do
	case "$1" in
	--core-only) core_only=1 do_install=0 ;;
	--build-only) do_install=0 ;;
	--skip-build) do_build=0 ;;
	--list) list_only=1 ;;
	--userdir)
		shift
		[ $# -gt 0 ] || {
			echo "--userdir needs a directory" >&2
			exit 2
		}
		userdir=$1
		;;
	-h | --help)
		sed -n '2,18p' "$0"
		exit 0
		;;
	*)
		echo "unknown option: $1 (try --help)" >&2
		exit 2
		;;
	esac
	shift
done

info() { printf '\033[1;32m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33mwarn:\033[0m %s\n' "$*" >&2; }

# mise pins java 21 + maven; fall back to a PATH mvn without mise.
if command -v mise >/dev/null 2>&1; then MVN="mise exec -- mvn"; else MVN="mvn"; fi

# The nbm is compiled against one platform release (module/pom.xml's
# netbeans.version); an older userdir would only collect a module that IDE
# cannot enable, so version-named dirs below that release are skipped.
release=$(sed -n 's|.*<netbeans\.version>RELEASE\([0-9][0-9]*\)</netbeans\.version>.*|\1|p' \
	"$here/module/pom.xml" 2>/dev/null | head -1)
min_release=""
if [ -n "$release" ]; then min_release=$((release / 10)); fi

usable_userdir() {
	name=$(basename "$1")
	case "$name" in
	*cache* | dev) return 1 ;;
	esac
	case "$name" in
	*[!0-9]*) return 0 ;;
	esac
	if [ -n "$min_release" ] && [ "$name" -lt "$min_release" ]; then
		warn "$1 predates NetBeans $min_release — skipped"
		return 1
	fi
	return 0
}

# Userdirs, newest last: macOS keeps them under Application Support, Linux
# under ~/.netbeans or the XDG data dir.
detect_userdirs() {
	for base in \
		"$HOME/Library/Application Support/NetBeans" \
		"$HOME/.netbeans" \
		"$HOME/.local/share/netbeans"; do
		[ -d "$base" ] || continue
		for candidate in "$base"/*; do
			[ -d "$candidate" ] || continue
			usable_userdir "$candidate" || continue
			printf '%s\n' "$candidate"
		done
	done
	detect_windows_userdirs
}

# Under WSL the IDE is usually the Windows build, whose userdir sits on a
# mounted drive as %APPDATA%\NetBeans\<version>. Glob the mounts rather than ask
# cmd.exe for APPDATA (interop can be off), and require the config/ a booted
# userdir always has — which also drops an unmatched glob.
detect_windows_userdirs() {
	grep -qi microsoft /proc/sys/kernel/osrelease 2>/dev/null || return 0
	for candidate in /mnt/*/Users/*/AppData/Roaming/NetBeans/*; do
		[ -d "$candidate/config" ] || continue
		usable_userdir "$candidate" || continue
		printf '%s\n' "$candidate"
	done
}

targets() {
	if [ -n "$userdir" ]; then printf '%s\n' "$userdir"; else detect_userdirs; fi
}

if [ "$list_only" -eq 1 ]; then
	found=$(targets || true)
	if [ -z "$found" ]; then
		echo "no NetBeans userdir found — start the IDE once so it creates one"
	else
		printf '%s\n' "$found" | while IFS= read -r d; do echo "  $d"; done
	fi
	exit 0
fi

# ------------------------------------------------------------------- build

if [ "$do_build" -eq 1 ]; then
	if [ "$core_only" -eq 1 ]; then
		info "building + testing the core"
		(cd "$here" && $MVN -q -pl core verify)
	else
		info "building + testing the core, then the module"
		(cd "$here" && $MVN -q install)
	fi
	info "build ok"
fi

[ "$do_install" -eq 1 ] || exit 0

# ----------------------------------------------------------------- install

[ -d "$cluster" ] || {
	echo "no built cluster at $cluster — run without --skip-build first" >&2
	exit 1
}

# a list file, not `for dir in $(targets)`: userdir paths contain spaces on
# macOS, and a pipeline would run the counter in a subshell
list=$(mktemp "${TMPDIR:-/tmp}/netmeow-userdirs.XXXXXX")
trap 'rm -f "$list"' EXIT
targets >"$list"

installed=0
while IFS= read -r dir; do
	[ -n "$dir" ] || continue
	[ -d "$dir" ] || {
		warn "$dir does not exist — skipped"
		continue
	}
	info "installing into $dir"
	# modules/, modules/ext/, config/Modules/ and update_tracking/
	(cd "$cluster" && find . -type d -exec mkdir -p "$dir/{}" \;)
	(cd "$cluster" && find . -type f -exec cp {} "$dir/{}" \;)
	installed=$((installed + 1))
done <"$list"

if [ "$installed" -eq 0 ]; then
	echo "nothing installed: no NetBeans userdir found." >&2
	echo "Start NetBeans once to create one, or pass --userdir DIR." >&2
	exit 1
fi

if pgrep -f "Apache NetBeans" >/dev/null 2>&1 || pgrep -x netbeans >/dev/null 2>&1; then
	warn "NetBeans is running — restart it, the userdir is read only at boot"
fi

echo
info "installed into $installed userdir(s). Restart NetBeans, then:"
echo "  * open any file; the status bar right-hand end should read NORMAL"
echo "  * typing should MOVE rather than insert (h j k l), i enters INSERT,"
echo "    ESC returns to NORMAL"
echo "  * the keymap lives in ~/.netmeowrc, layered over the bundled default"
