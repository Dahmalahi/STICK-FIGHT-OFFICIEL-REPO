/**
 * Arena v5 — ETERNAL REALMS FINAL EDITION
 * StickFight vFINAL
 *
 * NEW vs v4:
 *  - 10 arenas (was 8): +THUNDER PEAK (8), +VOID SANCTUM (9)
 *  - PT_BOUNCE  (5): trampoline — 220% upward velocity on land
 *  - PT_CRUMBLE (6): 1-hit instant break, respawns 8s
 *  - FX_WIND   (5): periodic directional push force (Thunder Peak)
 *  - FX_DARKNESS(6): suppresses minimap/visibility (Void Sanctum)
 *  - FX_QUAKE  (7): periodic platform-break cascade (Spike Pit)
 *  - lavaRise now actually increments each 8s tick (max 60px)
 *  - DANGER_Y / DANGER_DMG_PER_SEC: passive tick damage zones
 *  - 4 spawn points per arena (full melee brawl support)
 *  - getFriction(platformType) per-surface friction
 *  - getWindForce(), getDangerY(), getDangerDmgPerSec() helpers
 *  - breakHealth/Timer arrays expanded to 16 slots
 */
public class Arena {

    // ── World size ──────────────────────────────────────────────────────
    public static final int W = 480;
    public static final int H = 480;

    // ── Platform types ──────────────────────────────────────────────────
    public static final int PT_SOLID   = 0;
    public static final int PT_SPIKE   = 1;
    public static final int PT_LAVA    = 2;
    public static final int PT_BREAK   = 3;
    public static final int PT_ICE     = 4;
    public static final int PT_BOUNCE  = 5;  // trampoline
    public static final int PT_CRUMBLE = 6;  // 1-hit instant break

    public static final float BOUNCE_MULT = 2.2f;

    // ── Hazard FX IDs ──────────────────────────────────────────────────
    public static final int FX_NONE      = 0;
    public static final int FX_LAVA_RISE = 1;
    public static final int FX_ICE_FLOOR = 2;
    public static final int FX_GRAVITY   = 3;
    public static final int FX_POISON    = 4;
    public static final int FX_WIND      = 5;
    public static final int FX_DARKNESS  = 6;
    public static final int FX_QUAKE     = 7;

    public static final int ARENA_COUNT  = 10;

    public static final int[] ARENA_FX = {
        FX_NONE,      // 0 The Ring
        FX_NONE,      // 1 Sky Bridge
        FX_LAVA_RISE, // 2 Infernal Crucible
        FX_ICE_FLOOR, // 3 Frozen Citadel
        FX_GRAVITY,   // 4 Neo Arcadia
        FX_POISON,    // 5 Jungle Ruins
        FX_QUAKE,     // 6 Spike Pit
        FX_LAVA_RISE, // 7 Lava Cave
        FX_WIND,      // 8 Thunder Peak
        FX_DARKNESS   // 9 Void Sanctum
    };

    // Passive damage zones: y >= DANGER_Y triggers tick damage
    public static final int[] DANGER_Y = {
        -1, -1, 445, -1, -1, 460, -1, 445, -1, 460
    };
    public static final int[] DANGER_DMG_PER_SEC = {
        0, 0, 18, 0, 0, 10, 0, 22, 0, 15
    };

    // ── 0: THE RING ────────────────────────────────────────────────────
    public static final int[][] P0 = {
        {   0, 452, 480, 28, PT_SOLID  },
        {  20, 380, 160, 10, PT_SOLID  },
        { 300, 380, 160, 10, PT_SOLID  },
        { 150, 305, 180, 10, PT_SOLID  },
        {   0, 230, 110, 10, PT_SOLID  },
        { 370, 230, 110, 10, PT_SOLID  },
        { 190, 160, 100, 10, PT_BOUNCE },
    };
    public static final int[] SP0 = { 80, 430, 400, 430, 160, 290, 310, 290 };
    public static final int[] WS0 = { 240, 448, 150, 296, 20, 221, 370, 221 };

