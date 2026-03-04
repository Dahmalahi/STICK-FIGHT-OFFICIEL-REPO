/**
 * Weapon v3 — ETERNAL REALMS FINAL EDITION
 * StickFight vFINAL
 *
 * NEW vs v2:
 *  - 5 new weapon types (was 13 types, now 18):
 *      TYPE_MINE      (13) — proximity mine, placed on floor, triggers on walk-over
 *      TYPE_THUNDER   (14) — chain lightning, bounces between fighters
 *      TYPE_SHURIKEN  (15) — rapid 4-shot burst, low damage, very fast
 *      TYPE_RAILGUN   (16) — ultra-slow reload, 1-shot, pierces platforms
 *      TYPE_SCYTHE    (17) — wide melee arc, hits both sides simultaneously
 *  - Bullet.owner expanded: 0=player, 1-3=meleeAI slot for melee brawl
 *  - Bullet.piercing  flag: bullets that pass through fighters (Railgun, Crossbow)
 *  - Bullet.chainLeft flag: thunder chain counter
 *  - Bullet.placed    flag: mine has been placed on ground (stationary)
 *  - Bullet.hitMask   bitmask: tracks which fighters a piercing shot already hit
 *  - TRAIT_* constants: per-weapon special behaviour flags
 *  - getTraits(type): returns OR'd trait flags for AI/render decisions
 *  - isExplosive(type), isMelee(type), isPiercing(type) convenience methods
 *  - RANGE[] array: effective range in world pixels (used by AI)
 *  - KNOCKBACK[] array: horizontal impulse on hit
 */
public class Weapon {

    // ── Weapon type constants ─────────────────────────────────────────
    public static final int TYPE_NONE         =  0;
    public static final int TYPE_PISTOL       =  1;
    public static final int TYPE_SHOTGUN      =  2;
    public static final int TYPE_SWORD        =  3;
    public static final int TYPE_AK47         =  4;
    public static final int TYPE_GRENADE      =  5;
    public static final int TYPE_SNIPER       =  6;
    public static final int TYPE_REVOLVER     =  7;
    public static final int TYPE_ROCKET       =  8;
    public static final int TYPE_LASER        =  9;
    public static final int TYPE_FLAMETHROWER = 10;
    public static final int TYPE_CROSSBOW     = 11;
    public static final int TYPE_BOOMERANG    = 12;
    public static final int TYPE_MINE         = 13; // NEW: proximity trap
    public static final int TYPE_THUNDER      = 14; // NEW: chain lightning
    public static final int TYPE_SHURIKEN     = 15; // NEW: rapid 4-burst
    public static final int TYPE_RAILGUN      = 16; // NEW: piercing sniper
    public static final int TYPE_SCYTHE       = 17; // NEW: 360 melee arc
    public static final int TYPE_COUNT        = 18;

    // ── Ammo ──────────────────────────────────────────────────────────
    public static final int[] AMMO = {
        0,   //  0 none
        15,  //  1 pistol
        6,   //  2 shotgun
        0,   //  3 sword  (infinite melee)
        30,  //  4 ak47
        3,   //  5 grenade
        5,   //  6 sniper
        8,   //  7 revolver
        2,   //  8 rocket
        60,  //  9 laser
        80,  // 10 flamethrower
        10,  // 11 crossbow
        8,   // 12 boomerang
        4,   // 13 mine
        12,  // 14 thunder
        16,  // 15 shuriken (4 shots per fire = 4 bursts)
        2,   // 16 railgun
        0,   // 17 scythe  (infinite melee)
    };

    // ── Damage ────────────────────────────────────────────────────────
    public static final int[] DAMAGE = {
        0,   //  0
        25,  //  1 pistol
        18,  //  2 shotgun (per pellet × 5)
        40,  //  3 sword
        22,  //  4 ak47
        65,  //  5 grenade (explosion)
        85,  //  6 sniper
        38,  //  7 revolver
        90,  //  8 rocket
        12,  //  9 laser (per tick)
        8,   // 10 flame  (per tick)
        55,  // 11 crossbow
        32,  // 12 boomerang (per hit, hits twice)
        70,  // 13 mine (trigger explosion)
        28,  // 14 thunder (per chain hop, up to 3 hops)
        16,  // 15 shuriken (per star, fast)
        130, // 16 railgun (1-hit very high damage)
        50,  // 17 scythe (wide arc)
    };

