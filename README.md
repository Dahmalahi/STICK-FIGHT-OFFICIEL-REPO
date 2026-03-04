

![icon](https://github.com/user-attachments/assets/36552f95-685b-4ab7-be36-7c428c5dae94)


# ⚔️ StickFight J2ME — Complete Source

Stickman combat game for **Itel 5615 (240×320)** built on **J2ME MIDP 2.0 / CLDC 1.1**.  
Zero external assets — all graphics drawn procedurally with `Graphics` primitives.

---

## 📁 Project Structure

```
StickFight/
├── src/
│   ├── StickFightMIDlet.java   ← MIDlet entry point
│   ├── StickFightCanvas.java   ← Game loop, input, state machine (MAIN FILE)
│   ├── GameConstants.java      ← All shared constants (interface)
│   ├── Stickman.java           ← Player entity: physics, animation, combat
│   ├── Platform.java           ← Platform data (solid/spike/lava/moving/breakable)
│   ├── Arena.java              ← 5 handcrafted arenas with draw + hazards
│   ├── WeaponPickup.java       ← Weapon item on the floor
│   ├── Bullet.java             ← All projectile types
│   ├── ParticleSystem.java     ← Blood, sparks, explosion, death particles
│   ├── AIController.java       ← FSM AI with 3 difficulty levels
│   └── HUD.java                ← HP bars, lives, timer, kill feed
├── res/                        ← (empty — no PNGs needed!)
├── build.xml                   ← Ant build script
├── MANIFEST.MF
└── StickFight.jad
```

---

## 🎮 Features

### Gameplay
- **Physics engine**: gravity, friction, AABB collision, double jump, wall jump
- **Combat**: punch, block (75% damage reduction), knockback
- **11 weapon types**: Pistol, Revolver, Shotgun, AK-47, Sniper, Sword, Spear, Grenade, Rocket, Laser, Spike
- **Weapon mechanics**: ammo, recoil (shotgun/rocket push player), grenade bounce, explosion radius, sniper headshot one-shot
- **3 AI difficulty levels**: Easy (60% miss), Normal (35% miss), Hard (10% miss) with full FSM

### Arenas (5)
| # | Name    | Special Feature |
|---|---------|----------------|
| 0 | Classic | Spike pit center, symmetric layout |
| 1 | Tower   | Vertical inner tower, wall-climb required |
| 2 | Space   | Moving platforms, no floor (fall = death) |
| 3 | Lava    | Rising lava floor, breakable bridges |
| 4 | Chain   | Suspended platforms, spike ground pits |

### Game Modes
- **VS Computer** (3 difficulty levels)
- **2 Players** (hotseat on same device)
- **Tournament** (Best of 3 rounds)
- **Survival** (coming: wave-based)

### Visual Effects
- Procedural stickman animation (walk cycle, punch pose, ragdoll death)
- HP bar per player + heart lives display
- Kill feed (last 3 kills scrolling)
- Particle system: blood, sparks, explosions, death burst
- Lava animation, spike rendering, moving platform arrows
- Big flash messages (HEADSHOT!, EMPTY!, GOT AK-47!)

---

## 🕹️ Controls (Itel 5615 Numpad)

| Key | Player 1 | Player 2 (2P mode) |
|-----|----------|--------------------|
| `4` | Move Left | — |
| `6` | Move Right | — |
| `2` | Jump (x2 = double jump) | — |
| `5` | Attack / Shoot | — |
| `0` | Block (75% dmg reduction, 2s cooldown) | — |
| `#` | Drop/Throw weapon, Pause | — |
| `1` | — | Move Left |
| `3` | — | Move Right |
| `7` | — | Jump |
| `9` | — | Attack/Shoot |
| `*` | — | Block |

**Wall Jump**: Press jump while touching a wall → burst off the wall!

---

## 🔧 How to Build

### Prerequisites
- **JDK 1.4+** (Java 8 also works)
- **Sun WTK 2.5** (Wireless Toolkit) — for `preverify` + J2ME APIs
  - Download: [archive.org WTK 2.5.2](https://archive.org/details/sun-java-wireless-toolkit-2.5.2)
- **Apache Ant** — for the build script
- **MicroEmulator** (optional, for PC testing)

### Build Steps
```bash
# 1. Edit build.xml → set wtk.home to your WTK path

# 2. Build
ant build
# → dist/StickFight.jar + dist/StickFight.jad

# 3. Test on PC (optional)
ant run

# 4. Deploy to phone
# Copy BOTH .jar AND .jad via USB/Bluetooth
# Install .jad (it references the .jar)
```

### Alternative: NetBeans / Eclipse
1. NetBeans: Install **Mobility Pack**, New Project → MIDP → add all `src/*.java`
2. Eclipse: Install **MTJ plugin**, import project

### Alternative: No-WTK (MicroEmulator standalone)
```bash
# Compile with J2ME stubs from MicroEmulator
javac -source 1.4 -target 1.1 \
      -bootclasspath microemulator.jar \
      -classpath microemulator.jar \
      -d build/classes src/*.java

# Pack (no preverify needed for MicroEmulator)
jar cfm StickFight.jar MANIFEST.MF -C build/classes .

# Run
java -jar microemulator.jar --device 240x320 StickFight.jar
```

---

## 🔥 Architecture Notes

### Fixed-Point Math
All physics uses **integer fixed-point** (`FP = 100`):
- `x = 5000` means `50.00` pixels on screen
- `x / FP` → screen pixel position
- Avoids float (faster on old JVMs), prevents CLDC 1.0 compatibility issues

### Memory Budget (Itel 5615 heap ~512KB)
| Component | Est. Memory |
|-----------|-------------|
| 2× Stickman objects | ~4 KB |
| 30 Bullet pool | ~3 KB |
| 40 Particle pool | ~5 KB |
| 5 WeaponPickup pool | ~1 KB |
| Arena + Platforms | ~2 KB |
| Code (classes) | ~80 KB |
| **Total** | **~95 KB** |

### Game Loop (30 FPS)
```
pollInput() → processInput() → applyPhysics() →
arena.update() → processAttacks() → updateBullets() →
particles.update() → render() → flushGraphics() → sleep(33ms)
```

### AI FSM
```
IDLE ──────────── enemy close ─────→ CHASE
  ↑                                     │
  └──── escaped ────←─── ATTACK ←── close enough
                            │
                      bullet nearby? → EVADE
                      no weapon?    → PICKUP
```

---

## 📈 Extending the Game

### Add a new weapon
1. In `GameConstants.java`: add `WPN_FLAMETHROWER = 12`
2. In `WeaponPickup.java`: add name + ammo to tables, draw icon in `drawIcon()`
3. In `StickFightCanvas.java`: add speed to `getBulletSpeed()`, special logic in `fireBullet()`
4. In `Bullet.java`: add damage, color, behavior in `fire()`
5. In `Stickman.java`: draw held weapon in `drawHeldWeapon()`

### Add a new arena
1. In `Arena.java`: add `ARENA_MYMAP = 5` to `GameConstants`
2. Add `case ARENA_MYMAP: buildMyMap(); break;`
3. Write `buildMyMap()` — place platforms with `addPlat(x, y, w, h, type)`
4. Set `wpnSpawnX/Y` arrays

### Add save/high score (RecordStore)
```java
import javax.microedition.rms.*;
RecordStore rs = RecordStore.openRecordStore("StickFightHS", true);
byte[] data = String.valueOf(score).getBytes();
rs.addRecord(data, 0, data.length);
rs.closeRecordStore();
```

---

## 🐛 Troubleshooting

| Problem | Fix |
|---------|-----|
| `VerifyError` on device | Run `preverify` step — required for real J2ME |
| `OutOfMemoryError` | Reduce `MAX_BULLETS` (30→15) and `MAX_PARTICLES` (40→20) |
| Black screen | Check `setFullScreenMode(true)` and `flushGraphics()` in render |
| Keys not working | Some phones remap numpad — test with `getGameAction()` keys |
| Slow / <20 FPS | Reduce `MAX_PARTICLES`, simplify particle drawing |
| Crash on startup | Confirm MIDP 2.0 + CLDC 1.1 support on target device |

---

## 📝 License
Free to use, modify, and distribute. Credit appreciated.  
Made with 🔥 for Itel 5615.
