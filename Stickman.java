import javax.microedition.lcdui.*;
import java.util.Random;

/**
 * Stickman v2 PRO MAX
 *
 * Pixel-art inspired procedural stickman with:
 *  - 8 distinct animation states (idle, walk, run, jump, punch, kick, block, die)
 *  - All 12 weapon held-graphics
 *  - Combo system (punch → kick → uppercut)
 *  - Wall jump
 *  - Final Attack (ULTRA move, 4s cooldown, kills with one hit)
 *  - Ragdoll death with tumble
 *  - Double jump particle burst
 */
public class Stickman {

    private static final Random rand = new Random();

    // ---- Body dimensions (pixel-art scale) ---------------------------
    public static final int W   = 24;   // body width (collision half = 12)
    public static final int H   = 48;   // total height

    // ---- Physics constants -------------------------------------------
    static final float GRAVITY    =  900f;
    static final float JUMP_VY    = -420f;
    static final float JUMP2_VY   = -360f;  // second jump weaker
    static final float WALLJUMP_VX=  220f;  // wall-jump horizontal burst
    static final float WALLJUMP_VY= -380f;
    static final float WALK_SPEED =  110f;
    static final float RUN_SPEED  =  165f;  // sprint after 300ms hold
    static final float FRICTION   =  0.82f;
    static final float AIR_DRAG   =  0.96f; // air resistance

    // ---- States -------------------------------------------------------
    public static final int ST_IDLE      = 0;
    public static final int ST_WALK      = 1;
    public static final int ST_RUN       = 2;
    public static final int ST_JUMP      = 3;
    public static final int ST_FALL      = 4;
    public static final int ST_PUNCH     = 5;
    public static final int ST_KICK      = 6;
    public static final int ST_BLOCK     = 7;
    public static final int ST_SHOOT     = 8;
    public static final int ST_DEAD      = 9;
    public static final int ST_ULTRA     = 10; // Final Attack state
    public static final int ST_WALLJUMP  = 11;

    // ---- Combat constants --------------------------------------------
    public static final int PUNCH_DMG      = 10;  // reduced for longer fights
    public static final int KICK_DMG       = 14;  // reduced for longer fights
    public static final int UPPERCUT_DMG   = 22;  // reduced for longer fights
    public static final int ULTRA_DMG      = 80;   // big but not one-shot always
    public static final int PUNCH_RANGE    = 34;
    public static final int KICK_RANGE     = 38;
    public static final int PUNCH_DUR      = 200;
    public static final int KICK_DUR       = 260;
    public static final int ULTRA_DUR      = 500;
    public static final int PUNCH_COOLDOWN = 300;

    // ---- Position / velocity -----------------------------------------
    public float x, y;
    public float vx, vy;

    // ---- Status -------------------------------------------------------
    public int     health    = 100;
    public int     maxHealth = 100;
    public boolean alive     = true;
    public boolean onGround  = false;
    public boolean onWall    = false;
    public boolean facingRight;
    public boolean isPlayer;
    public int     state     = ST_IDLE;
    // v3 stat fields
    public int charId    = 0;
    public int speedMult = 100;
    public int dmgMult   = 100;
    public int defMult   = 100;
    public int burnTimer = 0;

    // ---- Combo system ------------------------------------------------
    public int comboStep    = 0;  // 0=ready, 1=after punch, 2=after kick
    public int comboTimer   = 0;  // ms to accept next combo input

    // ---- Timers (ms) -------------------------------------------------
    public int punchTimer     = 0;
    public int kickTimer      = 0;
    public int ultraTimer     = 0;
    public int attackCooldown = 0;
    public int blockCooldown  = 0;
    public int invincTimer    = 0;
    public int animTimer      = 0;
    public int animFrame      = 0;  // 0-7 cycle
    public int runHoldTimer   = 0;  // ms holding move key → triggers run

    // ---- Ultra (Final Attack) cooldown -------------------------------
    public int ultraCooldown  = 0;  // ms until available again (4000ms)

    // ---- Jump --------------------------------------------------------
    public int jumpsLeft = 2;

    // ---- Weapon -------------------------------------------------------
    public int weaponType = Weapon.TYPE_PISTOL;
    public int weaponAmmo = Weapon.AMMO[Weapon.TYPE_PISTOL];
    public int shootCooldown = 0;

    // ---- Signals to canvas -------------------------------------------
    public boolean wantsToShoot  = false;
    public boolean jumpParticle  = false; // canvas spawns burst on double jump

    // ---- Death animation ---------------------------------------------
    public float deathAngle = 0f;
    public float deathVX = 0f, deathVY = 0f;
    public int   deathTimer = 0;       // tumble frames

    // ---- AI fields ---------------------------------------------------
    private int aiStateTimer = 0;
    private int aiMode       = 0;  // 0=chase 1=attack 2=retreat 3=get weapon
    private int aiJumpTimer  = 0;
    private int aiStrafeDir  = 1;
    private int aiDiffTimer  = 0;

    // ---- Knockback ---------------------------------------------------
    private float kbX = 0f, kbY = 0f;

    // ---- Survival kill count -----------------------------------------
    public int kills = 0;

    // ---- Special Charge (fills by HITTING enemy, not being hit) --------
    public static final int SP_CHARGE_MAX     = 100;
    public static final int SP_CHARGE_PER_HIT = 20;
    public int specialCharge    = 0;   // 0..100
    public boolean specialFull  = false; // flashes when at 100
    public int specialChargeFlash = 0;  // flash timer ms

    // ---- Victory animation -------------------------------------------
    public static final int ST_VICTORY  = 12;
    public static final int ST_DEFEAT   = 13;
    public int victoryTimer   = 0;
    public int victoryPose    = 0;  // animated frame
    public int victoryFlash   = 0;

    // ---- Armor/shop item effects active on this stickman --------------
    public boolean hasEmberGauntlets = false; // burn on punch
    public boolean hasCyberPlating   = false; // +20 HP on init
    public boolean hasDragonhide     = false; // 25% bullet reduction
    public boolean hasFrostShield    = false; // blocks 1 hit free
    public boolean shadowweaveActive = false; // invis while blocking
    public boolean speedBoost        = false; // key 7: 2x speed for 5s
    public boolean shieldWall        = false; // key 9: invulnerable for 3s
    // ---- Character-specific trait flags (set by charId) ---------------
    public boolean traitBladeStorm   = false; // charId 8: spinning blade hitbox
    public boolean traitVoltDash     = false; // charId 9: electric dash stun
    public boolean traitIronHide     = false; // charId 10: 50% knockback reduction
    public boolean traitPhantom      = false; // charId 11: 10% dodge on hit
    public int     bladeSpinTimer    = 0;     // BLADESTORM spin state
    public int     voltStunChain     = 0;     // VOLTRA hit chain count
    // ---- Special Move System -----------------------------------------
    public static final int SP_NONE          = 0;
    public static final int SP_DRAGON_BREATH = 1;
    public static final int SP_LIGHTNING     = 2;
    public static final int SP_SOUL_REAP     = 3;
    public static final int SP_PERMAFROST    = 4;
    public static final int SP_GRAVITY_WELL  = 5;
    public static final int SP_VINE_CAGE     = 6;
    public static final int SP_MAGMA_SLAM    = 7;
    public static final int SP_BLIZZARD      = 8;

    public static final int SP_COOLDOWN_MS  = 12000;
    public static final int SP_KILLS_NEEDED = 2;

    public int  specialId       = SP_NONE;
    public int  specialTimer    = 0;
    public int  specialCooldown = 0;
    public int  killsThisRound  = 0;
    public boolean specialReady = false;
    public int  chargeTimer     = 0;
    public boolean charging     = false;
    public float dashVX         = 0f;
    public int   dashTimer      = 0;
    public boolean dashInvuln   = false;
    public int  frozenTimer     = 0;
    public int  rootedTimer     = 0;
    public int  gravWellTimer   = 0;
    public float gravWellX      = 0f;
    public float gravWellY      = 0f;
    public int  boltTimer       = 0;
    public int  boltCount       = 0;
    public int  blizzardSlowTimer = 0;
    // FX signals (canvas reads & clears each frame)
    public int   spFxType       = 0;
    public float spFxX          = 0f;
    public float spFxY          = 0f;
    public static final int FX_NONE        = 0;
    public static final int FX_FLAME_BURST = 1;
    public static final int FX_LIGHTNING   = 2;
    public static final int FX_SOUL_WISP   = 3;
    public static final int FX_ICE_SHARD   = 4;
    public static final int FX_GRAV_PULL   = 5;
    public static final int FX_VINE        = 6;
    public static final int FX_QUAKE       = 7;
    public static final int FX_BLIZZARD    = 8;

    // =================================================================
    //  Constructor
    // =================================================================

    public Stickman(float x, float y, boolean isPlayer) {
        this.x           = x;
        this.y           = y;
        this.isPlayer    = isPlayer;
        this.facingRight = isPlayer;
        // Special assigned later via charId (call assignSpecial() after charId set)
    }

    // =================================================================
    //  Update
    // =================================================================