    // ── Fire cooldown (ms) ────────────────────────────────────────────
    public static final int[] COOLDOWN = {
        0,    //  0
        480,  //  1 pistol
        900,  //  2 shotgun
        0,    //  3 sword
        180,  //  4 ak47
        1100, //  5 grenade
        750,  //  6 sniper
        600,  //  7 revolver
        1400, //  8 rocket
        80,   //  9 laser
        60,   // 10 flame
        500,  // 11 crossbow
        700,  // 12 boomerang
        800,  // 13 mine (place cooldown)
        650,  // 14 thunder
        300,  // 15 shuriken (fires burst of 4, 75ms apart)
        2200, // 16 railgun (very slow)
        0,    // 17 scythe
    };

    // ── Bullet speed px/s ─────────────────────────────────────────────
    public static final int[] BULLET_SPEED = {
        0,   //  0
        360, //  1 pistol
        280, //  2 shotgun
        0,   //  3 sword
        430, //  4 ak47
        220, //  5 grenade (arced)
        580, //  6 sniper
        400, //  7 revolver
        300, //  8 rocket  (homing)
        700, //  9 laser
        150, // 10 flame   (short)
        350, // 11 crossbow
        240, // 12 boomerang
        0,   // 13 mine    (stationary)
        500, // 14 thunder (homing arc)
        520, // 15 shuriken
        900, // 16 railgun (very fast)
        0,   // 17 scythe
    };

    // ── Effective range in world px (for AI shoot decision) ───────────
    public static final int[] RANGE = {
        0,    //  0
        220,  //  1 pistol
        110,  //  2 shotgun
        50,   //  3 sword
        260,  //  4 ak47
        180,  //  5 grenade
        480,  //  6 sniper (full world)
        200,  //  7 revolver
        400,  //  8 rocket
        140,  //  9 laser
        80,   // 10 flame
        300,  // 11 crossbow
        260,  // 12 boomerang
        40,   // 13 mine (place near feet)
        200,  // 14 thunder
        200,  // 15 shuriken
        480,  // 16 railgun (full world)
        60,   // 17 scythe
    };

    // ── Knockback horizontal impulse px/s ─────────────────────────────
    public static final int[] KNOCKBACK = {
        0,   //  0
        80,  //  1 pistol
        120, //  2 shotgun
        100, //  3 sword
        60,  //  4 ak47
        200, //  5 grenade
        150, //  6 sniper
        110, //  7 revolver
        250, //  8 rocket
        40,  //  9 laser
        20,  // 10 flame
        130, // 11 crossbow
        90,  // 12 boomerang
        180, // 13 mine
        140, // 14 thunder
        50,  // 15 shuriken
        220, // 16 railgun
        160, // 17 scythe
    };

    // ── Rarity ────────────────────────────────────────────────────────
    public static final int[] RARITY = {
        0,  //  0 none
        1,  //  1 pistol     common
        1,  //  2 shotgun    common
        2,  //  3 sword      uncommon
        1,  //  4 ak47       common
        2,  //  5 grenade    uncommon
        3,  //  6 sniper     rare
        2,  //  7 revolver   uncommon
        3,  //  8 rocket     rare
        3,  //  9 laser      rare
        3,  // 10 flame      rare
        2,  // 11 crossbow   uncommon
        2,  // 12 boomerang  uncommon
        2,  // 13 mine       uncommon
        3,  // 14 thunder    rare
        1,  // 15 shuriken   common
        3,  // 16 railgun    rare
        2,  // 17 scythe     uncommon
    };

    // ── Weapon trait flags ────────────────────────────────────────────
    public static final int TRAIT_NONE      = 0;
    public static final int TRAIT_MELEE     = 1;   // close-range swing
    public static final int TRAIT_EXPLOSIVE = 2;   // area damage on impact
    public static final int TRAIT_PIERCING  = 4;   // passes through targets
    public static final int TRAIT_HOMING    = 8;   // seeks nearest enemy
    public static final int TRAIT_PLACED    = 16;  // placed on ground (mine)
    public static final int TRAIT_CHAIN     = 32;  // chains to nearby targets
    public static final int TRAIT_BURST     = 64;  // fires multiple projectiles
    public static final int TRAIT_RETURNING = 128; // boomerang-style return

