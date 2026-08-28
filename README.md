# Aero — Phase 1: Core Hot-Loadable Module Runtime

This is Aero's Phase 1 proof of concept: proving that **Aero can dynamically
load, run, disable, unload, and replace a Community Module while Minecraft
keeps running**, without touching the marketplace, payments, licensing
backend, or any polished UI. Those are explicitly out of scope here.

## Starting point

This repository had no commits, branches, or files when this phase began -
there was no existing Aero codebase to inspect or build on top of. Everything
below is new. Nothing pre-existing was modified because nothing pre-existing
existed.

## Layout

```
aero/
├── aero-api/               pure Java, zero Minecraft/Fabric deps - the public module API
├── aero-module-runtime/    pure Java, zero Minecraft/Fabric deps - the engine (classloading,
│                           lifecycle, event/HUD/keybind bookkeeping, failure isolation)
├── aero-core/               the actual Fabric mod (Loom project) - the *only* thing
│                           Fabric Loader loads. Bridges the engine to real
│                           Minecraft: tick, HUD rendering, keybinding, game state,
│                           and one illustrative Mixin.
└── aero-official-modules/
    └── example-module/     first-party dev module (v1 + v2) used to exercise the
                            runtime; built as its own jar, never bundled into aero.jar
```

`aero-api` and `aero-module-runtime` build and test with plain `java`/JUnit -
no Minecraft, no Loom, nothing game-related on their classpath. That is
deliberate: it is both the honest architectural boundary ("Community Modules
talk to the API, not to Minecraft") and the only way the module runtime's
classloader-isolation and GC-leak tests could run reliably in this sandboxed
environment (see **What was actually verified** below).

`aero-core` embeds the compiled classes of the other two straight into
`aero.jar` (see `aero-core/build.gradle.kts`, the `jar { from(...) }` block) -
they are libraries, not separate mods. The final build produces one jar,
`aero-core-<version>.jar` (Fabric's own naming; conceptually this is
`aero.jar`), which is the single Fabric mod a user installs.

## Version decisions

The task brief asked to inspect the existing Minecraft/Fabric version,
Gradle structure, mixins, etc. before writing code. Since the repo was
empty, those choices had to be made fresh:

- **Minecraft 1.21.11**, Fabric Loader `0.19.3`, Fabric API
  `0.141.6+1.21.11`, Fabric Loom `1.17-SNAPSHOT`, official Mojang mappings
  (Yarn is no longer used for this line - Loom resolves official mappings
  directly). This is the newest Minecraft version with a fully mature,
  stable Fabric toolchain.
- **Not** the very latest Minecraft release (`26.2`, on Mojang's new
  year.release versioning scheme, current as of writing). Its official
  Fabric template requires **Java 25**, and this environment only has a
  JDK 21 toolchain available with no network path to provision a JDK 25 (no
  `sdk`/`asdf`, no `apt` package for it). Rather than fight an unfamiliar,
  very recent toolchain shift blind, 1.21.11 (Java 21, the same JDK already
  installed) was chosen as the safer, still-current target. Bumping to
  26.2 later is a small, mostly mechanical change (see
  `gradle.properties` and `aero-core/build.gradle.kts`) once a Java 25
  toolchain is available.
- **Gradle 9.5.1** via the wrapper (`./gradlew`), not the system Gradle
  (8.14.3) - Fabric Loom `1.17.x` requires Gradle 9. All commands below
  should use `./gradlew`.
- All real Minecraft/Fabric API class and method names used throughout
  `aero-core` (`Minecraft`, `LocalPlayer`, `GuiGraphics`, `KeyMapping`,
  `ClientTickEvents`, `HudRenderCallback`, `KeyBindingHelper`, etc.) were
  confirmed against the actual downloaded 1.21.11 mappings and Fabric API
  jars, not recalled from memory - naming has changed noticeably from
  older Minecraft/Yarn conventions.

## Mapping the Phase 1 objectives to code

| Objective | Where |
|---|---|
| Aero API (`Module`, `ModuleContext`, `ModuleManager`, events, keybinds, HUD, config, game state) | `aero-api/src/main/java/dev/aero/api/` |
| Module manifest (`module.json`) | `dev/aero/api/ModuleManifest.java` (schema, forward-compatible with unused fields), `dev/aero/runtime/manifest/ManifestParser.java` (parsing/validation), `dev/aero/runtime/json/Json.java` (dependency-free JSON reader - see below) |
| Dynamic loading, per-module classloader | `dev/aero/runtime/classloader/ModuleClassLoader.java`, `dev/aero/runtime/ModuleManagerImpl.install()` |
| Dynamic disabling, error isolation | `ModuleManagerImpl.disable()/disableHandle()`, `dev/aero/runtime/event/EventBusImpl.java`, `dev/aero/runtime/ui/HudRegistryImpl.java`, `dev/aero/runtime/ui/KeybindRegistryImpl.java` (every callback boundary is try/catch) |
| Dynamic unloading (classloader unreachability) | `ModuleManagerImpl.disposeHandle()` / `ModuleHandleImpl.clear()`; verified by `ClassloaderIsolationTest` (real `WeakReference` + `System.gc()` loop) |
| Dynamic updating | `ModuleManagerImpl.update()`; verified by `UpdateTest` and `ExampleModuleIntegrationTest` |
| Basic Minecraft integration (tick, HUD, keybind, game state) | `aero-core/src/client/java/dev/aero/core/client/` - `AeroClientMod`, `GuiGraphicsHudCanvas`, `KeybindBridge`, `GameStateProviderImpl` |
| Test module | `aero-official-modules/example-module/` (`ExampleModule` = v1, `ExampleModuleV2` = v2) |
| Module manager (install/enable/disable/update/uninstall/get) | `dev/aero/api/ModuleManager.java` + `dev/aero/runtime/ModuleManagerImpl.java` |
| Error isolation / auto-disable after repeated failures | `dev/aero/runtime/failure/FailureTracker.java` (default threshold 5 consecutive failures); see `FailureIsolationTest` |
| `ModulePackage` abstraction (so `.aero` can slot in later) | `dev/aero/api/ModulePackage.java` (interface) + `dev/aero/runtime/pkg/JarModulePackage.java` (Phase 1's only implementation: a plain dev jar) |
| One illustrative internal Mixin | `dev/aero/core/client/mixin/ScreenTrackingMixin.java` - hooks `Screen.added()/removed()` to feed `ScreenInfo`; tick/HUD/keybind hooks deliberately use Fabric API instead, since re-implementing already-stable Fabric API hooks as raw Mixins would add risk for no benefit in Phase 1 |

### Why a hand-rolled JSON parser (`dev/aero/runtime/json/Json.java`)

`aero-module-runtime` has zero third-party runtime dependencies on purpose -
it is the piece most likely to be extracted into its own repository later,
and it needs to keep working identically whether or not it happens to share
a JVM with Minecraft's own bundled Gson. A ~200-line recursive-descent
parser for the small, flat manifest schema was cheaper and safer than
pulling in and potentially version-clashing with a general-purpose JSON
library. It has its own tests (`JsonTest`).

## Building and testing

```
./gradlew build          # builds everything: aero-api, aero-module-runtime,
                          # aero-core (a real Fabric mod jar against real
                          # Minecraft 1.21.11), and both example-module jars

./gradlew :aero-module-runtime:test              # 20 unit tests, no Minecraft involved
./gradlew :aero-official-modules:example-module:test   # integration test against the *real* built module jars
```

The Aero mod jar ends up at `aero-core/build/libs/aero-core-<version>.jar`.
The two example module jars end up at
`aero-official-modules/example-module/build/libs/example-module-1.0.0.jar`
and `example-module-2.0.0.jar`.

## What was actually verified

This ran in a headless container with no display and no way to launch an
interactive Minecraft client - so the in-game demonstration script from the
task brief (watch the HUD appear/disappear/update on screen) could not be
performed visually. What **was** verified, and is the strongest evidence
the runtime actually works:

1. **`./gradlew build` succeeds end-to-end**, including `aero-core`
   compiling real Mixins and real Minecraft/Fabric API calls against the
   actual downloaded Minecraft 1.21.11 jar, and producing a working,
   correctly-assembled `aero.jar` (verified by unzipping it and confirming
   `fabric.mod.json`, the mixin config, the mixin class, and the merged
   `aero-api`/`aero-module-runtime` classes are all present).
2. **20 JUnit tests in `aero-module-runtime`**, all passing, including:
   - `ClassloaderIsolationTest`: two modules get distinct classloaders; a
     module's classloader becomes genuinely unreachable (`WeakReference`
     + `System.gc()` loop) after `uninstall()`; five load/unload cycles in
     a row do not accumulate classloaders.
   - `FailureIsolationTest`: a module whose tick listener always throws
     never crashes the tick loop, never affects a second, healthy module,
     and gets auto-disabled after a configurable number of consecutive
     failures.
   - `UpdateTest`: updating a running module swaps to the new version's
     code, keeps it enabled (matching its prior state), and the old
     version's classloader becomes unreachable.
   - `ModuleLifecycleTest`, `DependencyCleanupTest`, `ManifestParserTest`,
     `JsonTest`: the rest of the lifecycle and manifest parsing.

   These tests compile small fixture modules on the fly with the JDK's own
   `javax.tools.JavaCompiler` and load them through the real
   `ModuleManagerImpl` - not mocks.
3. **`ExampleModuleIntegrationTest`** drives the actual, independently
   built `example-module-1.0.0.jar` and `example-module-2.0.0.jar` through
   the real runtime end to end: install → enable → HUD text renders →
   keybind press hides it → disable → re-enable → hot-update to v2 while
   still enabled → v2's (different) HUD text renders → uninstall. This is
   the closest thing to the task's demonstration script that can run
   headlessly.

What was **not** verified: actually launching `aero-core-<version>.jar`
inside a real Minecraft client and watching the HUD/keybind/update behavior
on screen. The mixin, the Fabric API wiring, and the GuiGraphics/KeyMapping
calls are all written against real, confirmed method signatures from the
actual downloaded 1.21.11 jars, and everything compiles against them - but
that is not the same as an in-game visual confirmation, and it is not
claimed here as one.

### To actually run it

On a machine with a display and Minecraft's assets reachable:

```
./gradlew :aero-core:runClient
```

Fabric Loom's `runClient` task launches a dev-environment Minecraft client
with `aero.jar` installed. Drop `example-module-1.0.0.jar` into
`aero-core/run/aero/modules/` before launching (or use `/aero reload` once
in-game) and it will install and enable automatically; the HUD should show
"Aero Module Loaded!". In-game, `/aero list`, `/aero disable
example-module`, `/aero enable example-module`, and `/aero update
example-module example-module-2.0.0.jar` exercise the rest of the pipeline
without restarting the game. (`/aero` is a minimal debug command, not the
Community Modules UI - see below.)

## What was deliberately not built

Per the brief: no marketplace, no Community Modules website, no payments/
subscriptions, no licensing backend, no account linking, no `.aero`
packaging polish, no module signing/verification, no advanced permissions,
no telemetry, and no polished module browser UI. The one UI-shaped thing
added, `/aero <list|enable|disable|update|uninstall|reload>`, is a plain
Brigadier debug command - it exists only so the dynamic lifecycle could be
exercised in-game without building any screen, and is explicitly not a
substitute for the eventual Community Modules UI.

`aero-bootstrap`, `aero-network`, `aero-licensing`, and `aero-ui` from the
brief's target directory layout do not exist yet as separate modules -
`aero-core` currently plays the role of `aero-bootstrap` too (it's small
enough that splitting it further would be premature). Nothing about the
current structure blocks adding them later.

## Known Phase 1 limitations (intentional, not oversights)

- **Keybind unregistration is best-effort.** Fabric API supports
  registering a `KeyMapping` after client init (used here so a module
  enabled at runtime gets a working binding immediately), but has no
  matching "unregister". Disabling a module stops polling its keybind
  (making it inert) rather than removing it from the controls screen. See
  `KeybindBridge`'s Javadoc.
- **Module config (`ModuleConfig`) is in-memory only** and resets when a
  module is unloaded - there is no persistence backend yet.
- **No dependency resolution, permissions enforcement, or signature
  verification**, even though the manifest schema already has fields for
  them - the brief was explicit that Phase 1 should not implement these.
- **No sandboxing.** As the brief states: this is the architectural
  boundary (modules use the API, not Aero internals), not a security
  boundary against hostile code. `Community Module -> Aero Module Runtime
  -> try/catch -> Aero Core` isolates *bugs*, not malice.