    public void update(int delta, boolean left, boolean right,
                       boolean jump, boolean attack, boolean block,
                       boolean ultraKey, Stickman opponent) {
        // Victory / defeat animation loops — no physics
        if (state == ST_VICTORY) {
            victoryTimer += delta;
            specialChargeFlash = 100;
            return;
        }
        if (state == ST_DEFEAT) {
            victoryTimer += delta;
            return;
        }
        if (!alive) {
            updateDeath(delta);
            return;
        }

        float dt = delta / 1000f;

        // -- Timers ----------------------------------------------------
        if (invincTimer    > 0) invincTimer    -= delta;
        if (burnTimer      > 0) { burnTimer -= delta; if ((burnTimer % 500) < delta) { health -= 3; if (health < 0) health = 0; } }
        if (attackCooldown > 0) attackCooldown -= delta;
        if (blockCooldown  > 0) blockCooldown  -= delta;
        if (punchTimer     > 0) punchTimer     -= delta;
        if (kickTimer      > 0) kickTimer      -= delta;
        if (ultraTimer     > 0) ultraTimer     -= delta;
        if (shootCooldown  > 0) shootCooldown  -= delta;
        if (ultraCooldown  > 0) ultraCooldown  -= delta;
        if (specialChargeFlash > 0) specialChargeFlash -= delta;
        victoryPose = (int)(System.currentTimeMillis() / 200) % 8;
        if (specialCooldown> 0) { specialCooldown -= delta; if (specialCooldown<0) specialCooldown=0; }
        if (specialTimer   > 0) specialTimer -= delta;
        if (frozenTimer    > 0) { frozenTimer  -= delta; if (frozenTimer  <0) frozenTimer  =0; }
        if (rootedTimer    > 0) { rootedTimer  -= delta; if (rootedTimer  <0) rootedTimer  =0; }
        if (blizzardSlowTimer>0){ blizzardSlowTimer-=delta; if(blizzardSlowTimer<0)blizzardSlowTimer=0; }
        if (dashTimer      > 0) { dashTimer    -= delta; if (dashTimer    <0) dashTimer    =0; }
        if (boltTimer      > 0) boltTimer -= delta;
        if (bladeSpinTimer > 0) bladeSpinTimer -= delta;
        if (gravWellTimer  > 0) gravWellTimer -= delta;
        if (chargeTimer    > 0 && charging) chargeTimer += delta;
        // Special ready when charge is full (deal damage to charge, no kill needed)
        specialReady = (specialCooldown <= 0 && specialCharge >= SP_CHARGE_MAX);
        spFxType = FX_NONE;  // clear signal each frame
        if (comboTimer     > 0) comboTimer     -= delta;
        else comboStep = 0; // reset combo if timeout

        // -- Knockback -------------------------------------------------
        if (Math.abs(kbX) > 2f) {
            vx += kbX; vy += kbY;
            kbX *= 0.45f; kbY = 0f;
        } else { kbX = 0f; }

        // -- Input -----------------------------------------------------
        if (isPlayer) handlePlayerInput(left, right, jump, attack, block, ultraKey, dt, delta);
        else          handleAI(delta, dt, opponent);

        // -- Special move update --------------------------------------
        updateSpecial(delta, dt);

        // -- Physics: gravity + air drag --------------------------------
        if (!onGround) vx *= AIR_DRAG;
        // Suppress gravity during soul reap dash
        if (!dashInvuln) {
            vy += GRAVITY * Arena.getGravityMult() * dt;
        }
        x  += vx * dt;
        y  += vy * dt;

        // -- Platform collision ----------------------------------------
        checkPlatformCollision(delta);

        // -- Ground friction -------------------------------------------
        if (onGround) {
            vx *= Arena.getSurfaceFriction();
            if (Math.abs(vx) < 2f) vx = 0f;
        }

        // -- Screen clamp -----------------------------------------------
        if (x < W / 2f)           { x = W / 2f;           vx = 0f; onWall = true; }
        else if (x > Arena.W-W/2f){ x = Arena.W - W / 2f; vx = 0f; onWall = true; }
        else                       { onWall = false; }

        // -- Fell off screen → die ------------------------------------
        if (y > Arena.H + 60) { health = 0; triggerDeath(); }

        // -- State machine update -------------------------------------
        updateStateAuto();

        // -- Animation cycle -------------------------------------------
        int animSpeed = (state == ST_RUN) ? 80 : (state == ST_WALK) ? 130 : 200;
        animTimer += delta;
        if (animTimer >= animSpeed) {
            animTimer = 0;
            animFrame = (animFrame + 1) % 8;
        }

        // -- Run hold timer -------------------------------------------
        if ((left || right) && onGround) {
            runHoldTimer += delta;
        } else {
            runHoldTimer = 0;
        }
    }

    // -----------------------------------------------------------------
    //  Player input
    // -----------------------------------------------------------------
    private void handlePlayerInput(boolean left, boolean right,
                                   boolean jump, boolean attack,
                                   boolean block, boolean ultraKey,
                                   float dt, int delta) {
        // CC prevents movement
        if (isCrowdControlled()) { vx *= 0.5f; return; }
        float ccFactor = getEffectiveSpeedFactor();
        float spd = ((runHoldTimer > 320) ? RUN_SPEED : WALK_SPEED) * speedMult / 100f * ccFactor * (speedBoost ? 2.0f : 1.0f);

        if (left)  { vx = -spd; facingRight = false; }
        if (right) { vx =  spd; facingRight = true;  }

        // Jump / wall-jump
        if (jump) {
            if (onGround && jumpsLeft > 0) {
                vy = JUMP_VY; jumpsLeft--; onGround = false;
            } else if (!onGround && jumpsLeft > 0) {
                vy = JUMP2_VY; jumpsLeft--;
                jumpParticle = true;
            } else if (onWall && !onGround) {
                vy = WALLJUMP_VY;
                vx = facingRight ? -WALLJUMP_VX : WALLJUMP_VX;
                facingRight = !facingRight;
                state = ST_WALLJUMP;
            }
        }

        // Block
        if (!block && state == ST_BLOCK) blockCooldown = 2000;

        // Final Attack (Ultra)
        if (ultraKey && ultraCooldown <= 0 && attackCooldown <= 0 && onGround) {
            ultraTimer    = ULTRA_DUR;
            ultraCooldown = 4000;
            attackCooldown = ULTRA_DUR + 200;
            state = ST_ULTRA;
            punchTimer = ULTRA_DUR; // reuse punch timer for hitbox
            return;
        }

        // Attack chain
        if (attack && attackCooldown <= 0) {
            if (weaponType == Weapon.TYPE_NONE || weaponType == Weapon.TYPE_SWORD) {
                doComboAttack();
            } else if (weaponAmmo > 0 && shootCooldown <= 0) {
                wantsToShoot  = true;
                shootCooldown = Weapon.COOLDOWN[weaponType];
                weaponAmmo--;
                if (weaponAmmo <= 0) weaponType = Weapon.TYPE_NONE;
                attackCooldown = 80;
                state = ST_SHOOT;
            }
        }
    }

    private void doComboAttack() {
        if (comboTimer > 0 && comboStep == 1) {
            // Second hit: Kick
            kickTimer      = KICK_DUR;
            attackCooldown = KICK_COOLDOWN();
            comboStep      = 2;
            comboTimer     = 400;
            state          = ST_KICK;
            punchTimer     = 0;
        } else if (comboTimer > 0 && comboStep == 2) {
            // Third hit: Uppercut (big launch)
            punchTimer     = PUNCH_DUR + 100;
            attackCooldown = 600;
            comboStep      = 0;
            comboTimer     = 0;
            state          = ST_PUNCH;
            kickTimer      = 0;
        } else {
            // First hit: Punch
            punchTimer     = PUNCH_DUR;
            attackCooldown = PUNCH_COOLDOWN;
            comboStep      = 1;
            comboTimer     = 500;
            state          = ST_PUNCH;
        }
    }

    private int KICK_COOLDOWN() { return 350; }