    public static final int[] TRAITS = {
        TRAIT_NONE,                          //  0 none
        TRAIT_NONE,                          //  1 pistol
        TRAIT_BURST,                         //  2 shotgun (5 pellets)
        TRAIT_MELEE,                         //  3 sword
        TRAIT_NONE,                          //  4 ak47
        TRAIT_EXPLOSIVE,                     //  5 grenade
        TRAIT_PIERCING,                      //  6 sniper (light pierce)
        TRAIT_NONE,                          //  7 revolver
        TRAIT_EXPLOSIVE|TRAIT_HOMING,        //  8 rocket
        TRAIT_NONE,                          //  9 laser
        TRAIT_NONE,                          // 10 flame
        TRAIT_PIERCING,                      // 11 crossbow
        TRAIT_RETURNING,                     // 12 boomerang
        TRAIT_PLACED|TRAIT_EXPLOSIVE,        // 13 mine
        TRAIT_CHAIN|TRAIT_HOMING,            // 14 thunder
        TRAIT_BURST,                         // 15 shuriken
        TRAIT_PIERCING,                      // 16 railgun (full pierce)
        TRAIT_MELEE|TRAIT_BURST,             // 17 scythe (arc hits all)
    };

    /** Returns OR'd trait flags for the given weapon type. */
    public static int getTraits(int type) {
        if (type < 0 || type >= TYPE_COUNT) return TRAIT_NONE;
        return TRAITS[type];
    }

    public static boolean isExplosive(int type) { return (getTraits(type)&TRAIT_EXPLOSIVE)!=0; }
    public static boolean isMelee(int type)     { return (getTraits(type)&TRAIT_MELEE)    !=0; }
    public static boolean isPiercing(int type)  { return (getTraits(type)&TRAIT_PIERCING) !=0; }
    public static boolean isPlaced(int type)    { return (getTraits(type)&TRAIT_PLACED)   !=0; }
    public static boolean isChain(int type)     { return (getTraits(type)&TRAIT_CHAIN)    !=0; }

    // ── HUD labels ────────────────────────────────────────────────────
    public static final String[] LABEL = {
        "FIST", "PISTOL", "SHOTGUN", "SWORD", "AK-47",
        "GRENADE", "SNIPER", "REVOLVER", "ROCKET",
        "LASER", "FLAME", "CROSSBOW", "BOOMERANG",
        "MINE", "THUNDER", "SHURIKEN", "RAILGUN", "SCYTHE"
    };

    // ── Short pickup icons ────────────────────────────────────────────
    public static final String[] ICON = {
        "F",  "P",  "SG", "SW", "AK",
        "G",  "SN", "RV", "RK",
        "LZ", "FL", "CB", "BM",
        "MN", "TH", "SH", "RL", "SC"
    };

    // ── Ground pickup instance ────────────────────────────────────────
    public int     type;
    public float   x, y;
    public boolean active;
    public int     bobTimer;

    public Weapon(int type, float x, float y) {
        this.type   = type;
        this.x      = x;
        this.y      = y;
        this.active = true;
    }

    // ── Bullet pool ───────────────────────────────────────────────────
    public static final class Bullet {
        public float   x, y;
        public float   vx, vy;
        public boolean active;
        public int     owner;       // 0=player, 1-3=meleeAI[0-2]
        public int     damage;
        public int     weaponType;
        public int     life;        // remaining life ms

        // ---- Original fields ----
        public float   originX;     // boomerang: origin X for return
        public boolean returning;   // boomerang: is returning
        public int     bounces;     // grenade: remaining wall bounces
        public float   homingVX;    // rocket: homing velocity accumulator

        // ---- New fields (v3) ----
        public boolean piercing;    // railgun/crossbow: passes through fighters
        public int     hitMask;     // bitmask of already-hit fighter slots (pierce)
        public int     chainLeft;   // thunder: hops remaining (max 3)
        public boolean placed;      // mine: has been placed on ground (vx=vy=0)
        public int     burstLeft;   // shuriken: remaining shots in burst
        public int     burstTimer;  // shuriken: ms until next burst shot

        public Bullet() { active = false; }

        public void reset() {
            active    = false;
            life      = 0;
            returning = false;
            bounces   = 0;
            piercing  = false;
            hitMask   = 0;
            chainLeft = 0;
            placed    = false;
            burstLeft = 0;
            burstTimer= 0;
        }

        /** Convenience: mark fighter slot as already hit (for piercing). */
        public void markHit(int slot) { hitMask |= (1 << slot); }
        public boolean alreadyHit(int slot) { return (hitMask & (1<<slot)) != 0; }
    }
}