    // ── 1: SKY BRIDGE ─────────────────────────────────────────────────
    public static final int[][] P1 = {
        {   0, 452, 480, 28, PT_SOLID   },
        {  40, 396, 100, 10, PT_SOLID   },
        { 340, 396, 100, 10, PT_SOLID   },
        { 190, 340, 100, 10, PT_SOLID   },
        {   0, 280,  80, 10, PT_SOLID   },
        { 400, 280,  80, 10, PT_SOLID   },
        { 160, 210, 160, 10, PT_CRUMBLE },
        { 195, 155,  90, 10, PT_BOUNCE  },
    };
    public static final int[] SP1 = { 60, 430, 420, 430, 80, 370, 380, 370 };
    public static final int[] WS1 = { 240, 443, 190, 330, 160, 200, 380, 265 };

    // ── 2: INFERNAL CRUCIBLE ───────────────────────────────────────────
    public static final int[][] P2 = {
        {   0, 452, 480, 28, PT_SOLID   },
        { 180, 428, 120, 10, PT_LAVA    },
        {   0, 400, 140, 10, PT_SOLID   },
        { 340, 400, 140, 10, PT_SOLID   },
        { 170, 350, 140, 10, PT_BREAK   },
        {   0, 290, 110, 10, PT_SOLID   },
        { 370, 290, 110, 10, PT_SOLID   },
        { 180, 220, 120, 10, PT_BREAK   },
        {  60, 165,  80, 10, PT_CRUMBLE },
        { 340, 165,  80, 10, PT_CRUMBLE },
    };
    public static final int[] SP2 = { 60, 425, 410, 425, 20, 385, 455, 385 };
    public static final int[] WS2 = { 170, 342, 10, 282, 370, 282, 240, 212 };

    // ── 3: FROZEN CITADEL ─────────────────────────────────────────────
    public static final int[][] P3 = {
        {   0, 452, 480, 28, PT_ICE    },
        {  20, 416, 120, 10, PT_ICE    },
        { 340, 416, 120, 10, PT_ICE    },
        { 170, 376, 140, 10, PT_ICE    },
        {   0, 326, 100, 10, PT_SOLID  },
        { 380, 326, 100, 10, PT_SOLID  },
        { 170, 276, 140, 10, PT_SOLID  },
        {  40, 216, 110, 10, PT_SPIKE  },
        { 330, 216, 110, 10, PT_SPIKE  },
        { 185, 155, 110, 10, PT_BOUNCE },
    };
    public static final int[] SP3 = { 70, 430, 410, 430, 20, 310, 455, 310 };
    public static final int[] WS3 = { 170, 368, 40, 318, 170, 268, 350, 208 };

    // ── 4: NEO ARCADIA ────────────────────────────────────────────────
    public static final int[][] P4 = {
        {   0, 452, 480, 28, PT_SOLID  },
        {  30, 412, 110, 10, PT_SOLID  },
        { 340, 412, 110, 10, PT_SOLID  },
        { 180, 378, 120, 10, PT_SOLID  },
        {  40, 336,  90, 10, PT_SOLID  },
        { 350, 336,  90, 10, PT_SOLID  },
        { 170, 294, 140, 10, PT_SOLID  },
        {   0, 254,  80, 10, PT_SOLID  },
        { 400, 254,  80, 10, PT_SOLID  },
        { 180, 210, 120, 10, PT_BREAK  },
        { 200, 155,  80, 10, PT_BOUNCE },
    };
    public static final int[] SP4 = { 70, 432, 410, 432, 20, 240, 455, 240 };
    public static final int[] WS4 = { 180, 370, 40, 328, 180, 286, 400, 240 };

    // ── 5: JUNGLE RUINS ───────────────────────────────────────────────
    public static final int[][] P5 = {
        {   0, 452, 480, 28, PT_SOLID  },
        { 120, 430,  60, 10, PT_SPIKE  },
        { 300, 430,  60, 10, PT_SPIKE  },
        {   0, 400, 120, 10, PT_SOLID  },
        { 360, 400, 120, 10, PT_SOLID  },
        { 160, 350, 160, 10, PT_BREAK  },
        {  40, 300, 100, 10, PT_SOLID  },
        { 340, 300, 100, 10, PT_SOLID  },
        { 170, 240, 140, 10, PT_BREAK  },
        { 210, 170,  60, 10, PT_SOLID  },
        {  10, 200,  70, 10, PT_BOUNCE },
        { 400, 200,  70, 10, PT_BOUNCE },
    };
    public static final int[] SP5 = { 50, 425, 430, 425, 15, 385, 450, 385 };
    public static final int[] WS5 = { 160, 342, 40, 292, 170, 232, 210, 162 };