    // -----------------------------------------------------------------
    //  AI logic  (FSM with 4 modes)
    // -----------------------------------------------------------------
    private void handleAI(int delta, float dt, Stickman target) {
        if (target == null || !target.alive) return;

        float dx   = target.x - x;
        float dist = Math.abs(dx);

        facingRight = (dx > 0);
        aiStateTimer -= delta;
        aiJumpTimer  -= delta;

        if (aiStateTimer <= 0) {
            if (dist > 200 && target.weaponType != Weapon.TYPE_NONE) {
                aiMode = 3; aiStateTimer = 600; // try to get a weapon
            } else if (dist > 120) {
                aiMode = 0; aiStateTimer = 250 + rand.nextInt(200);
            } else if (dist > 40) {
                aiMode = 0; aiStateTimer = 180 + rand.nextInt(120);
                if ((target.y - y) < -30 && onGround && jumpsLeft > 0 && aiJumpTimer <= 0) {
                    vy = JUMP_VY; jumpsLeft--; onGround = false; aiJumpTimer = 700;
                }
            } else {
                aiMode = 1; aiStateTimer = 300 + rand.nextInt(250);
            }
        }

        if (isCrowdControlled()) { vx *= 0.5f; return; }
        float aiSpeedF = getEffectiveSpeedFactor();
        float spd = ((dist > 80) ? RUN_SPEED : WALK_SPEED) * aiSpeedF;

        switch (aiMode) {
            case 0: // Chase
                if (dx > 14)  vx =  spd;
                else if (dx < -14) vx = -spd;
                // Random jump to platform
                if (onGround && jumpsLeft > 0 && aiJumpTimer <= 0 && rand.nextInt(200) < 3) {
                    vy = JUMP_VY; jumpsLeft--; onGround = false; aiJumpTimer = 900;
                }
                break;
            case 1: // Attack
                if (attackCooldown <= 0) {
                    if (weaponType == Weapon.TYPE_NONE || weaponType == Weapon.TYPE_SWORD) {
                        doComboAttack();
                    } else if (weaponAmmo > 0 && shootCooldown <= 0) {
                        wantsToShoot  = true;
                        shootCooldown = Weapon.COOLDOWN[weaponType];
                        weaponAmmo--;
                        if (weaponAmmo <= 0) weaponType = Weapon.TYPE_NONE;
                        attackCooldown = 280 + rand.nextInt(300);
                    }
                }
                // Micro-strafe
                aiDiffTimer += delta;
                if (aiDiffTimer > 400) {
                    aiStrafeDir = -aiStrafeDir; aiDiffTimer = 0;
                }
                vx = aiStrafeDir * WALK_SPEED * 0.5f;
                break;
            case 2: // Retreat
                vx = facingRight ? -WALK_SPEED : WALK_SPEED;
                break;
            case 3: // Get weapon (move toward platform center)
                vx = (x < Arena.W / 2) ? WALK_SPEED : -WALK_SPEED;
                break;
        }
    }

    // -----------------------------------------------------------------
    //  Combat helpers
    // -----------------------------------------------------------------

    public void takeDamage(int dmg, float knockX, float knockY) {
        if (!alive || invincTimer > 0) return;
        if (dashInvuln) return;  // soul reap dash = invulnerable
        if (shieldWall) return;  // key 9 shield wall: invulnerable
        // WRAITH: 10% dodge on hit
        if (traitPhantom && rand.nextInt(10) == 0) return;
        // BLADESTORM: activate spin when hit
        if (traitBladeStorm && bladeSpinTimer <= 0 && rand.nextInt(4) == 0) bladeSpinTimer = 400;
        // VOLTRA: electric spark when taking damage
        if (traitVoltDash) voltStunChain = 0;
        // GOLEM: halve knockback
        float kx = traitIronHide ? knockX * 0.5f : knockX;
        float ky = traitIronHide ? knockY * 0.5f : knockY;
        knockX = kx; knockY = ky;
        if (state == ST_BLOCK) {
            dmg = dmg / 4;
            if (shadowweaveActive) return; // shadowweave: no damage while blocking
        }
        // Armor: dragonhide reduces bullet damage (handled in canvas)
        if (hasFrostShield && dmg > 10) {
            hasFrostShield = false; // one-time block
            return;
        }
        // Defmult defense
        dmg = dmg * 100 / Math.max(1, defMult);
        health -= dmg;
        invincTimer = 280 + dmg;
        kbX = knockX * 0.8f;
        kbY = knockY * 0.6f;
        if (health <= 0) { health = 0; triggerDeath(); }
    }

    public void triggerDeath() {
        alive      = false;
        state      = ST_DEAD;
        deathVX    = facingRight ? -100f : 100f;
        deathVY    = -300f;
        deathTimer = 2000;
    }

    // -----------------------------------------------------------------
    //  Platform collision
    // -----------------------------------------------------------------
    private void checkPlatformCollision(int delta) {
        onGround = false;
        int[] landType = { Arena.PT_SOLID };
        int surfaceY = Arena.checkLanding(x, y, W / 2f, vy, landType);
        if (surfaceY >= 0) {
            // Apply platform type effect
            if (landType[0] == Arena.PT_LAVA) {
                takeDamage(200, 0f, -400f); // instant lava kill
                return;
            }
            if (landType[0] == Arena.PT_SPIKE && vy > 50f) {
                takeDamage(35, 0f, -350f);  // spike damage on land
            }
            y        = surfaceY;
            vy       = 0f;
            onGround = true;
            jumpsLeft = 2;
        }
    }

    // -----------------------------------------------------------------
    //  State auto-update
    // -----------------------------------------------------------------
    private void updateStateAuto() {
        if (state == ST_ULTRA  && ultraTimer > 0)  return;
        if (state == ST_PUNCH  && punchTimer > 0)  return;
        if (state == ST_KICK   && kickTimer  > 0)  return;
        if (state == ST_WALLJUMP && !onGround && vy < 0) return;
        if (!onGround)  { state = (vy > 0) ? ST_FALL : ST_JUMP; return; }
        if (state == ST_BLOCK) return;
        if (state == ST_SHOOT) { state = ST_IDLE; return; }
        if (runHoldTimer > 320 && Math.abs(vx) > 5f) { state = ST_RUN;  return; }
        if (Math.abs(vx) > 5f)                        { state = ST_WALK; return; }
        state = ST_IDLE;
    }

    // -----------------------------------------------------------------
    //  Death update
    // -----------------------------------------------------------------
    private void updateDeath(int delta) {
        float dt = delta / 1000f;
        deathVY  += 420f * dt;
        x        += deathVX * dt;
        y        += deathVY * dt;
        deathAngle += 320f * dt;
        deathVX  *= 0.97f;
        if (deathTimer > 0) deathTimer -= delta;
    }

    // -----------------------------------------------------------------
    //  Hitbox helpers
    // -----------------------------------------------------------------

    public int[] getBounds() {
        return new int[]{ (int)(x - W/2), (int)(y - H), W, H };
    }

    /** Active attack hitbox for punch/kick/ultra; null if none active. */
    public int[] getAttackRect() {
        if (state == ST_ULTRA && ultraTimer > 0) {
            // Big AOE around the player
            return new int[]{ (int)(x - 45), (int)(y - H - 10), 90, H + 20 };
        }
        if (punchTimer > 0) {
            int hx = facingRight ? (int)x : (int)x - PUNCH_RANGE;
            return new int[]{ hx, (int)(y - H * 2/3), PUNCH_RANGE, H / 3 };
        }
        if (kickTimer > 0) {
            int hx = facingRight ? (int)x : (int)x - KICK_RANGE;
            return new int[]{ hx, (int)(y - H/3), KICK_RANGE, H/3 };
        }
        return null;
    }

    /** Damage for the currently active attack rect. */
    public int getAttackDamage() {
        if (state == ST_ULTRA) return ULTRA_DMG;
        if (punchTimer > 0)    return (comboStep == 0) ? UPPERCUT_DMG : PUNCH_DMG;
        if (kickTimer  > 0)    return KICK_DMG;
        return 0;
    }

    public boolean overlapsRect(int rx, int ry, int rw, int rh) {
        int[] b = getBounds();
        return b[0] < rx+rw && b[0]+b[2] > rx && b[1] < ry+rh && b[1]+b[3] > ry;
    }

    // =================================================================
    //  DRAWING  — pixel-art style procedural stickman
    // =================================================================

    /**
     * Draw at current position.
     * @param g      Graphics context
     * @param color  body color
     */
    public void draw(Graphics g, int color) {
        if (!alive) { drawDead(g, (int)x, (int)y, color); return; }

        // Invincibility flicker
        if (invincTimer > 0 && (invincTimer / 60) % 2 == 1) return;

        int sx = (int) x;
        int sy = (int) y;

        // Key body Y points (pixel-art proportions: chunkier joints)
        int feetY  = sy;
        int waistY = sy - 16;
        int chestY = sy - 30;
        int neckY  = sy - 38;
        int headCY = sy - 44;   // centre of head circle
        int dir    = facingRight ? 1 : -1;

        // ---- Shadow --------------------------------------------------
        g.setColor(0x0A0A22);
        g.fillArc(sx - 11, feetY, 22, 7, 0, 360);

        // ---- Ultra flash aura ----------------------------------------
        if (state == ST_ULTRA && ultraTimer > 0) {
            int pulse = (ultraTimer / 80) % 2;
            g.setColor(pulse == 0 ? 0xFFFFAA : 0xFF6600);
            g.drawArc(sx - 22, sy - H - 12, 44, H + 20, 0, 360);
            g.drawArc(sx - 20, sy - H - 10, 40, H + 16, 0, 360);
        }

        // ---- Special move auras --------------------------------------
        drawSpecialAura(g, sx, sy, color);

        // ---- CC effects ----------------------------------------------
        if (frozenTimer > 0) {
            int fp = (frozenTimer / 100) % 2;
            g.setColor(fp == 0 ? 0x88CCFF : 0xAAEEFF);
            g.drawArc(sx - 14, sy - H - 4, 28, H + 8, 0, 360);
            g.drawArc(sx - 13, sy - H - 3, 26, H + 6, 0, 360);
        }
        if (rootedTimer > 0) {
            g.setColor(0x22AA44);
            g.drawLine(sx - 8, sy, sx - 12, sy + 8);
            g.drawLine(sx + 8, sy, sx + 12, sy + 8);
            g.drawLine(sx,     sy, sx,       sy + 10);
        }
        if (blizzardSlowTimer > 0 && (blizzardSlowTimer / 200) % 2 == 0) {
            g.setColor(0xAADDFF);
            g.drawArc(sx - 12, sy - H, 24, H, 0, 360);
        }
        // ---- WRAITH: ghost shimmer effect -----------------------
        if (traitPhantom && (System.currentTimeMillis() / 80) % 3 == 0) {
            g.setColor(0x330066);
            g.drawArc(sx - 16, sy - H - 4, 32, H + 8, 0, 360);
        }
        // ---- VOLTRA: electric corona --------------------------------
        if (traitVoltDash && (System.currentTimeMillis() / 60) % 4 < 2) {
            g.setColor(0x004488);
            g.drawArc(sx - 14, sy - H - 2, 28, H + 4, 0, 360);
            g.setColor(0x00AAFF);
            int ep = (int)(System.currentTimeMillis() / 80) % 8;
            double ea = Math.toRadians(ep * 45);
            g.drawLine(sx, sy - H/2, sx + (int)(Math.cos(ea)*14), sy - H/2 + (int)(Math.sin(ea)*14));
        }
        // ---- Special ready flash above head --------------------------
        if (specialReady && (System.currentTimeMillis() / 400) % 2 == 0) {
            g.setColor(0xFFDD00);
            g.fillRect(sx - 3, sy - H - 26, 6, 5);
            g.fillRect(sx - 1, sy - H - 30, 2, 3);
        }

        // ---- Legs (pixel-art thick lines — draw 2 parallel lines) ---
        drawThickLegs(g, sx, waistY, feetY, color);

        // ---- Body (torso — 2-pixel wide) ----------------------------
        // GOLEM: extra-thick torso (stone body)
        if (traitIronHide) {
            g.setColor(0x886633);
            g.fillRect(sx - 4, neckY, 10, waistY - neckY + 4); // thick stone body
            g.setColor(0x443322);
            g.drawRect(sx - 4, neckY, 10, waistY - neckY + 4);
        } else {
            g.setColor(color);
            g.drawLine(sx,   neckY, sx,   waistY);
            g.drawLine(sx+1, neckY, sx+1, waistY); // 2nd line for thickness
        }

        // ---- Head (pixel-art circle + face detail) -------------------
        drawHead(g, sx, headCY, dir, color);

        // ---- Arms + weapon -------------------------------------------
        drawArms(g, sx, neckY, chestY, dir, color);

        // ---- Victory / defeat pose override --------------------------
        if (state == ST_VICTORY) {
            drawVictoryPose(g, sx, sy, color, dir);
        } else if (state == ST_DEFEAT) {
            drawDefeatPose(g, sx, sy, color);
        }

        // ---- Shop item visual indicators (armor glow) ----------------
        if (hasDragonhide) {
            g.setColor(0x882200);
            g.drawArc(sx-14, sy-H-2, 28, H+4, 0, 360);
        }
        if (hasCyberPlating) {
            // Cyan tech lines on torso
            g.setColor(0x004488);
            g.drawLine(sx-6, sy-28, sx+6, sy-28);
            g.drawLine(sx-6, sy-22, sx+6, sy-22);
        }
        if (hasEmberGauntlets) {
            // Orange fists
            g.setColor(0xFF4400);
            g.fillRect(sx + dir*9, sy-30, 5, 5);
        }
        if (shadowweaveActive && state == ST_BLOCK) {
            // Shadow shimmer when blocking
            g.setColor(0x220044);
            g.drawArc(sx-16, sy-H-4, 32, H+8, 0, 360);
        }

        // ---- HP bar above head (when hurt) ---------------------------
        if (health < 100) {
            int bw = 28, bx = sx - 14, by = headCY - 18;
            // Background
            g.setColor(0x111122);
            g.fillRect(bx, by, bw, 4);
            // Bar fill
            int hw = bw * health / 100;
            g.setColor(health > 60 ? 0x22EE44 : (health > 30 ? 0xFFCC00 : 0xFF2222));
            g.fillRect(bx, by, hw, 4);
            // Border
            g.setColor(0x445566);
            g.drawRect(bx, by, bw, 4);
        }

        // ---- Ultra cooldown tiny bar (when charging) ----------------
        if (ultraCooldown > 0) {
            int uby = headCY - 24;
            g.setColor(0x113322);
            g.fillRect(sx - 14, uby, 28, 2);
            int ufill = 28 - (28 * ultraCooldown / 4000);
            g.setColor(0x00FFAA);
            g.fillRect(sx - 14, uby, ufill, 2);
        }
    }

    // -----------------------------------------------------------------
    //  Head drawing
    // -----------------------------------------------------------------
    private void drawHead(Graphics g, int sx, int headCY, int dir, int color) {
        // Head circle (filled dark + outline in color for pixel-art feel)
        g.setColor(0x1A1A2E);
        g.fillArc(sx - 8, headCY - 8, 16, 16, 0, 360);
        g.setColor(color);
        g.drawArc(sx - 8, headCY - 8, 16, 16, 0, 360);
        g.drawArc(sx - 7, headCY - 7, 14, 14, 0, 360); // double outline = pixel thick

        // Eyes  (two-pixel dots — pixel art style)
        int eyeX = sx + dir * 3;
        g.setColor(0xFFFFFF);
        g.fillRect(eyeX - 1, headCY - 2, 3, 3);

        // Pupil
        g.setColor(0x000000);
        g.fillRect(eyeX, headCY - 1, 1, 2);

        // Angry eyebrow when in combat states
        if (state == ST_PUNCH || state == ST_KICK || state == ST_ULTRA) {
            g.setColor(color);
            g.drawLine(eyeX - 2, headCY - 5, eyeX + 2, headCY - 7);
        }

        // Mouth (grin during ultra, normal line otherwise)
        if (state == ST_ULTRA) {
            g.setColor(0xFFFFAA);
            g.drawLine(sx + dir, headCY + 4, sx + dir * 5, headCY + 4);
        }
    }

    // -----------------------------------------------------------------
    //  Thick legs (pixel-art double-line)
    // -----------------------------------------------------------------
    private void drawThickLegs(Graphics g, int sx, int waistY, int feetY, int color) {
        g.setColor(color);
        // GOLEM: heavy stomp legs
        if (traitIronHide) {
            g.setColor(0x886633);
            if (state == ST_WALK || state == ST_RUN) {
                int stomp = ((animFrame % 4) < 2) ? 0 : -4;
                g.fillRect(sx - 14, waistY, 12, feetY - waistY + stomp); // left stone leg
                g.fillRect(sx + 3,  waistY, 12, feetY - waistY - stomp); // right stone leg
                g.setColor(0x553322);
                g.fillRect(sx - 15, feetY + stomp - 2, 14, 6); // left boot
                g.fillRect(sx + 2,  feetY - stomp - 2, 14, 6);
            } else {
                g.fillRect(sx - 14, waistY, 12, feetY - waistY);
                g.fillRect(sx + 3,  waistY, 12, feetY - waistY);
                g.setColor(0x553322);
                g.fillRect(sx - 15, feetY - 2, 14, 6);
                g.fillRect(sx + 2,  feetY - 2, 14, 6);
            }
            return;
        }
        if (state == ST_WALK || state == ST_RUN) {
            int swingAmp = (state == ST_RUN) ? 13 : 9;
            int legSwing = ((animFrame % 4) < 2) ? swingAmp : -swingAmp;
            // Left leg
            g.drawLine(sx,   waistY, sx - 9,  feetY + legSwing/2);
            g.drawLine(sx+1, waistY, sx - 8,  feetY + legSwing/2);
            // Right leg
            g.drawLine(sx,   waistY, sx + 9,  feetY - legSwing/2);
            g.drawLine(sx+1, waistY, sx + 10, feetY - legSwing/2);
        } else if (state == ST_JUMP || state == ST_FALL || state == ST_WALLJUMP) {
            int bent = (vy < 0) ? -14 : -6;
            g.drawLine(sx,   waistY, sx - 11, feetY + bent);
            g.drawLine(sx+1, waistY, sx - 10, feetY + bent);
            g.drawLine(sx,   waistY, sx + 11, feetY + bent);
            g.drawLine(sx+1, waistY, sx + 12, feetY + bent);
        } else if (state == ST_KICK && kickTimer > 0) {
            // Kick pose: one leg fully extended forward
            int dir = facingRight ? 1 : -1;
            g.drawLine(sx, waistY, sx + dir * 20, feetY - 8);
            g.drawLine(sx+1, waistY, sx + dir*21, feetY - 7);
            g.drawLine(sx, waistY, sx - dir * 8,  feetY);
            g.drawLine(sx+1, waistY, sx - dir*7,  feetY);
        } else {
            // Idle / standing
            g.drawLine(sx,   waistY, sx - 9,  feetY);
            g.drawLine(sx+1, waistY, sx - 8,  feetY);
            g.drawLine(sx,   waistY, sx + 9,  feetY);
            g.drawLine(sx+1, waistY, sx + 10, feetY);
        }
        // Feet dots (boots)
        g.setColor(darken(color));
        g.fillRect(sx - 12, feetY, 10, 3);
        g.fillRect(sx + 3,  feetY, 10, 3);
    }