    // ── 6: SPIKE PIT ──────────────────────────────────────────────────
    public static final int[][] P6 = {
        {   0, 452, 480, 28, PT_SOLID   },
        { 170, 428, 140, 10, PT_SPIKE   },
        {   0, 390, 150, 10, PT_SOLID   },
        { 330, 390, 150, 10, PT_SOLID   },
        { 170, 336, 140, 10, PT_SOLID   },
        {   0, 270, 100, 10, PT_SPIKE   },
        { 380, 270, 100, 10, PT_SPIKE   },
        { 190, 214, 100, 10, PT_BREAK   },
        { 170, 160, 140, 10, PT_CRUMBLE },
        {  60, 108,  80, 10, PT_BOUNCE  },
        { 340, 108,  80, 10, PT_BOUNCE  },
    };
    public static final int[] SP6 = { 60, 425, 420, 425, 15, 375, 455, 375 };
    public static final int[] WS6 = { 170, 328, 10, 262, 380, 262, 190, 206 };

    // ── 7: LAVA CAVE ──────────────────────────────────────────────────
    public static final int[][] P7 = {
        {   0, 452, 480, 28, PT_LAVA    },
        {  20, 428, 120, 10, PT_SOLID   },
        { 340, 428, 120, 10, PT_SOLID   },
        {   0, 390, 200, 10, PT_SOLID   },
        { 280, 390, 200, 10, PT_SOLID   },
        { 180, 344, 120, 10, PT_SOLID   },
        {  40, 298, 140, 10, PT_SOLID   },
        { 300, 298, 140, 10, PT_SOLID   },
        { 170, 250, 140, 10, PT_SOLID   },
        { 200, 195,  80, 10, PT_CRUMBLE },
        {  10, 150,  70, 10, PT_SOLID   },
        { 400, 150,  70, 10, PT_SOLID   },
    };
    public static final int[] SP7 = { 70, 418, 410, 418, 20, 374, 450, 374 };
    public static final int[] WS7 = { 180, 336, 40, 290, 300, 290, 200, 242 };

    // ── 8: THUNDER PEAK (NEW) ─────────────────────────────────────────
    public static final int[][] P8 = {
        {   0, 452, 480, 28, PT_SOLID   },
        {   0, 410, 100, 10, PT_SOLID   },
        { 380, 410, 100, 10, PT_SOLID   },
        { 140, 370, 200, 10, PT_ICE     },
        {  20, 330,  80, 10, PT_SOLID   },
        { 380, 330,  80, 10, PT_SOLID   },
        { 180, 290, 120, 10, PT_BOUNCE  },
        {  60, 250,  80, 10, PT_CRUMBLE },
        { 340, 250,  80, 10, PT_CRUMBLE },
        { 180, 205, 120, 10, PT_SOLID   },
        {  10, 160,  70, 10, PT_SPIKE   },
        { 400, 160,  70, 10, PT_SPIKE   },
        { 180, 115, 120, 10, PT_SOLID   },
    };
    public static final int[] SP8 = { 50, 430, 430, 430, 15, 395, 455, 395 };
    public static final int[] WS8 = { 190, 282, 70, 242, 390, 242, 190, 107 };

    // ── 9: VOID SANCTUM (NEW) ─────────────────────────────────────────
    public static final int[][] P9 = {
        {   0, 452, 480, 28, PT_SOLID   },
        {  30, 420, 120, 10, PT_CRUMBLE },
        { 330, 420, 120, 10, PT_CRUMBLE },
        { 160, 385, 160, 10, PT_SOLID   },
        {   0, 348,  90, 10, PT_CRUMBLE },
        { 390, 348,  90, 10, PT_CRUMBLE },
        { 170, 310, 140, 10, PT_SOLID   },
        {  50, 270,  90, 10, PT_CRUMBLE },
        { 340, 270,  90, 10, PT_CRUMBLE },
        { 170, 228, 140, 10, PT_BOUNCE  },
        {  10, 185,  80, 10, PT_SOLID   },
        { 390, 185,  80, 10, PT_SOLID   },
        { 180, 140, 120, 10, PT_CRUMBLE },
        { 200,  95,  80, 10, PT_SOLID   },
    };
    public static final int[] SP9 = { 65, 430, 415, 430, 20, 333, 455, 333 };
    public static final int[] WS9 = { 200, 302, 60, 258, 400, 258, 200, 220 };