    private int darken(int col) {
        int r = ((col >> 16) & 0xFF) / 2;
        int gv= ((col >>  8) & 0xFF) / 2;
        int b = (col & 0xFF) / 2;
        return (r << 16) | (gv << 8) | b;
    }

    // -----------------------------------------------------------------
    //  Arms drawing
    // -----------------------------------------------------------------
    private void drawArms(Graphics g, int sx, int neckY, int chestY, int dir, int color) {
        int armY  = chestY;
        int swing = ((animFrame % 4) < 2) ? 5 : -5;

        g.setColor(color);

        // ---- BLADESTORM (char 8): spinning blade halo when hit or attack ----
        if (traitBladeStorm && bladeSpinTimer > 0) {
            int ang = (bladeSpinTimer / 40) * 45; // rotate per frame
            for (int bi = 0; bi < 4; bi++) {
                double a = Math.toRadians(ang + bi * 90);
                int bx = sx + (int)(Math.cos(a) * 18);
                int by = armY + (int)(Math.sin(a) * 18);
                g.setColor(0xFFCC00);
                g.fillRect(bx-3, by-1, 7, 2);
                g.fillRect(bx-1, by-3, 2, 7);
            }
            g.setColor(color);
        }

        // ---- VOLTRA (char 9): electric arcs on arms -------------------------
        if (traitVoltDash && (System.currentTimeMillis() / 150) % 2 == 0) {
            g.setColor(0x00CCFF);
            g.drawLine(sx, armY, sx + dir*20, armY - 4 + (int)(Math.sin(animFrame)*3));
            g.drawLine(sx, armY, sx - dir*12, armY + 6 + (int)(Math.cos(animFrame)*3));
            return;
        }

        // ---- GOLEM (char 10): massive blocky arms ---------------------------
        if (traitIronHide) {
            g.setColor(0xAA8844);
            // Left rock arm
            g.fillRect(sx - 18, armY - 3, 10, 10); // stone fist
            g.drawLine(sx, armY, sx - 14, armY);
            g.drawLine(sx+1, armY+1, sx - 13, armY + 1);
            // Right rock arm
            g.fillRect(sx + dir * 10, armY - 3, 10, 10);
            g.drawLine(sx, armY, sx + dir * 14, armY);
            g.drawLine(sx+1, armY+1, sx + dir*15, armY + 1);
            return;
        }

        // ---- WRAITH (char 11): ghostly ethereal arms ------------------------
        if (traitPhantom) {
            // Wispy semi-transparent arms (drawn with gaps)
            g.setColor(0x8866CC);
            g.drawLine(sx, armY, sx + dir*14, armY - 8);
            g.drawLine(sx, armY, sx - dir*10, armY + 4);
            // Phantom finger wisps
            g.setColor(0xAA88EE);
            g.drawLine(sx + dir*14, armY - 8, sx + dir*20, armY - 14);
            g.drawLine(sx + dir*14, armY - 8, sx + dir*18, armY - 5);
            return;
        }

        if (state == ST_ULTRA && ultraTimer > 0) {
            // ULTRA: both fists thrust forward in a massive X pattern
            g.setColor(0xFFFF00);
            g.drawLine(sx, armY, sx + dir * 26, armY - 10);
            g.drawLine(sx+1, armY, sx + dir*27, armY - 9);
            g.drawLine(sx, armY, sx + dir * 22, armY + 6);
            g.drawLine(sx+1, armY, sx + dir*23, armY + 7);
            // Fist flash
            g.fillRect(sx + dir * 23, armY - 13, 5, 10);
            return;
        }

        if (state == ST_PUNCH && punchTimer > 0) {
            int isUpper = (comboStep == 0) ? 1 : 0;
            // Extended punch arm (thick)
            g.drawLine(sx, armY, sx + dir * 24, armY - isUpper * 12);
            g.drawLine(sx+1, armY, sx + dir*25, armY - isUpper * 12);
            // Fist square pixel
            g.fillRect(sx + dir * 22, armY - isUpper * 13, 4, 5);
            // Back arm
            g.setColor(darken(color));
            g.drawLine(sx, armY, sx - dir * 9, armY + 8);
            g.drawLine(sx+1, armY, sx - dir*8, armY + 9);
            return;
        }

        if (state == ST_KICK && kickTimer > 0) {
            // One arm balancing
            g.drawLine(sx, armY, sx - dir * 14, armY - 3);
            g.drawLine(sx+1, armY, sx - dir*13, armY - 2);
            g.drawLine(sx, armY, sx + dir * 8,  armY + 5);
            g.drawLine(sx+1, armY, sx + dir*9,  armY + 6);
            return;
        }

        if (state == ST_BLOCK) {
            // Cross-guard block
            g.drawLine(sx, armY, sx + dir * 11, armY - 7);
            g.drawLine(sx+1, armY, sx + dir*12, armY - 6);
            g.drawLine(sx, armY, sx + dir * 7,  armY + 7);
            g.drawLine(sx+1, armY, sx + dir*8,  armY + 8);
            // Shield bar
            g.setColor(0x8899FF);
            g.drawLine(sx + dir * 10, armY - 9, sx + dir * 10, armY + 9);
            g.drawLine(sx + dir * 11, armY - 9, sx + dir * 11, armY + 9);
            return;
        }

        if (state == ST_JUMP || state == ST_FALL || state == ST_WALLJUMP) {
            g.drawLine(sx, armY, sx - 14, armY - 7);
            g.drawLine(sx+1, armY, sx - 13, armY - 6);
            g.drawLine(sx, armY, sx + 14, armY - 7);
            g.drawLine(sx+1, armY, sx + 15, armY - 6);
            return;
        }

        // Default: idle/walk/run with weapon
        if (weaponType != Weapon.TYPE_NONE && weaponType != Weapon.TYPE_SWORD) {
            // Ranged weapon: one arm forward aiming
            g.drawLine(sx, armY, sx + dir * 15, armY + (int)(Math.sin(animFrame) * 2));
            g.drawLine(sx+1, armY, sx + dir*16, armY + (int)(Math.sin(animFrame)*2));
            g.setColor(darken(color));
            g.drawLine(sx, armY, sx - dir * 9, armY + swing);
            g.drawLine(sx+1, armY, sx - dir*8, armY + swing + 1);
            drawHeldWeapon(g, sx + dir * 15, armY, dir);
        } else if (weaponType == Weapon.TYPE_SWORD) {
            g.drawLine(sx, armY, sx + dir * 13, armY - 5);
            g.drawLine(sx+1, armY, sx + dir*14, armY - 4);
            g.setColor(darken(color));
            g.drawLine(sx, armY, sx - dir * 8, armY + 8);
            // Sword
            g.setColor(0xDDDDFF);
            g.drawLine(sx + dir * 12, armY - 5, sx + dir * 24, armY - 13);
            g.drawLine(sx + dir * 12, armY - 4, sx + dir * 24, armY - 12); // thick blade
            // Guard
            g.setColor(0xAA8833);
            g.drawLine(sx + dir * 10, armY - 8, sx + dir * 10, armY - 2);
        } else {
            // Fists: natural walk swing
            g.drawLine(sx, armY, sx - 11, armY + swing);
            g.drawLine(sx+1, armY, sx - 10, armY + swing + 1);
            g.drawLine(sx, armY, sx + 11, armY - swing);
            g.drawLine(sx+1, armY, sx + 12, armY - swing + 1);
            // Fist dot
            g.fillRect(sx - 13, armY + swing - 1, 3, 3);
            g.fillRect(sx + 11, armY - swing - 1, 3, 3);
        }
    }

    // -----------------------------------------------------------------
    //  Held weapon graphics (pixel-art detailed)
    // -----------------------------------------------------------------
    private void drawHeldWeapon(Graphics g, int ax, int ay, int dir) {
        switch (weaponType) {
            case Weapon.TYPE_PISTOL:
                g.setColor(0xCCCCCC); g.fillRect(ax, ay - 3, 10 * dir, 5);
                g.setColor(0x555555); g.fillRect(ax + dir, ay + 2, 4, 4);
                g.setColor(0xFFFF88); g.fillRect(ax + dir * 9, ay - 3, 2, 2); // muzzle
                break;
            case Weapon.TYPE_SHOTGUN:
                g.setColor(0x886633); g.fillRect(ax - dir, ay - 2, 16 * dir, 6);
                g.setColor(0x444444); g.fillRect(ax, ay + 4, 5, 4);
                g.setColor(0xFFDD88); g.fillRect(ax + dir * 14, ay - 2, 2, 2);
                break;
            case Weapon.TYPE_AK47:
                g.setColor(0x556633); g.fillRect(ax, ay - 3, 18 * dir, 5);
                g.setColor(0x333322); g.fillRect(ax + 5 * dir, ay + 2, 6, 5); // mag
                g.setColor(0x333333); g.fillRect(ax + dir, ay - 3, 4, 3);    // stock
                g.setColor(0xFFFF88); g.fillRect(ax + dir * 17, ay - 3, 2, 2);
                break;
            case Weapon.TYPE_SNIPER:
                g.setColor(0x667788); g.fillRect(ax, ay - 2, 22 * dir, 4);
                g.setColor(0x888888); g.fillRect(ax + 7 * dir, ay - 6, 4, 3); // scope
                g.setColor(0x334455); g.fillRect(ax + 3 * dir, ay + 2, 5, 3);
                g.setColor(0xAADDFF); g.fillRect(ax + dir * 21, ay - 2, 2, 2); // glint
                break;
            case Weapon.TYPE_GRENADE:
                g.setColor(0x557733); g.fillArc(ax - 4, ay - 5, 10, 10, 0, 360);
                g.setColor(0x999999); g.drawLine(ax + dir, ay - 5, ax + dir * 2, ay - 9);
                g.setColor(0xFF6600); g.fillRect(ax + dir * 2, ay - 10, 2, 2); // fuse
                break;
            case Weapon.TYPE_REVOLVER:
                g.setColor(0xAA8855); g.fillRect(ax, ay - 3, 12 * dir, 5);
                g.setColor(0x776644); g.fillArc(ax + 3 * dir, ay - 5, 8, 8, 0, 360); // cylinder
                g.setColor(0xDDAA44); g.fillRect(ax + dir * 11, ay - 3, 2, 2);
                break;
            case Weapon.TYPE_ROCKET:
                g.setColor(0x885533); g.fillRect(ax, ay - 3, 16 * dir, 6);
                g.setColor(0xFF4400); g.fillRect(ax + dir * 14, ay - 2, 4 * dir, 4); // warhead
                g.setColor(0xFF8800); g.fillArc(ax + dir * 14, ay, 5, 5, 0, 360);
                break;
            case Weapon.TYPE_LASER:
                g.setColor(0x4444CC); g.fillRect(ax, ay - 2, 18 * dir, 4);
                g.setColor(0x8888FF); g.fillRect(ax + 4 * dir, ay - 4, 4, 2);
                g.setColor(0x00AAFF); g.drawLine(ax + dir * 17, ay, ax + dir * 32, ay); // beam
                g.setColor(0x0044FF); g.drawLine(ax + dir * 17, ay + 1, ax + dir * 32, ay + 1);
                break;
            case Weapon.TYPE_FLAMETHROWER:
                g.setColor(0x885522); g.fillRect(ax, ay - 4, 14 * dir, 7);
                g.setColor(0xFF6600); // flame jets
                g.fillArc(ax + dir * 13, ay - 5, 8, 8, 0, 360);
                g.fillArc(ax + dir * 15, ay - 3, 6, 6, 0, 360);
                g.setColor(0xFF9900);
                g.fillArc(ax + dir * 17, ay - 1, 5, 5, 0, 360);
                break;
            case Weapon.TYPE_CROSSBOW:
                g.setColor(0x886644); g.fillRect(ax, ay - 2, 16 * dir, 4);
                g.setColor(0x664422); // bow arms
                g.drawLine(ax + 4 * dir, ay - 2, ax + 2 * dir, ay - 8);
                g.drawLine(ax + 4 * dir, ay + 2, ax + 2 * dir, ay + 8);
                g.setColor(0xFFFFAA); // arrow
                g.drawLine(ax + dir * 5, ay, ax + dir * 18, ay);
                g.fillRect(ax + dir * 17, ay - 1, 2, 3);
                break;
            case Weapon.TYPE_BOOMERANG:
                g.setColor(0xCC8833);
                // Curved shape: 3 line segments
                g.drawLine(ax, ay, ax + dir * 10, ay - 6);
                g.drawLine(ax + dir * 10, ay - 6, ax + dir * 16, ay);
                g.drawLine(ax, ay, ax + dir * 6, ay + 4);
                // Thickness
                g.drawLine(ax, ay + 1, ax + dir * 10, ay - 5);
                break;
        }
    }

    // -----------------------------------------------------------------
    //  Dead ragdoll
    // -----------------------------------------------------------------
    private void drawDead(Graphics g, int sx, int sy, int color) {
        // Grayed-out tumbling ragdoll
        g.setColor(0x666677);
        int hx = sx + (deathVX > 0 ? 10 : -10);
        // Head
        g.drawArc(hx - 8, sy - 22, 16, 16, 0, 360);
        // X eyes
        g.setColor(0xFFFFFF);
        g.drawLine(hx - 4, sy - 19, hx - 1, sy - 16);
        g.drawLine(hx - 1, sy - 19, hx - 4, sy - 16);
        g.drawLine(hx + 1, sy - 19, hx + 4, sy - 16);
        g.drawLine(hx + 4, sy - 19, hx + 1, sy - 16);
        // Body flat
        g.setColor(0x666677);
        g.drawLine(sx - 16, sy - 8, sx + 16, sy - 8);
        g.drawLine(sx - 16, sy - 7, sx + 16, sy - 7);
        // Limbs splayed
        g.drawLine(sx - 16, sy - 8, sx - 22, sy + 6);
        g.drawLine(sx + 16, sy - 8, sx + 22, sy + 6);
        g.drawLine(sx - 6,  sy - 2, sx - 12, sy + 12);
        g.drawLine(sx + 6,  sy - 2, sx + 12, sy + 12);
    }

    // =================================================================
    //  SPECIAL MOVE SYSTEM
    // =================================================================

    /**
     * Assign the correct special based on character ID.
     * Call this after setting charId.
     */
    /** Call after charId is set to apply character-specific passive traits. */
    public void applyCharTraits() {
        traitBladeStorm = (charId == 8);
        traitVoltDash   = (charId == 9);
        traitIronHide   = (charId == 10);
        traitPhantom    = (charId == 11);
        // Golem: much less knockback
        if (traitIronHide) defMult = (int)(defMult * 1.5f);
    }

    public void assignSpecial() {
        switch (charId) {
            case GameData.CH_KAEL:   specialId = SP_LIGHTNING;    break;
            case GameData.CH_IGNIS:  specialId = SP_DRAGON_BREATH;break;
            case GameData.CH_FROST:  specialId = SP_PERMAFROST;   break;
            case GameData.CH_NEXUS:  specialId = SP_GRAVITY_WELL; break;
            case GameData.CH_ZHARA:  specialId = SP_VINE_CAGE;    break;
            case GameData.CH_SHADOW: specialId = SP_SOUL_REAP;    break;
            case GameData.CH_TITAN:  specialId = SP_MAGMA_SLAM;   break;
            case GameData.CH_QUEEN:  specialId = SP_BLIZZARD;     break;
            case 8:  specialId = SP_SOUL_REAP;     break; // BLADESTORM: spin dash
            case 9:  specialId = SP_LIGHTNING;    break; // VOLTRA: chain lightning
            case 10: specialId = SP_MAGMA_SLAM;   break; // GOLEM: rock slam
            case 11: specialId = SP_SOUL_REAP;    break; // WRAITH: phantom step
            default: specialId = SP_LIGHTNING;    break;
        }
    }

    /** Called each time this stickman deals a hit — charges special. */
    public void chargeSpecialOnHit(int dmgDealt) {
        if (specialCooldown > 0) return;  // can't charge during cooldown
        specialCharge += SP_CHARGE_PER_HIT;
        if (specialCharge > SP_CHARGE_MAX) specialCharge = SP_CHARGE_MAX;
    }