    // ── Aggregate ──────────────────────────────────────────────────────
    private static final int[][][] ALL_P  = {
        P0,P1,P2,P3,P4,P5,P6,P7,P8,P9
    };
    private static final int[][] ALL_SP = {
        SP0,SP1,SP2,SP3,SP4,SP5,SP6,SP7,SP8,SP9
    };
    private static final int[][] ALL_WS = {
        WS0,WS1,WS2,WS3,WS4,WS5,WS6,WS7,WS8,WS9
    };

    // ── Active state ───────────────────────────────────────────────────
    public static int[][] PLATFORMS = P0;
    public static int[]   SPAWN     = SP0;
    public static int[]   WPNSPAWN  = WS0;
    private static int    arenaIdx  = 0;

    public static int[] breakHealth = new int[16];
    public static int[] breakTimer  = new int[16];

    public static int     fxTimer       = 0;
    public static int     lavaRise      = 0;
    public static boolean iceFloor      = false;
    public static float   gravityMult   = 1.0f;
    public static int     gravityTimer  = 0;
    public static boolean poisonFloor   = false;
    public static int     windForce     = 0;
    private static int    windFlipTimer = 0;
    public static boolean quakeActive   = false;
    public static int     quakeTimer    = 0;
    public static boolean darknessActive= false;
    public static int     lavaFrame     = 0;
    public static int     lavaTimer     = 0;

    // ==================================================================
    //  setArena
    // ==================================================================
    public static void setArena(int idx) {
        arenaIdx     = ((idx % ARENA_COUNT) + ARENA_COUNT) % ARENA_COUNT;
        PLATFORMS    = ALL_P[arenaIdx];
        SPAWN        = ALL_SP[arenaIdx];
        WPNSPAWN     = ALL_WS[arenaIdx];
        resetBreakables();
        fxTimer      = 0;
        lavaRise     = 0;
        gravityMult  = 1.0f;
        gravityTimer = 0;
        windForce    = 0;
        windFlipTimer= 6000;
        quakeActive  = false;
        quakeTimer   = 0;
        int fx       = ARENA_FX[arenaIdx];
        iceFloor        = (fx == FX_ICE_FLOOR);
        poisonFloor     = (fx == FX_POISON);
        darknessActive  = (fx == FX_DARKNESS);
        if (fx == FX_WIND) windForce = 55;
    }

    public static void nextArena()     { setArena(arenaIdx + 1); }
    public static int  getArenaIndex() { return arenaIdx; }

    private static void resetBreakables() {
        for (int i = 0; i < breakHealth.length; i++) {
            breakHealth[i] = 3;
            breakTimer[i]  = 0;
        }
    }

    // ==================================================================
    //  update
    // ==================================================================
    public static void update(int delta) {
        updateBreakables(delta);
        updateLavaAnim(delta);
        int fx = ARENA_FX[arenaIdx];
        fxTimer += delta;

        if (fx == FX_LAVA_RISE) {
            if (fxTimer > 8000) { fxTimer = 0; if (lavaRise < 60) lavaRise++; }

        } else if (fx == FX_GRAVITY) {
            if (gravityTimer > 0) {
                gravityTimer -= delta;
                if (gravityTimer <= 0) gravityMult = 1.0f;
            } else if (fxTimer > 5000) {
                fxTimer     = 0;
                gravityMult = 0.25f + (float)(System.currentTimeMillis() % 100) / 100f * 1.05f;
                gravityTimer= 2000;
            }

        } else if (fx == FX_WIND) {
            windFlipTimer -= delta;
            if (windFlipTimer <= 0) {
                windFlipTimer = 6000;
                int mag = 40 + (int)(System.currentTimeMillis() % 41);
                windForce = (windForce < 0) ? mag : -mag;
            }

        } else if (fx == FX_QUAKE) {
            if (quakeActive) {
                quakeTimer -= delta;
                if (quakeTimer <= 0) {
                    quakeActive = false; fxTimer = 0;
                    for (int i = 0; i < PLATFORMS.length && i < breakTimer.length; i++) {
                        if (PLATFORMS[i][4] == PT_BREAK && breakTimer[i] == 0) {
                            breakTimer[i] = 7000; break;
                        }
                    }
                }
            } else if (fxTimer > 9000) {
                quakeActive = true; quakeTimer = 1500;
            }
        }
    }