    /** Called by canvas to trigger the special. */
    public void fireSpecial() {
        if (!specialReady || specialCooldown > 0) return;
        specialCooldown = SP_COOLDOWN_MS;
        specialReady    = false;
        specialCharge   = 0;
        charging        = false;
        chargeTimer     = 0;

        switch (specialId) {

            // ---- 1. DRAGON BREATH (Ignis) ---------------------------------
            // Massive flamethrower cone, 2.5s DoT aura, screen tint
            case SP_DRAGON_BREATH:
                specialTimer = 2500;
                spFxType = FX_FLAME_BURST;
                spFxX = x + (facingRight ? 30f : -30f);
                spFxY = y - 20f;
                break;

            // ---- 2. LIGHTNING STORM (Kael) --------------------------------
            // 6 bolts rain down over 1.8s, chain-hit enemies
            case SP_LIGHTNING:
                specialTimer = 1800;
                boltTimer    = 0;
                boltCount    = 0;
                vy = -300f;   // levitate up
                spFxType = FX_LIGHTNING;
                spFxX = x; spFxY = y - 40f;
                break;

            // ---- 3. SOUL REAP (Shadowborn) --------------------------------
            // Invulnerable dash + lifesteal slash
            case SP_SOUL_REAP:
                specialTimer = 400;
                dashVX       = facingRight ? 600f : -600f;
                dashTimer    = 400;
                dashInvuln   = true;
                invincTimer  = 400;
                spFxType = FX_SOUL_WISP;
                spFxX = x; spFxY = y - 20f;
                break;

            // ---- 4. PERMAFROST (Frostbane) --------------------------------
            // Freeze aura, slow all enemies for 3s
            case SP_PERMAFROST:
                specialTimer = 3000;
                spFxType = FX_ICE_SHARD;
                spFxX = x; spFxY = y - 20f;
                break;

            // ---- 5. GRAVITY WELL (Nexus) ----------------------------------
            // Drop a gravity well that sucks enemies in for 2.5s
            case SP_GRAVITY_WELL:
                specialTimer = 2500;
                gravWellTimer= 2500;
                gravWellX    = x;
                gravWellY    = y - 30f;
                spFxType = FX_GRAV_PULL;
                spFxX = gravWellX; spFxY = gravWellY;
                break;

            // ---- 6. VINE CAGE (Zhara) -------------------------------------
            // Root enemies in place for 2s + poison DoT
            case SP_VINE_CAGE:
                specialTimer = 2000;
                spFxType = FX_VINE;
                spFxX = x; spFxY = y;
                break;

            // ---- 7. MAGMA SLAM (Titan) ------------------------------------
            // Leap up, slam down, massive shockwave AoE
            case SP_MAGMA_SLAM:
                specialTimer = 1200;
                vy = -520f;    // big leap
                spFxType = FX_QUAKE;
                spFxX = x; spFxY = y;
                break;

            // ---- 8. BLIZZARD (Frost Queen) --------------------------------
            // Ice storm covers whole screen, slows all enemies 4s
            case SP_BLIZZARD:
                specialTimer = 4000;
                blizzardSlowTimer = 4000;
                spFxType = FX_BLIZZARD;
                spFxX = x; spFxY = y;
                break;
        }
    }

    /**
     * Per-frame special logic (damage, pull, etc).
     * Canvas handles the visual particles separately via spFxType.
     */
    public void updateSpecial(int delta, float dt) {
        if (specialTimer <= 0) return;

        switch (specialId) {

            case SP_DRAGON_BREATH:
                // Signal canvas to spray flame particles every 80ms
                if ((specialTimer % 80) < delta) {
                    spFxType = FX_FLAME_BURST;
                    spFxX = x + (facingRight ? 28f : -28f);
                    spFxY = y - 18f;
                }
                break;

            case SP_LIGHTNING:
                // Fire a bolt every 300ms (6 bolts total)
                boltTimer -= delta;
                if (boltTimer <= 0 && boltCount < 6) {
                    boltTimer = 300;
                    boltCount++;
                    spFxType = FX_LIGHTNING;
                    spFxX = x + (new java.util.Random().nextFloat() - 0.5f) * 120f;
                    spFxY = 0f;  // canvas draws from top down to enemy
                }
                break;

            case SP_SOUL_REAP:
                // During dash, signal soul wisp
                if (dashTimer > 0) {
                    spFxType = FX_SOUL_WISP;
                    spFxX = x - (facingRight ? 12f : -12f);
                    spFxY = y - 20f;
                }
                break;

            case SP_PERMAFROST:
                // Pulse ice shard ring every 500ms
                if ((specialTimer % 500) < delta) {
                    spFxType = FX_ICE_SHARD;
                    spFxX = x; spFxY = y - 24f;
                }
                break;

            case SP_GRAVITY_WELL:
                // Pulse grav pull ring every 200ms
                if ((specialTimer % 200) < delta) {
                    spFxType = FX_GRAV_PULL;
                    spFxX = gravWellX; spFxY = gravWellY;
                }
                break;

            case SP_VINE_CAGE:
                if ((specialTimer % 400) < delta) {
                    spFxType = FX_VINE;
                    spFxX = x + (facingRight ? 40f : -40f);
                    spFxY = y - 10f;
                }
                break;

            case SP_MAGMA_SLAM:
                // When landing (vy > 0 and near ground), do quake burst
                if (vy > 50f && onGround) {
                    spFxType = FX_QUAKE;
                    spFxX = x; spFxY = y;
                    specialTimer = 0; // consume
                }
                break;

            case SP_BLIZZARD:
                if ((specialTimer % 120) < delta) {
                    spFxType = FX_BLIZZARD;
                    spFxX = new java.util.Random().nextFloat() * Arena.W;
                    spFxY = 0f;
                }
                break;
        }
    }

    /**
     * Apply special effects to an opponent stickman.
     * Canvas calls this after checking overlap/distance.
     */
    public void applySpecialToTarget(Stickman target) {
        if (target == null || !target.alive) return;
        switch (specialId) {
            case SP_DRAGON_BREATH:
                target.takeDamage(8, facingRight ? 80f : -80f, 0f);
                target.burnTimer = Math.max(target.burnTimer, 1500);
                break;
            case SP_LIGHTNING:
                target.takeDamage(65, 0f, -200f);
                target.invincTimer = 300;
                break;
            case SP_SOUL_REAP:
                int stolen = 42;
                target.takeDamage(stolen + 43, facingRight ? 400f : -400f, -300f);
                health = Math.min(maxHealth, health + stolen); // lifesteal
                break;
            case SP_PERMAFROST:
                target.frozenTimer = Math.max(target.frozenTimer, 3000);
                target.vx = 0f;
                target.takeDamage(20, 0f, 0f);
                break;
            case SP_GRAVITY_WELL:
                // Pull toward well — canvas handles physics, but damage on contact
                target.takeDamage(10, 0f, 0f);
                break;
            case SP_VINE_CAGE:
                target.rootedTimer = Math.max(target.rootedTimer, 2000);
                target.burnTimer   = Math.max(target.burnTimer, 2000); // poison
                target.takeDamage(15, 0f, 0f);
                break;
            case SP_MAGMA_SLAM:
                target.takeDamage(90, facingRight ? 350f : -350f, -400f);
                break;
            case SP_BLIZZARD:
                target.blizzardSlowTimer = Math.max(target.blizzardSlowTimer, 4000);
                target.takeDamage(12, 0f, 0f);
                break;
        }
    }

    /** Is this stickman currently CC'd (frozen/rooted/blizzard)? */
    public boolean isCrowdControlled() {
        return frozenTimer > 0 || rootedTimer > 0;
    }

    /** Speed factor including CC effects. */
    public float getEffectiveSpeedFactor() {
        if (frozenTimer > 0) return 0f;
        if (rootedTimer > 0) return 0f;
        if (blizzardSlowTimer > 0) return 0.35f;
        return 1.0f;
    }



    // -----------------------------------------------------------------
    //  Special aura (drawn behind stickman body)
    // -----------------------------------------------------------------
    private void drawSpecialAura(Graphics g, int sx, int sy, int color) {
        if (specialTimer <= 0 && specialCooldown <= 0) return;

        int auraAlpha = specialTimer > 0 ? 255 : 0;
        if (auraAlpha == 0) return;

        int tick = (int)(System.currentTimeMillis() / 100);

        switch (specialId) {
            case SP_DRAGON_BREATH:
                // Flame halo around body
                g.setColor((tick%2==0) ? 0xFF4400 : 0xFF8800);
                g.drawArc(sx - 20, sy - H - 8, 40, H + 12, 0, 360);
                // Flame mouth forward
                int fdir = facingRight ? 1 : -1;
                g.setColor(0xFF6600);
                g.drawLine(sx + fdir*10, sy-28, sx + fdir*30, sy-18);
                g.drawLine(sx + fdir*10, sy-28, sx + fdir*28, sy-32);
                g.setColor(0xFFFF00);
                g.drawLine(sx + fdir*16, sy-26, sx + fdir*24, sy-22);
                break;

            case SP_LIGHTNING:
                // Electric arc halo
                g.setColor((tick%2==0) ? 0x0088FF : 0x88EEFF);
                g.drawArc(sx - 18, sy - H - 6, 36, H + 10, 0, 360);
                // Levitation lines
                g.setColor(0x6666FF);
                g.drawLine(sx-6, sy-H-8, sx-8, sy-H-14);
                g.drawLine(sx,   sy-H-8, sx,   sy-H-16);
                g.drawLine(sx+6, sy-H-8, sx+8, sy-H-14);
                break;

            case SP_SOUL_REAP:
                // Purple ghost trail while dashing
                if (dashTimer > 0) {
                    g.setColor(0x880088);
                    int ghost = facingRight ? -16 : 16;
                    g.drawLine(sx+ghost, sy-H+4, sx+ghost, sy-8);
                    g.drawArc(sx+ghost-6, sy-H-4, 12, 12, 0, 360);
                }
                // Soul wisps
                g.setColor((tick%3==0) ? 0xAA44FF : (tick%3==1 ? 0xDD88FF : 0x8822CC));
                g.drawArc(sx - 16, sy - H - 4, 32, H + 8, 0, 360);
                break;

            case SP_PERMAFROST:
                // Ice crystal shards orbiting
                g.setColor(0x88CCFF);
                int ang = (tick * 30) % 360;
                int orb = 22;
                int ox1 = sx + (int)(Math.cos(ang * 3.14159f / 180f) * orb);
                int oy1 = sy - H/2 + (int)(Math.sin(ang * 3.14159f / 180f) * orb / 2);
                int ox2 = sx - (ox1-sx); int oy2 = sy - H/2 - (oy1-(sy-H/2));
                g.fillRect(ox1-2, oy1-3, 4, 6);
                g.fillRect(ox2-2, oy2-3, 4, 6);
                g.setColor(0xAAEEFF);
                g.drawArc(sx-16, sy-H-4, 32, H+8, 0, 360);
                break;

            case SP_GRAVITY_WELL:
                // Swirling rings at well position
                g.setColor((tick%2==0) ? 0x00FF88 : 0x00CC66);
                int gwx = (int)gravWellX, gwy = (int)gravWellY;
                g.drawArc(gwx-18, gwy-9, 36, 18, 0, 360);
                g.drawArc(gwx-14, gwy-7, 28, 14, 0, 360);
                g.setColor(0x00FFAA);
                g.drawLine(gwx-18, gwy, gwx+18, gwy);
                g.drawLine(gwx, gwy-9, gwx, gwy+9);
                break;

            case SP_VINE_CAGE:
                // Green vines spreading from feet
                g.setColor(0x228822);
                g.drawLine(sx-4, sy, sx-16, sy+10);
                g.drawLine(sx-4, sy, sx-20, sy+6);
                g.drawLine(sx+4, sy, sx+16, sy+10);
                g.drawLine(sx+4, sy, sx+20, sy+6);
                g.setColor(0x44CC44);
                g.drawLine(sx-10, sy+8, sx-10, sy+16);
                g.drawLine(sx+10, sy+8, sx+10, sy+16);
                // Thorn circle
                g.setColor(0x33AA33);
                g.drawArc(sx-20, sy-H-4, 40, H+8, 0, 360);
                break;

            case SP_MAGMA_SLAM:
                // Fiery ring expanding from body
                g.setColor((tick%2==0) ? 0xFF4400 : 0xFF8800);
                g.drawArc(sx - 24, sy - H - 6, 48, H + 12, 0, 360);
                g.drawArc(sx - 20, sy - H - 2, 40, H + 4,  0, 360);
                // Magma drips at feet
                g.setColor(0xFF6600);
                g.fillRect(sx-3, sy-2, 6, 4);
                g.fillRect(sx-8, sy-1, 4, 3);
                g.fillRect(sx+5, sy-1, 4, 3);
                break;

            case SP_BLIZZARD:
                // Ice crown on head
                g.setColor(0x88CCFF);
                int[] crownX = {sx-8, sx-4, sx, sx+4, sx+8};
                int[] crownH = {8, 12, 10, 12, 8};
                for (int ci = 0; ci < 5; ci++) {
                    g.drawLine(crownX[ci], sy-H-8, crownX[ci], sy-H-8-crownH[ci]);
                }
                g.setColor(0xCCEEFF);
                g.drawLine(sx-8, sy-H-8, sx+8, sy-H-8);
                // Full-body ice shimmer
                if ((tick%3) == 0) {
                    g.setColor(0x4488CC);
                    g.drawArc(sx-17, sy-H-3, 34, H+6, 0, 360);
                }
                break;
        }
    }



    // =================================================================
    //  Victory / Defeat poses (character-specific)
    // =================================================================
    private void drawVictoryPose(Graphics g, int sx, int sy, int col, int dir) {
        // Character-specific victory pose
        int pulse = (victoryTimer / 120) % 2;
        int glowCol = pulse == 0 ? col : 0xFFFFFF;

        // Gold star burst above head
        g.setColor(0xFFDD00);
        int[] sx2 = {sx, sx-12, sx+12, sx-8, sx+8};
        int[] sy2 = {sy-H-20, sy-H-10, sy-H-10, sy-H-30, sy-H-30};
        for (int pi = 0; pi < 5; pi++) {
            g.drawLine(sx, sy-H-12, sx2[pi], sy2[pi]);
        }

        // Arms up (both raised triumphantly)
        g.setColor(glowCol);
        int wave = (victoryPose % 4 < 2) ? -8 : -14;
        g.drawLine(sx, sy-30, sx-18, sy-30+wave);  // left arm up-wave
        g.drawLine(sx, sy-30, sx+18, sy-30+wave);  // right arm up-wave
        g.fillRect(sx-20, sy-30+wave-3, 5, 5);  // left fist
        g.fillRect(sx+16, sy-30+wave-3, 5, 5);  // right fist

        // Character-unique victory flourish
        switch (charId) {
            case GameData.CH_IGNIS:
                // Ignis: fire crown
                g.setColor((victoryPose%2==0) ? 0xFF4400 : 0xFF8800);
                g.fillArc(sx-14, sy-H-18, 28, 10, 0, 180);
                g.drawLine(sx-10, sy-H-22, sx-8, sy-H-14);
                g.drawLine(sx,    sy-H-26, sx,   sy-H-14);
                g.drawLine(sx+10, sy-H-22, sx+8, sy-H-14);
                break;
            case GameData.CH_FROST: case GameData.CH_QUEEN:
                // Ice: sparkle crown
                g.setColor(0x88CCFF);
                for (int si = -12; si <= 12; si += 6) {
                    g.fillRect(sx+si-1, sy-H-16, 2, 6);
                    g.fillRect(sx+si-3, sy-H-14, 6, 2);
                }
                break;
            case GameData.CH_NEXUS:
                // Nexus: hologram ring
                g.setColor((victoryPose%2==0) ? 0x00FF88 : 0x00FFCC);
                g.drawArc(sx-20, sy-H-24, 40, 16, 0, 360);
                break;
            case GameData.CH_SHADOW:
                // Shadow: disappear flicker
                if (victoryPose % 3 == 0) return;
                g.setColor(0xAA44FF);
                g.drawArc(sx-16, sy-H-8, 32, H+4, 0, 360);
                break;
            case GameData.CH_TITAN:
                // Titan: ground crack
                g.setColor(0xFF4400);
                g.drawLine(sx-30, sy, sx, sy-4);
                g.drawLine(sx+30, sy, sx, sy-4);
                g.drawLine(sx-20, sy+3, sx, sy-2);
                break;
            case GameData.CH_ZHARA:
                // Zhara: flower bloom
                g.setColor(0x44CC44);
                g.fillArc(sx-6, sy-H-22, 12, 12, 0, 360);
                g.setColor(0xFF88AA);
                for (int pi2 = 0; pi2 < 6; pi2++) {
                    float ang = pi2 * 60f * 3.14159f / 180f;
                    int px3 = sx + (int)(Math.cos(ang) * 10);
                    int py3 = sy-H-16 + (int)(Math.sin(ang) * 8);
                    g.fillArc(px3-3, py3-3, 6, 6, 0, 360);
                }
                break;
        }
    }

    private void drawDefeatPose(Graphics g, int sx, int sy, int col) {
        // Fallen character — slumped on the ground
        g.setColor(col);
        // Body lying sideways
        int gy = sy - 8; // near ground
        g.drawLine(sx - 20, gy,      sx + 20, gy);      // torso horizontal
        g.drawLine(sx - 20, gy,      sx - 28, gy + 6);  // legs drooped
        g.drawLine(sx - 14, gy,      sx - 18, gy + 10);
        g.drawLine(sx + 14, gy - 4,  sx + 20, gy + 4);  // arms out
        g.drawLine(sx +  6, gy - 4,  sx + 10, gy + 6);
        // Head resting
        g.setColor(0x1A1A2E); g.fillArc(sx + 18, gy - 14, 14, 14, 0, 360);
        g.setColor(col);      g.drawArc(sx + 18, gy - 14, 14, 14, 0, 360);
        // X eyes
        g.setColor(0xFF2222);
        g.drawLine(sx+22, gy-12, sx+26, gy-8);
        g.drawLine(sx+26, gy-12, sx+22, gy-8);
    }

    // =================================================================
    //  Public helpers for canvas special charge
    // =================================================================

    /** Call this every time THIS stickman's attack HITS the enemy. */
    public void addSpecialCharge(int amount) {
        if (specialFull) return;
        specialCharge += amount;
        if (specialCharge >= 100) {
            specialCharge = 100;
            if (!specialFull) {
                specialFull = true;
                specialChargeFlash = 2000; // 2s flash
            }
        }
    }

    /** Consume full charge to fire special. Returns false if not ready. */
    public boolean consumeSpecialCharge() {
        if (!specialFull) return false;
        specialCharge = 0;
        specialFull   = false;
        specialChargeFlash = 0;
        return true;
    }

    /** Reset charge at start of round. */
    public void resetRound() {
        specialCharge     = 0;
        speedBoost        = false;
        shieldWall        = false;
        specialFull       = false;
        specialChargeFlash= 0;
        killsThisRound    = 0;
        specialCharge     = 0;
        specialCooldown   = 0;
        victoryTimer      = 0;
        state             = ST_IDLE;
    }

    /** Set victory or defeat state. */
    public void setVictory(boolean won) {
        state        = won ? ST_VICTORY : ST_DEFEAT;
        victoryTimer = 0;
        vx = 0f; vy = 0f;
        alive = true;  // keep alive for animation
    }


}