    private static void updateLavaAnim(int delta) {
        lavaTimer += delta;
        if (lavaTimer > 200) { lavaTimer = 0; lavaFrame = (lavaFrame+1)%4; }
    }

    public static void updateBreakables(int delta) {
        for (int i = 0; i < PLATFORMS.length && i < breakTimer.length; i++) {
            if (breakTimer[i] > 0) {
                breakTimer[i] -= delta;
                if (breakTimer[i] < 0) breakTimer[i] = 0;
            }
        }
    }

    // ==================================================================
    //  Collision
    // ==================================================================
    public static int checkLanding(float footX, float footY,
                                   float halfW, float vy, int[] landType) {
        if (vy < 0) return -1;
        for (int i = 0; i < PLATFORMS.length; i++) {
            if (i < breakTimer.length && breakTimer[i] > 0) continue;
            int px=PLATFORMS[i][0], py=PLATFORMS[i][1];
            int pw=PLATFORMS[i][2], ph=PLATFORMS[i][3];
            int pt=PLATFORMS[i][4];
            int drawPy = (pt==PT_LAVA) ? py-lavaRise : py;
            if (footX+halfW > px && footX-halfW < px+pw) {
                float prevY = footY - vy*0.033f;
                if (prevY <= drawPy+4 && footY >= drawPy && footY <= drawPy+ph+12) {
                    if (landType != null) landType[0] = pt;
                    // Crumble: break immediately on first touch
                    if (pt==PT_CRUMBLE && i<breakTimer.length && breakTimer[i]==0)
                        breakTimer[i] = 8000;
                    return drawPy;
                }
            }
        }
        return -1;
    }

    public static boolean insidePlatform(float x, float y) {
        for (int i = 0; i < PLATFORMS.length; i++) {
            if (i < breakTimer.length && breakTimer[i] > 0) continue;
            int px=PLATFORMS[i][0], py=PLATFORMS[i][1];
            int pw=PLATFORMS[i][2], ph=PLATFORMS[i][3];
            int pt=PLATFORMS[i][4];
            int drawPy=(pt==PT_LAVA)?py-lavaRise:py;
            if (x>=px&&x<=px+pw&&y>=drawPy&&y<=drawPy+ph) return true;
        }
        return false;
    }

    public static boolean hitBreakable(float x, float y) {
        for (int i = 0; i < PLATFORMS.length && i < breakHealth.length; i++) {
            int pt=PLATFORMS[i][4];
            if ((pt!=PT_BREAK&&pt!=PT_CRUMBLE)||breakTimer[i]>0) continue;
            int px=PLATFORMS[i][0],py=PLATFORMS[i][1];
            int pw=PLATFORMS[i][2],ph=PLATFORMS[i][3];
            if (x>=px-5&&x<=px+pw+5&&y>=py-5&&y<=py+ph+5) {
                if (pt==PT_CRUMBLE) { breakTimer[i]=8000; breakHealth[i]=3; return true; }
                breakHealth[i]--;
                if (breakHealth[i]<=0) { breakTimer[i]=12000; breakHealth[i]=3; return true; }
            }
        }
        return false;
    }

    // ==================================================================
    //  Surface / environment helpers
    // ==================================================================
    public static float getFriction(int platformType) {
        if (platformType==PT_ICE)    return 0.97f;
        if (platformType==PT_BOUNCE) return 0.90f;
        return 0.82f;
    }

    public static float getSurfaceFriction() { return iceFloor ? 0.97f : 0.82f; }
    public static float getGravityMult()     { return gravityMult; }
    public static int   getWindForce()       { return windForce; }
    public static boolean isQuakeActive()    { return quakeActive; }
    public static boolean isDark()           { return darknessActive; }

    public static int getDangerY() {
        return (arenaIdx<DANGER_Y.length) ? DANGER_Y[arenaIdx] : -1;
    }
    public static int getDangerDmgPerSec() {
        return (arenaIdx<DANGER_DMG_PER_SEC.length) ? DANGER_DMG_PER_SEC[arenaIdx] : 0;
    }
}
