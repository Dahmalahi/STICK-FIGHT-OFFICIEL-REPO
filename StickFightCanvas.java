import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.*;
import java.util.Random;

/**
 * StickFightCanvas v7.0 ETERNAL REALMS
 * DASH ANIMATION V2 - OMEGA Edition
 *
 * NOTE (developer): The controls are a little inconvenient... But it's ok!
 *
 * NEW in v7 (OMEGA Edition):
 *  ?? FULLY WIRED v5/v6 SYSTEMS ???????????????????????????????????????
 *  - renderGame now draws: parallax bg, arena bg FX, char shadows, weapon
 *    glow, dash trail, hit flashes, damage numbers, weather, ragdoll,
 *    enemy arrow, minimap, stamina bar, level badge, rank badge, charge
 *    indicator, confetti, achievement popup, tooltip
 *  - checkMeleeHit now tracks statDmgDealt/statHitsLanded + triggers hit
 *    flash + spawns floating damage numbers + weapon-drop on kill
 *  - endRound now spawns confetti on player win + ranked update
 *  - initRound now calls initWeather + initArenaBg + daily challenge check
 *  - menuAction extended with Ranked/Tournament/Settings cases
 *  - keyPressed: GS_TOURNAMENT + GS_SETTINGS fully handled; in-game *
 *    shows tooltip; charge attack logic on KEY_NUM5 hold
 *  - renderPause replaced by renderPauseStats (with match stats)
 *  - renderMenu shows Settings and Ranked items + player rank badge
 *  ?? NEW GAMEPLAY ?????????????????????????????????????????????????????
 *  - DODGE ROLL: KEY_NUM0 in air ? quick horizontal dodge + invincibility
 *    frames (500ms cooldown, 60ms invincible, costs 15 stamina)
 *  - ENVIRONMENTAL HAZARD PULSE: lava/spike platforms glow + flash on hit
 *  - WEAPON DROP: enemy drops held weapon at death position as ground pickup
 *  - SUDDEN DEATH OVERTIME: if timer expires in draw, 30s sudden-death starts
 *  ?? NEW VISUAL ???????????????????????????????????????????????????????
 *  - KILL CAM: 0.5s slow-mo + zoom-in on killing blow
 *  - SCORE CROWN: winner's name displays gold crown icon above character
 *  - ARENA NAME SPLASH: arena name fades in for 1.5s at fight start
 *  ?? NEW PROGRESSION ??????????????????????????????????????????????????
 *  - CHALLENGE TRACKER HUD: daily challenge progress shown in-match
 *  - FIRST BLOOD: first hit of match earns bonus shards + message
 *
 * NEW in v3:
 *  - Animated main menu with lore title + parallax realm layers
 *  - Character Select screen (8 fighters with lore + stats)
 *  - Arena Select screen (8 arenas with lore + hazard preview)
 *  - Shop: Eternal Bazaar (weapons, armor, cosmetics, powerups)
 *  - Skill Tree per character (6 skills)
 *  - Progression: Realm Shards currency, unlock system
 *  - Story Mode: 4 chapters with boss fights
 *  - Arena hazard effects: lava rise, ice friction, gravity flip, poison
 *  - Character passive abilities
 *  - Power-up inventory and in-match activation
 *  - Shard reward screen after every match
 *  - Full lore text panels
 *  - RecordStore save/load (SaveData)
 *
 * NEW in v4 (DASH ANIMATION V2 Enhanced Edition):
 *  - 2D scrolling camera system - world is now 480x480 (2x larger than screen)
 *  - Camera tracks midpoint between player and enemy with smooth lerp
 *  - Camera clamped to world bounds; HUD stays fixed on screen
 *  - Boot logo COMPLETELY overhauled:
 *      * Pulsing nebula glow rings behind orb
 *      * 12 animated fire particles rising from orb
 *      * 3 rotating electric arc bolts around orb
 *      * 2 orbiting sparks (gold + cyan) using trigonometry
 *      * RGB chromatic aberration cycling on DASH ANIMATION V2 text
 *      * Golden shimmer sweep across text
 *      * Decorative diamond separator lines
 *      * Phase-transition explosion starburst
 *      * Blinking TAP TO SKIP hint
 *  - Shadowborn now has purple glowing dual eyes; Kael has flame hair highlight
 */
public class StickFightCanvas extends GameCanvas implements Runnable {

    private static final Random rand = new Random();

    // ---- Screen -------------------------------------------------------
    static final int SW = 240;
    static final int SH = 320;

    // ---- 2D Camera & World -------------------------------------------
    // World is 2x larger than screen. All game objects live in world coords.
    // Camera viewport = SW x SH; offset drawing by (-camX, -camY).
    // HUD drawn AFTER restoring translate so it stays fixed on screen.
    static final int WORLD_W = 480;   // world width  (2 x SW)
    static final int WORLD_H = 480;   // world height (2 x SH with headroom)
    private float camX = 0f;          // camera top-left X in world space
    private float camY = 0f;          // camera top-left Y in world space
    private static final float CAM_LERP = 0.10f; // smooth-follow speed

    // ---- Palette ------------------------------------------------------
    static final int C_BG       = 0x070712;
    static final int C_BG2      = 0x0D0D22;
    static final int C_PLAT     = 0x334455;
    static final int C_PLAT_TOP = 0x6688AA;
    static final int C_PLAT_SHD = 0x0A1525;
    static final int C_SPIKE    = 0xAA1111;
    static final int C_LAVA_A   = 0xFF5500;
    static final int C_LAVA_B   = 0xFF8800;
    static final int C_ICE_A    = 0x88CCFF;
    static final int C_ICE_B    = 0xAADDFF;
    static final int C_BREAK_OK = 0x886644;
    static final int C_BREAK_CK = 0x553322;
    static final int C_WHITE    = 0xFFFFFF;
    static final int C_YELLOW   = 0xFFFF00;
    static final int C_GREEN    = 0x22EE44;
    static final int C_CYAN     = 0x00FFEE;
    static final int C_RED      = 0xFF2222;
    static final int C_DARK     = 0x141428;
    static final int C_GREY     = 0x778899;
    static final int C_ORANGE   = 0xFF6600;
    static final int C_GOLD     = 0xFFDD00;
    static final int C_BULLET   = 0xFFFF77;
    static final int C_WEAPON   = 0xFFAA00;
    static final int C_PURPLE   = 0xAA44FF;
    static final int C_MGENTA   = 0xFF44AA;

    // ---- Game states --------------------------------------------------
    static final int GS_LOGO        = 0;
    static final int GS_MENU        = 1;
    static final int GS_CHAR_SEL    = 2;
    static final int GS_ARENA_SEL   = 3;
    static final int GS_LORE        = 4;
    static final int GS_SHOP        = 5;
    static final int GS_SKILLS      = 6;
    static final int GS_GAME        = 7;
    static final int GS_ROUND_END   = 8;
    static final int GS_SHARD_REWARD= 9;
    static final int GS_GAME_OVER   = 10;
    static final int GS_SURVIVAL    = 11;
    static final int GS_WAVE_END    = 12;
    static final int GS_HOWTO       = 13;
    static final int GS_STORY_INTRO = 14;
    static final int GS_VICTORY_ANIM= 15;  // character victory animation
    static final int GS_CINEMATIC   = 16;  // post-fight cinematic dialogue
    static final int GS_STORY_SEL   = 17;  // story chapter selection screen
    static final int GS_MELEE       = 18;  // 4-fighter melee brawl
    static final int GS_MELEE_END   = 19;  // melee end screen
    static final int GS_PAUSE       = 20;  // pause overlay
    static final int GS_SETTINGS    = 21;  // settings screen
    static final int GS_TOURNAMENT  = 22;  // tournament bracket screen
    static final int GS_RANKED      = 23;  // ranked mode (uses normal GS_GAME flow)

    private int gameState  = GS_LOGO;
    private int prevState  = GS_MENU;

    // ---- Menu navigation ----------------------------------------------
    private int menuSel    = 0;
    static final int MENU_ITEMS = 10; // Story/VS AI/Survival/Melee/Ranked/Tournament/Shop/Skills/HowTo/Settings

    private int charSel    = 0;
    private int arenaSel   = 0;
    private int shopTab    = 0;    // 0=weapons 1=armor 2=cosmetics 3=powerups
    private int shopSel    = 0;
    private int skillSel   = 0;
    private int lorePage   = 0;    // which arena/char lore
    private int storyChapter   = 0;
    private boolean isStoryFight  = false;  // true when playing story mode
    private int victoryAnimTimer = 0;   // ms for victory animation
    // ---- In-fight power keys (3/7/9) --------------------------------
    private int power3Cooldown = 0;  // ms remaining cooldown: HEAL BURST
    private int power7Cooldown = 0;  // ms remaining cooldown: SPEED BOOST
    private int power9Cooldown = 0;  // ms remaining cooldown: SHIELD WALL
    private static final int P3_CD = 20000; // 20s cooldown
    private static final int P7_CD = 15000; // 15s cooldown  
    private static final int P9_CD = 18000; // 18s cooldown
    private int power7Timer = 0;     // duration of speed boost
    private int power9Timer = 0;     // duration of shield wall
    private boolean playerWonRound = false;  // did player win the latest fight

    // ?? Cinematic / dialogue system ????????????????????????????????
    private int  cinematicLine    = 0;   // current dialogue line
    private int  cinematicTimer   = 0;   // auto-advance timer
    private int  cinematicChar    = 0;   // who's speaking (0=narrator 1=player 2=enemy)
    private boolean cinematicWaitKey = false;

    // Story chapter completion tracking
    private static boolean[] storyDone = new boolean[5]; // 5 chapters

    // Story cinematics: [chapter][line] = "SPEAKER: text"
    // =====================================================================
    //  STORY CONTENT - 4 Chapters, rich cinematic dialogue
    // =====================================================================
    private static final String[][] STORY_DIALOGUE = {
        // Chapter 0 - WANDERER'S TRIAL (vs Ignis)
        { "NARRATOR: Between realms, where time bends...",
          "NARRATOR: A warrior falls from the sky. Memory: shattered.",
          "Kael wakes in the scorching Arena of Flame.",
          "IGNIS: *laughs* Another lost soul enters my Crucible.",
          "KAEL: ...Who are you? Where is this place?",
          "IGNIS: I am Ignis, Champion of Eternal Flame!",
          "IGNIS: This arena is where the weak burn to nothing.",
          "KAEL: *clenches fist* I don't burn. I fight.",
          "IGNIS: Ha! Then fight, wanderer. And DIE!" },
        // Chapter 1 - FIRES OF THE CRUCIBLE (vs Titan)
        { "NARRATOR: Kael stands at the mouth of the great forge.",
          "NARRATOR: The ground trembles. Stone cracks. A shadow stirs.",
          "TITAN: I HAVE SLEPT FOR A THOUSAND YEARS.",
          "TITAN: WHO DARES WAKE THE GUARDIAN OF FLAME?!",
          "KAEL: I beat Ignis. The Crucible is mine now.",
          "TITAN: Ignis... FALLEN?! That is IMPOSSIBLE!",
          "TITAN: You are an ant before a mountain, little wanderer!",
          "KAEL: Mountains can be climbed. Come on, big guy.",
          "NARRATOR: The most powerful battle of Kael's life begins..." },
        // Chapter 2 - FROZEN CITADEL SIEGE (vs Frostbane)
        { "NARRATOR: Beyond the burning lands lies an eternal winter.",
          "NARRATOR: The Frozen Citadel gleams like a shattered blade.",
          "FROSTBANE: HALT. None pass these gates by order of the Queen!",
          "KAEL: I'm looking for the Frost Queen. Move aside.",
          "FROSTBANE: The Queen receives no wanderers. Especially YOUR kind.",
          "KAEL: Then I'll just have to go through you.",
          "FROSTBANE: *cracks knuckles, ice forms on gauntlets*",
          "FROSTBANE: I was hoping you would say that.",
          "NARRATOR: Frostbane - the Frost Queen's deadliest knight." },
        // Chapter 3 - NEO ARCADIA UPRISING (vs Nexus)
        { "NARRATOR: The ruins of Neo Arcadia pulse with dying light.",
          "NARRATOR: Half city. Half circuit. All chaos.",
          "NEXUS: INTRUDER DETECTED. DESIGNATION: KAEL. ORIGIN: UNKNOWN.",
          "NEXUS: THREAT LEVEL: ELEVATED. EXTERMINATION: AUTHORIZED.",
          "KAEL: You used to be human, Nexus. I can still see it.",
          "NEXUS: HUMANITY IS A BUG. I HAVE BEEN PATCHED.",
          "KAEL: Perfection doesn't bleed. Let's find out if you do.",
          "NEXUS: COMBAT PROTOCOLS ENGAGED. PREPARE FOR TOTAL DELETION.",
          "NARRATOR: Machine versus Man. The last battle for Arcadia." },
        // Chapter 4 - BEYOND THE VOID GATE (vs Shadowborn)
        { "NARRATOR: Kael reaches the edge of all realms.",
          "NARRATOR: The Void Gate - a tear in reality itself - pulses black.",
          "NARRATOR: And from within the darkness... a familiar silhouette.",
          "KAEL: ...You. The one who brought me here.",
          "SHADOWBORN: *steps from shadow* You were never meant to leave.",
          "KAEL: You kidnapped me from my world. You destroyed everything!",
          "SHADOWBORN: I chose you because you are the strongest. A test.",
          "KAEL: A TEST?! People died! Realms burned!",
          "SHADOWBORN: And now one battle remains. Win - and I open the gate home.",
          "SHADOWBORN: Lose... and the void consumes you. Forever.",
          "KAEL: *raises fists* Then let's end this. Once and for all.",
          "NARRATOR: The void crackles. The final battle begins." }
    };

    // Post-fight cinematics [chapter][0=player wins, 1=enemy wins]
    // Each entry uses \n to separate dialogue lines shown one by one
    private static final String[][] STORY_VICTORY = {
        // Chapter 0
        { "IGNIS: *gasps* I... lost? To a WANDERER?!\nKAEL: Start talking. Where am I?\nIGNIS: The Eternal Realms... you fell from beyond the veil.\nIGNIS: Only the Titan of the Forge knows the way back.\nKAEL: Then I'm going to find that Titan.\nNARRATOR: A path opens through the dying flames. Onward.",
          "IGNIS: *stands over Kael, laughing* Too weak, wanderer!\nIGNIS: The Crucible claims another broken soul.\nKAEL: *on knees* ...Not... finished yet...\nNARRATOR: Kael falls. But fire cannot kill what is already burning inside him." },
        // Chapter 1
        { "TITAN: *crashes to knees* You... you defeated the Guardian...\nKAEL: Rest, old one. The forge doesn't need a warden anymore.\nTITAN: I have not known rest in a thousand years, wanderer.\nTITAN: Seek the Frost Queen. She alone can open the path home.\nKAEL: Frozen Citadel. Got it.\nNARRATOR: The eternal fire dims. The realm holds its breath.",
          "TITAN: *ROARS* THE GUARDIAN IS UNDEFEATED! FALL BEFORE THE FLAME!\nTITAN: The forge will burn for a thousand more years!\nKAEL: *struggling to rise* ...Won't...stop...\nNARRATOR: The Titan stands. The world trembles. But Kael's will... does not break." },
        // Chapter 2
        { "FROSTBANE: *kneels slowly* I... yield. You fight like no soul I've ever faced.\nKAEL: I need to reach the Queen. It's life or death.\nFROSTBANE: ...Go. The throne chamber is above. She'll be watching.\nFROST QUEEN: *from the heights* I have been watching, wanderer.\nFROST QUEEN: Defeat my knight... and earn an audience with a queen.\nNARRATOR: For the first time in a century, the citadel's heart opens.",
          "FROSTBANE: FOR THE QUEEN! FOR ETERNAL WINTER!\nFROSTBANE: This citadel HAS NEVER FALLEN!\nKAEL: *frozen solid* ...Can't... move...\nFROST QUEEN: *coldly* Seal the gates. Let the wanderer reflect on his failure.\nNARRATOR: The ice claims another. But a frozen heart can still beat." },
        // Chapter 3
        { "NEXUS: *sparking, stumbling* ERROR... ERROR... I... feel... something...\nKAEL: That's humanity trying to come back, Nexus.\nNEXUS: I... I remember my name... before all this... it was... David.\nKAEL: David. Stay with me. How do I go home?\nNEXUS/DAVID: The Realm Core... above the spire... it can open any door.\nNARRATOR: The circuits fade. A man awakens beneath the machine.\nNARRATOR: And Kael... finally... finds his way home.",
          "NEXUS: HUMAN COMBATANT: ELIMINATED. SYSTEMS: DOMINANT.\nNEXUS: ARCADIA IS MINE. ALL ETERNAL REALMS WILL FOLLOW.\nKAEL: *on the ground* ...David... if you're in there... don't give up...\nNARRATOR: The machine wins. The eternal realms fall silent.\nNARRATOR: And the wanderer... is lost to the void... forever." },
        // Chapter 4 - THE VOID GATE
        { "SHADOWBORN: *collapses* ...Impossible. No one has ever...\nKAEL: You said you chose the strongest. You were right.\nSHADOWBORN: *fading into shadow* ...I underestimated what humans call... will.\nKAEL: Open the gate. You promised.\nSHADOWBORN: *final words* ...It is... already open. It was always open.\nNARRATOR: The void tears apart. Light floods the darkness.\nNARRATOR: Kael steps through the gate...\nNARRATOR: ...and returns home. A wanderer no more.\nNARRATOR: THE END. Thank you for playing STICKFIGHT: ETERNAL REALMS.",
          "SHADOWBORN: *stands over Kael* You fought well, wanderer. But the void always wins.\nKAEL: *barely conscious* ...Not... done...\nSHADOWBORN: There is no shame in falling here. None ever reach this far.\nSHADOWBORN: Rest. The void is... merciful in its own way.\nNARRATOR: The void closes. Kael sleeps in the dark between worlds.\nNARRATOR: But somewhere, very faintly... a fire still burns." }
    };

    // Chapter selection display data
    private static final String[] CHAPTER_TITLES = {
        "CH.1: WANDERER'S TRIAL",
        "CH.2: FIRES OF CRUCIBLE",
        "CH.3: FROZEN CITADEL",
        "CH.4: ARCADIA UPRISING",
        "CH.5: BEYOND THE VOID GATE"
    };
    private static final String[] CHAPTER_ENEMIES = {
        "IGNIS - Fire Champion",
        "TITAN - Eternal Guardian",
        "FROSTBANE - Ice Knight",
        "NEXUS - Cyber Soldier",
        "SHADOWBORN - Void Rogue"
    };
    private static final int[] CHAPTER_SHARD_REWARD = { 120, 180, 200, 250, 400 };

    // ---- Round management --------------------------------------------
    static final int ROUNDS_TO_WIN = 3;
    int  playerScore   = 0;
    int  enemyScore    = 0;
    int  roundWinner   = -1;
    int  roundEndTimer = 0;
    int  roundTimeLeft = 180000;
    int  pendingShards = 0;  // shards to award at end screen

    // ---- Survival ----------------------------------------------------
    int  survivalWave  = 1;
    // ---- MELEE BRAWL (4 fighters free-for-all) ---------------------
    private static final int MELEE_COUNT = 4;
    private Stickman[] meleeAI          = new Stickman[3];
    private int[]      meleeRespawnTimer = new int[3]; // ms countdown per slot; 0=not respawning
    private int[]      meleeCharIds = new int[3];
    private boolean    meleePlayerAlive = true;
    private int        meleeWinner = -1; // 0=player 1/2/3=AI index+1
    private int        meleeEndTimer = 0;
    private int        meleeRound       = 1;
    private int        meleePlayerWins  = 0;  // wins across rounds
    private int        meleeAIWins      = 0;  // AI wins across rounds
    private boolean    meleeChampion    = false; // player won final
    int  survivalKills = 0;
    private int  comboKills   = 0;   // consecutive round wins
    private int  comboTimer   = 0;   // combo display timer (ms)
    int  waveEndTimer  = 0;
    int  waveStartTimer= 0;

    // ---- Stickmen ----------------------------------------------------
    Stickman player;
    Stickman enemy;
    int  playerCharId  = 0;
    int  enemyCharId   = 1;

    // ---- Bullet pool (32 slots) --------------------------------------
    static final int MAX_BULLETS = 32;
    Weapon.Bullet[] bullets = new Weapon.Bullet[MAX_BULLETS];

    // ---- Ground weapons (8 slots) ------------------------------------
    static final int MAX_GW = 8;
    float[]   gwX      = new float[MAX_GW];
    float[]   gwY      = new float[MAX_GW];
    int[]     gwType   = new int[MAX_GW];
    boolean[] gwActive = new boolean[MAX_GW];
    int weaponSpawnTimer  = 6000;
    int rareWeaponCounter = 0;  // counts weapon spawns, triggers rare drop every 4th

    // ---- Particles (96 slots - expanded for blood/smoke/embers) --------
    static final int MAX_PART = 96;
    float[] pX = new float[MAX_PART], pY = new float[MAX_PART];
    float[] pVX= new float[MAX_PART], pVY= new float[MAX_PART];
    int[]   pLife=new int[MAX_PART],  pCol=new int[MAX_PART];
    int[]   pSize=new int[MAX_PART];
    int[]   pType=new int[MAX_PART];  // 0=spark 1=blood 2=smoke 3=ember 4=splat
    float[] pGrav=new float[MAX_PART]; // per-particle gravity multiplier
    // Blood pool decals (up to 8 splats on ground)
    static final int MAX_POOLS = 8;
    int[] poolX = new int[MAX_POOLS], poolY = new int[MAX_POOLS];
    int[] poolR  = new int[MAX_POOLS], poolCol = new int[MAX_POOLS];
    int   poolHead = 0;
    // Slow-motion state
    private int  slomoDur   = 0;   // ms remaining
    private float slomoScale = 1f; // time multiplier (0.25 during slo-mo)
    // Kill streak
    private int  killStreak = 0;
    private int  killStreakTimer = 0;
    // Low-HP heartbeat sound timer
    private int  heartbeatTimer = 0;
    // Critical hit flag
    private boolean lastHitWasCrit = false;

    // ==================================================================
    //  NEW v5 FEATURES - Variables
    // ==================================================================

    // ---- STAMINA SYSTEM (0-100) --------------------------------------
    private int   playerStamina    = 100;
    private int   staminaRegen     = 0;    // regen tick timer
    private static final int STAM_DASH   = 30;  // cost per air-dash
    private static final int STAM_DJUMP  = 20;  // cost per double-jump
    private static final int STAM_REGEN_RATE = 15; // ms per 1 stamina point

    // ---- DOUBLE JUMP + AIR DASH ------------------------------------
    private boolean playerAirDashed = false;  // reset on land
    private int     airDashTimer    = 0;      // ms of dash duration (120ms)
    private int     airDashDir      = 1;      // +1 right, -1 left
    private static final int AIR_DASH_DUR = 120;
    // Dash trail particles
    private float[] dashTrailX = new float[8];
    private float[] dashTrailY = new float[8];
    private int[]   dashTrailL = new int[8];   // life ms
    private int     dashTrailHead = 0;

    // ---- WALL SLIDE / WALL JUMP ------------------------------------
    private boolean playerWallSliding = false;
    private int     playerWallDir     = 0;     // +1 = touching right wall, -1 = left
    private int     wallJumpTimer     = 0;     // grace window for wall jump

    // ---- PARRY SYSTEM ----------------------------------------------
    private int     parryWindow    = 0;   // ms remaining in parry active window
    private int     parryFlashTimer= 0;   // visual flash when parry succeeds
    private static final int PARRY_WINDOW_MS = 150;
    private static final int PARRY_COOLDOWN  = 800;
    private int     parryCooldown  = 0;

    // ---- LEVEL-UP SYSTEM -------------------------------------------
    private int  playerLevel   = 1;
    private int  playerXP      = 0;
    private int  levelUpFlash  = 0;   // ms of level-up flash overlay
    private int  levelUpTimer  = 0;   // ms of "LEVEL UP!" message
    // XP needed per level (quadratic curve)
    private static final int[] XP_TO_LEVEL = {
        0, 100, 220, 360, 520, 700, 900, 1120, 1360, 1620,
        1900, 2200, 2520, 2860, 3220, 3600, 4000, 4420, 4860, 5320
    };
    private static final int MAX_LEVEL = 20;
    private int  prestigeCount = 0;

    // ---- ARENA WEATHER ---------------------------------------------
    private static final int WEATHER_NONE      = 0;
    private static final int WEATHER_RAIN      = 1;
    private static final int WEATHER_STORM     = 2;
    private static final int WEATHER_SANDSTORM = 3;
    private static final int WEATHER_BLIZZARD  = 4;
    private int currentWeather = WEATHER_NONE;
    private int weatherTimer   = 0;
    // Weather particle pool (separate from main particles - 32 slots)
    private static final int MAX_WEATHER = 32;
    private float[] wX = new float[MAX_WEATHER];
    private float[] wY = new float[MAX_WEATHER];
    private float[] wVX= new float[MAX_WEATHER];
    private float[] wVY= new float[MAX_WEATHER];
    private int[]   wL = new int[MAX_WEATHER];  // life

    // ---- FLOATING DAMAGE NUMBERS -----------------------------------
    private static final int MAX_DMG_NUM = 16;
    private float[]  dnX    = new float[MAX_DMG_NUM];
    private float[]  dnY    = new float[MAX_DMG_NUM];
    private int[]    dnVal  = new int[MAX_DMG_NUM];
    private int[]    dnLife = new int[MAX_DMG_NUM];
    private boolean[] dnCrit= new boolean[MAX_DMG_NUM];
    private int      dnHead = 0;

    // ---- MINIMAP ---------------------------------------------------
    private static final int MM_X = SW - 42;  // minimap top-left X
    private static final int MM_Y = 42;       // minimap top-left Y
    private static final int MM_W = 38;
    private static final int MM_H = 32;

    // ---- OFFSCREEN ENEMY INDICATOR ---------------------------------
    // Arrow shown at screen edge when enemy is off-camera

    // ---- SETTINGS SCREEN -------------------------------------------
    private int settingsSel   = 0;
    private static final int SETTINGS_ITEMS = 5;
    // show/hide controls hint in HUD
    private boolean showControlsHint = true;

    // ---- SHARD REWARD ANIMATION ------------------------------------
    private int  rewardAnimShards = 0;   // currently-displayed shard count (counts up)
    private int  rewardAnimTimer  = 0;   // timer for count-up animation

    // ---- ACHIEVEMENT POPUP -----------------------------------------
    private String achPopupMsg    = "";
    private int    achPopupTimer  = 0;
    private static final int ACH_POPUP_DUR = 3200;

    // ==================================================================
    //  NEW v6 VARIABLES
    // ==================================================================

    // ---- RANKED MODE -----------------------------------------------
    private static final String[] RANK_NAMES  = { "BRONZE","SILVER","GOLD","DIAMOND","CHAMPION" };
    private static final int[]    RANK_WINS   = { 0, 5, 15, 30, 50 };
    private static final int[]    RANK_COLS   = { 0xAA6633, 0xCCCCCC, C_GOLD, 0x44DDFF, 0xFF44FF };
    private boolean isRankedMode  = false;
    private int     rankedWins    = 0;   // total ranked wins
    private int     rankedRank    = 0;   // 0=Bronze..4=Champion
    private int     rankedStreak  = 0;
    private int     rankedFlash   = 0;   // rank-up animation timer

    // ---- TOURNAMENT MODE -------------------------------------------
    private static final int TOURN_SIZE = 8;
    private int[]   tournSlots    = new int[TOURN_SIZE]; // char IDs
    private boolean[] tournDone   = new boolean[TOURN_SIZE];
    private int     tournRound    = 0;    // 0=quarter, 1=semi, 2=final
    private int     tournWinner   = -1;
    private boolean isTournMode   = false;
    private int     tournBracketSel = 0;

    // ---- DAILY CHALLENGE -------------------------------------------
    private static final String[] DAILY_NAMES = {
        "IRON MAN",       // no health regen items
        "GLASS CANNON",   // +100% dmg dealt & received
        "PACIFIST RUSH",  // time-limit win (30s)
        "DISARMED",       // no weapon pickups
        "SUDDEN DEATH",   // one-hit KO mode
        "SPEED DEMON"     // 2x movement speed
    };
    private int  dailyChallenge    = -1;  // -1 = none active
    private int  dailyChallengeDay = -1;  // day-of-year when last set
    private boolean dailyComplete  = false;

    // ---- CHARGE ATTACK ---------------------------------------------
    private int  chargeTimer   = 0;    // ms held
    private boolean chargeReady= false;
    private static final int CHARGE_THRESHOLD = 600;
    private int  chargeFlash   = 0;

    // ---- HIT FLASH (white flash on damage) -------------------------
    private int  playerHitFlash  = 0;   // ms remaining
    private int  enemyHitFlash   = 0;
    private static final int HIT_FLASH_DUR = 80;

    // ---- MATCH STATISTICS ------------------------------------------
    private int  statDmgDealt    = 0;
    private int  statDmgReceived = 0;
    private int  statHitsLanded  = 0;
    private int  statPerfectBlocks = 0;

    // ---- WEAPON DROP ON DEATH --------------------------------------
    // enemy drops weapon on defeat - already uses gwX/gwY pool

    // ---- RAGDOLL DEATH SEGMENTS ------------------------------------
    private static final int RAGDOLL_SEGS = 6;  // head, torso, L-arm, R-arm, L-leg, R-leg
    private float[] rdX   = new float[RAGDOLL_SEGS];
    private float[] rdY   = new float[RAGDOLL_SEGS];
    private float[] rdVX  = new float[RAGDOLL_SEGS];
    private float[] rdVY  = new float[RAGDOLL_SEGS];
    private int[]   rdCol = new int[RAGDOLL_SEGS];
    private int     rdLife= 0;   // 0=inactive, >0=ms remaining
    private boolean rdActive = false;

    // ---- ANIMATED ARENA BACKGROUND ---------------------------------
    private int arenaBgTimer = 0;
    private float[] bgLavaX  = new float[6];   // animated lava blobs
    private float bgIceGlint = 0;

    // ---- GROUND SHADOW ---------------------------------------------
    // Computed per character in render, no extra state needed

    // ---- TOOLTIP SYSTEM --------------------------------------------
    private boolean tooltipVisible  = false;
    private int     tooltipTimer    = 0;
    private String  tooltipLine1    = "";
    private String  tooltipLine2    = "";
    private static final int TOOLTIP_DUR = 3000;

    // ---- CONFETTI (round win) --------------------------------------
    private static final int MAX_CONFETTI = 24;
    private float[] cfX   = new float[MAX_CONFETTI];
    private float[] cfY   = new float[MAX_CONFETTI];
    private float[] cfVX  = new float[MAX_CONFETTI];
    private float[] cfVY  = new float[MAX_CONFETTI];
    private int[]   cfCol = new int[MAX_CONFETTI];
    private int     cfLife= 0;

    // ---- PAUSE STATS -----------------------------------------------
    // reuses statDmgDealt / statHitsLanded above

    // ==================================================================
    //  NEW v7 VARIABLES
    // ==================================================================

    // ---- DODGE ROLL ------------------------------------------------
    private int  dodgeTimer    = 0;  // ms remaining in dodge
    private int  dodgeCooldown = 0;  // ms until next dodge available
    private int  dodgeDir      = 1;
    private boolean dodgeInvincible = false;
    private static final int DODGE_DUR      = 200;  // ms of dash movement
    private static final int DODGE_INVINCE  = 160;  // ms of invincibility frames
    private static final int DODGE_COOLDOWN = 800;
    private static final int DODGE_STAM     = 15;

    // ---- SUDDEN DEATH OVERTIME -------------------------------------
    private boolean suddenDeathActive = false;
    private int     suddenDeathTimer  = 30000; // 30s

    // ---- ARENA NAME SPLASH -----------------------------------------
    private int     arenaSplashTimer  = 0;  // countdown ms (1500ms)
    private static final int ARENA_SPLASH_DUR = 1500;

    // ---- FIRST BLOOD -----------------------------------------------
    private boolean firstBloodDone = false;
    private static final int FIRST_BLOOD_SHARDS = 15;

    // ---- KILL CAM --------------------------------------------------
    private int  killCamTimer  = 0;   // 0=inactive, counts down
    private float killCamZoom  = 1f;  // zoom factor (1.0 to 1.4)
    private float killCamX     = 0f;  // centre of zoom in world coords
    private float killCamY     = 0f;
    private static final int KILL_CAM_DUR = 500;

    // ---- SCORE CROWN -----------------------------------------------
    // Shown above winner for 2s after round end
    private int crownTimer     = 0;
    private boolean crownOnPlayer = true;

    // ---- CHALLENGE TRACKER HUD -------------------------------------
    // Shows daily challenge progress (hits / damage / time) in corner

    // ---- Screen shake ------------------------------------------------
    int shakeX=0, shakeY=0, shakeDur=0, shakeAmp=0;

    // ---- Kill feed (4 lines) -----------------------------------------
    String[] feedLines = new String[4];
    int[]    feedLife  = new int[4];
    int      feedHead  = 0;

    // ---- Big flash message -------------------------------------------
    String bigMsg = ""; int bigMsgLife = 0; int bigMsgColor = C_YELLOW;

    // ---- Input -------------------------------------------------------
    private volatile boolean jumpQueued  = false;
    private volatile boolean blockHeld   = false;
    private volatile boolean throwQueued = false;
    private volatile boolean ultraQueued = false;

    // ---- Stars -------------------------------------------------------
    static final int STAR_COUNT = 28;
    int[] starX = new int[STAR_COUNT];
    int[] starY = new int[STAR_COUNT];
    int[] starB = new int[STAR_COUNT];

    // ---- Menu/UI animation -------------------------------------------
    int  menuAnimTimer = 0, menuAnimFrame = 0;
    // Animated scroll for menu
    private float menuScrollOffset = 0f;   // current rendered offset (fractional)
    private float menuScrollTarget = 0f;   // target offset (integer)
    private static final int MENU_VISIBLE = 5; // items visible at once
    int  menuFlashTimer= 0;
    boolean menuFlashOn= false;
    float menuLX = 60f, menuRX = 178f;
    int  menuFightTimer= 0, menuFightState= 0;
    int  bannerY = -60;
    int  logoTimer = 2800;   // intro logo hold time
    int  logoAlpha = 0;      // fade-in counter (0-255)
    int  scrollY = SH;       // lore text scroll

    // Gravity indicator for Neo Arcadia
    int gravFlashTimer = 0;

    // ---- Fonts -------------------------------------------------------
    private final Font fSmall  = Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN,  Font.SIZE_SMALL);
    private final Font fMedium = Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD,   Font.SIZE_MEDIUM);
    private final Font fLarge  = Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD,   Font.SIZE_LARGE);

    // ---- PAUSE OVERLAY --------------------------------------------------
    private boolean gamePaused    = false;   // is game paused
    private int     pauseSel      = 0;       // 0=resume 1=exit
    private int     prevGameState = GS_GAME; // state before pause

    // ---- SIMULATED SOUND (visual feedback when audio missing) -----------
    // Each simulated sound = screen tint flash + particle burst
    private int simSndTimer  = 0;  // remaining ms of current sound flash
    private int simSndColor  = 0;  // tint color for this sound
    private int simSndBeat   = 0;  // beat counter for BGM pulse simulation
    private int simBgmTimer  = 0;  // BGM pulse beat counter
    private static final int BGM_BEAT_INTERVAL = 550; // ~110bpm
    private int pendingBGM    = -1; // -2=stop -1=no change, 0-3=BGM id to start
    private int inputGuard    = 0;  // ms cooldown after state-change key press
    // Simulated sound IDs
    private static final int SSND_SHOOT   = 0;
    private static final int SSND_HIT     = 1;
    private static final int SSND_ULTRA   = 2;
    private static final int SSND_SPECIAL = 3;
    private static final int SSND_MENU    = 4;
    private static final int SSND_WIN     = 5;
    private static final int SSND_DEATH   = 6;

    // ---- COMBO COUNTER / DAILY BONUS HUD --------------------------------
    private int hudComboCount  = 0;   // current displayed combo
    private int hudComboTimer  = 0;   // ms before combo resets display
    private int dailyBonusTimer = 0;  // show daily bonus popup
    private String dailyBonusText = "";

    // ---- LOGO animation (DASH ANIMATION V2 - Enhanced) ----------------
    private int     logoPhase      = 0;   // 0=dark 1=ring-burst 2=chars-drop 3=text 4=hold+exit
    private int     logoSubTimer   = 0;   // sub-phase timer (ms)
    private int     logoRingR      = 0;   // expanding burst ring radius
    private int     logoCharY      = 0;   // character drop offset (starts negative)
    private int     logoGlitch     = 0;   // glitch flicker frame counter
    private int     logoFlare      = 0;   // lens flare intensity 0-255
    private int     logoAlpha2     = 0;   // text fade-in 0-255 (separate from logoAlpha)
    // Beat pulse (0-100 oscillator)
    private int     logoPulse      = 0;
    private boolean logoPulseUp    = true;
    // Shimmer sweep across text
    private int     logoShimmer    = 0;
    // Orbiting sparks angle (degrees)
    private int     logoOrbit      = 0;
    // Electric arc rotation timer
    private int     logoArcTimer   = 0;
    // Fire particles (12 slots) rising from the orb
    private int[]   logoFireX      = new int[12];
    private int[]   logoFireY      = new int[12];
    private int[]   logoFireLife   = new int[12];
    // Transition flash intensity
    private int     logoFlash      = 0;
    // Explosion burst frame (1-20)
    private int     logoExplode    = 0;
    // Glitch timer (ms before next glitch tick)
    private int     logoGlitchTimer = 0;

    // ---- Thread ------------------------------------------------------
    private StickFightMIDlet midlet;
    private Thread gameThread;
    private volatile boolean running = false;

    // ==================================================================
    //  Constructor
    // ==================================================================

    public StickFightCanvas(StickFightMIDlet midlet) {
        super(false);
        setFullScreenMode(true);
        this.midlet = midlet;

        for (int i = 0; i < MAX_BULLETS; i++) bullets[i] = new Weapon.Bullet();
        for (int i = 0; i < MAX_GW;      i++) gwActive[i] = false;
        for (int i = 0; i < MAX_PART;    i++) { pLife[i]=0; pType[i]=0; pGrav[i]=1f; }
        for (int i = 0; i < MAX_POOLS;   i++) poolR[i]=0;
        for (int i = 0; i < 4;           i++) feedLife[i] = 0;
        for (int i = 0; i < STAR_COUNT;  i++) {
            starX[i] = rand.nextInt(SW);
            starY[i] = rand.nextInt(SH - 80);
            starB[i] = rand.nextInt(3);
        }

        SaveData.load();
        SoundManager.init();
        playerCharId = SaveData.selectedChar;
        // Restore storyDone[] from saved bitmask so chapter unlock persists across sessions
        for (int i = 0; i < storyDone.length; i++) {
            storyDone[i] = SaveData.isChapterDone(i);
        }
        Arena.setArena(0);
        // Check for daily bonus on startup
        int dailyB = SaveData.claimDailyBonus();
        if (dailyB > 0) showBigMsg("DAILY BONUS! +" + dailyB + " SHARDS!", C_GOLD);
    }

    // ==================================================================
    //  Thread
    // ==================================================================
    public void start() { running = true; gameThread = new Thread(this); gameThread.start(); }
    public void stop()  { running = false; }

    // ==================================================================
    //  Game loop
    // ==================================================================
    public void run() {
        Graphics g = getGraphics();
        long last  = System.currentTimeMillis();
        while (running) {
            long now   = System.currentTimeMillis();
            int  delta = (int)(now - last); if (delta<=0) delta=1; if (delta>50) delta=50;
            last = now;
            updateAll(delta);
            renderAll(g);
            flushGraphics();
            long sleep = 33 - (System.currentTimeMillis() - now);
            if (sleep > 0) try { Thread.sleep(sleep); } catch (InterruptedException e) {}
        }
    }

    // ==================================================================
    //  Update dispatch
    // ==================================================================
    private void updateAll(int delta) {
        // Apply slow-motion scaling to game update
        int gameDelta = (slomoDur > 0) ? Math.max(1,(int)(delta * slomoScale)) : delta;
        updateBgAnim(delta);

        if (shakeDur > 0) {
            shakeDur -= delta;
            shakeX = shakeDur>0 ? rand.nextInt(shakeAmp*2+1)-shakeAmp : 0;
            shakeY = shakeDur>0 ? rand.nextInt(shakeAmp*2+1)-shakeAmp : 0;
        }
        if (bigMsgLife > 0) bigMsgLife -= delta;
        if (hudComboTimer > 0) { hudComboTimer -= delta; if (hudComboTimer<=0) hudComboCount=0; }
        if (dailyBonusTimer > 0) dailyBonusTimer -= delta;
        if (unlockNotifTimer > 0) unlockNotifTimer -= delta;
        for (int i=0;i<4;i++) if (feedLife[i]>0) feedLife[i]-=delta;

        switch (gameState) {
            case GS_LOGO:        updateLogo(delta);       break;
            case GS_GAME:        updateGame(gameDelta);     break;
            case GS_SURVIVAL:    updateSurvival(gameDelta); break;
            case GS_MELEE:       updateMelee(delta);        break;
            case GS_MELEE_END:   updateMeleeEnd(delta);     break;
            case GS_ROUND_END:   updateRoundEnd(delta);    break;
            case GS_WAVE_END:    updateWaveEnd(delta);     break;
            case GS_SHARD_REWARD:/* static, key exits */   break;
            case GS_STORY_INTRO: updateStoryIntro(delta);  break;
            case GS_VICTORY_ANIM: updateVictoryAnim(delta);  break;
            case GS_CINEMATIC:    updateCinematic(delta);    break;
            case GS_STORY_SEL:    /* input only */            break;
            case GS_SETTINGS:     /* input only */            break;
            case GS_TOURNAMENT:   /* input only */            break;
        }

        SaveData.updatePowerup(delta);
        updateSimSound(delta);
        updateSticktension(delta);
        updateWeather(delta);
        updateDamageNumbers(delta);
        updateAirDash(delta);
        updateStamina(delta);
        updateRagdoll(delta);
        updateConfetti(delta);
        updateArenaBg(delta);
        updateDodgeRoll(delta);
        updateKillCamAndCrown(delta);
        updateArenaSplash(delta);
        if (chargeFlash    > 0) chargeFlash    -= delta;
        if (playerHitFlash > 0) playerHitFlash -= delta;
        if (enemyHitFlash  > 0) enemyHitFlash  -= delta;
        if (rankedFlash    > 0) rankedFlash     -= delta;
        if (tooltipTimer   > 0) { tooltipTimer -= delta; if (tooltipTimer<=0) tooltipVisible=false; }
        if (achPopupTimer  > 0) achPopupTimer  -= delta;
        if (levelUpFlash   > 0) levelUpFlash   -= delta;
        if (levelUpTimer   > 0) levelUpTimer   -= delta;
        if (parryWindow    > 0) parryWindow    -= delta;
        if (parryFlashTimer> 0) parryFlashTimer-= delta;
        if (parryCooldown  > 0) parryCooldown  -= delta;
        applyPendingBGM();
        if (inputGuard > 0) inputGuard -= delta;
    }

    // ==================================================================
    //  NEW v5: WEATHER SYSTEM
    // ==================================================================
    private void initWeather(int arenaIdx) {
        // Each arena gets a probability-weighted weather type
        int[][] weatherTable = {
            { WEATHER_NONE },                               // 0: The Ring
            { WEATHER_RAIN, WEATHER_STORM },                // 1: Sky Bridge
            { WEATHER_NONE, WEATHER_SANDSTORM },            // 2: Infernal Crucible
            { WEATHER_BLIZZARD, WEATHER_BLIZZARD },         // 3: Frozen Citadel
            { WEATHER_RAIN, WEATHER_NONE },                 // 4: Neo Arcadia
            { WEATHER_RAIN, WEATHER_STORM, WEATHER_RAIN },  // 5: Jungle Ruins
            { WEATHER_NONE, WEATHER_STORM },                // 6: Spike Pit
            { WEATHER_NONE, WEATHER_SANDSTORM }             // 7: Lava Cave
        };
        int ai = arenaIdx < weatherTable.length ? arenaIdx : 0;
        int[] opts = weatherTable[ai];
        currentWeather = opts[rand.nextInt(opts.length)];
        weatherTimer   = 0;
        // Spawn initial weather particles
        for (int i = 0; i < MAX_WEATHER; i++) respawnWeatherParticle(i);
    }

    private void respawnWeatherParticle(int i) {
        wX[i] = rand.nextInt(SW);
        wY[i] = -(rand.nextInt(40));
        switch (currentWeather) {
            case WEATHER_RAIN:
                wVX[i] = -0.5f + rand.nextFloat();
                wVY[i] = 5f + rand.nextFloat() * 3f;
                wL[i]  = 800 + rand.nextInt(400);
                break;
            case WEATHER_STORM:
                wVX[i] = -3f + rand.nextFloat() * 1.5f;
                wVY[i] = 6f + rand.nextFloat() * 4f;
                wL[i]  = 600 + rand.nextInt(300);
                break;
            case WEATHER_SANDSTORM:
                wVX[i] = 2f + rand.nextFloat() * 3f;
                wVY[i] = 0.5f + rand.nextFloat();
                wL[i]  = 1200 + rand.nextInt(600);
                break;
            case WEATHER_BLIZZARD:
                wVX[i] = -1.5f + rand.nextFloat();
                wVY[i] = 2f + rand.nextFloat() * 2f;
                wL[i]  = 1000 + rand.nextInt(500);
                break;
            default:
                wL[i] = 0;
        }
    }

    private void updateWeather(int delta) {
        if (currentWeather == WEATHER_NONE) return;
        float dt = delta / 16.6f;
        for (int i = 0; i < MAX_WEATHER; i++) {
            if (wL[i] <= 0) continue;
            wL[i] -= delta;
            wX[i] += wVX[i] * dt;
            wY[i] += wVY[i] * dt;
            if (wY[i] > SH + 10 || wX[i] < -10 || wX[i] > SW + 10 || wL[i] <= 0) {
                respawnWeatherParticle(i);
            }
        }
    }

    private void renderWeather(Graphics g) {
        if (currentWeather == WEATHER_NONE) return;
        for (int i = 0; i < MAX_WEATHER; i++) {
            if (wL[i] <= 0) continue;
            int wx2 = (int) wX[i], wy2 = (int) wY[i];
            switch (currentWeather) {
                case WEATHER_RAIN:
                    g.setColor(0x4466AA);
                    g.drawLine(wx2, wy2, wx2 - 1, wy2 + 5);
                    break;
                case WEATHER_STORM:
                    g.setColor(0x2244AA);
                    g.drawLine(wx2, wy2, wx2 - 3, wy2 + 7);
                    // Lightning flash occasionally
                    if (weatherTimer % 4000 < 100) {
                        g.setColor(0xCCDDFF);
                        g.fillRect(0, 0, SW, SH);
                    }
                    break;
                case WEATHER_SANDSTORM:
                    g.setColor(0xAA7733);
                    g.fillRect(wx2, wy2, 3, 2);
                    break;
                case WEATHER_BLIZZARD:
                    g.setColor(0xCCDDFF);
                    g.fillRect(wx2, wy2, 2, 2);
                    break;
            }
        }
        // Ambient tint overlay
        switch (currentWeather) {
            case WEATHER_SANDSTORM:
                g.setColor(0x221100); // brownish tint lines
                for (int sl = 0; sl < SH; sl += 8) g.drawLine(0, sl, SW, sl);
                break;
            case WEATHER_BLIZZARD:
                g.setColor(0x000622); // blue tint lines
                for (int sl = 0; sl < SH; sl += 10) g.drawLine(0, sl, SW, sl);
                break;
        }
        weatherTimer += 16;
    }

    // ==================================================================
    //  NEW v5: FLOATING DAMAGE NUMBERS
    // ==================================================================
    private void spawnDamageNumber(int wx2, int wy2, int val, boolean crit) {
        dnX[dnHead]    = wx2;
        dnY[dnHead]    = wy2 - 8;
        dnVal[dnHead]  = val;
        dnLife[dnHead] = crit ? 1200 : 900;
        dnCrit[dnHead] = crit;
        dnHead = (dnHead + 1) % MAX_DMG_NUM;
    }

    private void updateDamageNumbers(int delta) {
        for (int i = 0; i < MAX_DMG_NUM; i++) {
            if (dnLife[i] > 0) {
                dnLife[i] -= delta;
                dnY[i]    -= delta * 0.04f;  // float upward
            }
        }
    }

    private void renderDamageNumbers(Graphics g) {
        g.setFont(fSmall);
        for (int i = 0; i < MAX_DMG_NUM; i++) {
            if (dnLife[i] <= 0) continue;
            int sx = (int)(dnX[i] - camX);
            int sy = (int)(dnY[i] - camY);
            if (sx < -20 || sx > SW + 20) continue;
            // Fade out
            boolean bright = dnLife[i] > 400;
            if (dnCrit[i]) {
                g.setColor(bright ? 0xFFDD00 : 0x886600);
                g.drawString("!" + dnVal[i] + "!", sx, sy, Graphics.HCENTER|Graphics.TOP);
            } else {
                g.setColor(bright ? 0xFF4444 : 0x883322);
                g.drawString(String.valueOf(dnVal[i]), sx, sy, Graphics.HCENTER|Graphics.TOP);
            }
        }
    }

    // ==================================================================
    //  NEW v5: AIR DASH + TRAIL
    // ==================================================================
    private void updateAirDash(int delta) {
        if (airDashTimer > 0) {
            airDashTimer -= delta;
            // Move player
            if (player != null && airDashTimer > 0) {
                float dashSpeed = 280f * (delta / 1000f);
                player.x += airDashDir * dashSpeed;
            }
            // Spawn trail particles
            if (player != null) {
                dashTrailX[dashTrailHead] = player.x;
                dashTrailY[dashTrailHead] = player.y - 14;
                dashTrailL[dashTrailHead] = 200;
                dashTrailHead = (dashTrailHead + 1) % 8;
            }
        }
        // Decay trail
        for (int i = 0; i < 8; i++) if (dashTrailL[i] > 0) dashTrailL[i] -= delta;
        // Reset air dash on landing
        if (player != null && player.onGround) {
            playerAirDashed = false;
            airDashTimer    = 0;
        }
    }

    private void renderDashTrail(Graphics g) {
        for (int i = 0; i < 8; i++) {
            if (dashTrailL[i] <= 0) continue;
            int alpha = dashTrailL[i] * 4;
            int col = GameData.CHAR_COLOR[playerCharId];
            int r = ((col >> 16) & 0xFF) * alpha / 800;
            int gr= ((col >>  8) & 0xFF) * alpha / 800;
            int b2= ( col        & 0xFF) * alpha / 800;
            if (r > 255) r = 255; if (gr > 255) gr = 255; if (b2 > 255) b2 = 255;
            g.setColor((r << 16) | (gr << 8) | b2);
            g.fillArc((int)(dashTrailX[i] - camX) - 3, (int)(dashTrailY[i] - camY) - 3, 6, 6, 0, 360);
        }
    }

    // ==================================================================
    //  NEW v5: STAMINA
    // ==================================================================
    private void updateStamina(int delta) {
        if (gameState != GS_GAME && gameState != GS_SURVIVAL && gameState != GS_MELEE) return;
        if (player == null || !player.alive) return;
        if (playerStamina < 100) {
            staminaRegen += delta;
            int pts = staminaRegen / STAM_REGEN_RATE;
            if (pts > 0) {
                playerStamina = Math.min(100, playerStamina + pts);
                staminaRegen  = staminaRegen % STAM_REGEN_RATE;
            }
        }
    }

    private void renderStaminaBar(Graphics g) {
        // Stamina bar is now rendered inside renderHUD (ROW C, left side).
        // This method kept for compatibility but draws nothing.
    }

    // ==================================================================
    //  NEW v5: LEVEL-UP SYSTEM
    // ==================================================================
    private void awardXP(int amount) {
        if (playerLevel >= MAX_LEVEL) return;
        playerXP += amount;
        int needed = XP_TO_LEVEL[Math.min(playerLevel, MAX_LEVEL - 1)];
        if (playerXP >= needed && playerLevel < MAX_LEVEL) {
            playerLevel++;
            playerXP -= needed;
            levelUpFlash = 800;
            levelUpTimer = 2000;
            // Stat bonus per level
            if (player != null) {
                player.maxHealth += 8;
                player.health    = Math.min(player.health + 8, player.maxHealth);
                player.dmgMult   += 2;
            }
            showBigMsg("LEVEL " + playerLevel + "!", C_GOLD);
            SoundManager.playSFX(SoundManager.SFX_WIN);
            // Check prestige at max level
            if (playerLevel >= MAX_LEVEL) {
                showBigMsg("MAX LEVEL! PRESTIGE?", C_GOLD);
            }
            SaveData.save();
        }
    }

    private void renderLevelBadge(Graphics g) {
        // Small level badge top-center
        int lx = SW / 2 - 12, ly = 28;
        boolean flash = levelUpFlash > 0 && (levelUpFlash / 100) % 2 == 0;
        g.setColor(flash ? C_GOLD : 0x1A1A30);
        g.fillRoundRect(lx, ly, 24, 12, 4, 4);
        g.setColor(flash ? 0x000000 : C_GOLD);
        g.setFont(fSmall);
        // Prestige star
        String badge = prestigeCount > 0 ? "*" + playerLevel : "L" + playerLevel;
        g.drawString(badge, SW / 2, ly + 1, Graphics.HCENTER|Graphics.TOP);
    }

    // ==================================================================
    //  NEW v5: MINIMAP
    // ==================================================================
    private void renderMinimap(Graphics g) {
        if (Arena.isDark()) return; // Void Sanctum suppresses minimap
        // Only in game states
        if (gameState != GS_GAME && gameState != GS_SURVIVAL &&
            gameState != GS_MELEE && gameState != GS_WAVE_END &&
            gameState != GS_ROUND_END) return;

        int mx = MM_X, my = MM_Y;
        // Background
        g.setColor(0x080810); g.fillRect(mx, my, MM_W, MM_H);
        g.setColor(0x223344); g.drawRect(mx, my, MM_W, MM_H);

        // Scale factor: world -> minimap
        float sx = (float) MM_W / WORLD_W;
        float sy = (float) MM_H / WORLD_H;

        // Draw platforms as grey lines
        g.setColor(0x334455);
        int[][] plats = Arena.PLATFORMS;
        for (int i = 0; i < plats.length; i++) {
            if (i < Arena.breakTimer.length && Arena.breakTimer[i] > 0) continue;
            int pmx = mx + (int)(plats[i][0] * sx);
            int pmy = my + (int)(plats[i][1] * sy);
            int pmw = Math.max(1, (int)(plats[i][2] * sx));
            g.drawLine(pmx, pmy, pmx + pmw, pmy);
        }

        // Camera viewport rectangle
        g.setColor(0x334422);
        g.drawRect(mx + (int)(camX * sx), my + (int)(camY * sy),
                   Math.max(1,(int)(SW * sx)), Math.max(1,(int)(SH * sy)));

        // Player dot - character color
        if (player != null) {
            int pdx = mx + (int)(player.x * sx);
            int pdy = my + (int)(player.y * sy);
            pdx = Math.max(mx + 1, Math.min(mx + MM_W - 2, pdx));
            pdy = Math.max(my + 1, Math.min(my + MM_H - 2, pdy));
            g.setColor(GameData.CHAR_COLOR[playerCharId]);
            g.fillRect(pdx - 1, pdy - 1, 3, 3);
        }

        // Enemy dots
        if (enemy != null && enemy.alive) {
            int edx = mx + (int)(enemy.x * sx);
            int edy = my + (int)(enemy.y * sy);
            edx = Math.max(mx + 1, Math.min(mx + MM_W - 2, edx));
            edy = Math.max(my + 1, Math.min(my + MM_H - 2, edy));
            g.setColor(C_RED);
            g.fillRect(edx - 1, edy - 1, 3, 3);
        }
        // Melee fighters
        for (int i = 0; i < 3; i++) {
            if (meleeAI[i] == null || !meleeAI[i].alive) continue;
            int adx = mx + (int)(meleeAI[i].x * sx);
            int ady = my + (int)(meleeAI[i].y * sy);
            adx = Math.max(mx + 1, Math.min(mx + MM_W - 2, adx));
            ady = Math.max(my + 1, Math.min(my + MM_H - 2, ady));
            g.setColor(GameData.CHAR_COLOR[meleeCharIds[i]]);
            g.fillRect(adx, ady, 2, 2);
        }
    }

    // ==================================================================
    //  NEW v5: OFF-SCREEN ENEMY ARROW INDICATOR
    // ==================================================================
    private void renderEnemyArrow(Graphics g) {
        if (enemy == null || !enemy.alive) return;
        // Convert enemy world pos to screen pos
        int ex = (int)(enemy.x - camX);
        int ey = (int)(enemy.y - camY);
        // If on screen, no arrow needed
        if (ex >= 10 && ex <= SW - 10 && ey >= 10 && ey <= SH - 10) return;

        // Compute direction vector - CLDC 1.1 safe (no atan2/cos/sin needed)
        int dx = ex - SW / 2;
        int dy = ey - SH / 2;

        // Normalise dx,dy to unit scale (fixed-point *1000)
        int len = (int) Math.sqrt((long)dx*dx + (long)dy*dy);
        if (len == 0) return;
        int cosK = dx * 1000 / len;  // cos * 1000
        int sinK = dy * 1000 / len;  // sin * 1000

        // Find intersection with screen margin rect
        int arrowX, arrowY;
        int halfW = SW/2 - 20, halfH = SH/2 - 20;
        // Parametric: t such that |cosK*t/1000| = halfW or |sinK*t/1000| = halfH
        int tX = cosK != 0 ? halfW * 1000 / Math.abs(cosK) : 99999;
        int tY = sinK != 0 ? halfH * 1000 / Math.abs(sinK) : 99999;
        int t  = tX < tY ? tX : tY;
        arrowX = SW/2 + cosK * t / 1000;
        arrowY = SH/2 + sinK * t / 1000;
        arrowX = Math.max(8, Math.min(SW - 8, arrowX));
        arrowY = Math.max(8, Math.min(SH - 8, arrowY));

        // Draw arrow triangle (tip in direction of enemy, base perpendicular)
        int enemyCol = GameData.CHAR_COLOR[enemy.charId];
        boolean flash = (System.currentTimeMillis() % 600L) < 300L;
        g.setColor(flash ? enemyCol : 0x662222);
        int tipX  = arrowX + cosK * 6 / 1000;
        int tipY  = arrowY + sinK * 6 / 1000;
        int baseX1 = arrowX + (-sinK) * 4 / 1000;
        int baseY1 = arrowY + ( cosK) * 4 / 1000;
        int baseX2 = arrowX - (-sinK) * 4 / 1000;
        int baseY2 = arrowY - ( cosK) * 4 / 1000;
        g.drawLine(tipX, tipY, baseX1, baseY1);
        g.drawLine(tipX, tipY, baseX2, baseY2);
        g.drawLine(baseX1, baseY1, baseX2, baseY2);
        // Distance label (use integer len already computed)
        if (len < 9990) {
            g.setFont(fSmall);
            g.setColor(0x884444);
            g.drawString((len / 10) + "m", arrowX, arrowY - 10, Graphics.HCENTER|Graphics.TOP);
        }
    }

    // ==================================================================
    //  NEW v5: ACHIEVEMENT POPUP TOAST
    // ==================================================================
    private void triggerAchievementPopup(String msg) {
        achPopupMsg   = msg;
        achPopupTimer = ACH_POPUP_DUR;
    }

    private void renderAchievementPopup(Graphics g) {
        if (achPopupTimer <= 0 || achPopupMsg.length() == 0) return;
        // Slide in from top
        int slideY = achPopupTimer > ACH_POPUP_DUR - 300 ?
            -20 + (ACH_POPUP_DUR - achPopupTimer) / 5 : 2;
        g.setColor(0x1A1400); g.fillRoundRect(20, slideY, SW - 40, 20, 4, 4);
        g.setColor(C_GOLD);   g.drawRoundRect(20, slideY, SW - 40, 20, 4, 4);
        g.setFont(fSmall); g.setColor(C_GOLD);
        g.drawString("ACH: " + achPopupMsg, SW / 2, slideY + 4, Graphics.HCENTER|Graphics.TOP);
    }

    // ==================================================================
    //  NEW v5: SETTINGS SCREEN
    // ==================================================================
    private void renderSettings(Graphics g) {
        drawBg(g);
        g.setColor(0x0A0A1C); g.fillRect(0, 0, SW, SH);
        g.setFont(fMedium); g.setColor(C_GOLD);
        g.drawString("SETTINGS", SW/2, 8, Graphics.HCENTER|Graphics.TOP);
        g.setColor(C_GREY); g.setFont(fSmall);
        g.drawString("v5.0 ETERNAL REALMS", SW/2, 26, Graphics.HCENTER|Graphics.TOP);

        String[] labels = {
            "SFX VOLUME",
            "MUSIC",
            "CONTROLS HINT",
            "RESET SAVE",
            "BACK"
        };
        int[]    cols   = { C_CYAN, C_CYAN, C_GREEN, C_RED, C_WHITE };

        for (int i = 0; i < SETTINGS_ITEMS; i++) {
            int iy = 50 + i * 40;
            boolean sel = (i == settingsSel);
            g.setColor(sel ? 0x1A1A30 : 0x0D0D1C);
            g.fillRoundRect(16, iy, SW - 32, 32, 6, 6);
            if (sel) { g.setColor(cols[i]); g.drawRoundRect(16, iy, SW - 32, 32, 6, 6); }

            g.setFont(fSmall); g.setColor(sel ? cols[i] : C_GREY);
            g.drawString(labels[i], 28, iy + 4, Graphics.LEFT|Graphics.TOP);

            // Value display
            g.setColor(C_WHITE);
            switch (i) {
                case 0: g.drawString(SaveData.sfxEnabled   ? "ON"  : "OFF", SW - 28, iy + 4, Graphics.RIGHT|Graphics.TOP); break;
                case 1: g.drawString(SaveData.musicEnabled ? "ON"  : "OFF", SW - 28, iy + 4, Graphics.RIGHT|Graphics.TOP); break;
                case 2: g.drawString(showControlsHint      ? "SHOW": "HIDE",SW - 28, iy + 4, Graphics.RIGHT|Graphics.TOP); break;
                case 3: g.setColor(C_RED); g.drawString("HOLD 5s", SW - 28, iy + 4, Graphics.RIGHT|Graphics.TOP); break;
                case 4: g.drawString(">", SW - 28, iy + 4, Graphics.RIGHT|Graphics.TOP); break;
            }

            // Volume bar for SFX
            if (i == 0) {
                int vbx = 28, vby = iy + 18;
                g.setColor(0x112233); g.fillRect(vbx, vby, 100, 6);
                g.setColor(C_CYAN);  g.fillRect(vbx, vby, SaveData.volume, 6);
                g.setColor(0x334455); g.drawRect(vbx, vby, 100, 6);
            }
        }

        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString("2/8:select  5:toggle  0:back", SW/2, SH-10, Graphics.HCENTER|Graphics.TOP);
    }

    private void handleSettingsKey(int key) {
        if (key == KEY_NUM2 || key == KEY_NUM8) {
            settingsSel = (settingsSel + (key == KEY_NUM2 ? -1 : 1) + SETTINGS_ITEMS) % SETTINGS_ITEMS;
        } else if (key == KEY_NUM5) {
            switch (settingsSel) {
                case 0: SaveData.sfxEnabled   = !SaveData.sfxEnabled;   SaveData.save(); break;
                case 1: SaveData.musicEnabled = !SaveData.musicEnabled; SaveData.save(); break;
                case 2: showControlsHint = !showControlsHint; break;
                case 3: /* RESET - requires hold, handled elsewhere */ break;
                case 4: gameState = GS_MENU; break;
            }
        } else if (key == KEY_NUM0) {
            gameState = GS_MENU;
        }
    }

    // ==================================================================
    //  NEW v5: PARALLAX BACKGROUND LAYERS
    // ==================================================================
    private void renderParallaxBg(Graphics g, float cx2, float cy2) {
        // Layer 0: far mountains/silhouettes at 0.2x camera speed
        float px02 = cx2 * 0.2f;
        // Draw distant arena-themed silhouettes
        int arenaCol = getArenaParallaxColor();
        g.setColor(arenaCol);
        // Rolling hills / distant spires
        int[] hillH = { 80, 110, 70, 130, 90, 120, 60, 100, 85, 115 };
        int hillW = 48;
        for (int h = 0; h < hillH.length; h++) {
            int hx = (int)(-px02 % (SW + 100)) + h * hillW;
            if (hx < -hillW) hx += SW + hillW * hillH.length;
            if (hx > SW + hillW) continue;
            g.fillArc(hx - hillW/2, SH - hillH[h], hillW, hillH[h] * 2, 0, 180);
        }

        // Layer 1: mid-distance at 0.5x camera speed
        float px05 = cx2 * 0.5f;
        g.setColor(darken(arenaCol));
        int[] platH2 = { 200, 150, 220, 170, 240 };
        for (int p = 0; p < platH2.length; p++) {
            int px3 = (int)(-px05 % (SW + 60)) + p * 52;
            if (px3 < -60) px3 += SW + 60 * platH2.length;
            g.fillRect(px3, platH2[p], 40, SH - platH2[p]);
        }
    }

    private int getArenaParallaxColor() {
        int ai = Arena.getArenaIndex();
        int[] cols = {
            0x0A0F1A, 0x070E1A, 0x180800, 0x081018,
            0x060818, 0x081205, 0x100A0A, 0x1A0800
        };
        return ai < cols.length ? cols[ai] : 0x0A0A14;
    }

    private static int darken(int col) {
        int r = ((col >> 16) & 0xFF) / 2;
        int g2= ((col >>  8) & 0xFF) / 2;
        int b2= ( col        & 0xFF) / 2;
        return (r << 16) | (g2 << 8) | b2;
    }

    // ==================================================================
    //  NEW v6: RAGDOLL DEATH
    // ==================================================================
    private void spawnRagdoll(float ox, float oy, float vx, int charCol) {
        rdActive = true;
        rdLife   = 2200;
        // 6 segments: head, torso, larm, rarm, lleg, rleg
        float[] offX = { 0,  0, -12, 12, -8,  8 };
        float[] offY = {-28,-10, -12,-12,  6,  6 };
        float[] vxMult = { 0.5f, 0.1f, -1.2f, 1.2f, -0.8f, 0.8f };
        float[] vyBase = { -4f, -1f, -3f, -3f, -2f, -2f };
        for (int i = 0; i < RAGDOLL_SEGS; i++) {
            rdX[i]  = ox + offX[i];
            rdY[i]  = oy + offY[i];
            rdVX[i] = vx * vxMult[i] + (rand.nextFloat()-0.5f)*120f;
            rdVY[i] = vyBase[i] * 80f + (rand.nextFloat()-0.3f)*60f;
            rdCol[i]= charCol;
        }
    }

    private void updateRagdoll(int delta) {
        if (!rdActive || rdLife <= 0) { rdActive = false; return; }
        rdLife -= delta;
        float dt = delta / 1000f;
        for (int i = 0; i < RAGDOLL_SEGS; i++) {
            rdVY[i] += 600f * dt;   // gravity
            rdVX[i] *= 0.96f;       // friction
            rdX[i]  += rdVX[i] * dt;
            rdY[i]  += rdVY[i] * dt;
            // floor bounce
            if (rdY[i] > Arena.H - 20) { rdY[i] = Arena.H - 20; rdVY[i] *= -0.4f; }
        }
    }

    private void renderRagdoll(Graphics g) {
        if (!rdActive || rdLife <= 0) return;
        int alpha = Math.min(255, rdLife / 8);
        // Segments: head=0, torso=1, larm=2, rarm=3, lleg=4, rleg=5
        int sx = (int)(rdX[1] - camX), sy = (int)(rdY[1] - camY);
        // Torso
        g.setColor(darken(rdCol[1]));
        g.drawLine(sx, sy, (int)(rdX[0]-camX), (int)(rdY[0]-camY));
        // Arms
        g.setColor(rdCol[2]); g.drawLine(sx, sy, (int)(rdX[2]-camX), (int)(rdY[2]-camY));
        g.setColor(rdCol[3]); g.drawLine(sx, sy, (int)(rdX[3]-camX), (int)(rdY[3]-camY));
        // Legs
        g.setColor(rdCol[4]); g.drawLine(sx, sy+14, (int)(rdX[4]-camX), (int)(rdY[4]-camY));
        g.setColor(rdCol[5]); g.drawLine(sx, sy+14, (int)(rdX[5]-camX), (int)(rdY[5]-camY));
        // Head
        g.setColor(rdCol[0]);
        g.fillArc((int)(rdX[0]-camX)-5, (int)(rdY[0]-camY)-5, 10, 10, 0, 360);
    }

    // ==================================================================
    //  NEW v6: CONFETTI (on round win)
    // ==================================================================
    private void spawnConfetti() {
        cfLife = 2800;
        int[] cols = { C_GOLD, C_CYAN, 0xFF4488, C_GREEN, C_ORANGE, C_PURPLE };
        for (int i = 0; i < MAX_CONFETTI; i++) {
            cfX[i]  = rand.nextInt(SW);
            cfY[i]  = -rand.nextInt(SH / 2);
            cfVX[i] = (rand.nextFloat() - 0.5f) * 60f;
            cfVY[i] = 40f + rand.nextFloat() * 80f;
            cfCol[i]= cols[rand.nextInt(cols.length)];
        }
    }

    private void updateConfetti(int delta) {
        if (cfLife <= 0) return;
        cfLife -= delta;
        float dt = delta / 1000f;
        for (int i = 0; i < MAX_CONFETTI; i++) {
            cfX[i] += cfVX[i] * dt;
            cfY[i] += cfVY[i] * dt;
            if (cfY[i] > SH + 10) cfY[i] = -10;
        }
    }

    private void renderConfetti(Graphics g) {
        if (cfLife <= 0) return;
        for (int i = 0; i < MAX_CONFETTI; i++) {
            g.setColor(cfCol[i]);
            int cx2 = (int) cfX[i], cy2 = (int) cfY[i];
            boolean rect = (i % 3 != 0);
            if (rect) g.fillRect(cx2, cy2, 3, 5);
            else      g.fillArc(cx2, cy2, 4, 4, 0, 360);
        }
    }

    // ==================================================================
    //  NEW v6: ANIMATED ARENA BACKGROUND
    // ==================================================================
    private void initArenaBg() {
        arenaBgTimer = 0;
        for (int i = 0; i < bgLavaX.length; i++) {
            bgLavaX[i] = rand.nextFloat() * WORLD_W;
        }
        bgIceGlint = 0;
    }

    private void updateArenaBg(int delta) {
        arenaBgTimer += delta;
        int ai = Arena.getArenaIndex();
        if (ai == 2 || ai == 7) {   // lava arenas
            for (int i = 0; i < bgLavaX.length; i++) {
                bgLavaX[i] += (0.8f + i * 0.15f) * delta / 40f;
                if (bgLavaX[i] > WORLD_W + 20) bgLavaX[i] = -20;
            }
        }
        if (ai == 3) {  // ice arena
            bgIceGlint = (bgIceGlint + delta * 0.08f) % 360f;
        }
    }

    private void renderArenaBg(Graphics g) {
        int ai = Arena.getArenaIndex();
        // Behind platforms - use world coords (camera already applied)
        switch (ai) {
            case 2: case 7: // Lava arenas - rising heat shimmer lines
                g.setColor(0x1A0500);
                for (int row = 0; row < WORLD_H; row += 22) {
                    int shift = (int)(Math.sin(arenaBgTimer * 0.002f + row * 0.05f) * 4);
                    g.drawLine(shift, row, WORLD_W + shift, row);
                }
                // Floating lava blobs in background
                for (int i = 0; i < bgLavaX.length; i++) {
                    int bx = (int) bgLavaX[i];
                    int by = (int)(WORLD_H - 80 - i * 30
                             + Math.sin(arenaBgTimer * 0.003f + i) * 12);
                    g.setColor(i % 2 == 0 ? 0x2A0800 : 0x1A0400);
                    g.fillArc(bx - 16, by - 8, 32, 16, 0, 360);
                }
                break;

            case 3: // Frozen Citadel - ice crystal sparkles
                g.setColor(0x04080F);
                for (int row = 0; row < WORLD_H; row += 18) {
                    g.drawLine(0, row, WORLD_W, row);
                }
                // Crystal glint sweeping
                int glintX = (int)((bgIceGlint / 360f) * WORLD_W);
                g.setColor(0x224466);
                g.drawLine(glintX, 0, glintX + 40, WORLD_H);
                g.setColor(0x112233);
                g.drawLine(glintX + 4, 0, glintX + 44, WORLD_H);
                break;

            case 1: // Sky Bridge - cloud layers
                g.setColor(0x0A0A18);
                int cloud0 = (arenaBgTimer / 60) % (WORLD_W + 80);
                int cloud1 = (arenaBgTimer / 45 + 120) % (WORLD_W + 80);
                g.fillArc(cloud0 - 40, 60, 80, 30, 0, 360);
                g.fillArc(cloud1 - 30, 140, 60, 24, 0, 360);
                break;

            case 4: // Neo Arcadia - neon grid
                g.setColor(0x04040E);
                int gridOff = (arenaBgTimer / 80) % 32;
                for (int gx2 = gridOff; gx2 < WORLD_W; gx2 += 32) g.drawLine(gx2, 0, gx2, WORLD_H);
                for (int gy2 = gridOff; gy2 < WORLD_H; gy2 += 32) g.drawLine(0, gy2, WORLD_W, gy2);
                break;
        }
    }

    // ==================================================================
    //  NEW v6: HIT FLASH
    // ==================================================================
    private void triggerHitFlash(boolean isPlayer) {
        if (isPlayer) playerHitFlash = HIT_FLASH_DUR;
        else          enemyHitFlash  = HIT_FLASH_DUR;
    }

    private void renderHitFlash(Graphics g, Stickman s, boolean isPlayer) {
        if (s == null) return;
        int flashMs = isPlayer ? playerHitFlash : enemyHitFlash;
        if (flashMs <= 0) return;
        // White overlay rect around character
        int sx = (int)(s.x - camX) - 10;
        int sy = (int)(s.y - camY) - 36;
        g.setColor(0xFFFFFF);
        g.drawRect(sx, sy, 20, 40);
        g.drawRect(sx-1, sy-1, 22, 42);
    }

    // ==================================================================
    //  NEW v6: WEAPON GLOW (ground weapons pulse)
    // ==================================================================
    private void renderWeaponGlow(Graphics g) {
        long t = System.currentTimeMillis();
        for (int i = 0; i < MAX_GW; i++) {
            if (!gwActive[i]) continue;
            int wx2 = (int) gwX[i], wy2 = (int) gwY[i];
            int rar = Weapon.RARITY[gwType[i]];
            int glowCol = GameData.RARITY_COLOR[rar];
            int pulse = (int)(Math.abs(Math.sin(t * 0.003 + i)) * 6);
            g.setColor(darken(glowCol));
            g.drawArc(wx2 - 8 - pulse, wy2 - 8 - pulse,
                      16 + pulse*2, 16 + pulse*2, 0, 360);
            if (rar >= 2) {
                g.setColor(glowCol);
                g.drawArc(wx2 - 6 - pulse/2, wy2 - 6 - pulse/2,
                          12 + pulse, 12 + pulse, 0, 360);
            }
        }
    }

    // ==================================================================
    //  NEW v6: CHARACTER SHADOW
    // ==================================================================
    private void renderCharShadow(Graphics g, Stickman s) {
        if (s == null || !s.alive) return;
        // Find ground below character
        int groundY = Arena.H;
        int[] lt = new int[1];
        int land = Arena.checkLanding(s.x, s.y + 200, 8, 1, lt);
        if (land > 0) groundY = land;
        int height = groundY - (int) s.y;
        if (height < 0 || height > 160) return;
        // Shadow shrinks with height
        int sw2 = Math.max(4, 20 - height / 8);
        int sh2 = Math.max(2, 8  - height / 16);
        int alpha = Math.max(20, 80 - height);
        g.setColor(0x000000 | ((alpha / 3) << 16)); // crude alpha via dark grey
        g.setColor(0x111118);
        g.fillArc((int) s.x - sw2, groundY - sh2/2, sw2*2, sh2, 0, 360);
    }

    // ==================================================================
    //  NEW v6: CHARGE ATTACK VISUAL
    // ==================================================================
    private void renderChargeIndicator(Graphics g) {
        if (chargeTimer < 200 || player == null) return;
        int pct = Math.min(100, chargeTimer * 100 / CHARGE_THRESHOLD);
        int px2 = (int)(player.x - camX);
        int py2 = (int)(player.y - camY) - 44;
        // Charge arc ring
        int arcAngle = 360 * pct / 100;
        g.setColor(chargeReady ? C_GOLD : C_ORANGE);
        g.drawArc(px2 - 10, py2 - 4, 20, 20, 90, -arcAngle);
        if (chargeReady) {
            // Pulsing glow when fully charged
            boolean pulse = (System.currentTimeMillis() % 300L) < 150L;
            if (pulse) {
                g.setColor(0xFF8800);
                g.drawArc(px2 - 12, py2 - 6, 24, 24, 0, 360);
            }
            g.setFont(fSmall); g.setColor(C_GOLD);
            g.drawString("CHARGE!", px2, py2 - 8, Graphics.HCENTER|Graphics.TOP);
        }
    }

    // ==================================================================
    //  NEW v6: RANKED MODE
    // ==================================================================
    private void initRankedMode() {
        isRankedMode = true;
        isTournMode  = false;
        playerCharId = SaveData.selectedChar;
        enemyCharId  = 1 + rand.nextInt(GameData.CHAR_COUNT - 1);
        arenaSel     = rand.nextInt(Arena.ARENA_COUNT);
        playerScore  = 0; enemyScore = 0; pendingShards = 0;
        initRound();
        showBigMsg("RANKED MATCH START!", RANK_COLS[rankedRank]);
    }

    private void updateRankedOnWin() {
        if (!isRankedMode) return;
        rankedWins++;
        rankedStreak++;
        // Bonus shards for streaks
        int bonus = rankedStreak >= 3 ? 60 : 20;
        SaveData.addShards(bonus);
        // Check rank-up
        int newRank = 0;
        for (int r = RANK_WINS.length - 1; r >= 0; r--) {
            if (rankedWins >= RANK_WINS[r]) { newRank = r; break; }
        }
        if (newRank > rankedRank) {
            rankedRank  = newRank;
            rankedFlash = 1800;
            triggerAchievementPopup("RANK UP: " + RANK_NAMES[rankedRank] + "!");
            SoundManager.playSFX(SoundManager.SFX_UNLOCK);
        }
        SaveData.save();
    }

    private void renderRankBadge(Graphics g) {
        if (!isRankedMode) return;
        int rx = SW / 2 - 22, ry = 28;
        g.setColor(rankedFlash > 0 ? RANK_COLS[rankedRank] : darken(RANK_COLS[rankedRank]));
        g.fillRoundRect(rx, ry, 44, 12, 4, 4);
        g.setColor(0x000000); g.setFont(fSmall);
        g.drawString(RANK_NAMES[rankedRank], SW/2, ry+1, Graphics.HCENTER|Graphics.TOP);
        // Streak counter
        if (rankedStreak >= 2) {
            g.setColor(C_GOLD); g.setFont(fSmall);
            g.drawString(rankedStreak + " STREAK", SW - 4, 42, Graphics.RIGHT|Graphics.TOP);
        }
    }

    // ==================================================================
    //  NEW v6: TOURNAMENT MODE
    // ==================================================================
    private void initTournament() {
        isTournMode = true;
        isRankedMode= false;
        tournRound  = 0;
        tournWinner = -1;
        tournBracketSel = 0;
        // Slot 0 = player's character, rest random
        tournSlots[0] = SaveData.selectedChar;
        boolean[] used = new boolean[GameData.CHAR_COUNT];
        used[tournSlots[0]] = true;
        for (int i = 1; i < TOURN_SIZE; i++) {
            int c;
            do { c = rand.nextInt(GameData.CHAR_COUNT); } while (used[c]);
            tournSlots[i] = c;
            used[c] = true;
        }
        for (int i = 0; i < TOURN_SIZE; i++) tournDone[i] = false;
        gameState = GS_TOURNAMENT;
        showBigMsg("TOURNAMENT BEGINS!", C_GOLD);
    }

    private void renderTournament(Graphics g) {
        drawBg(g);
        g.setColor(0x09091C); g.fillRect(0, 0, SW, SH);
        drawBg(g);

        g.setFont(fMedium); g.setColor(C_GOLD);
        g.drawString("TOURNAMENT", SW/2, 6, Graphics.HCENTER|Graphics.TOP);
        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString("Round " + (tournRound + 1) + " of 3", SW/2, 24, Graphics.HCENTER|Graphics.TOP);

        // Draw bracket: 8 fighters in QF pairs
        int pairsThisRound = TOURN_SIZE >> (tournRound + 1); // 4, 2, 1
        int startSlot = 0;
        for (int r = 0; r < tournRound; r++) startSlot += (TOURN_SIZE >> (r + 1));

        for (int p = 0; p < 4; p++) {
            int slotA = p * 2;
            int slotB = p * 2 + 1;
            int py2 = 50 + p * 60;

            boolean active = (p < pairsThisRound);
            int colA = active ? GameData.CHAR_COLOR[tournSlots[slotA]] : C_GREY;
            int colB = active ? GameData.CHAR_COLOR[tournSlots[slotB]] : C_GREY;

            // Panel
            g.setColor(active ? 0x0F0F22 : 0x080810);
            g.fillRoundRect(8, py2, SW - 16, 50, 6, 6);
            g.setColor(active ? C_YELLOW : C_GREY);
            g.drawRoundRect(8, py2, SW - 16, 50, 6, 6);

            // Fighter A
            g.setFont(fSmall); g.setColor(colA);
            String nameA = tournSlots[slotA] < GameData.CHAR_COUNT ?
                GameData.CHAR_NAME[tournSlots[slotA]] : "???";
            g.drawString(nameA, 18, py2 + 6, Graphics.LEFT|Graphics.TOP);

            // VS
            g.setColor(active ? C_ORANGE : C_GREY);
            g.drawString("VS", SW/2, py2 + 18, Graphics.HCENTER|Graphics.TOP);

            // Fighter B
            g.setColor(colB);
            String nameB = tournSlots[slotB] < GameData.CHAR_COUNT ?
                GameData.CHAR_NAME[tournSlots[slotB]] : "???";
            g.drawString(nameB, SW - 18, py2 + 6, Graphics.RIGHT|Graphics.TOP);

            // Done mark
            if (tournDone[p]) {
                g.setColor(C_GREEN); g.setFont(fSmall);
                g.drawString("DONE", SW/2, py2 + 34, Graphics.HCENTER|Graphics.TOP);
            }
        }

        // Controls
        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString("[5] FIGHT  [0] EXIT", SW/2, SH - 12, Graphics.HCENTER|Graphics.TOP);
    }

    // ==================================================================
    //  NEW v6: DAILY CHALLENGE
    // ==================================================================
    private void checkDailyChallenge() {
        // Use day-of-year approximation from System.currentTimeMillis
        int dayOfYear = (int)((System.currentTimeMillis() / 86400000L) % 365);
        if (dayOfYear != dailyChallengeDay) {
            dailyChallengeDay = dayOfYear;
            dailyChallenge    = dayOfYear % DAILY_NAMES.length;
            dailyComplete     = false;
            showBigMsg("DAILY: " + DAILY_NAMES[dailyChallenge] + "!", C_CYAN);
        }
    }

    private void applyDailyChallenge() {
        if (dailyChallenge < 0 || player == null) return;
        switch (dailyChallenge) {
            case 0: // IRON MAN - block healing
                power3Cooldown = P3_CD * 10;
                break;
            case 1: // GLASS CANNON - double dmg both ways
                player.dmgMult  = player.dmgMult * 2;
                if (enemy != null) enemy.dmgMult = enemy.dmgMult * 2;
                break;
            case 3: // DISARMED - no weapon pickups
                clearGW();
                weaponSpawnTimer = 999999;
                break;
            case 5: // SPEED DEMON - 2x speed
                player.speedBoost = true;
                break;
        }
    }

    // ==================================================================
    //  NEW v6: TOOLTIP SYSTEM
    // ==================================================================
    private void showTooltip(String line1, String line2) {
        tooltipLine1  = line1;
        tooltipLine2  = line2;
        tooltipVisible= true;
        tooltipTimer  = TOOLTIP_DUR;
    }

    private void renderTooltip(Graphics g) {
        if (!tooltipVisible || tooltipTimer <= 0) return;
        int fadeAlpha = Math.min(255, tooltipTimer / 4);
        int ty2 = SH - 46;
        g.setColor(0x0A1020); g.fillRoundRect(10, ty2, SW - 20, 36, 6, 6);
        g.setColor(C_CYAN);   g.drawRoundRect(10, ty2, SW - 20, 36, 6, 6);
        g.setFont(fSmall);
        g.setColor(C_WHITE); g.drawString(tooltipLine1, SW/2, ty2+4,  Graphics.HCENTER|Graphics.TOP);
        g.setColor(C_GREY);  g.drawString(tooltipLine2, SW/2, ty2+18, Graphics.HCENTER|Graphics.TOP);
    }

    // ==================================================================
    //  NEW v6: PAUSE SCREEN with MATCH STATS
    // ==================================================================
    //  NEW v6: ENHANCED ROUND BANNER with confetti
    // ==================================================================
    private void renderEnhancedRoundBanner(Graphics g) {
        renderRoundBanner(g);
        renderConfetti(g);
    }

    // ==================================================================
    //  NEW v7: DODGE ROLL
    // ==================================================================
    private void tryDodgeRoll(int delta) {
        if (player == null || player.onGround) return;
        if (dodgeCooldown > 0 || playerStamina < DODGE_STAM) return;
        // Consume stamina
        playerStamina   = Math.max(0, playerStamina - DODGE_STAM);
        dodgeDir        = player.facingRight ? 1 : -1;
        dodgeTimer      = DODGE_DUR;
        dodgeCooldown   = DODGE_COOLDOWN;
        dodgeInvincible = true;
        spawnParticles((int)player.x, (int)(player.y-16), C_CYAN, 8);
        showBigMsg("DODGE!", C_CYAN);
    }

    private void updateDodgeRoll(int delta) {
        if (dodgeCooldown > 0) dodgeCooldown -= delta;
        if (dodgeTimer > 0) {
            dodgeTimer -= delta;
            if (player != null) {
                float spd = 300f * (delta / 1000f);
                player.x += dodgeDir * spd;
                // Add afterimage trail
                dashTrailX[dashTrailHead] = player.x;
                dashTrailY[dashTrailHead] = player.y - 16;
                dashTrailL[dashTrailHead] = 180;
                dashTrailHead = (dashTrailHead + 1) % 8;
            }
            if (dodgeTimer <= DODGE_DUR - DODGE_INVINCE) dodgeInvincible = false;
        } else {
            dodgeInvincible = false;
        }
    }

    // ==================================================================
    //  NEW v7: KILL CAM
    // ==================================================================
    private void updateKillCam(int delta) {
        if (killCamTimer <= 0) { killCamZoom = 1f; return; }
        killCamTimer -= delta;
        // Zoom decays back to 1 over duration
        float t = killCamTimer / (float) KILL_CAM_DUR;
        killCamZoom = 1f + 0.35f * t;
    }

    // ==================================================================
    //  NEW v7: SUDDEN DEATH OVERTIME
    // ==================================================================
    private void checkSuddenDeath(int delta) {
        if (roundTimeLeft > 0 || suddenDeathActive) return;
        // Timer hit 0 - is it a draw?
        if (player != null && enemy != null &&
            player.health > 0 && enemy.health > 0 &&
            Math.abs(player.health - enemy.health) < 30) {
            suddenDeathActive = true;
            suddenDeathTimer  = 30000;
            showBigMsg("SUDDEN DEATH!", 0xFF2200);
            triggerShake(8, 400);
            roundTimeLeft = suddenDeathTimer;
        }
    }

    // ==================================================================
    //  NEW v7: ARENA SPLASH + CROWN rendering helpers
    // ==================================================================
    private void renderArenaSplash(Graphics g) {
        if (arenaSplashTimer <= 0) return;
        int alpha = Math.min(255, arenaSplashTimer * 3);
        boolean bright = arenaSplashTimer > ARENA_SPLASH_DUR / 2;
        // Dark overlay fades in then out
        g.setColor(bright ? 0x000000 : 0x000000);
        // Semi-transparent by drawing lines every other pixel
        if (bright) { for (int yy=0;yy<SH;yy+=2) g.drawLine(0,yy,SW,yy); }

        // Arena name text centred
        String aname = arenaSel<GameData.ARENA_NAME.length?GameData.ARENA_NAME[arenaSel]:(arenaSel==8?"THUNDER PEAK":(arenaSel==9?"VOID SANCTUM":"UNKNOWN REALM"));
        g.setFont(fLarge);
        int col = bright ? 0xFFFFFF : C_GOLD;
        // Shadow
        g.setColor(0x000000);
        g.drawString(aname, SW/2+2, SH/2-14, Graphics.HCENTER|Graphics.TOP);
        g.setColor(col);
        g.drawString(aname, SW/2, SH/2-16, Graphics.HCENTER|Graphics.TOP);

        // Sub-label
        g.setFont(fSmall);
        g.setColor(C_GREY);
        String sub = arenaSel<GameData.ARENA_SUBTITLE.length?GameData.ARENA_SUBTITLE[arenaSel]:(arenaSel==8?"Fight in the storm":(arenaSel==9?"Nothing is permanent":""));
        g.drawString(sub, SW/2, SH/2+4, Graphics.HCENTER|Graphics.TOP);
    }

    private void renderScoreCrown(Graphics g) {
        if (crownTimer <= 0 || player == null) return;
        Stickman crowned = crownOnPlayer ? player : enemy;
        if (crowned == null) return;
        int cx2 = (int)(crowned.x - camX);
        int cy2 = (int)(crowned.y - camY) - 46;
        // Crown shape: 3 spikes
        g.setColor(C_GOLD);
        g.drawLine(cx2-8, cy2+8, cx2-8, cy2);
        g.drawLine(cx2-8, cy2,   cx2,   cy2-8);
        g.drawLine(cx2,   cy2-8, cx2+8, cy2);
        g.drawLine(cx2+8, cy2,   cx2+8, cy2+8);
        g.drawLine(cx2-8, cy2+8, cx2+8, cy2+8);
        // Gem in centre
        g.setColor(0xFF4444);
        g.fillArc(cx2-2, cy2-5, 4, 4, 0, 360);
    }

    private void updateArenaSplash(int delta) {
        if (arenaSplashTimer > 0) arenaSplashTimer -= delta;
    }

    private void updateKillCamAndCrown(int delta) {
        updateKillCam(delta);
        if (crownTimer > 0) crownTimer -= delta;
    }

    // ==================================================================
    //  NEW v7: CHALLENGE TRACKER HUD
    // ==================================================================
    private void renderChallengeTracker(Graphics g) {
        if (dailyChallenge < 0 || dailyComplete) return;
        // Single compact line below the player HP bar (which ends ~y=22)
        int tx = 4, ty = 24;
        g.setFont(fSmall); g.setColor(0x006666);
        // Short name only - max 8 chars to fit left side
        String name = DAILY_NAMES[dailyChallenge];
        if (name.length() > 8) name = name.substring(0, 8);
        g.drawString("D:" + name, tx, ty, Graphics.LEFT|Graphics.TOP);
    }

    private void updateLogo(int delta) {
        logoSubTimer += delta;

        // ---- Always-running animators -----------------------------------
        // Beat pulse oscillator
        if (logoPulseUp) {
            logoPulse += delta / 8;
            if (logoPulse >= 100) { logoPulse = 100; logoPulseUp = false; }
        } else {
            logoPulse -= delta / 12;
            if (logoPulse <= 0)  { logoPulse = 0;   logoPulseUp = true;  }
        }
        // Shimmer sweep (wraps across screen width + padding)
        logoShimmer = (logoShimmer + delta / 4) % (SW + 80);
        // Orbiting sparks
        logoOrbit = (logoOrbit + delta / 6) % 360;
        // Arc rotation
        logoArcTimer = (logoArcTimer + delta) % 1200;
        // Flash decay
        if (logoFlash > 0) { logoFlash -= delta / 5; if (logoFlash < 0) logoFlash = 0; }

        // ---- Fire particles (always update once phase >= 2) -------------
        if (logoPhase >= 2) {
            for (int i = 0; i < 12; i++) {
                logoFireLife[i] -= delta;
                logoFireY[i]    -= delta / 55;
                if (logoFireLife[i] <= 0) {
                    logoFireX[i]    = rand.nextInt(64) - 32;
                    logoFireY[i]    = rand.nextInt(8);
                    logoFireLife[i] = 350 + rand.nextInt(650);
                }
            }
        }

        // ---- Phase state machine ----------------------------------------
        switch (logoPhase) {
            case 0: // Black - 300 ms
                if (logoSubTimer > 300) {
                    logoPhase = 1; logoSubTimer = 0; logoRingR = 0; logoFlash = 160;
                }
                break;

            case 1: // Ring burst expand
                logoRingR += delta / 2;
                logoFlare  = Math.min(255, logoFlare + delta * 2);
                if (logoRingR > 180) {
                    logoPhase = 2; logoSubTimer = 0; logoCharY = -80; logoFlash = 200;
                }
                break;

            case 2: // Characters drop in + nebula settles
                logoCharY = Math.min(0, logoCharY + delta / 3);
                logoGlitch = (logoSubTimer / 60) % 3;
                if (logoSubTimer > 900) {
                    logoPhase = 3; logoSubTimer = 0; logoAlpha2 = 0; logoFlash = 120;
                }
                break;

            case 3: // Text fade in + glitch + explosion
                logoAlpha2 = Math.min(255, logoAlpha2 + delta * 3);
                logoGlitch = (logoSubTimer / 40) % 5;
                logoFlare  = Math.max(0, logoFlare - delta);
                // Trigger explosion burst at text-reveal moment
                if (logoAlpha2 >= 180 && logoExplode == 0) {
                    logoExplode = 1; logoFlash = 220;
                }
                if (logoExplode > 0 && logoExplode < 20) logoExplode++;
                if (logoSubTimer > 1400) {
                    logoPhase = 4; logoSubTimer = 0;
                    logoGlitchTimer = 80 + rand.nextInt(150);
                }
                break;

            case 4: // Hold + glitch pulses, then exit
                logoGlitchTimer -= delta;
                if (logoGlitchTimer <= 0) {
                    logoGlitch      = (logoGlitch + 1) % 6;
                    logoGlitchTimer = 80 + rand.nextInt(180);
                }
                if (logoSubTimer > 900) {
                    gameState = GS_MENU;
                    simSound(SSND_MENU);
                }
                break;
        }
    }

    private void updateStoryIntro(int delta) {
        // Scroll text down, then stop at resting position - player presses 5 to fight
        if (scrollY > 20) {
            scrollY -= (delta / 25);
            if (scrollY < 20) scrollY = 20;
        }
    }

    // ==================================================================
    //  Init helpers
    // ==================================================================

    /** CLDC-safe string split on \n character. */
    private static String[] splitLines(String s) {
        int count = 1;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == '\n') count++;
        String[] result = new String[count];
        int idx = 0, start = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                result[idx++] = s.substring(start, i);
                start = i + 1;
            }
        }
        result[idx] = s.substring(start);
        return result;
    }

    private void initRound() {
        if (isStoryFight) setPendingBGM(SoundManager.BGM_STORY);
        else setPendingBGM(SoundManager.BGM_COMBAT);
        SaveData.applySkills();
        Arena.setArena(arenaSel);

        player = makeChar(playerCharId, Arena.SPAWN[0], Arena.SPAWN[1], true);
        enemy  = makeChar(enemyCharId,  Arena.SPAWN[2], Arena.SPAWN[3], false);

        // Reset camera to centre on player spawn
        camX = Arena.SPAWN[0] - SW / 2f;
        camY = Arena.SPAWN[1] - SH / 2f;
        if (camX < 0) camX = 0;
        if (camY < 0) camY = 0;

        power3Cooldown=0; power7Cooldown=0; power9Cooldown=0;
        power7Timer=0; power9Timer=0;
        clearBullets(); clearGW(); clearParticles();
        roundTimeLeft = 180000;
        weaponSpawnTimer = 5000;
        jumpQueued = ultraQueued = throwQueued = false;
        if (player != null) { player.killsThisRound = 0; player.specialCooldown = 0; }
        blockHeld = false;
        bigMsg = ""; bigMsgLife = 0;
        spawnArenaWeapons();
        simBgmTimer=0;
        // Story/VS: give enemy a challenging weapon based on chapter/arena
        if (isStoryFight && enemy != null && enemy.weaponType == Weapon.TYPE_NONE) {
            int ch = Math.max(0, Math.min(storyChapter, 7));
            int[] bossWpn = {Weapon.TYPE_PISTOL, Weapon.TYPE_AK47, Weapon.TYPE_SHOTGUN,
                             Weapon.TYPE_SNIPER, Weapon.TYPE_ROCKET, Weapon.TYPE_LASER,
                             Weapon.TYPE_REVOLVER, Weapon.TYPE_CROSSBOW};
            enemy.weaponType = bossWpn[ch % bossWpn.length];
            enemy.weaponAmmo = Weapon.AMMO[enemy.weaponType] * 2;
        }
        // Flash weapon pickup hint at round start
        if (Arena.WPNSPAWN.length >= 2)
            showBigMsg("WEAPONS ON GROUND - PICK UP!", C_YELLOW);

        // ?? v5/v6/v7 init ?????????????????????????????????????????
        initWeather(arenaSel);
        initArenaBg();
        checkDailyChallenge();
        // Reset match stats
        statDmgDealt = 0; statDmgReceived = 0;
        statHitsLanded = 0; statPerfectBlocks = 0;
        // Reset per-round flags
        firstBloodDone  = false;
        suddenDeathActive = false;
        suddenDeathTimer  = 30000;
        // Arena name splash
        arenaSplashTimer  = ARENA_SPLASH_DUR;
        // Reset kill cam
        killCamTimer = 0; killCamZoom = 1f;
        // Reset stamina
        playerStamina = 100; staminaRegen = 0;
        // Reset dodge
        dodgeTimer = 0; dodgeCooldown = 0;
        // Reset charge
        chargeTimer = 0; chargeReady = false;
        // Reset ragdoll
        rdActive = false; rdLife = 0;
        // Reset confetti
        cfLife = 0;
        // Reset crown
        crownTimer = 0;
        // Apply daily challenge modifiers
        applyDailyChallenge();

        gameState = GS_GAME;
    }

    private Stickman makeChar(int charId, float sx, float sy, boolean isPlayer) {
        Stickman s = new Stickman(sx, sy, isPlayer);
        // Apply stat multipliers
        // Scale HP to 300 base for longer, more epic fights
        int baseHP = (GameData.getCharHP(charId) * 6) + SaveData.skillHPBonus;  // 6x for epic long fights
        s.health    = baseHP;
        s.maxHealth = baseHP;
        s.maxHealth = s.health;
        // Speed modifier baked into WALK_SPEED override via multiplier stored on stickman
        s.speedMult = GameData.getCharSpeed(charId) + (isPlayer ? SaveData.skillSpeedBonus : 0);
        s.dmgMult   = GameData.getCharDmg(charId)   + (isPlayer ? SaveData.skillDmgBonus  : 0);
        s.defMult   = GameData.getCharDef(charId);
        s.charId    = charId;
        // Use equipped weapon if player has one equipped, otherwise use default
        if (isPlayer) {
            int eqSlot = SaveData.getEquippedWeaponType(charId);
            if (eqSlot >= 0 && eqSlot < 8 && SaveData.isShopItemOwned(eqSlot)
                && GameData.SHOP_WEAPON_ID[eqSlot] >= 0) {
                s.weaponType = GameData.SHOP_WEAPON_ID[eqSlot];
            } else {
                s.weaponType = GameData.CHAR_START_WEAPON[charId];
            }
        } else {
            s.weaponType = GameData.CHAR_START_WEAPON[charId];
        }
        s.weaponAmmo = Weapon.AMMO[Math.min(s.weaponType, Weapon.AMMO.length-1)];
        s.assignSpecial();  // assign special move for this character
        s.applyCharTraits(); // apply charId-specific passive traits
        // Apply owned shop armor items to player character
        if (isPlayer) {
            s.hasDragonhide    = SaveData.isShopItemOwned(8);   // Dragonhide Suit
            s.hasCyberPlating  = SaveData.isShopItemOwned(9);   // Cyber Plating
            s.hasFrostShield   = SaveData.isShopItemOwned(10);  // Frost Shield
            s.shadowweaveActive= SaveData.isShopItemOwned(11);  // Shadowweave
            s.hasEmberGauntlets= SaveData.isShopItemOwned(12);  // Ember Gauntlets
            if (s.hasCyberPlating) { s.health += 60; s.maxHealth += 60; }
        }
        return s;
    }

    private void initSurvival() {
        setPendingBGM(SoundManager.BGM_COMBAT);
        survivalWave = 1; survivalKills = 0;
        Arena.setArena(0);
        SaveData.applySkills();
        player = makeChar(playerCharId, Arena.SPAWN[0], Arena.SPAWN[1], true);
        enemy  = makeSurvivalEnemy();
        clearBullets(); clearGW(); clearParticles();
        weaponSpawnTimer = 8000;
        spawnArenaWeapons();   // BUG FIX: weapons spawn at wave start
        camX = Arena.SPAWN[0] - SW / 2f; if (camX < 0) camX = 0; // BUG FIX: reset camera
        camY = Arena.SPAWN[1] - SH / 2f; if (camY < 0) camY = 0;
        jumpQueued = ultraQueued = throwQueued = false;
        blockHeld = false;
        showBigMsg("WAVE 1!", C_CYAN);
        gameState = GS_SURVIVAL;
    }

    private Stickman makeSurvivalEnemy() {
        int eid = 1 + (survivalWave % (GameData.CHAR_COUNT - 1));
        Stickman e = makeChar(eid, Arena.SPAWN[2], Arena.SPAWN[3], false);
        e.health    = Math.min(200, 80 + survivalWave * 10);
        e.maxHealth = e.health;
        int wt = Weapon.TYPE_PISTOL;
        if (survivalWave>=3)  wt = Weapon.TYPE_SHOTGUN;
        if (survivalWave>=5)  wt = Weapon.TYPE_SHURIKEN;
        if (survivalWave>=6)  wt = Weapon.TYPE_AK47;
        if (survivalWave>=8)  wt = Weapon.TYPE_THUNDER;
        if (survivalWave>=10) wt = Weapon.TYPE_SNIPER;
        if (survivalWave>=12) wt = Weapon.TYPE_SCYTHE;
        if (survivalWave>=15) wt = Weapon.TYPE_ROCKET;
        if (survivalWave>=18) wt = Weapon.TYPE_RAILGUN;
        e.weaponType = wt;
        e.weaponAmmo = Weapon.AMMO[wt] + survivalWave;
        return e;
    }

    private void startStoryFight() {
        isStoryFight = true;
        simBgmTimer  = 0;
        // Safe chapter clamp
        int ch = Math.max(0, Math.min(storyChapter, GameData.CHAPTER_BOSS_CHAR.length - 1));
        int boss = GameData.CHAPTER_BOSS_CHAR[ch];
        playerCharId = SaveData.selectedChar;
        enemyCharId  = Math.max(0, Math.min(boss, GameData.CHAR_COUNT - 1));
        arenaSel     = Math.max(0, Math.min(GameData.CHAR_HOME_ARENA[enemyCharId],
                                            GameData.ARENA_COUNT - 1));
        playerScore  = 0; enemyScore = 0;
        initRound();
        // BGM starts AFTER initRound to avoid threading race on real devices
        if (ch >= 4) setPendingBGM(SoundManager.BGM_VOID);
        else         setPendingBGM(SoundManager.BGM_STORY);
    }

    // ==================================================================
    //  2D CAMERA - smooth-follows midpoint between player and enemy
    //  World size: WORLD_W x WORLD_H.  Camera is clamped so it never
    //  shows outside the world.  HUD is drawn in screen space (camera
    //  translate is reversed before HUD calls in renderGame).
    // ==================================================================
    // CAMERA v2 - melee-aware bounding box camera.
    // ROOT CAUSE of melee camera bug: old camera only tracked
    // player<->enemy midpoint. enemy==meleeAI[0] in melee. When
    // meleeAI[0] died enemy became null, camera locked to player
    // alone, ignoring remaining 3 fighters. FIX: in GS_MELEE,
    // expand bounding box to include ALL alive fighters.
    private void updateCamera(int delta) {
        if (player == null) return;
        float minX=player.x, maxX=player.x, minY=player.y, maxY=player.y;
        if (gameState == GS_MELEE) {
            for (int i=0; i<3; i++) {
                if (meleeAI[i]!=null && meleeAI[i].alive) {
                    if (meleeAI[i].x<minX) minX=meleeAI[i].x;
                    if (meleeAI[i].x>maxX) maxX=meleeAI[i].x;
                    if (meleeAI[i].y<minY) minY=meleeAI[i].y;
                    if (meleeAI[i].y>maxY) maxY=meleeAI[i].y;
                }
            }
        } else {
            if (enemy!=null && enemy.alive) {
                if (enemy.x<minX) minX=enemy.x; if (enemy.x>maxX) maxX=enemy.x;
                if (enemy.y<minY) minY=enemy.y; if (enemy.y>maxY) maxY=enemy.y;
            }
        }
        float targetX=(minX+maxX)/2f, targetY=(minY+maxY)/2f;
        if (maxX-minX > SW*0.6f) targetX=player.x*0.55f+targetX*0.45f;
        if (maxY-minY > SH*0.6f) targetY=player.y*0.55f+targetY*0.45f;
        float dcx=targetX-SW/2f, dcy=targetY-SH/2f;
        if (dcx<0) dcx=0; if (dcx>WORLD_W-SW) dcx=WORLD_W-SW;
        if (dcy<0) dcy=0; if (dcy>WORLD_H-SH) dcy=WORLD_H-SH;
        float lerp=(gameState==GS_MELEE)?Math.min(CAM_LERP*1.5f,1f):CAM_LERP;
        camX+=(dcx-camX)*lerp; camY+=(dcy-camY)*lerp;
    }

    private void clearBullets()   { for (int i=0;i<MAX_BULLETS;i++) bullets[i].active=false; }
    private void clearGW()        { for (int i=0;i<MAX_GW;i++) gwActive[i]=false; }
    private void clearParticles() { for (int i=0;i<MAX_PART;i++) pLife[i]=0; for (int i=0;i<MAX_POOLS;i++) poolR[i]=0; killStreak=0; killStreakTimer=0; slomoDur=0; slomoScale=1f; heartbeatTimer=0; }

    private void spawnArenaWeapons() {
        int[] ws = Arena.WPNSPAWN;
        for (int i = 0; i+1 < ws.length; i += 2)
            spawnGWAt(randWeapon(1), ws[i], ws[i+1]);
    }

    private int randWeapon(int minRarity) {
        for (int t=0;t<30;t++) {
            int w = 1 + rand.nextInt(Weapon.TYPE_COUNT-1);
            if (Weapon.RARITY[w] >= minRarity) return w;
        }
        return Weapon.TYPE_PISTOL;
    }

    // ==================================================================
    //  Game update (VS + Story)
    // ==================================================================
    private void updateGame(int delta) {
        int keys  = getKeyStates();
        boolean L = (keys & LEFT_PRESSED)  != 0;
        boolean R = (keys & RIGHT_PRESSED) != 0;
        boolean F = (keys & FIRE_PRESSED)  != 0;
        boolean D = (keys & DOWN_PRESSED)  != 0;
        boolean blk = blockHeld || D;
        boolean jmp = jumpQueued;  jumpQueued  = false;
        boolean ult = ultraQueued; ultraQueued = false;
        boolean wasOnGround = (player != null && player.onGround);

        // ?? v7: Charge attack - track how long attack key held ????????
        if (F && player != null && player.alive) {
            chargeTimer += delta;
            if (chargeTimer >= CHARGE_THRESHOLD && !chargeReady) {
                chargeReady = true;
                chargeFlash = 400;
                SoundManager.playSFX(SoundManager.SFX_POWERUP);
            }
        } else {
            if (!chargeReady) chargeTimer = 0;  // reset if released before threshold
        }

        // Tick power key cooldowns
        if (power3Cooldown > 0) power3Cooldown -= delta;
        if (power7Cooldown > 0) power7Cooldown -= delta;
        if (power9Cooldown > 0) power9Cooldown -= delta;
        if (power7Timer > 0) { power7Timer -= delta; if (power7Timer<=0) { if(player!=null) player.speedBoost=false; } }
        if (power9Timer > 0) { power9Timer -= delta; if (power9Timer<=0) { if(player!=null) player.shieldWall=false; } }

        // Power-up activation: if Realm Surge active, restore HP
        if (SaveData.activePowerup == 2 && SaveData.powerupTimer > 0) {
            player.health = player.maxHealth;
            player.ultraCooldown = 0;
            SaveData.activePowerup = -1;
        }

        player.update(delta, L, R, jmp, F, blk, ult, enemy);
        if (enemy != null) enemy.update(delta, false,false,false,false,false,false, player);
        applyBounce(player); applyBounce(enemy);
        // Land thud: was in air, now on ground
        if (!wasOnGround && player.onGround && player.vy > 60f) SoundManager.playSFX(SoundManager.SFX_LAND);

        consumeShootRequests();
        // -- Special move FX dispatch ----------------------------------
        dispatchSpecialFX(player, enemy);
        if (enemy != null) dispatchSpecialFX(enemy, player);

        checkMeleeHit(player, enemy, GameData.CHAR_COLOR[enemyCharId]);
        if (enemy != null) checkMeleeHit(enemy, player, GameData.CHAR_COLOR[playerCharId]);

        if (throwQueued) { throwQueued = false; tryPickup(player); }

        updateBullets(delta);
        updateGW(delta);
        updateParticles(delta);
        Arena.update(delta);

        // Arena FX: Wind force (Thunder Peak) ? push all alive fighters
        int windF = Arena.getWindForce();
        if (windF != 0) {
            float wPush = windF * (delta / 1000f);
            if (player != null && player.alive) player.x += wPush;
            if (enemy  != null && enemy.alive)  enemy.x  += wPush;
        }

        // Arena FX: Quake ? camera shake during quake
        if (Arena.isQuakeActive() && (System.currentTimeMillis()/150)%2==0) triggerShake(3,80);

        // Arena hazard: DANGER ZONE tick damage (lava floor, poison pool, void abyss)
        int dangerY=Arena.getDangerY(), dangerDmg=Arena.getDangerDmgPerSec();
        if (dangerY>0 && dangerDmg>0) {
            if (player!=null&&player.alive&&player.y>=dangerY)
                if ((roundTimeLeft%1000)<delta) player.takeDamage(dangerDmg/4,0,0);
            if (enemy!=null&&enemy.alive&&enemy.y>=dangerY)
                if ((roundTimeLeft%1000)<delta) enemy.takeDamage(dangerDmg/4,0,0);
        }

        // Arena hazard: poison floor drains standing player
        if (Arena.poisonFloor && player.onGround && player.alive) {
            if ((roundTimeLeft % 1000) < delta) player.takeDamage(3, 0, 0);
        }

        // Kael passive: per-kill combo bonus
        if (playerCharId == GameData.CH_KAEL && player.kills > SaveData.killComboBonus) {
            SaveData.killComboBonus = player.kills;
            player.dmgMult += 5;
        }

        roundTimeLeft -= delta;
        checkSuddenDeath(delta);
        if (roundTimeLeft <= 0) { roundTimeLeft=0; endRound(player.health>=enemy.health?0:1); }
        // Low HP heartbeat
        if (player.health > 0 && player.health <= player.maxHealth / 4) {
            heartbeatTimer -= delta;
            if (heartbeatTimer <= 0) {
                heartbeatTimer = 800;
                SoundManager.playSFX(SoundManager.SFX_HEARTBEAT);
            }
        }
        // Kill streak timer decay
        if (killStreakTimer > 0) { killStreakTimer -= delta; if (killStreakTimer<=0) killStreak=0; }
        if (!player.alive && player.y > Arena.H + 20) { killStreak=0; SoundManager.playSFX(SoundManager.SFX_DEATH2); endRound(1); }
        if (enemy!=null && !enemy.alive && enemy.y > Arena.H + 20) endRound(0);
        updateCamera(delta);
    }

    // ==================================================================
    //  Survival update
    // ==================================================================
    private void updateSurvival(int delta) {
        if (waveStartTimer > 0) { waveStartTimer -= delta; return; }

        int keys  = getKeyStates();
        boolean L = (keys & LEFT_PRESSED)  != 0;
        boolean R = (keys & RIGHT_PRESSED) != 0;
        boolean F = (keys & FIRE_PRESSED)  != 0;
        boolean D = (keys & DOWN_PRESSED)  != 0;
        boolean blk = blockHeld || D;
        boolean jmp = jumpQueued;  jumpQueued  = false;
        boolean ult = ultraQueued; ultraQueued = false;

        player.update(delta, L, R, jmp, F, blk, ult, enemy);
        if (enemy != null) enemy.update(delta,false,false,false,false,false,false,player);

        consumeShootRequests();
        if (enemy != null) {
            dispatchSpecialFX(player, enemy);
            dispatchSpecialFX(enemy, player);
            checkMeleeHit(player, enemy, GameData.CHAR_COLOR[enemy.charId]);
            checkMeleeHit(enemy,  player, GameData.CHAR_COLOR[playerCharId]);
        }
        if (throwQueued) { throwQueued=false; tryPickup(player); }
        if (power3Cooldown > 0) power3Cooldown -= delta;
        if (power7Cooldown > 0) power7Cooldown -= delta;
        if (power9Cooldown > 0) power9Cooldown -= delta;
        if (power7Timer > 0) { power7Timer -= delta; if (power7Timer<=0) { if(player!=null) player.speedBoost=false; } }
        if (power9Timer > 0) { power9Timer -= delta; if (power9Timer<=0) { if(player!=null) player.shieldWall=false; } }

        updateBullets(delta);
        updateGW(delta);
        updateParticles(delta);
        Arena.update(delta);

        // Wind + danger zone physics for survival
        int windFS2=Arena.getWindForce();
        if (windFS2!=0&&player!=null&&player.alive) player.x+=windFS2*(delta/1000f);
        int dangerYS2=Arena.getDangerY(), dangerDS2=Arena.getDangerDmgPerSec();
        if (dangerYS2>0&&dangerDS2>0&&player!=null&&player.alive&&player.y>=dangerYS2)
            if ((roundTimeLeft%1000)<delta) player.takeDamage(dangerDS2/4,0,0);
        if (Arena.isQuakeActive()&&(System.currentTimeMillis()/200)%2==0) triggerShake(2,60);

        // Player death
        if (!player.alive && player.y > Arena.H + 20) {
            SaveData.addShards(survivalWave * GameData.SHARDS_SURVIVAL_WAVE);
            HighScore.save(survivalWave, survivalKills);
            HighScore.invalidate();
            pendingShards = survivalWave * GameData.SHARDS_SURVIVAL_WAVE;
            prevGameState = GS_SURVIVAL;
            gameState = GS_GAME_OVER;
            simSound(SSND_DEATH);
            SaveData.totalDeaths++; SaveData.save();
            return;
        }

        // Enemy died
        if (enemy != null && !enemy.alive && enemy.y > Arena.H+20) {
            survivalKills++;
            SaveData.addShards(GameData.SHARDS_SURVIVAL_WAVE);
            awardXP(20 + survivalWave * 5);  // v7: XP per wave kill
            // v7: weapon drop
            if (enemy.weaponType != Weapon.TYPE_NONE)
                spawnGWAt(enemy.weaponType, (int)enemy.x, (int)(enemy.y - 10));
            enemy = null;
            spawnParticles(Arena.SPAWN[2], Arena.SPAWN[3]-20, C_ORANGE, 12);
            addKillFeed("WAVE " + survivalWave + " DOWN!");
        }

        // Wave clear
        if (enemy == null) {
            waveEndTimer = 3200;
            bannerY = -60;
            gameState = GS_WAVE_END;
        }
        updateCamera(delta);   // BUG FIX: camera tracks player in survival
    }

    private void updateWaveEnd(int delta) {
        waveEndTimer -= delta;
        if (bannerY < 100) bannerY += (int)(delta * 0.14f);
        if (waveEndTimer <= 0) {
            survivalWave++;
            player.health = Math.min(player.maxHealth, player.health + 25);
            enemy = makeSurvivalEnemy();
            Arena.setArena(survivalWave % Arena.ARENA_COUNT);
            clearBullets(); clearGW(); clearParticles();
            spawnArenaWeapons(); weaponSpawnTimer = 8000;
            showBigMsg("WAVE " + survivalWave + "!", survivalWave % 5 == 0 ? C_GOLD : C_CYAN);
            waveStartTimer = 1500;
            gameState = GS_SURVIVAL;
        }
        updateCamera(0);
    }

    // ==================================================================
    //  Round end
    // ==================================================================
    private void endRound(int winner) {
        if (gameState != GS_GAME) return;
        prevGameState = GS_GAME;
        roundWinner = winner;
        if (winner==0) {
            playerScore++;
            pendingShards += GameData.SHARDS_PER_WIN;
            simSound(SSND_WIN);
            spawnConfetti();
            crownTimer = 2200; crownOnPlayer = true;
            if (isRankedMode) updateRankedOnWin();
            awardXP(50);
        } else if (winner==1) {
            enemyScore++;
            simSound(SSND_DEATH);
            crownTimer = 2200; crownOnPlayer = false;
        }
        // Weapon drop from enemy death
        if (winner == 0 && enemy != null && enemy.weaponType != Weapon.TYPE_NONE) {
            spawnGWAt(enemy.weaponType, (int)enemy.x, (int)(enemy.y - 10));
        }
        // Ragdoll for defeated character
        if (winner == 0 && enemy != null)
            spawnRagdoll(enemy.x, enemy.y, enemy.facingRight ? 80f : -80f,
                         GameData.CHAR_COLOR[enemy.charId]);
        if (winner == 1 && player != null)
            spawnRagdoll(player.x, player.y, player.facingRight ? 80f : -80f,
                         GameData.CHAR_COLOR[playerCharId]);
        roundEndTimer = 3000;
        bannerY = -60;
        gameState = GS_ROUND_END;
        addKillFeed(winner==0 ? "P1 WINS ROUND!" : "AI WINS ROUND!");
        if (winner==0) { comboKills++; comboTimer=3000; if(comboKills>=2) showBigMsg("COMBO x"+comboKills+"!", C_GOLD); }
    }

    private void updateRoundEnd(int delta) {
        roundEndTimer -= delta;
        if (bannerY < 115) bannerY += (int)(delta * 0.14f);
        if (roundEndTimer <= 0) {
            if (playerScore >= ROUNDS_TO_WIN || enemyScore >= ROUNDS_TO_WIN) {
                // Award shards, check boss unlock
                SaveData.addShards(pendingShards);
                if (playerScore >= ROUNDS_TO_WIN) {
                    SaveData.totalWins++;
                    playerWonRound = true;
                    checkBossUnlock();
                } else {
                    playerWonRound = false;
                }
                // Always show victory animation for both story and VS AI
                if (player != null) player.setVictory(playerWonRound);
                if (enemy  != null) enemy.setVictory(!playerWonRound);
                victoryAnimTimer = playerWonRound ? 2800 : 2000;
                gameState = GS_VICTORY_ANIM;
                SaveData.save();
            } else {
                arenaSel = (arenaSel + 1) % Arena.ARENA_COUNT;
                initRound();
            }
        }
    }

    // =================================================================
    //  VICTORY ANIMATION UPDATE
    // =================================================================
    private void updateVictoryAnim(int delta) {
        victoryAnimTimer -= delta;
        if (player != null) player.update(delta, false, false, false, false, false, false, enemy);
        if (enemy  != null) enemy.update(delta, false, false, false, false, false, false, player);
        if (victoryAnimTimer <= 0) {
            if (isStoryFight) {
                // Story: go to cinematic dialogue
                cinematicLine  = 0;
                cinematicTimer = 0;
                gameState = GS_CINEMATIC;
            } else {
                // VS AI / other: go straight to shard reward
                gameState = GS_SHARD_REWARD;
            }
        }
    }

    // =================================================================
    //  CINEMATIC / DIALOGUE UPDATE
    // =================================================================
    private void updateCinematic(int delta) {
        cinematicTimer += delta;
        // Auto advance narrator lines after 2.5s if player doesn't press
        if (cinematicTimer > 2500) {
            cinematicTimer = 0;
            cinematicLine++;
            int ch = Math.min(storyChapter, STORY_VICTORY.length - 1);
            String[] lines = splitLines(playerWonRound ? STORY_VICTORY[ch][0]
                                            : STORY_VICTORY[ch][1]);
            if (cinematicLine >= lines.length) {
                finishCinematic();
            }
        }
    }

    private void finishCinematic() {
        if (playerWonRound) {
            // Mark chapter done and give shard reward
            int ch = Math.min(storyChapter, GameData.CHAPTER_COUNT - 1);
            storyDone[ch] = true;
            SaveData.setChapterDone(ch); // persist to save
            int reward = (ch < CHAPTER_SHARD_REWARD.length) ? CHAPTER_SHARD_REWARD[ch] : 120;
            SaveData.addShards(reward);
            showBigMsg("CH."+( ch+1)+" DONE! +" + reward + " SHARDS!", C_GOLD);

            // Advance to next chapter
            // Chapter clear performance bonus
            int perfBonus = 30 + ch * 15;
            SaveData.addShards(perfBonus);
            pendingShards += perfBonus;
            storyChapter = ch + 1;
            if (storyChapter >= GameData.CHAPTER_COUNT) {
                // ALL CHAPTERS COMPLETE - true ending
                storyChapter = GameData.CHAPTER_COUNT - 1;
                showBigMsg("ALL CHAPTERS DONE! TRUE ENDING!", C_GOLD);
                SaveData.checkAchievement(3); // ach: all chapters
                gameState = GS_GAME_OVER;
                playerWonRound = true; // flag as victory for GS_GAME_OVER display
                prevGameState = GS_GAME;
            } else {
                // Go to story select showing unlocked next chapter
                gameState = GS_STORY_SEL;
            }
            SaveData.save();
        } else {
            // Player lost - show retry option via game over screen
            gameState = GS_GAME_OVER;
        }
    }

    // ==================================================================
    //  STICKTENSION - Territory Expansion System
    // ==================================================================
    private void activateSticktension() {
        stPhase     = ST_INVOKE;
        stTimer     = 0;
        stRadius    = 0f;
        stRuneAngle = 0;
        stCrackSeed = rand.nextInt(1000);
        // Consume both meters
        if (player != null) {
            player.specialCharge = 0;
            player.specialReady  = false;
            player.ultraCooldown = 4000;
        }
        triggerShake(10, 300);
        SoundManager.playSFX(SoundManager.SFX_ULTRA);
        int col = playerCharId < DOMAIN_COLORS.length ? DOMAIN_COLORS[playerCharId] : C_CYAN;
        String name = playerCharId < DOMAIN_NAMES.length ? DOMAIN_NAMES[playerCharId] : "STICKTENSION";
        showBigMsg(name + "!", col);
    }

    private void updateSticktension(int delta) {
        if (stPhase == ST_IDLE) return;
        stTimer += delta;
        // Rotate runes
        stRuneAngle = (stRuneAngle + delta * 3) % 3600;

        switch (stPhase) {
            case ST_INVOKE:
                if (stTimer >= 200) {
                    stPhase = ST_EXPAND; stTimer = 0; stRadius = 10f;
                    SoundManager.playSFX(SoundManager.SFX_SPECIAL);
                }
                break;
            case ST_EXPAND:
                stRadius = Math.min(180f, 10f + 170f * stTimer / 800f);
                if (stTimer >= 800) { stPhase = ST_ACTIVE; stTimer = 0; }
                break;
            case ST_ACTIVE:
                stRadius = 180f;
                // Slow enemy to 30% speed inside domain
                if (enemy != null && enemy.alive) enemy.speedMult = 30;
                // Regen player HP every 200ms (+3 HP tick)
                stRegenTimer += delta;
                if (stRegenTimer >= 200) {
                    stRegenTimer = 0;
                    // Heal PLAYER (not enemy!)
                    if (player != null && player.alive)
                        player.health = Math.min(player.maxHealth, player.health + 3);
                    // DoT ENEMY - domain deals 2 damage per 200ms tick
                    if (enemy != null && enemy.alive) {
                        enemy.health -= 2;
                        if (enemy.health < 0) enemy.health = 0;
                    }
                    // In melee, also DoT the other AI fighters
                    for (int mi = 0; mi < 3; mi++) {
                        if (meleeAI[mi] != null && meleeAI[mi].alive) {
                            meleeAI[mi].health -= 2;
                            if (meleeAI[mi].health < 0) meleeAI[mi].health = 0;
                        }
                    }
                }
                // 2x damage bonus for player hits handled in checkMeleeHit
                if (stTimer >= 5000) {
                    stPhase = ST_COLLAPSE; stTimer = 0;
                    SoundManager.playSFX(SoundManager.SFX_DEATH);
                    if (enemy != null) enemy.speedMult = GameData.getCharSpeed(enemyCharId);
                }
                break;
            case ST_COLLAPSE:
                stRadius = Math.max(0f, 180f - 180f * stTimer / 400f);
                if (stTimer >= 400) {
                    stPhase = ST_IDLE;
                    if (enemy != null) enemy.speedMult = GameData.getCharSpeed(Math.min(enemyCharId, GameData.CHAR_COUNT-1));
                    triggerShake(6, 200);
                }
                break;
        }
    }

    private void renderSticktension(Graphics g) {
        if (stPhase == ST_IDLE) return;
        int col = playerCharId < DOMAIN_COLORS.length ? DOMAIN_COLORS[playerCharId] : C_CYAN;
        int px  = player != null ? (int)player.x : SW/2;
        int py  = player != null ? (int)(player.y - 20) : SH/2;

        // ---- INVOKE PHASE: per-character unique attack burst ----
        if (stPhase == ST_INVOKE) {
            g.setColor(0x000000);
            for (int sl=0;sl<SH;sl+=2) g.drawLine(0,sl,SW,sl);
            int prog = stTimer * 1000 / 200;  // 0-1000 progress
            g.setColor(col);
            g.drawArc(px-20, py-20, 40, 40, 0, 360);
            renderSTInvoke(g, playerCharId, px, py, col, prog);
            return;
        }

        // ---- EXPAND / ACTIVE / COLLAPSE: full domain ----
        int r = (int)stRadius;
        if (r < 2) return;

        // ---- VOID BACKGROUND inside territory ----
        // Use fillRect approach: fill a bounding box then mask with arc
        // No Math.sqrt - J2ME fillArc does the clipping for us
        g.setColor(0x08000E);
        // Draw dark filled rectangle (approximate - real devices clip this fast)
        int rx2 = Math.max(0, px - r), ry2 = Math.max(0, py - r/2);
        int rw2 = Math.min(SW - rx2, r*2), rh2 = Math.min(SH - ry2, r);
        if (rw2 > 0 && rh2 > 0) g.fillArc(px - r, py - r/2, r*2, r, 0, 360);

        // ---- DOMAIN GRID - sparse, no sqrt, fixed step ----
        g.setColor(col & 0x111111 | 0x110011);
        int gridStep = 24; // wider grid = fewer lines = faster
        // Only draw lines within bounding box - no per-line clipping calc needed
        for (int gx = rx2; gx <= px + r; gx += gridStep) {
            if (gx < 0 || gx >= SW) continue;
            g.drawLine(gx, ry2, gx, ry2 + rh2);
        }
        for (int gy = ry2; gy <= py + r/2; gy += gridStep) {
            if (gy < 0 || gy >= SH) continue;
            g.drawLine(rx2, gy, rx2 + rw2, gy);
        }

        // ---- CENTRAL SIGIL (unique per character) ----
        int sigilSize = 24;
        g.setColor(col & 0x666666 | 0x220022);
        // Outer hexagon
        // Pre-computed hex points (cos/sin for 0,60,120,180,240,300 degrees)
        // cos: 1, 0.5, -0.5, -1, -0.5, 0.5  sin: 0, 0.87, 0.87, 0, -0.87, -0.87
        final int[] hcx = {sigilSize, sigilSize/2, -sigilSize/2, -sigilSize, -sigilSize/2, sigilSize/2};
        final int[] hcy = {0, sigilSize*87/200, sigilSize*87/200, 0, -sigilSize*87/200, -sigilSize*87/200};
        for (int v=0; v<6; v++) {
            int sx1 = px + hcx[v]; int sy1 = py + hcy[v]/2;
            int sx2 = px + hcx[(v+1)%6]; int sy2 = py + hcy[(v+1)%6]/2;
            g.setColor(col & 0x444444);
            g.drawLine(sx1, sy1, sx2, sy2);
        }
        // Inner star
        // Star lines using same hex points (inner star at half radius)
        for (int v=0; v<6; v++) {
            int sxi = px + hcx[v]/2;
            int syi = py + hcy[v]/4;
            g.setColor(col & 0x555555);
            g.drawLine(px, py, sxi, syi);
        }

        // ---- ORBITING RUNES (6 runes spin around player) ----
        // Rune orbit: 6 positions using pre-computed cos/sin*1000 for 60deg steps
        // cos(0,60,120,180,240,300)*1000: 1000,500,-500,-1000,-500,500
        // sin(0,60,120,180,240,300)*1000: 0,866,866,0,-866,-866
        final int[] rcx = {1000, 500, -500, -1000, -500, 500};
        final int[] rcy = {0, 866, 866, 0, -866, -866};
        int runeR = 36;
        // Rotate by adding offset index based on stRuneAngle
        int rotOff = (stRuneAngle / 600) % 6; // step every 60 degrees
        for (int ri=0; ri<6; ri++) {
            int rIdx = (ri + rotOff) % 6;
            int rx = px + rcx[rIdx] * runeR / 1000;
            int ry = py + rcy[rIdx] * runeR / 2000;
            if (rx < 0||rx>=SW||ry<0||ry>=SH) continue;
            // Rune glyph - draw a small distinctive shape per slot
            g.setColor(col);
            switch (ri % 4) {
                case 0: // diamond
                    g.drawLine(rx, ry-4, rx+4, ry); g.drawLine(rx+4, ry, rx, ry+4);
                    g.drawLine(rx, ry+4, rx-4, ry); g.drawLine(rx-4, ry, rx, ry-4);
                    break;
                case 1: // cross
                    g.drawLine(rx-4, ry, rx+4, ry); g.drawLine(rx, ry-4, rx, ry+4);
                    break;
                case 2: // triangle
                    g.drawLine(rx, ry-4, rx+4, ry+3); g.drawLine(rx+4, ry+3, rx-4, ry+3);
                    g.drawLine(rx-4, ry+3, rx, ry-4);
                    break;
                case 3: // circle
                    g.drawArc(rx-3, ry-3, 7, 7, 0, 360);
                    break;
            }
        }

        // ---- TERRITORY BORDER (pulsing ring) ----
        boolean pulse = stPhase == ST_ACTIVE && (stTimer / 300) % 2 == 0;
        g.setColor(pulse ? col : darken(col));
        g.drawArc(px - r, py - r/2, r*2, r, 0, 360);
        g.drawArc(px - r+1, py - r/2+1, r*2-2, r-2, 0, 360);
        // Outer glow ring
        g.setColor(col & 0x333333);
        g.drawArc(px - r-2, py - r/2-1, r*2+4, r+2, 0, 360);

        // ---- DOMAIN NAME ----
        if (stPhase == ST_ACTIVE) {
            String dname = playerCharId < DOMAIN_NAMES.length ? DOMAIN_NAMES[playerCharId] : "STICKTENSION";
            g.setFont(fSmall);
            // Draw at top of territory
            int nameY = py - r/2 - 14;
            if (nameY < 20) nameY = 20;
            g.setColor(0x000000);
            g.drawString(dname, SW/2+1, nameY+1, Graphics.HCENTER|Graphics.TOP);
            g.setColor(col);
            g.drawString(dname, SW/2, nameY, Graphics.HCENTER|Graphics.TOP);
            // Timer bar at bottom of territory
            int timerW = (int)(r * 2 * (5000 - stTimer) / 5000);
            g.setColor(col & 0x444444);
            g.fillRect(px - r, py + r/2, r*2, 3);
            g.setColor(col);
            g.fillRect(px - r, py + r/2, timerW, 3);
        }

        // ---- COLLAPSE SHOCKWAVE ----
        if (stPhase == ST_COLLAPSE) {
            int shockR = r + 8;
            g.setColor(col);
            g.drawArc(px - shockR, py - shockR/2, shockR*2, shockR, 0, 360);
            g.setColor(0xFFFFFF);
            int fr = r + 2;
            g.drawArc(px - fr, py - fr/2, fr*2, fr, 0, 360);
        }
    }


    // ==================================================================
    //  PER-CHARACTER STICKTENSION INVOKE ANIMATIONS
    //  12 unique burst effects, one per character.
    //  CLDC 1.1 safe - no atan2/cos/sin. Fixed-point trig where needed.
    //  prog = 0..1000 (animation progress within 200ms invoke window)
    // ==================================================================
    private void renderSTInvoke(Graphics g, int cid, int px, int py, int col, int prog) {
        switch (cid) {
        case 0: { // KAEL - Lightning Wanderer: 8 jagged electric bolts
            final int[] bx={60,42,0,-42,-60,-42,0,42};
            final int[] by={0,42,60,42,0,-42,-60,-42};
            g.setColor(col);
            for (int i=0;i<8;i++) {
                int ex=px+bx[i]*prog/1000, ey=py+by[i]*prog/1000;
                int mx=px+bx[i]*prog/2000+(i%2==0?7:-7);
                int my=py+by[i]*prog/2000+(i%3==0?5:-5);
                g.drawLine(px,py,mx,my); g.drawLine(mx,my,ex,ey);
            }
            g.setColor(0xFFFFFF);
            int k=prog*8/1000; g.drawLine(px-k,py,px+k,py); g.drawLine(px,py-k,px,py+k);
            break; }
        case 1: { // IGNIS - Ember Champion: fire ring + 6 ember streams
            int fr=prog*50/1000;
            g.setColor(0xFF4400); g.drawArc(px-fr,py-fr/2,fr*2,fr,0,360);
            final int[] dx={fr,-fr,fr,-fr,0,0}, dy={0,0,fr/2,-fr/2,fr/2,-fr/2};
            g.setColor(col);
            for (int i=0;i<6;i++) {
                int ex=px+dx[i]*prog/1000, ey=py+dy[i]*prog/1000;
                g.drawLine(px,py,ex,ey); g.fillArc(ex-2,ey-2,5,5,0,360);
            }
            g.setColor(0xFF8800); g.drawLine(px-3,py-prog*40/1000,px+3,py-prog*40/1000);
            break; }
        case 2: { // FROSTBANE - Ice Knight: 6-point crystal snowflake
            final int[] fx={60,30,-30,-60,-30,30}, fy={0,52,52,0,-52,-52};
            int fl=prog*55/1000;
            for (int i=0;i<6;i++) {
                int ex=px+fx[i]*fl/60, ey=py+fy[i]*fl/60;
                g.setColor(col); g.drawLine(px,py,ex,ey); g.fillArc(ex-2,ey-2,5,5,0,360);
                int mx=px+fx[i]*fl/120, my=py+fy[i]*fl/120;
                int n=(i+1)%6, shx=fx[n]*8/60, shy=fy[n]*8/60;
                g.setColor(0x88DDFF); g.drawLine(mx-shx/2,my-shy/2,mx+shx/2,my+shy/2);
            }
            g.setColor(0x44AACC); int fr2=prog*30/1000; g.drawArc(px-fr2,py-fr2/2,fr2*2,fr2,0,360);
            break; }
        case 3: { // NEXUS - Cyber Soldier: digital scan grid
            g.setColor(0x00FF44);
            for (int i=0;i<5;i++) { int sy=py-prog*30/1000+i*prog*12/1000; g.drawLine(px-prog*50/1000,sy,px+prog*50/1000,sy); }
            for (int i=0;i<4;i++) { int cx=px-prog*30/1000+i*prog*20/1000; g.drawLine(cx,py-prog*30/1000,cx,py+prog*30/1000); }
            g.setColor(col); int b=prog*40/1000;
            g.drawLine(px-b,py-b/2,px-b+8,py-b/2); g.drawLine(px-b,py-b/2,px-b,py-b/2+6);
            g.drawLine(px+b-8,py-b/2,px+b,py-b/2); g.drawLine(px+b,py-b/2,px+b,py-b/2+6);
            break; }
        case 4: { // ZHARA - Spirit Caller: 8 nature vine tendrils
            final int[] vx={0,30,52,60,52,30,0,-30}, vy={-60,-52,-30,0,30,52,60,52};
            for (int i=0;i<8;i++) {
                int e=prog*45/1000, ex=px+vx[i]*e/60, ey=py+vy[i]*e/60;
                g.setColor(0x44AA22); g.drawLine(px,py,ex,ey);
                g.setColor(0x88FF44); g.fillArc(ex-3,ey-3,7,7,0,360);
            }
            g.setColor(0x006600); int zr=prog*35/1000; g.drawArc(px-zr,py-zr/2,zr*2,zr,0,360);
            break; }
        case 5: { // SHADOWBORN - Void Rogue: shadow tendrils + ghost clones
            final int[] sdx={-60,-40,0,40,60}, sdy={0,-50,-60,-50,0};
            for (int i=0;i<5;i++) {
                int ex=px+sdx[i]*prog/1000, ey=py+sdy[i]*prog/1000;
                g.setColor(0x330044); g.drawLine(px,py,ex,ey);
                g.setColor(col); g.fillArc(ex-3,ey-3,7,7,0,360);
            }
            for (int gi=1;gi<=3;gi++) {
                int go=gi*prog*8/1000;
                g.setColor(col&0x222222);
                g.drawLine(px-go,py-20,px-go,py); g.drawLine(px-go-6,py-16,px-go+6,py-16);
                g.drawLine(px-go,py,px-go-5,py+14); g.drawLine(px-go,py,px-go+5,py+14);
            }
            break; }
        case 6: { // TITAN - Fire Colossus: concentric shockwaves + magma pillars
            for (int i=1;i<=4;i++) { int rr=prog*i*15/1000; if(rr<2)continue; g.setColor(0xFF4400); g.drawArc(px-rr,py-rr/2,rr*2,rr,0,360); }
            g.setColor(0xFF8800);
            for (int i=-3;i<=3;i++) { if(i==0)continue; g.drawLine(px,py+10,px+i*20,py+10+prog*60/1000); }
            g.setColor(col);
            for (int i=-2;i<=2;i++) g.drawLine(px+i*14,py,px+i*14,py-prog*(18+i*4)/1000);
            break; }
        case 7: { // FROST QUEEN - Ice Sovereign: ice crown + blizzard ring
            int ch=prog*30/1000;
            final int[] cpx={-20,-10,0,10,20}, cph={10,20,30,20,10};
            g.setColor(col);
            for (int i=0;i<5;i++) { int cy=py-24-cph[i]*ch/30; g.drawLine(px+cpx[i],py-24,px+cpx[i],cy); g.fillArc(px+cpx[i]-2,cy-2,5,5,0,360); }
            g.setColor(0x88EEFF); int bz=prog*45/1000; g.drawArc(px-bz,py-bz/2,bz*2,bz,0,360);
            g.setColor(0xFFFFFF); int sf=prog*25/1000;
            g.drawLine(px-sf,py,px+sf,py); g.drawLine(px,py-sf/2,px,py+sf/2);
            g.drawLine(px-sf*7/10,py-sf*4/10,px+sf*7/10,py+sf*4/10);
            g.drawLine(px-sf*7/10,py+sf*4/10,px+sf*7/10,py-sf*4/10);
            break; }
        case 8: { // BLADESTORM - Wind Dancer: spinning blade arcs
            final int[] bax={40,20,-20,-40,-20,20}, bay={0,35,35,0,-35,-35};
            int ba=(stTimer*18)%360;
            for (int i=0;i<6;i++) {
                int ri=(i+ba/60)%6, brx=px+bax[ri]*prog/1000, bry=py+bay[ri]*prog/1000;
                g.setColor(col); g.drawLine(brx-6,bry-4,brx+6,bry+4); g.drawLine(brx+6,bry-4,brx-6,bry+4);
                g.setColor(0xFFFFAA); g.fillArc(brx-2,bry-2,5,5,0,360);
            }
            g.setColor(0xCCCC88);
            for (int i=1;i<=3;i++) { int sr=prog*i*14/1000; if(sr<2)continue; g.drawArc(px-sr,py-sr/2,sr*2,sr,0,360); }
            break; }
        case 9: { // VOLTRA - Lightning Striker: chain lightning + static burst
            g.setColor(0x00FFFF); int vl=prog*60/1000;
            for (int i=0;i<4;i++) { g.drawLine(px-i*vl/4,py+(i%2==0?-8:8),px-(i+1)*vl/4,py+(i%2==0?8:-8)); }
            for (int i=0;i<4;i++) { g.drawLine(px+i*vl/4,py+(i%2==0?8:-8),px+(i+1)*vl/4,py+(i%2==0?-8:8)); }
            g.setColor(col);
            final int[] vx={50,35,-35,-50,-35,35,0,0}, vy={0,40,40,0,-40,-40,50,-50};
            for (int i=0;i<8;i++) g.drawLine(px,py,px+vx[i]*prog/1000,py+vy[i]*prog/1000);
            g.setColor(0xFFFFFF); int ve=prog*40/1000; g.drawArc(px-ve,py-ve/2,ve*2,ve,0,360);
            break; }
        case 10: { // GOLEM - Mountain Born: stone slab eruption + rubble burst
            g.setColor(0x886633); int gh=prog*50/1000;
            g.fillRect(px-8,py-gh,16,gh);
            for (int i=1;i<=3;i++) { int go=i*18, gs=gh*(4-i)/4; g.fillRect(px-8-go,py-gs,8,gs); g.fillRect(px+go,py-gs,8,gs); }
            g.setColor(col);
            final int[] grx={-40,-28,28,40,-18,18,-30,30}, gry={-20,-40,-40,-20,-50,-50,-10,-10};
            for (int i=0;i<8;i++) g.fillArc(px+grx[i]*prog/1000-3,py+gry[i]*prog/1000-3,7,7,0,360);
            g.setColor(0xAA8855); g.drawLine(px-prog*50/1000,py+8,px+prog*50/1000,py+8);
            break; }
        case 11: { // WRAITH - Phantom: ghost afterimages + phase portal
            final int[] wx={-30,30,-20,20,0,-15,15,0}, wy={-10,-10,-30,-30,-40,-10,-10,20};
            g.setColor(col&0x333333);
            for (int i=0;i<8;i++) {
                int wox=px+wx[i]*prog/1000, woy=py+wy[i]*prog/1000;
                g.drawArc(wox-4,woy-20,9,9,0,360); g.drawLine(wox,woy-11,wox,woy+2);
                g.drawLine(wox-5,woy-8,wox+5,woy-8); g.drawLine(wox,woy+2,wox-4,woy+14); g.drawLine(wox,woy+2,wox+4,woy+14);
            }
            g.setColor(col); int wp=prog*40/1000; g.drawArc(px-wp,py-wp/2,wp*2,wp,0,360);
            g.setColor(0x6644AA); int wp2=prog*22/1000; if(wp2>2) g.fillArc(px-wp2,py-wp2/2,wp2*2,wp2,0,360);
            if(prog>800){g.setColor(0xFFFFFF);g.drawArc(px-12,py-18,24,36,0,360);}
            break; }
        default: { // fallback radial burst
            final int[] dx={60,42,0,-42,-60,-42,0,42}, dy={0,42,60,42,0,-42,-60,-42};
            g.setColor(col);
            for (int i=0;i<8;i++) g.drawLine(px,py,px+dx[i]*prog/1000,py+dy[i]*prog/1000);
            break; }
        }
    }

    private void checkBossUnlock() {
        String unlockMsg = null;
        if (enemyCharId == GameData.CH_IGNIS) {
            SaveData.unlockArena(2); SaveData.unlockChar(GameData.CH_IGNIS);
            unlockMsg = "IGNIS UNLOCKED!"; SoundManager.playSFX(SoundManager.SFX_UNLOCK);
            SaveData.checkAchievement(0); // first win
        }
        if (enemyCharId == GameData.CH_TITAN) {
            SaveData.unlockChar(GameData.CH_TITAN);
            unlockMsg = "TITAN UNLOCKED!"; SoundManager.playSFX(SoundManager.SFX_UNLOCK);
        }
        if (enemyCharId == GameData.CH_FROST) {
            SaveData.unlockArena(3); SaveData.unlockChar(GameData.CH_FROST);
            unlockMsg = "FROSTBANE UNLOCKED!"; SoundManager.playSFX(SoundManager.SFX_UNLOCK);
        }
        if (enemyCharId == GameData.CH_QUEEN) {
            SaveData.unlockChar(GameData.CH_QUEEN);
            unlockMsg = "FROST QUEEN UNLOCKED!"; SoundManager.playSFX(SoundManager.SFX_UNLOCK);
        }
        if (enemyCharId == GameData.CH_NEXUS) {
            SaveData.unlockArena(4); SaveData.unlockChar(GameData.CH_NEXUS);
            unlockMsg = "NEXUS UNLOCKED!"; SoundManager.playSFX(SoundManager.SFX_UNLOCK);
        }
        if (enemyCharId == GameData.CH_SHADOW) {
            SaveData.unlockChar(GameData.CH_SHADOW);
            unlockMsg = "SHADOWBORN UNLOCKED!"; SoundManager.playSFX(SoundManager.SFX_UNLOCK);
        }
        // Total wins achievements
        if (SaveData.totalWins >= 1)  SaveData.checkAchievement(0);
        if (SaveData.totalKills >= 10) SaveData.checkAchievement(1);
        if (SaveData.totalWins >= 100) SaveData.checkAchievement(5);
        if (unlockMsg != null) { showBigMsg(unlockMsg, C_GOLD); unlockNotifTimer = 3000; unlockNotifMsg = unlockMsg; }
        SaveData.save();
    }
    private String unlockNotifMsg = "";
    private int    unlockNotifTimer = 0;

    // ---- STICKTENSION: Territory Expansion --------------------------------
    private static final int ST_IDLE     = 0;
    private static final int ST_INVOKE   = 1;  // 200ms - crack/shatter entry
    private static final int ST_EXPAND   = 2;  // 800ms - border expanding
    private static final int ST_ACTIVE   = 3;  // 5000ms - domain active
    private static final int ST_COLLAPSE = 4;  // 400ms - implosion exit

    private int   stPhase       = ST_IDLE;
    private int   stTimer       = 0;   // ms in current phase
    private float stRadius      = 0f;  // territory circle radius
    private int   stRegenTimer  = 0;   // HP regen tick
    private int   stRuneAngle   = 0;   // orbiting rune rotation angle (degrees*10)
    private int   stCrackSeed   = 0;   // random seed for crack lines (stable per cast)
    // Domain names per character
    private static final String[] DOMAIN_NAMES = {
        "THUNDERREALM: ETERNAL STORM",      // 0  Kael      - Lightning
        "INFERNO CRUCIBLE: BLAZING HELL",   // 1  Ignis     - Fire
        "FROZEN DOMINION: ABSOLUTE ZERO",   // 2  Frostbane - Ice
        "SHADOW VOID GATE: ABYSS SEAL",     // 3  Nexus     - Void
        "STONE BASTION: TITAN'S GRAVE",     // 4  Zhara     - Earth
        "STORM TEMPEST: HURRICANE CAGE",    // 5  Shadowborn- Wind
        "AQUATIC MAELSTROM: OCEAN PRISON",  // 6  Titan     - Water
        "HOLY RADIANCE: CELESTIAL JUDGMENT",// 7  Frost Queen- Light
        "TOXIC SWAMP: VENOM REALM",         // 8  Bladestorm- Poison
        "IRON INFINITY: BLADE FORGE",       // 9  Voltra    - Metal
        "VERDANT PRISON: LIVING FOREST",    // 10 Golem     - Nature
        "CHRONO PARADOX: ETERNAL LOOP"      // 11 Wraith    - Time
    };
    // Domain sigil colors
    private static final int[] DOMAIN_COLORS = {
        0x00DDFF, 0xFF4400, 0x88EEFF, 0x220033,
        0x886644, 0x88FFAA, 0x0088FF, 0xFFFF44,
        0x44FF00, 0xAAAAAA, 0x00AA44, 0xCC44FF
    };

    // ==================================================================
    //  Combat helpers
    // ==================================================================
    private void checkMeleeHit(Stickman atk, Stickman def, int bloodColor) {
        if (!atk.alive || def==null || !def.alive) return;
        int[] ar = atk.getAttackRect();
        if (ar == null) return;
        boolean firstFrame =
            (atk.state == Stickman.ST_ULTRA)  ? atk.ultraTimer == Stickman.ULTRA_DUR :
            (atk.punchTimer == Stickman.PUNCH_DUR || atk.kickTimer == Stickman.KICK_DUR);
        if (!firstFrame) return;
        if (def.overlapsRect(ar[0],ar[1],ar[2],ar[3])) {
            // Dodge roll invincibility frames
            if (def.isPlayer && dodgeInvincible) {
                spawnParticles((int)def.x, (int)(def.y-20), C_CYAN, 5);
                showBigMsg("DODGE!", C_CYAN); return;
            }
            int dmg = atk.getAttackDamage();
            dmg = dmg * atk.dmgMult / 100;
            dmg = dmg * 100 / def.defMult;
            // Charge attack bonus
            if (atk.isPlayer && chargeReady) { dmg *= 2; chargeReady=false; chargeTimer=0;
                showBigMsg("CHARGED HIT! " + dmg + "!", C_GOLD); chargeFlash=300; }
            // Sticktension: player deals 2x damage while domain is active
            if (stPhase == ST_ACTIVE && atk.isPlayer && !def.isPlayer) dmg *= 2;
            // CRITICAL HIT: 15% chance, 2x damage
            lastHitWasCrit = (rand.nextInt(100) < 15);
            if (lastHitWasCrit) {
                dmg *= 2;
                showBigMsg("CRITICAL! " + dmg + "!", C_YELLOW);
                triggerShake(8, 220);
                SoundManager.playSFX(SoundManager.SFX_CRIT);
            }
            float kbDir = atk.facingRight ? 200f : -200f;
            def.takeDamage(dmg, kbDir, -260f);
            // Rich blood animation
            spawnBlood((int)def.x, (int)(def.y-30), bloodColor, dmg);
            if (dmg >= 15) spawnSmoke((int)def.x, (int)(def.y-20), 3);

            // ?? v7: Floating damage number ?????????????????????????
            spawnDamageNumber((int)def.x, (int)(def.y - 38), dmg, lastHitWasCrit);

            // ?? v7: Hit flash ?????????????????????????????????????
            triggerHitFlash(def.isPlayer);

            // ?? v7: Stats tracking ????????????????????????????????
            if (atk.isPlayer) { statDmgDealt    += dmg; statHitsLanded++; }
            if (def.isPlayer) { statDmgReceived += dmg; }

            // ?? v7: First blood bonus ?????????????????????????????
            if (!firstBloodDone && atk.isPlayer) {
                firstBloodDone = true;
                SaveData.addShards(FIRST_BLOOD_SHARDS);
                showBigMsg("FIRST BLOOD! +" + FIRST_BLOOD_SHARDS + "S", 0xFF2222);
            }

            if (def.state == Stickman.ST_BLOCK) {
                SoundManager.playSFX(SoundManager.SFX_BLOCK);
                if (atk.isPlayer) statPerfectBlocks++;  // counted on any block for now
            }
            if (SaveData.isShopItemOwned(12) && atk.isPlayer &&
                (atk.state==Stickman.ST_PUNCH || atk.state==Stickman.ST_KICK)) {
                def.burnTimer = 3000;
            }
            atk.chargeSpecialOnHit(dmg);
            simSound(SSND_HIT);
            if (atk.state == Stickman.ST_ULTRA) {
                triggerShake(14,400); showBigMsg("ULTRA!", C_GOLD); simSound(SSND_ULTRA);
                slomoDur=600; slomoScale=0.25f; spawnSmoke((int)def.x,(int)(def.y-20),6);
                // ?? v7: Kill cam trigger ??????????????????????????
                if (!def.alive || def.health <= 0) {
                    killCamTimer = KILL_CAM_DUR;
                    killCamX     = def.x; killCamY = def.y - 20;
                    killCamZoom  = 1.35f;
                }
            } else if (dmg >= Stickman.UPPERCUT_DMG) {
                triggerShake(6,200); showBigMsg("COMBO!", C_ORANGE);
            }
        }
    }

    // ==================================================================
    //  Shooting
    // ==================================================================
    private void consumeShootRequests() {
        if (player.wantsToShoot) { player.wantsToShoot=false; fireBullets(player,0); }
        if (enemy!=null && enemy.wantsToShoot) { enemy.wantsToShoot=false; fireBullets(enemy,1); }
    }

    private void fireBullets(Stickman s, int owner) {
        if (owner == 0) simSound(SSND_SHOOT); // player fired
        int   wt  = s.weaponType;
        float bx  = s.x + (s.facingRight ? 18f : -18f);
        float by  = s.y - Stickman.H * 2 / 3f;
        float spd = Weapon.BULLET_SPEED[wt];
        float bvx = s.facingRight ? spd : -spd;
        int   dmg = Weapon.DAMAGE[wt] * s.dmgMult / 100;

        // Blessing of Ancients: +50% dmg
        if (owner==0 && SaveData.isPowerupActive(0)) dmg = dmg * 3 / 2;

        switch (wt) {
            case Weapon.TYPE_SHOTGUN:
                for (int p=-2;p<=2;p++) {
                    float pvx=bvx*0.95f+p*14f*3f, pvy=p*2f;
                    spawnBullet(bx,by,pvx,pvy,owner,dmg,wt,800);
                }
                triggerShake(5,140);
                break;
            case Weapon.TYPE_GRENADE:
                spawnBullet(bx,by,bvx*0.55f,-240f,owner,dmg,wt,3000).bounces=2;
                break;
            case Weapon.TYPE_ROCKET:
                spawnBullet(bx,by,bvx*0.7f,0f,owner,dmg,wt,4000);
                triggerShake(8,200);
                break;
            case Weapon.TYPE_LASER:
                spawnBullet(bx,by,bvx,0f,owner,dmg,wt,200);
                break;
            case Weapon.TYPE_FLAMETHROWER:
                for (int f=0;f<3;f++) {
                    float fvx=bvx*(0.7f+rand.nextFloat()*0.4f);
                    float fvy=(rand.nextFloat()-0.5f)*80f;
                    spawnBullet(bx,by,fvx,fvy,owner,dmg,wt,350);
                }
                break;
            case Weapon.TYPE_BOOMERANG:
                Weapon.Bullet bm = spawnBullet(bx,by,bvx*0.7f,-30f,owner,dmg,wt,3000);
                bm.originX=s.x; bm.returning=false;
                break;
            case Weapon.TYPE_CROSSBOW:
                spawnBullet(bx,by,bvx,0f,owner,dmg,wt,1200);
                break;
            case Weapon.TYPE_SNIPER:
                spawnBullet(bx,by,bvx,0f,owner,dmg,wt,900);
                triggerShake(7,180);
                break;
            case Weapon.TYPE_MINE:
                // Place mine at shooter feet as ground pickup
                spawnGWAt(Weapon.TYPE_MINE, s.x, s.y);
                break;
            case Weapon.TYPE_THUNDER:
                spawnBullet(bx,by,bvx*0.9f,-20f,owner,Weapon.DAMAGE[Weapon.TYPE_THUNDER],wt,1200);
                break;
            case Weapon.TYPE_SHURIKEN:
                // Burst of 4 rapid stars
                for (int sh=0;sh<4;sh++) {
                    int svy=-30+(sh*20);
                    Weapon.Bullet sb=spawnBullet(bx,by+(sh*4-8),bvx+(sh*15),svy,owner,Weapon.DAMAGE[Weapon.TYPE_SHURIKEN],wt,700);
                }
                break;
            case Weapon.TYPE_RAILGUN:
                spawnBullet(bx,by,bvx*1.6f,0f,owner,Weapon.DAMAGE[Weapon.TYPE_RAILGUN],wt,1500);
                triggerShake(5,120);
                break;
            case Weapon.TYPE_SCYTHE:
                // Wide arc: 5 hitboxes fanning
                for (int sc=-2;sc<=2;sc++)
                    spawnBullet(bx,by+(sc*12),(bvx>0?100:-100),sc*50,owner,Weapon.DAMAGE[Weapon.TYPE_SCYTHE],wt,250);
                break;
            default:
                spawnBullet(bx,by,bvx,0f,owner,dmg,wt,1200);
                break;
        }
    }

    private Weapon.Bullet spawnBullet(float x,float y,float vx,float vy,
                                       int owner,int dmg,int wt,int life) {
        for (int i=0;i<MAX_BULLETS;i++) {
            Weapon.Bullet b=bullets[i];
            if (!b.active) {
                b.x=x;b.y=y;b.vx=vx;b.vy=vy;
                b.owner=owner;b.damage=dmg;b.weaponType=wt;
                b.life=life;b.active=true;b.returning=false;b.bounces=0;
                return b;
            }
        }
        return bullets[0];
    }

    private void updateBullets(int delta) {
        float dt = delta/1000f;
        for (int i=0;i<MAX_BULLETS;i++) {
            Weapon.Bullet b=bullets[i];
            if (!b.active) continue;
            b.life -= delta;
            if (b.life <= 0) { b.active=false; continue; }

            switch (b.weaponType) {
                case Weapon.TYPE_GRENADE:
                    b.vy += 400f*dt;
                    break;
                case Weapon.TYPE_BOOMERANG:
                    b.vy += 400f*dt;
                    if (!b.returning && b.life<1500) b.returning=true;
                    if (b.returning) {
                        float bdx=b.originX-b.x;
                        b.vx += (bdx>0?1:-1)*300f*dt;
                    }
                    break;
                case Weapon.TYPE_FLAMETHROWER:
                    b.vy -= 20f*dt; b.vx *= 0.93f;
                    break;
                case Weapon.TYPE_ROCKET:
                    Stickman rt=(b.owner==0)?enemy:player;
                    if (rt!=null && rt.alive) {
                        float rdx=rt.x-b.x, rdy=(rt.y-Stickman.H/2f)-b.y;
                        float rd=(float)Math.sqrt(rdx*rdx+rdy*rdy);
                        if (rd>10f) { b.vx+=(rdx/rd)*180f*dt; b.vy+=(rdy/rd)*180f*dt; }
                        float rs=(float)Math.sqrt(b.vx*b.vx+b.vy*b.vy);
                        if (rs>350f) { b.vx=b.vx/rs*350f; b.vy=b.vy/rs*350f; }
                    }
                    if ((b.life%80)<40) spawnParticles((int)b.x,(int)b.y,0x888888,1);
                    break;
                default:
                    if (b.weaponType!=Weapon.TYPE_LASER) b.vy += 120f*dt;
                    break;
            }

            b.x += b.vx*dt; b.y += b.vy*dt;

            // Use WORLD bounds (Arena.W), not screen bounds (SW)
            if (b.y<-80||b.y>Arena.H+40) { b.active=false; continue; }
            if (b.x<0) {
                if(b.weaponType==Weapon.TYPE_GRENADE||b.weaponType==Weapon.TYPE_BOOMERANG){b.vx=Math.abs(b.vx)*0.75f;b.x=2;}
                else { b.active=false; continue; }
            }
            if (b.x>Arena.W) {
                if(b.weaponType==Weapon.TYPE_GRENADE||b.weaponType==Weapon.TYPE_BOOMERANG){b.vx=-Math.abs(b.vx)*0.75f;b.x=Arena.W-2;}
                else { b.active=false; continue; }
            }

            if (Arena.insidePlatform(b.x, b.y)) {
                if (b.weaponType==Weapon.TYPE_GRENADE && b.bounces>0) {
                    b.vy=-Math.abs(b.vy)*0.55f; b.vx*=0.75f; b.bounces--;
                } else if (b.weaponType==Weapon.TYPE_ROCKET) {
                    explodeBullet(b);
                } else {
                    spawnParticles((int)b.x,(int)b.y,0xFFCC44,3);
                    Arena.hitBreakable(b.x, b.y);
                    b.active=false;
                }
                continue;
            }

            if (b.weaponType==Weapon.TYPE_GRENADE && b.life<200) { explodeBullet(b); continue; }
            if (b.weaponType==Weapon.TYPE_RAILGUN) b.piercing=true;
            if (b.weaponType==Weapon.TYPE_THUNDER && b.chainLeft==0) b.chainLeft=3;

            Stickman target = (b.owner==0) ? enemy : player;
            if (target!=null && target.alive) {
                int[] tb=target.getBounds();
                if (b.x>tb[0]&&b.x<tb[0]+tb[2]&&b.y>tb[1]&&b.y<tb[1]+tb[3]) {
                    int hd=b.damage;
                    if (b.weaponType==Weapon.TYPE_SNIPER) {
                        boolean hs=(b.y < target.y-Stickman.H+16);
                        if (hs) { hd=200; showBigMsg("HEADSHOT!", C_GOLD); triggerShake(10,300); }
                    }
                    // Dragonhide armor reduces bullet damage
                    if (b.owner != 0 && player.hasDragonhide) hd = hd * 75 / 100;
                    if (b.owner == 0 && enemy != null && enemy.hasDragonhide) hd = hd * 75 / 100;
                    // Headshot check (bullet above neck line)
                    boolean headshot = (b.y < target.y - Stickman.H + 10);
                    if (headshot && b.weaponType != Weapon.TYPE_SNIPER) {
                        hd = hd * 3 / 2; // 50% bonus for headshots
                        showBigMsg("HEADSHOT! +" + hd, C_GOLD);
                        SoundManager.playSFX(SoundManager.SFX_HEADSHOT);
                    }
                    target.takeDamage(hd, b.vx*0.35f, -150f);
                    Stickman shooter2 = (b.owner==0) ? player : enemy;
                    if (shooter2 != null) shooter2.chargeSpecialOnHit(hd);
                    int bldCol = b.owner==0 ? GameData.CHAR_COLOR[enemyCharId]
                                            : GameData.CHAR_COLOR[playerCharId];
                    spawnBlood((int)b.x, (int)b.y, bldCol, hd);
                    if (b.weaponType == Weapon.TYPE_SHOTGUN) spawnSmoke((int)b.x,(int)b.y,2);
                    if (b.weaponType==Weapon.TYPE_ROCKET) explodeBullet(b);
                    else if (b.weaponType==Weapon.TYPE_THUNDER && b.chainLeft>0) {
                        // Chain hop: seek nearest un-hit fighter
                        b.chainLeft--;
                        spawnParticles((int)b.x,(int)b.y,0x88DDFF,5);
                        Stickman nextTarget=null; float bestDist=120f;
                        Stickman[] cands={player,enemy,
                            meleeAI!=null&&meleeAI.length>0?meleeAI[0]:null,
                            meleeAI!=null&&meleeAI.length>1?meleeAI[1]:null,
                            meleeAI!=null&&meleeAI.length>2?meleeAI[2]:null};
                        for (int ci=0;ci<cands.length;ci++) {
                            if (cands[ci]==null||!cands[ci].alive||cands[ci]==target) continue;
                            // Don't chain back to owner
                            if (b.owner==0&&cands[ci]==player) continue;
                            if (b.owner==1&&cands[ci]==enemy) continue;
                            float cdx=cands[ci].x-b.x, cdy=cands[ci].y-b.y;
                            float cd=(float)Math.sqrt(cdx*cdx+cdy*cdy);
                            if (cd<bestDist) { bestDist=cd; nextTarget=cands[ci]; }
                        }
                        if (nextTarget!=null) {
                            float ddx=nextTarget.x-b.x, ddy=nextTarget.y-b.y;
                            float len=(float)Math.sqrt(ddx*ddx+ddy*ddy)+1f;
                            b.vx=ddx/len*Weapon.BULLET_SPEED[Weapon.TYPE_THUNDER];
                            b.vy=ddy/len*Weapon.BULLET_SPEED[Weapon.TYPE_THUNDER];
                            b.life=600; // reset life for hop
                        } else { b.active=false; }
                    } else if (!b.piercing && b.weaponType!=Weapon.TYPE_CROSSBOW) b.active=false;
                    else if (b.piercing) { /* railgun/crossbow pierce: stay active */ }
                    else b.active=false;
                    if (b.owner == 0) {
                        // Charge special when player lands a bullet hit
                        player.addSpecialCharge(14);
                        if (!target.alive) {
                            player.kills++;
                            player.killsThisRound++;
                            SaveData.totalKills++;
                            // Kill streak
                            killStreak++;
                            killStreakTimer = 4000;
                            if (killStreak == 3) { showBigMsg("TRIPLE KILL!", C_CYAN); SoundManager.playSFX(SoundManager.SFX_STREAK); }
                            else if (killStreak == 5) { showBigMsg("KILLING SPREE!", C_PURPLE); SoundManager.playSFX(SoundManager.SFX_STREAK); }
                            else if (killStreak >= 7) { showBigMsg("GODLIKE! x" + killStreak, C_GOLD); SoundManager.playSFX(SoundManager.SFX_STREAK); }
                        }
                    } else if (b.owner == 1) {
                        // AI also charges special on hit
                        if (enemy != null) enemy.addSpecialCharge(14);
                    }
                }
            }
        }
    }

    private void explodeBullet(Weapon.Bullet b) {
        triggerShake(12,350);
        spawnParticles((int)b.x,(int)b.y,C_ORANGE,14);
        spawnParticles((int)b.x,(int)b.y,C_RED,6);
        spawnParticles((int)b.x,(int)b.y,C_YELLOW,4);
        spawnSmoke((int)b.x,(int)b.y,8);
        spawnEmbers((int)b.x,(int)b.y,10);
        SoundManager.playSFX(SoundManager.SFX_EXPLODE);
        Stickman[] fs={player,enemy};
        for (int f=0;f<2;f++) {
            if (fs[f]==null||!fs[f].alive) continue;
            float dx=fs[f].x-b.x, dy=(fs[f].y-Stickman.H/2f)-b.y;
            float dist=(float)Math.sqrt(dx*dx+dy*dy);
            if (dist<55f) {
                int ed=(int)(b.damage*(1f-dist/55f));
                fs[f].takeDamage(ed,(dx>0?350f:-350f),-300f);
            }
        }
        b.active=false;
    }

    // ==================================================================
    //  Ground weapons
    // ==================================================================
    private void spawnGroundWeapon() {
        int[][] pl=Arena.PLATFORMS;
        int pi=rand.nextInt(pl.length);
        int[] p=pl[pi];
        if (p[2]<20) { spawnGroundWeapon(); return; }
        float gx=p[0]+8+rand.nextFloat()*(p[2]-16);
        float gy=p[1]-8;
        spawnGWAt(randWeapon(1), gx, gy);
    }

    private void spawnGWAt(int type, float gx, float gy) {
        for (int i=0;i<MAX_GW;i++) {
            if (!gwActive[i]) { gwX[i]=gx;gwY[i]=gy;gwType[i]=type;gwActive[i]=true; return; }
        }
    }

    private void updateGW(int delta) {
        // Mine proximity: check all alive fighters against mine GW slots
        for (int i=0;i<MAX_GW;i++) {
            if (!gwActive[i] || gwType[i]!=Weapon.TYPE_MINE) continue;
            Stickman[] fighters={player,enemy,
                meleeAI!=null&&meleeAI.length>0?meleeAI[0]:null,
                meleeAI!=null&&meleeAI.length>1?meleeAI[1]:null,
                meleeAI!=null&&meleeAI.length>2?meleeAI[2]:null};
            for (int f=0;f<fighters.length;f++) {
                Stickman fs=fighters[f]; if(fs==null||!fs.alive) continue;
                if (Math.abs(fs.x-gwX[i])<16 && Math.abs(fs.y-gwY[i])<16) {
                    // Detonate: area explosion
                    int mx=(int)gwX[i], my=(int)gwY[i];
                    triggerShake(10,300);
                    spawnParticles(mx,my,C_ORANGE,12); spawnParticles(mx,my,C_RED,6);
                    spawnSmoke(mx,my,6); spawnEmbers(mx,my,8);
                    SoundManager.playSFX(SoundManager.SFX_EXPLODE);
                    gwActive[i]=false;
                    // Damage nearby fighters
                    Stickman[] targets={player,enemy,
                        meleeAI!=null&&meleeAI.length>0?meleeAI[0]:null,
                        meleeAI!=null&&meleeAI.length>1?meleeAI[1]:null,
                        meleeAI!=null&&meleeAI.length>2?meleeAI[2]:null};
                    for (int t2=0;t2<targets.length;t2++) {
                        Stickman tgt=targets[t2]; if(tgt==null||!tgt.alive) continue;
                        float dx=tgt.x-mx, dy=(tgt.y-20)-my;
                        float dist=(float)Math.sqrt(dx*dx+dy*dy);
                        if (dist<60f) {
                            int ed=(int)(Weapon.DAMAGE[Weapon.TYPE_MINE]*(1f-dist/60f));
                            tgt.takeDamage(ed,(dx>0?300f:-300f),-260f);
                        }
                    }
                    break;
                }
            }
        }
        weaponSpawnTimer -= delta;
        if (weaponSpawnTimer<=0) {
            weaponSpawnTimer=5000+rand.nextInt(6000);
            spawnGroundWeapon();
            // Every 4th spawn: drop a rare weapon instead
            rareWeaponCounter++;
            if (rareWeaponCounter>=4) {
                rareWeaponCounter=0;
                // Rare pool: rocket(8), laser(9), flame(10), thunder(14), railgun(16)
                int[] rarePool={Weapon.TYPE_ROCKET,Weapon.TYPE_LASER,Weapon.TYPE_FLAMETHROWER,
                                Weapon.TYPE_THUNDER,Weapon.TYPE_RAILGUN};
                int rare=rarePool[rand.nextInt(rarePool.length)];
                float gx=Arena.W*0.3f+rand.nextFloat()*Arena.W*0.4f;
                spawnGWAt(rare, gx, 30);
                addKillFeed("RARE DROP: "+Weapon.LABEL[rare]+"!");
            }
        }
        for (int i=0;i<MAX_GW;i++) {
            if (!gwActive[i]) continue;
            checkPickup(player, i);
            if (enemy!=null) checkPickup(enemy, i);
        }
    }

    private void checkPickup(Stickman s, int idx) {
        if (!s.alive||!gwActive[idx]) return;
        if (Math.abs(s.x-gwX[idx])<18 && Math.abs(s.y-gwY[idx])<22) {
            s.weaponType=gwType[idx];
            s.weaponAmmo=Weapon.AMMO[gwType[idx]];
            gwActive[idx]=false;
            showBigMsg("+"+Weapon.LABEL[gwType[idx]], C_YELLOW);
            if (s.isPlayer) SoundManager.playSFX(SoundManager.SFX_PICKUP);
        }
    }

    private void tryPickup(Stickman s) {
        for (int i=0;i<MAX_GW;i++) {
            if (!gwActive[i]) continue;
            if (Math.abs(s.x-gwX[i])<30 && Math.abs(s.y-gwY[i])<32) {
                s.weaponType=gwType[i]; s.weaponAmmo=Weapon.AMMO[gwType[i]];
                gwActive[i]=false; return;
            }
        }
        if (s.weaponType!=Weapon.TYPE_NONE) {
            spawnGWAt(s.weaponType, s.x+(s.facingRight?20f:-20f), s.y-14f);
            s.weaponType=Weapon.TYPE_NONE; s.weaponAmmo=0;
        }
    }

    // ==================================================================
    //  Particles
    // ==================================================================
    // Generic sparks
    private void spawnParticles(int x, int y, int color, int count) {
        spawnTyped(x, y, color, count, 0);
    }

    // Blood splatter - character-color drops that arc out and drip down
    private void spawnBlood(int x, int y, int charColor, int dmg) {
        // Intensity scales with damage
        int drops = Math.min(16, 4 + dmg / 8);
        int darkBlood = darken(charColor) | 0x880000; // darker reddish tint
        // Main arc drops
        for (int n = 0; n < drops; n++) {
            int i = freePart(); if (i < 0) break;
            pX[i]=x; pY[i]=y;
            pVX[i]=(rand.nextFloat()-0.5f)*340f;
            pVY[i]=rand.nextFloat()*-380f-60f;
            pLife[i]=400+rand.nextInt(500);
            pCol[i]=(n%3==0) ? charColor : darkBlood;
            pSize[i]=1+rand.nextInt(3);
            pType[i]=1; // blood
            pGrav[i]=1.4f;
        }
        // Small fast specks
        for (int n = 0; n < drops/2; n++) {
            int i = freePart(); if (i < 0) break;
            pX[i]=x+(rand.nextInt(10)-5); pY[i]=y;
            pVX[i]=(rand.nextFloat()-0.5f)*480f;
            pVY[i]=rand.nextFloat()*-200f;
            pLife[i]=200+rand.nextInt(250);
            pCol[i]=darkBlood;
            pSize[i]=1;
            pType[i]=1;
            pGrav[i]=2.0f;
        }
        // Large splat on heavy hit
        if (dmg >= 20) {
            poolX[poolHead]=x; poolY[poolHead]=y;
            poolR[poolHead]=3+rand.nextInt(5);
            poolCol[poolHead]=darkBlood;
            poolHead=(poolHead+1)%MAX_POOLS;
        }
    }

    // Smoke puff
    private void spawnSmoke(int x, int y, int count) {
        for (int n = 0; n < count; n++) {
            int i = freePart(); if (i < 0) break;
            pX[i]=x; pY[i]=y;
            pVX[i]=(rand.nextFloat()-0.5f)*60f;
            pVY[i]=-40f-rand.nextFloat()*80f;
            pLife[i]=500+rand.nextInt(600);
            pCol[i]=0x556677+(rand.nextInt(3))*0x111111;
            pSize[i]=2+rand.nextInt(3);
            pType[i]=2; // smoke - rises slowly
            pGrav[i]=-0.2f; // floats up
        }
    }

    // Ember sparks for fire/lava
    private void spawnEmbers(int x, int y, int count) {
        for (int n = 0; n < count; n++) {
            int i = freePart(); if (i < 0) break;
            pX[i]=x; pY[i]=y;
            pVX[i]=(rand.nextFloat()-0.5f)*200f;
            pVY[i]=-80f-rand.nextFloat()*200f;
            pLife[i]=300+rand.nextInt(400);
            pCol[i]=(rand.nextInt(2)==0) ? 0xFF5500 : 0xFF9900;
            pSize[i]=1+rand.nextInt(2);
            pType[i]=3; // ember
            pGrav[i]=0.8f;
        }
    }

    private void spawnTyped(int x, int y, int color, int count, int type) {
        for (int n=0;n<count;n++) {
            int i = freePart(); if (i < 0) break;
            pX[i]=x; pY[i]=y;
            pVX[i]=(rand.nextFloat()-0.5f)*280f;
            pVY[i]=rand.nextFloat()*-300f-40f;
            pLife[i]=350+rand.nextInt(400);
            pCol[i]=color; pSize[i]=1+rand.nextInt(2);
            pType[i]=type; pGrav[i]=1f;
        }
    }

    private int freePart() {
        for (int i=0;i<MAX_PART;i++) if (pLife[i]<=0) return i;
        return -1; // all slots full
    }

    private void updateParticles(int delta) {
        float dt = delta * slomoScale / 1000f;
        for (int i=0;i<MAX_PART;i++) {
            if (pLife[i]<=0) continue;
            pVY[i] += 480f * pGrav[i] * dt;
            pX[i]  += pVX[i]*dt;
            pY[i]  += pVY[i]*dt;
            switch (pType[i]) {
                case 1: // blood - bounces slightly off ground
                    pVX[i] *= 0.96f;
                    if (pY[i] > Arena.H - 40 && pVY[i] > 0) {
                        pVY[i] *= -0.25f; pVX[i] *= 0.6f;
                        pY[i] = Arena.H - 40;
                    }
                    break;
                case 2: // smoke - widens slowly
                    pVX[i] *= 0.92f; pVY[i] *= 0.88f;
                    if (pLife[i] < 300) pSize[i] = Math.min(5, pSize[i]+1);
                    break;
                case 3: // ember - flickers
                    pVX[i] *= 0.94f;
                    break;
                default:
                    pVX[i] *= 0.97f;
                    break;
            }
            pLife[i] -= delta;
        }
        // Slo-mo decay
        if (slomoDur > 0) {
            slomoDur -= delta;
            slomoScale = (slomoDur > 0) ? 0.25f : 1f;
        }
        // Kill streak timer
        if (killStreakTimer > 0) killStreakTimer -= delta;
        // Heartbeat timer
        if (heartbeatTimer > 0) heartbeatTimer -= delta;
    }

    // ==================================================================
    //  Background / menu animation
    // ==================================================================
    private void updateBgAnim(int delta) {
        menuAnimTimer += delta;
        // Arena ambient particles
        if (gameState == GS_GAME || gameState == GS_SURVIVAL || gameState == GS_MELEE) {
            menuAnimTimer += 0; // already incremented above
            int fx = (arenaSel < GameData.ARENA_FX.length) ? GameData.ARENA_FX[arenaSel] : 0;
            if (fx == GameData.FX_LAVA_RISE && rand.nextInt(8) == 0) {
                spawnEmbers(rand.nextInt(240), 240+rand.nextInt(40), 2);
            }
            if (arenaSel == 2 || arenaSel == 7) { // Infernal Crucible / Lava Cave
                if (rand.nextInt(6) == 0) spawnEmbers(rand.nextInt(240), 200+rand.nextInt(60), 1);
            }
        }
        if (menuAnimTimer>=150) { menuAnimTimer=0; menuAnimFrame=(menuAnimFrame+1)%8; }
        menuFlashTimer += delta;
        if (menuFlashTimer>500) { menuFlashTimer=0; menuFlashOn=!menuFlashOn; }

        if (gameState==GS_MENU) {
            // Smooth scroll interpolation
            float diff = menuScrollTarget - menuScrollOffset;
            if (Math.abs(diff) > 0.01f) menuScrollOffset += diff * 0.22f;
            else menuScrollOffset = menuScrollTarget;
            menuFightTimer += delta;
            if (menuFightTimer>700) { menuFightTimer=0; menuFightState=rand.nextInt(4); }
            float mx=menuRX-menuLX;
            if (mx>10) menuLX+=delta*0.04f; else if (mx<8) menuLX-=delta*0.03f;
            if (menuLX<35) menuLX=35f; if (menuLX>105) menuLX=105f;
            if (menuRX<135) menuRX=135f; if (menuRX>205) menuRX=205f;
        }

        // Gravity anomaly indicator
        if (Arena.gravityMult != 1.0f) {
            gravFlashTimer += delta;
            if (gravFlashTimer>300) gravFlashTimer=0;
        }
    }

    // ==================================================================
    //  Helpers
    // ==================================================================
    private void triggerShake(int amp, int dur) { shakeAmp=amp; shakeDur=dur; }

    /** Apply bounce platform effect: if fighter just landed on PT_BOUNCE, launch them up. */
    private void applyBounce(Stickman s) {
        if (s==null||!s.alive||!s.onGround) return;
        int[] ltype={-1};
        Arena.checkLanding(s.x, s.y+1, 8f, 0.1f, ltype);
        if (ltype[0]==Arena.PT_BOUNCE) {
            s.vy = -Math.abs(s.vy > 20f ? s.vy : 200f) * Arena.BOUNCE_MULT;
            s.onGround = false;
            spawnParticles((int)s.x,(int)s.y,0x00FF66,6);
            SoundManager.playSFX(SoundManager.SFX_JUMP);
        }
    }

    // ==================================================================
    //  RENDER: Pause overlay
    // ==================================================================
    private void renderPause(Graphics g) {
        // Dim overlay (every other line dark)
        g.setColor(0x000000);
        for (int py2 = 0; py2 < SH; py2 += 2) g.drawLine(0, py2, SW, py2);

        // Box: full-width minus margins, tall enough for all content
        int bx = 14, by = 38, bw = SW - 28, bh = 244;
        g.setColor(0x06061A); g.fillRoundRect(bx, by, bw, bh, 8, 8);
        g.setColor(C_GOLD);   g.drawRoundRect(bx, by, bw, bh, 8, 8);

        // Title
        g.setFont(fMedium); g.setColor(C_GOLD);
        g.drawString("PAUSED", SW/2, by + 7, Graphics.HCENTER|Graphics.TOP);

        // Divider
        g.setColor(0x334455);
        g.drawLine(bx + 12, by + 28, bx + bw - 12, by + 28);

        // Stats header
        g.setFont(fSmall); g.setColor(0x556677);
        g.drawString("MATCH STATS", SW/2, by + 33, Graphics.HCENTER|Graphics.TOP);

        // Stats rows ??? label left, value right
        int sx = bx + 10, sv = bx + bw - 10, sy = by + 48;
        int rowH = 15;
        int sec = roundTimeLeft / 1000;
        String[][] rows = {
            { "DMG DEALT",   String.valueOf(statDmgDealt)    },
            { "DMG TAKEN",   String.valueOf(statDmgReceived) },
            { "HITS LANDED", String.valueOf(statHitsLanded)  },
            { "PARRIES",     String.valueOf(statPerfectBlocks) },
            { "LEVEL",       "L" + playerLevel               },
            { "TIME LEFT",   (sec/60)+":"+(sec%60<10?"0":"")+sec%60 },
        };
        for (int i = 0; i < rows.length; i++) {
            int ry = sy + i * rowH;
            g.setColor(0x6688AA);
            g.drawString(rows[i][0], sx, ry, Graphics.LEFT|Graphics.TOP);
            boolean isTime = (i == rows.length - 1);
            g.setColor(isTime && sec < 20 ? C_RED : C_WHITE);
            g.drawString(rows[i][1], sv, ry, Graphics.RIGHT|Graphics.TOP);
        }

        // Divider before buttons
        int divY = sy + rows.length * rowH + 4;
        g.setColor(0x334455);
        g.drawLine(bx + 12, divY, bx + bw - 12, divY);

        // Buttons
        String[] opts  = { "RESUME", "QUIT" };
        int[]    ocols = { C_GREEN, C_RED };
        for (int i = 0; i < 2; i++) {
            int oy = divY + 6 + i * 34;
            boolean sel = (i == pauseSel);
            g.setColor(sel ? 0x14142C : 0x0A0A1C);
            g.fillRoundRect(bx + 8, oy, bw - 16, 26, 4, 4);
            if (sel) {
                g.setColor(ocols[i]);
                g.drawRoundRect(bx + 8, oy, bw - 16, 26, 4, 4);
            }
            g.setFont(fSmall); g.setColor(sel ? ocols[i] : C_GREY);
            g.drawString((sel ? "> " : "  ") + opts[i],
                         SW/2, oy + 7, Graphics.HCENTER|Graphics.TOP);
        }

        // Nav hint at bottom of box
        g.setFont(fSmall); g.setColor(0x334455);
        g.drawString("2/8:nav  5:ok  LSK:resume",
                     SW/2, by + bh - 12, Graphics.HCENTER|Graphics.TOP);
    }

    // ==================================================================
    //  SIMULATED SOUND - visual audio feedback
    // ==================================================================
    /** Queue a BGM change - actually applied in updateAll to avoid blocking keyPressed */
    private void setPendingBGM(int id) { pendingBGM = id; }
    /** Arms input guard for 300ms - call after any state-changing key press */
    private void armGuard() { inputGuard = 300; }
    private boolean isGuarded() { return inputGuard > 0; }
    private void applyPendingBGM() {
        if (pendingBGM == -2) { SoundManager.stopBGM(); pendingBGM = -1; return; }
        if (pendingBGM < 0) return;
        int id = pendingBGM; pendingBGM = -1;
        SoundManager.playBGM(id);
    }

    private void simSound(int id) {
        if (!SaveData.sfxEnabled) return;
        // Real audio - synthesized PCM via SoundManager
        switch(id) {
            case SSND_SHOOT:   SoundManager.playSFX(SoundManager.SFX_SHOOT);   break;
            case SSND_HIT:     SoundManager.playSFX(SoundManager.SFX_ATTACK);  break;
            case SSND_ULTRA:   SoundManager.playSFX(SoundManager.SFX_ULTRA);   break;
            case SSND_SPECIAL: SoundManager.playSFX(SoundManager.SFX_SPECIAL); break;
            case SSND_MENU:    SoundManager.playSFX(SoundManager.SFX_CLICK);   break;
            case SSND_WIN:     SoundManager.playSFX(SoundManager.SFX_WIN);     break;
            case SSND_DEATH:   SoundManager.playSFX(SoundManager.SFX_DEATH);   break;
        }
        switch(id) {
            case SSND_SHOOT:
                simSndTimer=80; simSndColor=0x0A1100;
                if (player!=null) spawnParticles((int)player.x, (int)(player.y-20), 0xFFFF44, 3);
                break;
            case SSND_HIT:
                simSndTimer=110; simSndColor=0x1A0000;
                break;
            case SSND_ULTRA:
                simSndTimer=280; simSndColor=0x221100;
                spawnParticles(SW/2, SH/3, 0xFFAA00, 18);
                spawnParticles(SW/2, SH/3, 0xFF4400, 10);
                break;
            case SSND_SPECIAL:
                simSndTimer=240; simSndColor=0x001122;
                spawnParticles(SW/2, SH/3, 0x00FFFF, 14);
                break;
            case SSND_MENU:
                simSndTimer=50; simSndColor=0x110022;
                break;
            case SSND_WIN:
                simSndTimer=400; simSndColor=0x110800;
                for (int wx=20; wx<SW-20; wx+=20) spawnParticles(wx, SH/3, C_GOLD, 4);
                break;
            case SSND_DEATH:
                simSndTimer=350; simSndColor=0x1A0000;
                spawnParticles(SW/2, SH/3, C_RED, 20);
                break;
        }
    }

    private void updateSimSound(int delta) {
        if (simSndTimer > 0) simSndTimer -= delta;
        if (SaveData.musicEnabled) {
            simBgmTimer += delta;
            if (simBgmTimer >= BGM_BEAT_INTERVAL) {
                simBgmTimer -= BGM_BEAT_INTERVAL;
                simSndBeat = (simSndBeat + 1) % 8;
                if (simSndTimer <= 0) {
                    if (simSndBeat == 0 || simSndBeat == 4) {
                        simSndTimer=55; simSndColor=0x0A0005;
                    } else if (simSndBeat == 2 || simSndBeat == 6) {
                        simSndTimer=35; simSndColor=0x0A0A00;
                    }
                }
            }
        }
    }

    private void renderSimSound(Graphics g) {
        if (simSndTimer <= 0) return;
        g.setColor(simSndColor);
        for (int ry = 0; ry < SH; ry += 3) g.drawLine(0, ry, SW, ry);
    }
    private void showBigMsg(String m, int col)  { bigMsg=m; bigMsgLife=1600; bigMsgColor=col; }
    private void addKillFeed(String line) {
        feedLines[feedHead]=line; feedLife[feedHead]=3000; feedHead=(feedHead+1)%4;
    }

    // ==================================================================
    //  VICTORY ANIMATION RENDERER
    // ==================================================================
    private void renderVictoryAnim(Graphics g) {
        // Rich victory/defeat scene
        int ch = Math.min(storyChapter, GameData.CHAPTER_COUNT - 1);
        // Background - arena-tinted
        drawBg(g);
        renderPlatforms(g);

        // Draw stickmen in victory/defeat poses
        if (player != null) player.draw(g, GameData.CHAR_COLOR[playerCharId]);
        if (enemy  != null) enemy.draw(g, GameData.CHAR_COLOR[enemyCharId]);

        // Victory text overlay
        int t = victoryAnimTimer;
        if (playerWonRound) {
            // Pulsing gold banner
            boolean pulse = (t/200)%2==0;
            g.setColor(pulse ? C_GOLD : 0xFFAA00);
            g.fillRect(0, 100, SW, 44);
            g.setColor(0x000000);
            g.setFont(fLarge);
            g.drawString("VICTORY!", SW/2, 104, Graphics.HCENTER|Graphics.TOP);
            g.setFont(fSmall);
            g.setColor(C_CYAN);
            if (isStoryFight && ch < CHAPTER_TITLES.length) g.drawString(CHAPTER_TITLES[ch], SW/2, 126, Graphics.HCENTER|Graphics.TOP);

            // Stars fly in
            int stars = (2800 - t) / 400;
            g.setColor(C_GOLD);
            for (int si=0; si<stars && si<5; si++) {
                int sx2 = 30 + si*40, sy2 = 80;
                g.drawLine(sx2, sy2-6, sx2, sy2+6);
                g.drawLine(sx2-6, sy2, sx2+6, sy2);
                g.drawLine(sx2-4, sy2-4, sx2+4, sy2+4);
                g.drawLine(sx2+4, sy2-4, sx2-4, sy2+4);
            }
        } else {
            // Defeat - dark overlay + red text
            g.setColor(0x330000);
            g.fillRect(0, 100, SW, 44);
            g.setColor(0xFF3333);
            g.setFont(fLarge);
            g.drawString("DEFEAT...", SW/2, 104, Graphics.HCENTER|Graphics.TOP);
            g.setFont(fSmall);
            g.setColor(0xAAAAAA);
            g.drawString("Press 5 to retry", SW/2, 128, Graphics.HCENTER|Graphics.TOP);
        }

        // Shard particles sparkling
        int shardShow = playerWonRound ? (2800-t)/100 : 0;
        g.setColor(C_GOLD);
        for (int si=0; si<shardShow && si<12; si++) {
            int px2 = 20 + (si*si*7)%200;
            int py2 = 60 + (si*13)%40;
            g.fillArc(px2, py2, 4, 4, 0, 360);
        }
    }

    // ==================================================================
    //  CINEMATIC RENDERER - scrolling dialogue with portrait
    // ==================================================================
    private void renderCinematic(Graphics g) {
        int ch = Math.min(storyChapter, STORY_VICTORY.length - 1);
        if (ch < 0) ch = 0;
        String[] lines = splitLines(playerWonRound ? STORY_VICTORY[ch][0]
                                        : STORY_VICTORY[ch][1]);
        // Clamp cinematicLine
        if (cinematicLine >= lines.length) cinematicLine = lines.length - 1;
        String line = (lines[cinematicLine] != null) ? lines[cinematicLine] : "";

        // === BACKGROUND ===
        // Dark sky with realm color tint per chapter
        int bgCol = (ch==0) ? 0x1A0800 : (ch==1) ? 0x150005 : (ch==2) ? 0x000818 : (ch==3) ? 0x080018 : 0x060009;
        g.setColor(bgCol);
        g.fillRect(0, 0, SW, SH);
        // Atmospheric lines
        for (int y2 = 0; y2 < SH; y2 += 20) {
            int alpha = 30 + (y2*2/SH);
            g.setColor(darken(bgCol) | (alpha << 16 & 0xFF0000));
            g.drawLine(0, y2, SW, y2);
        }

        // === CHAPTER BANNER ===
        g.setColor(C_GOLD);
        g.setFont(fSmall);
        String title = (ch >= 0 && ch < CHAPTER_TITLES.length) ? CHAPTER_TITLES[ch] : "THE VOID GATE";
        g.drawString(title, SW/2, 8, Graphics.HCENTER|Graphics.TOP);
        g.setColor(playerWonRound ? C_GOLD : 0xFF4444);
        g.drawString(playerWonRound ? "VICTORY!" : "DEFEAT...", SW/2, 22, Graphics.HCENTER|Graphics.TOP);

        // === SPEAKER PORTRAIT ===
        // Detect speaker from line prefix
        int speakerChar = -1;
        int portraitCol = C_WHITE;
        if      (line.startsWith("KAEL:"))        { speakerChar = GameData.CH_KAEL;   portraitCol = GameData.CHAR_COLOR[GameData.CH_KAEL]; }
        else if (line.startsWith("IGNIS:"))       { speakerChar = GameData.CH_IGNIS;  portraitCol = GameData.CHAR_COLOR[GameData.CH_IGNIS]; }
        else if (line.startsWith("TITAN:"))       { speakerChar = GameData.CH_TITAN;  portraitCol = GameData.CHAR_COLOR[GameData.CH_TITAN]; }
        else if (line.startsWith("FROSTBANE:"))   { speakerChar = GameData.CH_FROST;  portraitCol = GameData.CHAR_COLOR[GameData.CH_FROST]; }
        else if (line.startsWith("FROST QUEEN:")) { speakerChar = GameData.CH_QUEEN;  portraitCol = GameData.CHAR_COLOR[GameData.CH_QUEEN]; }
        else if (line.startsWith("NEXUS:"))       { speakerChar = GameData.CH_NEXUS;  portraitCol = GameData.CHAR_COLOR[GameData.CH_NEXUS]; }
        else if (line.startsWith("SHADOWBORN:"))  { speakerChar = GameData.CH_SHADOW; portraitCol = GameData.CHAR_COLOR[GameData.CH_SHADOW]; }
        else if (line.startsWith("NARRATOR:"))    { speakerChar = -1; portraitCol = C_GREY; }

        // Portrait inside dialogue box (top-left of box)
        // === DIALOGUE BOX ===
        int boxTop = SH - 88;
        g.setColor(0x080814);
        g.fillRect(0, boxTop, SW, 88);
        g.setColor(C_GOLD);
        g.drawLine(0, boxTop, SW, boxTop);

        // Portrait - compact 36px stickman inside box
        if (speakerChar >= 0) {
            // Mini portrait frame
            g.setColor(portraitCol);
            g.drawRect(4, boxTop+2, 36, 50);
            g.setColor(0x0A0A1A); g.fillRect(5, boxTop+3, 35, 49);
            drawCharBig(g, speakerChar, 22, boxTop+46, portraitCol);
        } else {
            // Narrator: orb icon
            g.setColor(0x223355); g.fillArc(6, boxTop+4, 34, 34, 0, 360);
            g.setColor(C_GOLD);   g.drawArc(6, boxTop+4, 34, 34, 0, 360);
            g.setFont(fSmall); g.setColor(C_GOLD);
            g.drawString("N", 23, boxTop+14, Graphics.HCENTER|Graphics.TOP);
        }

        // Speaker name tag (right of portrait)
        String speaker = "NARRATOR";
        if      (line.startsWith("KAEL:"))        speaker = "KAEL";
        else if (line.startsWith("IGNIS:"))       speaker = "IGNIS";
        else if (line.startsWith("TITAN:"))       speaker = "TITAN";
        else if (line.startsWith("FROSTBANE:"))   speaker = "FROSTBANE";
        else if (line.startsWith("FROST QUEEN:")) speaker = "FROST QUEEN";
        else if (line.startsWith("NEXUS:"))       speaker = "NEXUS";
        else if (line.startsWith("NEXUS/DAVID:")) speaker = "NEXUS/DAVID";
        else if (line.startsWith("SHADOWBORN:"))  speaker = "SHADOWBORN";
        g.setColor(portraitCol);
        g.setFont(fSmall);
        g.drawString("[" + speaker + "]", 44, boxTop+3, Graphics.LEFT|Graphics.TOP);

        // Dialogue text (strip speaker prefix)
        String dlg = line;
        int colon = line.indexOf(':');
        if (colon >= 0 && colon < 20) dlg = line.substring(colon + 1).trim();

        // Word-wrap dialogue text - strict pixel width check for 240px screen
        g.setColor(C_WHITE);
        g.setFont(fSmall);
        int maxW = SW - 50;   // 190px: from x=44 to x=SW-6
        int ty2 = boxTop + 17;
        String rem = dlg;
        int linesDrawn = 0;
        while (rem.length() > 0 && ty2 < SH - 14 && linesDrawn < 4) {
            int cutAt = rem.length();
            // Binary-search for fit
            int lo = 1, hi = rem.length();
            while (lo < hi) {
                int mid = (lo + hi + 1) / 2;
                if (fSmall.stringWidth(rem.substring(0, mid)) <= maxW) lo = mid;
                else hi = mid - 1;
            }
            cutAt = lo;
            // Prefer word boundary
            if (cutAt < rem.length()) {
                int sp = rem.lastIndexOf(' ', cutAt);
                if (sp > 2) cutAt = sp;
            }
            g.drawString(rem.substring(0, cutAt), 44, ty2, Graphics.LEFT|Graphics.TOP);
            rem = rem.substring(cutAt).trim();
            ty2 += 13;
            linesDrawn++;
        }

        // Progress dots
        int totalLines = lines.length;
        int dotX = SW/2 - totalLines*6;
        for (int d = 0; d < totalLines; d++) {
            g.setColor(d == cinematicLine ? C_GOLD : 0x334455);
            g.fillArc(dotX + d*12, boxTop+76, 7, 7, 0, 360);
        }

        // Auto-advance hint
        int pct2 = Math.min(100, cinematicTimer * 100 / 2500);
        g.setColor(0x333355);
        g.fillRect(0, SH-4, SW, 4);
        g.setColor(C_GOLD);
        g.fillRect(0, SH-4, SW * pct2 / 100, 4);
        g.setColor(C_GREY);
        g.setFont(fSmall);
        g.drawString("5:skip", SW-40, SH-17, Graphics.LEFT|Graphics.TOP);
    }

    private int getSpeakerColor(String speaker, int ch) {
        if (speaker.equals("NARRATOR")) return C_GOLD;
        if (speaker.equals("KAEL"))     return GameData.CHAR_COLOR[GameData.CH_KAEL];
        if (speaker.equals("IGNIS") || speaker.equals("FIRE TITAN")) return 0xFF4400;
        if (speaker.equals("TITAN"))    return GameData.CHAR_COLOR[GameData.CH_TITAN];
        if (speaker.equals("FROST QUEEN")) return GameData.CHAR_COLOR[GameData.CH_QUEEN];
        if (speaker.equals("FROSTBANE"))return GameData.CHAR_COLOR[GameData.CH_FROST];
        if (speaker.equals("NEXUS"))    return GameData.CHAR_COLOR[GameData.CH_NEXUS];
        if (speaker.equals("ZHARA"))    return GameData.CHAR_COLOR[GameData.CH_ZHARA];
        if (speaker.equals("SHADOWBORN"))return GameData.CHAR_COLOR[GameData.CH_SHADOW];
        if (speaker.equals("SHADOWBORN"))return GameData.CHAR_COLOR[GameData.CH_SHADOW];
        return C_CYAN;
    }

    private void drawCinematicPortrait(Graphics g, int charId, int x, int y,
                                       boolean leftSide, boolean isDefeated) {
        int col = GameData.CHAR_COLOR[charId];
        int dir = leftSide ? 1 : -1;
        boolean isBoss = (charId == GameData.CH_TITAN || charId == GameData.CH_QUEEN);

        // Boss aura
        if (isBoss) {
            boolean bFlash = (System.currentTimeMillis() % 400L) < 200L;
            g.setColor(bFlash ? col : 0x220000);
            g.fillArc(x-20, y-70, 40, 50, 0, 360);
        }

        // Defeated slump
        if (isDefeated) {
            g.setColor(0x333344);
            g.fillRect(x-10, y-28, 20, 28);
            g.fillArc(x-8, y-36, 16, 16, 0, 360);
            g.setColor(col); g.drawArc(x-8, y-36, 16, 16, 0, 360);
            g.setColor(0xFF2222);
            g.drawLine(x-4, y-32, x,   y-28);
            g.drawLine(x,   y-32, x-4, y-28);
            return;
        }

        // Standing portrait (big stickman)
        g.setColor(col);
        // Legs
        g.drawLine(x, y-18, x-8*dir, y);
        g.drawLine(x, y-18, x+6*dir, y);
        // Body
        g.drawLine(x, y-18, x, y-40);
        g.drawLine(x, y-18, x+1, y-40); // thick
        // Arms (raised in victory or in guard)
        g.drawLine(x, y-34, x+14*dir, y-42);
        g.drawLine(x, y-34, x-10*dir, y-28);
        // Head
        g.setColor(0x1A1A2E); g.fillArc(x-9, y-54, 18, 18, 0, 360);
        g.setColor(col);       g.drawArc(x-9, y-54, 18, 18, 0, 360);
        g.drawArc(x-8, y-53, 16, 16, 0, 360);
        // Eyes
        g.setColor(C_WHITE);   g.fillRect(x+dir*2-1, y-48, 3, 3);
        g.setColor(0);         g.fillRect(x+dir*2,   y-47, 1, 2);
        // Character-specific weapon/prop
        switch (charId) {
            case GameData.CH_IGNIS:
                g.setColor(0xFF4400);
                g.drawLine(x+14*dir, y-42, x+20*dir, y-50);
                g.setColor(0xFFCC00);
                g.fillArc(x+16*dir-3, y-55, 6, 6, 0, 360);
                break;
            case GameData.CH_FROST: case GameData.CH_QUEEN:
                g.setColor(0x88CCFF);
                g.drawLine(x+14*dir, y-40, x+20*dir, y-52);
                g.fillRect(x+17*dir-2, y-58, 4, 8);
                break;
            case GameData.CH_NEXUS:
                g.setColor(0x00FF88);
                g.drawArc(x-12, y-52, 24, 12, 0, 360);
                break;
            case GameData.CH_TITAN:
                g.setColor(0xFF4400);
                g.fillArc(x-14, y-52, 28, 16, 0, 360);
                break;
        }
    }

    // ==================================================================
    //  STORY SELECT RENDERER
    // ==================================================================
    private void renderStorySelect(Graphics g) {
        // Background
        drawBg(g);
        int cy = 10;
        g.setFont(fMedium);
        g.setColor(C_GOLD);
        drawPixelText(g, "STORY", 60, cy, C_GOLD, 2);
        drawPixelText(g, "MODE", 62, cy+20, 0xFF8800, 2);
        cy += 48;

        int rowH = 44; // compact rows so 5 chapters fit in 320px screen
        for (int i = 0; i < GameData.CHAPTER_COUNT; i++) {
            if (cy + rowH > SH - 16) break; // safety: don't draw off-screen
            boolean done  = storyDone[i];
            boolean avail = (i == 0) || (i > 0 && i <= storyDone.length && storyDone[i-1]);
            boolean sel   = (storyChapter == i);

            // Selection highlight
            if (sel) {
                g.setColor(0x223344);
                g.fillRect(8, cy-2, SW-16, rowH);
                g.setColor(C_GOLD);
                g.drawRect(8, cy-2, SW-16, rowH);
            }

            // Chapter number badge
            int badgeCol = done ? C_GOLD : (avail ? C_CYAN : 0x334455);
            g.setColor(badgeCol);
            g.fillRect(12, cy, 18, 18);
            g.setColor(0x000000);
            g.setFont(fSmall);
            g.drawString(""+(i+1), 21, cy+1, Graphics.HCENTER|Graphics.TOP);

            // Title and enemy
            g.setFont(fSmall);
            g.setColor(avail ? (done ? C_GOLD : C_WHITE) : 0x445566);
            String title = (i < CHAPTER_TITLES.length) ? CHAPTER_TITLES[i] : "CH "+(i+1);
            // Truncate to fit
            if (title.length() > 22) title = title.substring(0, 22);
            g.drawString(title, 36, cy, Graphics.LEFT|Graphics.TOP);
            g.setColor(avail ? 0x888888 : 0x333344);
            String ename = (i < CHAPTER_ENEMIES.length) ? CHAPTER_ENEMIES[i] : "";
            if (ename.length() > 22) ename = ename.substring(0, 22);
            g.drawString(ename, 36, cy+13, Graphics.LEFT|Graphics.TOP);

            // Status + shard reward
            String status = done ? "DONE" : (avail ? (sel?">>>":"PLAY") : "LOCK");
            int stCol = done ? C_GOLD : (avail ? C_GREEN : 0x334455);
            g.setColor(stCol);
            g.drawString(status, SW-4, cy+2, Graphics.RIGHT|Graphics.TOP);
            if (avail && !done) {
                int rew = (i < CHAPTER_SHARD_REWARD.length) ? CHAPTER_SHARD_REWARD[i] : 0;
                g.setColor(C_CYAN);
                g.drawString("+"+rew, SW-4, cy+14, Graphics.RIGHT|Graphics.TOP);
            }
            cy += rowH;
        }

        // Controls hint
        g.setFont(fSmall);
        g.setColor(C_GREY);
        g.drawString("2/8:select  5:play  0:menu", SW/2, SH-14, Graphics.HCENTER|Graphics.TOP);
    }

    // ==================================================================
    //  Special FX dispatch - reads signal from stickman, spawns particles,
    //  applies damage to target, triggers screen shake
    // ==================================================================
    private void dispatchSpecialFX(Stickman src, Stickman target) {
        if (src == null || src.spFxType == Stickman.FX_NONE) return;
        int fx = src.spFxType;
        float fx_x = src.spFxX, fx_y = src.spFxY;

        switch (fx) {
            case Stickman.FX_FLAME_BURST:
                // Dragon Breath: cone of fire particles
                for (int f = 0; f < 6; f++) {
                    float ang = (src.facingRight ? 0f : 180f) + (rand.nextFloat()-0.5f)*50f;
                    float vx2 = (float)Math.cos(ang*3.14159f/180f) * (200f+rand.nextFloat()*120f);
                    float vy2 = (float)Math.sin(ang*3.14159f/180f) * 80f - 40f;
                    int col = rand.nextInt(3)==0 ? 0xFF8800 : (rand.nextInt(2)==0 ? 0xFF4400 : 0xFFCC00);
                    spawnSingleParticle(fx_x, fx_y, vx2, vy2, col, 280+rand.nextInt(200), 2);
                }
                // Damage target if in cone
                if (target != null && target.alive) {
                    float dx = target.x - src.x;
                    boolean inCone = src.facingRight ? (dx > 0 && dx < 200) : (dx < 0 && dx > -200);
                    float dy = Math.abs(target.y - src.y);
                    if (inCone && dy < 60) src.applySpecialToTarget(target);
                }
                if (src.specialTimer > 2400) { // First frame: big shake
                    triggerShake(10, 300);
                    showBigMsg("DRAGON BREATH!!", GameData.CHAR_SPECIAL_COLOR[src.charId]);
                    addKillFeed(GameData.CHAR_NAME[src.charId]+": DRAGON BREATH!!");
                }
                break;

            case Stickman.FX_LIGHTNING:
                // Lightning storm: bolt from top of screen
                spawnParticles((int)fx_x, (int)fx_y, 0x0088FF, 8);
                spawnParticles((int)fx_x, (int)fx_y, 0xCCEEFF, 4);
                triggerShake(8, 200);
                // Draw target bolt - find nearest enemy, apply damage
                if (target != null && target.alive) {
                    float dx2 = Math.abs(target.x - fx_x);
                    if (dx2 < 70f) { // bolt range
                        src.applySpecialToTarget(target);
                        spawnParticles((int)target.x, (int)target.y, 0x88EEFF, 12);
                        // Chain lightning: 40% chance
                        if (rand.nextInt(10) < 4) {
                            spawnParticles((int)target.x+20, (int)target.y, 0xCCEEFF, 6);
                        }
                    }
                }
                if (src.boltCount == 1) {
                    showBigMsg("LIGHTNING STORM!!", GameData.CHAR_SPECIAL_COLOR[src.charId]);
                    addKillFeed(GameData.CHAR_NAME[src.charId]+": LIGHTNING STORM!!");
                }
                break;

            case Stickman.FX_SOUL_WISP:
                // Soul Reap: purple ghost trail + lifesteal
                spawnParticles((int)fx_x, (int)fx_y, 0xAA44FF, 5);
                spawnParticles((int)fx_x, (int)fx_y, 0xDD88FF, 3);
                // Apply during dash
                if (src.dashTimer > 0 && target != null && target.alive) {
                    float dx3 = Math.abs(target.x - src.x);
                    float dy3 = Math.abs(target.y - src.y);
                    if (dx3 < 30f && dy3 < 50f) {
                        src.applySpecialToTarget(target);
                        spawnParticles((int)target.x, (int)target.y, 0xAA44FF, 10);
                        triggerShake(12, 300);
                    }
                }
                if (src.specialCooldown > 11900) { // first frame
                    showBigMsg("SOUL REAP!!", GameData.CHAR_SPECIAL_COLOR[src.charId]);
                    addKillFeed(GameData.CHAR_NAME[src.charId]+": SOUL REAP!!");
                }
                break;

            case Stickman.FX_ICE_SHARD:
                // Permafrost: ice shards burst out in ring
                for (int i2 = 0; i2 < 8; i2++) {
                    float a2 = i2 * 45f;
                    float vx3 = (float)Math.cos(a2*3.14159f/180f) * 180f;
                    float vy3 = (float)Math.sin(a2*3.14159f/180f) * 180f;
                    spawnSingleParticle(fx_x, fx_y, vx3, vy3, 0x88CCFF, 400, 3);
                    spawnSingleParticle(fx_x, fx_y, vx3*0.6f, vy3*0.6f, 0xAAEEFF, 300, 2);
                }
                if (target != null && target.alive) {
                    float d4 = (float)Math.sqrt((target.x-src.x)*(target.x-src.x)+(target.y-src.y)*(target.y-src.y));
                    if (d4 < 100f) src.applySpecialToTarget(target);
                }
                if (src.specialCooldown > 11900) {
                    showBigMsg("PERMAFROST!!", GameData.CHAR_SPECIAL_COLOR[src.charId]);
                    addKillFeed(GameData.CHAR_NAME[src.charId]+": PERMAFROST!!");
                    triggerShake(8, 250);
                }
                break;

            case Stickman.FX_GRAV_PULL:
                // Gravity well: pull target toward well
                spawnParticles((int)fx_x, (int)fx_y, 0x00FF88, 4);
                if (target != null && target.alive) {
                    float gdx = src.gravWellX - target.x;
                    float gdy = src.gravWellY - target.y;
                    float gdist = (float)Math.sqrt(gdx*gdx + gdy*gdy);
                    if (gdist < 120f && gdist > 5f) {
                        // Pull force
                        target.vx += (gdx/gdist)*200f * (1f/60f);
                        target.vy += (gdy/gdist)*200f * (1f/60f);
                        if (gdist < 35f) src.applySpecialToTarget(target);
                    }
                }
                if (src.specialCooldown > 11900) {
                    showBigMsg("GRAVITY WELL!!", GameData.CHAR_SPECIAL_COLOR[src.charId]);
                    addKillFeed(GameData.CHAR_NAME[src.charId]+": GRAVITY WELL!!");
                }
                break;

            case Stickman.FX_VINE:
                // Vine cage: spawn green particles, root target
                spawnParticles((int)fx_x, (int)fx_y, 0x228822, 5);
                spawnParticles((int)fx_x, (int)fx_y, 0x44CC44, 3);
                if (target != null && target.alive) {
                    float vd = (float)Math.sqrt((target.x-src.x)*(target.x-src.x)+(target.y-src.y)*(target.y-src.y));
                    if (vd < 90f) src.applySpecialToTarget(target);
                }
                if (src.specialCooldown > 11900) {
                    showBigMsg("VINE CAGE!!", GameData.CHAR_SPECIAL_COLOR[src.charId]);
                    addKillFeed(GameData.CHAR_NAME[src.charId]+": VINE CAGE!!");
                }
                break;

            case Stickman.FX_QUAKE:
                // Magma slam: massive particle burst + shockwave
                spawnParticles((int)src.x, (int)src.y, 0xFF4400, 16);
                spawnParticles((int)src.x, (int)src.y, 0xFF8800, 10);
                spawnParticles((int)src.x, (int)src.y, 0xFFCC00, 6);
                triggerShake(16, 500);
                if (target != null && target.alive) {
                    float md = Math.abs(target.x - src.x);
                    if (md < 130f) src.applySpecialToTarget(target);
                }
                showBigMsg("MAGMA SLAM!!", GameData.CHAR_SPECIAL_COLOR[src.charId]);
                addKillFeed(GameData.CHAR_NAME[src.charId]+": MAGMA SLAM!!");
                break;

            case Stickman.FX_BLIZZARD:
                // Blizzard: ice particles all over screen
                spawnSingleParticle(fx_x, (float)(rand.nextInt(SW)), 0f, 60f, 0x88CCFF, 600, 2);
                spawnSingleParticle(fx_x + rand.nextFloat()*40f-20f, 0, 0f, 50f, 0xCCEEFF, 500, 1);
                if (target != null && target.alive) src.applySpecialToTarget(target);
                if (src.specialCooldown > 11900) {
                    showBigMsg("BLIZZARD!!", GameData.CHAR_SPECIAL_COLOR[src.charId]);
                    addKillFeed(GameData.CHAR_NAME[src.charId]+": BLIZZARD!!");
                    triggerShake(10, 400);
                }
                break;
        }
    }

    private void spawnSingleParticle(float x, float y, float vx2, float vy2, int col, int life, int size) {
        for (int i = 0; i < MAX_PART; i++) {
            if (pLife[i] <= 0) {
                pX[i]=x; pY[i]=y; pVX[i]=vx2; pVY[i]=vy2;
                pLife[i]=life; pCol[i]=col; pSize[i]=size;
                return;
            }
        }
    }

    // ==================================================================
    //  RENDER dispatch
    // ==================================================================
    private void renderAll(Graphics g) {
        g.translate(shakeX, shakeY);
        switch (gameState) {
            case GS_LOGO:        renderLogo(g);         break;
            case GS_MENU:        renderMenu(g);         break;
            case GS_CHAR_SEL:    renderCharSel(g);      break;
            case GS_ARENA_SEL:   renderArenaSel(g);     break;
            case GS_LORE:        renderLoreScreen(g);   break;
            case GS_SHOP:        renderShop(g);         break;
            case GS_SKILLS:      renderSkills(g);       break;
            case GS_STORY_INTRO: renderStoryIntro(g);   break;
            case GS_MELEE:
            case GS_MELEE_END:   renderMelee(g);         break;
            case GS_HOWTO:       renderHowTo(g);        break;
            case GS_SHARD_REWARD:renderShardReward(g);  break;
            case GS_GAME:
            case GS_SURVIVAL:
                renderGame(g); break;
            case GS_ROUND_END:
                renderGame(g); renderEnhancedRoundBanner(g); break;
            case GS_WAVE_END:
                renderGame(g); renderWaveBanner(g);  break;
            case GS_GAME_OVER:
                renderGame(g); renderGameOver(g);    break;
            case GS_VICTORY_ANIM: renderVictoryAnim(g);    break;
            case GS_CINEMATIC:    renderCinematic(g);       break;
            case GS_STORY_SEL:    renderStorySelect(g);     break;
            case GS_PAUSE:        renderGame(g); renderPause(g); break;
            case GS_SETTINGS:     renderSettings(g);   break;
            case GS_TOURNAMENT:   renderTournament(g); break;
        }
        g.translate(-shakeX, -shakeY);
    }

    // ==================================================================
    //  RENDER: Logo splash
    // ==================================================================
    private void renderLogo(Graphics g) {
        int cx = SW / 2, cy = SH / 2 - 20;

        // ================================================================
        //  BACKGROUND - deep void + nebula glow
        // ================================================================
        g.setColor(0x000000); g.fillRect(0, 0, SW, SH);

        // Nebula rings behind orb (phase 1+)
        if (logoPhase >= 1) {
            int[] nRad = { 96, 82, 68, 55 };
            int[] nCol = { 0x0D0006, 0x1A000C, 0x2A0014, 0x3A001C };
            for (int n = 0; n < 4; n++) {
                g.setColor(nCol[n]);
                g.fillArc(cx - nRad[n], cy - nRad[n], nRad[n]*2, nRad[n]*2, 0, 360);
            }
            // Pulse-breathing outer glow
            int pExtra = logoPulse / 10;
            g.setColor(0x440010);
            g.drawArc(cx - 58 - pExtra, cy - 58 - pExtra,
                      (58 + pExtra)*2, (58 + pExtra)*2, 0, 360);
            g.setColor(0x660018);
            g.drawArc(cx - 54 - pExtra, cy - 54 - pExtra,
                      (54 + pExtra)*2, (54 + pExtra)*2, 0, 360);
        }

        // Twinkling starfield
        for (int i = 0; i < STAR_COUNT; i++) {
            boolean twinkle = ((logoSubTimer / 220 + i) % 5) < 2;
            g.setColor(twinkle ? 0xFFFFFF : (starB[i] == 2 ? 0x445588 : 0x223355));
            g.fillRect(starX[i], starY[i], starB[i] == 2 ? 2 : 1, starB[i] == 2 ? 2 : 1);
        }

        if (logoPhase == 0) return; // pure black intro

        // ================================================================
        //  PHASE 1: ring burst + lens flare
        // ================================================================
        if (logoPhase >= 1) {
            int rr = logoRingR;
            if (rr > 0 && rr < 200) {
                g.setColor(0xFF2200); g.drawArc(cx-rr,   cy-rr,   rr*2,   rr*2,   0, 360);
                g.setColor(0xFF6600); g.drawArc(cx-rr+4, cy-rr+4, rr*2-8, rr*2-8, 0, 360);
                g.setColor(0xFFDD00); g.drawArc(cx-rr+9, cy-rr+9, rr*2-18,rr*2-18,0, 360);
            }
            // Lens flare cross
            int fl = logoFlare / 40;
            if (fl > 0) {
                g.setColor(0xFFFFCC);
                g.drawLine(cx-fl*6, cy, cx+fl*6, cy);
                g.drawLine(cx, cy-fl*4, cx, cy+fl*4);
                g.setColor(0xFF4400);
                g.drawLine(cx-fl*3, cy-fl*3, cx+fl*3, cy+fl*3);
                g.drawLine(cx+fl*3, cy-fl*3, cx-fl*3, cy+fl*3);
            }
        }

        // ================================================================
        //  PHASE 2+: The Orb + Characters
        // ================================================================
        if (logoPhase >= 2) {
            int baseY = cy + logoCharY;
            int rad   = 56;

            // ---- Outer pulsing glow rings --------------------------------
            int pExtra = logoPulse / 10;
            g.setColor(0x550000);
            g.fillArc(cx-rad-6-pExtra, baseY-rad-6-pExtra,
                      (rad+6+pExtra)*2, (rad+6+pExtra)*2, 0, 360);
            g.setColor(0x880000);
            g.drawArc(cx-rad-4-pExtra, baseY-rad-4-pExtra,
                      (rad+4+pExtra)*2, (rad+4+pExtra)*2, 0, 360);
            g.setColor(0xBB1100);
            g.drawArc(cx-rad-2-pExtra, baseY-rad-2-pExtra,
                      (rad+2+pExtra)*2, (rad+2+pExtra)*2, 0, 360);

            // ---- Main orb body ------------------------------------------
            g.setColor(0x0A0A0A); g.fillArc(cx-rad, baseY-rad, rad*2, rad*2, 0, 360);
            // Left half - deep red with highlight
            g.setColor(0xBB1100); g.fillArc(cx-rad, baseY-rad, rad*2, rad*2, 90, 180);
            g.setColor(0xFF3311); g.fillArc(cx-rad+6, baseY-rad+6, rad-10, rad-10, 100, 120);
            // Right half - dark charcoal with subtle blue tint
            g.setColor(0x16162A); g.fillArc(cx-rad, baseY-rad, rad*2, rad*2, 270, 180);
            g.setColor(0x252540); g.fillArc(cx+8, baseY-rad+6, rad-20, rad-20, 280, 100);

            // ---- Glowing center divider line ----------------------------
            int lineFlicker = ((logoSubTimer / 100) % 3 == 0) ? 1 : 0;
            g.setColor(0xFFFFFF); g.fillRect(cx-1+lineFlicker, baseY-rad, 2, rad*2);
            g.setColor(0xFF8800); g.drawLine(cx-2, baseY-rad, cx-2, baseY+rad);
            g.drawLine(cx+2, baseY-rad, cx+2, baseY+rad);

            // ---- Orb border rings ---------------------------------------
            g.setColor(0xFF2200); g.drawArc(cx-rad,   baseY-rad,   rad*2,   rad*2,   0, 360);
            g.setColor(0x882200); g.drawArc(cx-rad-2, baseY-rad-2, rad*2+4, rad*2+4, 0, 360);
            g.setColor(0xFFAA00); g.drawArc(cx-rad-3, baseY-rad-3, rad*2+6, rad*2+6, 0, 360);

            // ---- Electric arc bolts (rotating) --------------------------
            int arcPh = logoArcTimer % 1200;
            g.setColor(0x00EEFF);
            if (arcPh < 200) {
                g.drawLine(cx, baseY-rad, cx+9, baseY-rad-14);
                g.drawLine(cx+9, baseY-rad-14, cx+5, baseY-rad-24);
                g.setColor(0xFFFFFF);
                g.drawLine(cx+1, baseY-rad-1, cx+5, baseY-rad-9);
            } else if (arcPh < 400) {
                g.drawLine(cx+rad, baseY, cx+rad+15, baseY-9);
                g.drawLine(cx+rad+15, baseY-9, cx+rad+11, baseY-20);
                g.setColor(0xFFFFFF);
                g.drawLine(cx+rad+1, baseY, cx+rad+8, baseY-5);
            } else if (arcPh < 600) {
                g.drawLine(cx-rad/2, baseY+rad-5, cx-rad/2-12, baseY+rad+12);
                g.drawLine(cx-rad/2-12, baseY+rad+12, cx-rad/2-7, baseY+rad+22);
                g.setColor(0xFFFFFF);
                g.drawLine(cx-rad/2, baseY+rad, cx-rad/2-6, baseY+rad+8);
            }

            // ---- Orbiting sparks ----------------------------------------
            double ang = logoOrbit * Math.PI / 180.0;
            int ox  = cx + (int)(Math.cos(ang) * (rad + 10));
            int oy  = baseY + (int)(Math.sin(ang) * (rad + 10));
            g.setColor(0xFFDD00); g.fillRect(ox-1, oy-1, 3, 3);
            g.setColor(0xFF8800); g.fillRect(ox,   oy,   2, 2);
            int ox2 = cx + (int)(Math.cos(ang + Math.PI) * (rad + 12));
            int oy2 = baseY + (int)(Math.sin(ang + Math.PI) * (rad + 12));
            g.setColor(0x00AAFF); g.fillRect(ox2-1, oy2-1, 3, 3);
            g.setColor(0x0055FF); g.fillRect(ox2,   oy2,   2, 2);

            // ================================================================
            //  LEFT character - Kael (red fighter)
            // ================================================================
            // Body glow
            g.setColor(0x550000); g.fillRect(cx-18, baseY-24, 14, 32);
            g.setColor(0xCC2200);
            g.fillArc(cx-22, baseY-22, 14, 16, 0, 360);         // head
            g.drawLine(cx-15, baseY-6,  cx-15, baseY+16);       // torso
            g.drawLine(cx-14, baseY-6,  cx-14, baseY+16);
            g.drawLine(cx-14, baseY+4,  cx-22, baseY+18);       // left leg
            g.drawLine(cx-14, baseY+4,  cx-5,  baseY+18);       // right leg
            g.drawLine(cx-14, baseY-2,  cx-23, baseY+4);        // left arm
            g.drawLine(cx-14, baseY-2,  cx-5,  baseY+4);        // right arm
            // Flame hair spikes
            g.setColor(0xFF4422);
            for (int sp = 0; sp < 4; sp++) {
                int hx = cx - 22 + sp * 3;
                g.drawLine(hx, baseY-22, hx+1, baseY-32+sp%2*3);
            }
            // Hot highlight on hair
            g.setColor(0xFF8844);
            g.drawLine(cx-21, baseY-23, cx-18, baseY-28);
            // Red glowing eye
            g.setColor(0xFF0000); g.fillRect(cx-20, baseY-16, 4, 3);
            g.setColor(0xFF8800); g.fillRect(cx-19, baseY-16, 2, 2);
            // Head highlight
            g.setColor(0xFF6644); g.fillRect(cx-21, baseY-21, 3, 2);

            // ================================================================
            //  RIGHT character - Shadowborn (dark rogue)
            // ================================================================
            g.setColor(0x0D0D22);
            g.fillArc(cx+8, baseY-22, 14, 16, 0, 360);          // head
            g.setColor(0x2A2A40);
            g.drawLine(cx+15, baseY-6,  cx+15, baseY+16);
            g.drawLine(cx+14, baseY-6,  cx+14, baseY+16);
            g.drawLine(cx+14, baseY+4,  cx+6,  baseY+18);
            g.drawLine(cx+14, baseY+4,  cx+23, baseY+18);
            g.drawLine(cx+14, baseY-2,  cx+5,  baseY+4);
            g.drawLine(cx+14, baseY-2,  cx+23, baseY+8);
            // Hood
            g.setColor(0x111120); g.fillArc(cx+6, baseY-26, 16, 10, 0, 180);
            g.setColor(0x333355); g.drawArc(cx+6, baseY-26, 16, 10, 0, 180);
            // Purple glowing dual eyes
            g.setColor(0xAA44FF);
            g.fillRect(cx+11, baseY-16, 3, 3);
            g.fillRect(cx+17, baseY-16, 3, 3);
            g.setColor(0xDD99FF);
            g.fillRect(cx+12, baseY-15, 1, 1);
            g.fillRect(cx+18, baseY-15, 1, 1);
            // Subtle purple aura under Shadowborn
            g.setColor(0x220033);
            g.fillArc(cx+2, baseY+10, 20, 8, 0, 360);

            // ================================================================
            //  Flame crown icon atop orb
            // ================================================================
            g.setColor(0xFF2200); g.fillArc(cx-8, baseY-rad-6, 16, 14, 0, 180);
            g.setColor(0xFF6600); g.fillArc(cx-5, baseY-rad-10, 10, 10, 0, 180);
            g.setColor(0xFFDD00); g.fillRect(cx-1, baseY-rad-10, 3, 5);
            g.setColor(0xFF2200); g.drawArc(cx-8, baseY-rad-6,  16, 14, 0, 180);
            g.setColor(0xFFFFFF); g.fillRect(cx-1, baseY-rad-4,  2,  6);

            // ================================================================
            //  Fire particles rising from orb
            // ================================================================
            for (int i = 0; i < 12; i++) {
                if (logoFireLife[i] <= 0) continue;
                int pfx = cx + logoFireX[i];
                int pfy = baseY + rad/2 - logoFireY[i];
                if (pfy < baseY - rad - 25 || pfy > baseY + rad) continue;
                int fsz = logoFireLife[i] > 500 ? 2 : 1;
                int fc  = (logoFireLife[i] > 500) ? 0xFF6600 :
                          (logoFireLife[i] > 200  ? 0xFF2200 : 0x882200);
                g.setColor(fc);
                g.fillRect(pfx, pfy, fsz, fsz + 1);
            }

            // ---- Black corner masks (rough circle clip) ------------------
            g.setColor(0x000000);
            g.fillRect(cx-rad-2, baseY-rad-2, 22, 22);
            g.fillRect(cx+rad-20, baseY-rad-2, 22, 22);
            g.fillRect(cx-rad-2,  baseY+rad-20, 22, 22);
            g.fillRect(cx+rad-20, baseY+rad-20, 22, 22);
        }

        // ================================================================
        //  PHASE 3+: DASH ANIMATION V2 text - enhanced
        // ================================================================
        if (logoPhase >= 3) {
            g.setFont(fSmall);
            int ty = cy + 52;

            // Shimmer sweep band behind text
            int shimX = logoShimmer - 40;
            if (shimX > cx - 90 && shimX < cx + 90) {
                g.setColor(0x553322);
                g.fillRect(shimX, ty - 1, 22, 12);
            }

            // Glitch offset
            int gx = (logoGlitch % 2 == 1) ? rand.nextInt(4) - 2 : 0;
            int gy = (logoGlitch % 3 == 2) ? rand.nextInt(3) - 1 : 0;

            // Deep shadow
            g.setColor(0x1A0000);
            g.drawString("DASH ANIMATION V2", cx+gx+2, ty+2, Graphics.HCENTER|Graphics.TOP);

            // RGB chromatic aberration - always cycling with pulse
            int cSplit = 2 + (logoPulse / 25);  // 2-6 px
            g.setColor(0xFF0044);
            g.drawString("DASH ANIMATION V2", cx-cSplit+gx, ty+gy, Graphics.HCENTER|Graphics.TOP);
            g.setColor(0x00FFEE);
            g.drawString("DASH ANIMATION V2", cx+cSplit+gx, ty+gy, Graphics.HCENTER|Graphics.TOP);

            // Main text - warm orange-red brightness tracks pulse
            int bright = 170 + logoAlpha2 / 6 + logoPulse / 5;
            if (bright > 255) bright = 255;
            int mainCol = (bright << 16) | ((bright / 4) << 8) | 0;
            g.setColor(mainCol);
            g.drawString("DASH ANIMATION V2", cx+gx, ty+gy, Graphics.HCENTER|Graphics.TOP);

            // Hot white core during high pulse
            if (logoPulse > 72) {
                g.setColor(0xFFEECC);
                g.drawString("DASH ANIMATION V2", cx, ty, Graphics.HCENTER|Graphics.TOP);
            }

            // ---- Decorative separator lines (gold diamond) --------------
            int lineY = ty + 12;
            g.setColor(0x772200);
            g.drawLine(cx-76, lineY, cx-4,  lineY);
            g.drawLine(cx+4,  lineY, cx+76, lineY);
            g.setColor(0xFF5500);
            g.drawLine(cx-60, lineY, cx-10, lineY);
            g.drawLine(cx+10, lineY, cx+60, lineY);
            // Diamond centre
            g.setColor(0xFFAA00);
            g.drawLine(cx-3, lineY-3, cx,   lineY-6);
            g.drawLine(cx,   lineY-6, cx+3, lineY-3);
            g.drawLine(cx+3, lineY-3, cx,   lineY  );
            g.drawLine(cx,   lineY,   cx-3, lineY-3);

            // ---- PRESENTS text ------------------------------------------
            g.setColor(logoAlpha2 > 200 ? 0xCCCCCC : 0x445566);
            g.drawString("presents", cx, lineY + 5, Graphics.HCENTER|Graphics.TOP);

            // ---- Random glitch scan lines --------------------------------
            if (logoGlitch > 2) {
                g.setColor(0x3D0011);
                for (int gl = 0; gl < 7; gl++) {
                    int gly = cy - 85 + rand.nextInt(170);
                    g.drawLine(0, gly, SW, gly);
                }
            }
        }

        // ================================================================
        //  PHASE 3-4: Explosion burst starburst
        // ================================================================
        if (logoExplode > 0 && logoExplode < 20) {
            int ex  = logoExplode * 4;
            int baseY2 = cy + logoCharY;
            g.setColor(ex < 44 ? 0xFF4400 : 0x880000);
            g.drawArc(cx-ex, baseY2-ex, ex*2, ex*2, 0, 360);
            g.setColor(0xFF8800);
            g.drawArc(cx-ex/2, baseY2-ex/2, ex, ex, 0, 360);
            // 8-direction spark lines
            g.setColor(0xFFDD00);
            int sp2 = logoExplode * 3;
            g.drawLine(cx, baseY2, cx, baseY2-sp2);
            g.drawLine(cx, baseY2, cx+sp2, baseY2);
            g.drawLine(cx, baseY2, cx, baseY2+sp2);
            g.drawLine(cx, baseY2, cx-sp2, baseY2);
            g.drawLine(cx, baseY2, cx+sp2/2, baseY2-sp2/2);
            g.drawLine(cx, baseY2, cx-sp2/2, baseY2-sp2/2);
            g.drawLine(cx, baseY2, cx+sp2/2, baseY2+sp2/2);
            g.drawLine(cx, baseY2, cx-sp2/2, baseY2+sp2/2);
        }

        // ================================================================
        //  Phase-transition flash overlay
        // ================================================================
        if (logoFlash > 0) {
            int baseY3 = cy + logoCharY;
            g.setColor(logoFlash > 100 ? 0xFF2200 : 0x881100);
            int fr = 60 + logoFlash / 3;
            g.drawArc(cx-fr, baseY3-fr, fr*2, fr*2, 0, 360);
            g.drawArc(cx-fr-2, baseY3-fr-2, fr*2+4, fr*2+4, 0, 360);
        }

        // ================================================================
        //  PHASE 4: beat-pulse outer rings
        // ================================================================
        if (logoPhase == 4) {
            int baseY4 = cy;
            int pExtra = logoPulse / 8;
            g.setColor(0xFF2200);
            g.drawArc(cx-60-pExtra, baseY4-80-pExtra, 120+pExtra*2, 120+pExtra*2, 0, 360);
            g.setColor(0x881100);
            g.drawArc(cx-62-pExtra, baseY4-82-pExtra, 124+pExtra*2, 124+pExtra*2, 0, 360);
        }

        // ================================================================
        //  Blinking TAP TO SKIP hint
        // ================================================================
        if (logoPhase >= 3 && logoSubTimer < 800) {
            boolean show = ((logoSubTimer / 350) % 2 == 0);
            if (show) {
                g.setFont(fSmall);
                g.setColor(0x334444);
                g.drawString("TAP TO SKIP", cx, SH - 14, Graphics.HCENTER|Graphics.TOP);
            }
        }
    }

    // ==================================================================
    //  RENDER: Main menu (full animated)
    // ==================================================================
    private void renderMenu(Graphics g) {
        // Trigger BGM on first menu render
        if (simBgmTimer == 0) setPendingBGM(SoundManager.BGM_MENU);
        drawBg(g);
        drawRealmLayers(g);   // parallax realm silhouettes

        // Title
        int ty = 10;
        g.setColor(0x1A1A33); g.fillRect(0,ty-2,SW,66);
        drawPixelText(g, "STICK",  20, ty+2,  0xFF3333, 2);
        drawPixelText(g, "FIGHT",  22, ty+24, 0x3399FF, 2);
        g.setFont(fSmall);
        g.setColor(C_GOLD);
        g.drawString("ETERNAL REALMS", SW/2, ty+48, Graphics.HCENTER|Graphics.TOP);
        if (menuFlashOn) {
            g.setColor(C_GOLD);
            g.fillRect(4,ty+14,3,3); g.fillRect(SW-7,ty+14,3,3);
        }

        // Demo arena
        g.setColor(0x334455); g.fillRect(18,184,204,7);
        g.setColor(C_PLAT_TOP); g.drawLine(18,184,221,184);
        g.setColor(0x334455); g.fillRect(18,167,58,6);
        g.setColor(C_PLAT_TOP); g.drawLine(18,167,75,167);
        g.setColor(0x334455); g.fillRect(164,167,58,6);
        g.setColor(C_PLAT_TOP); g.drawLine(164,167,221,167);
        drawDemoStick(g,(int)menuLX,184,true, GameData.CHAR_COLOR[SaveData.selectedChar],menuFightState);
        drawDemoStick(g,(int)menuRX,184,false,GameData.CHAR_COLOR[1],(menuFightState+2)%4);
        g.setFont(fMedium); g.setColor(menuFlashOn?C_WHITE:C_GREY);
        g.drawString("VS", SW/2, 162, Graphics.HCENTER|Graphics.TOP);

        // Shard display
        g.setColor(C_GOLD); g.setFont(fSmall);
        g.drawString("SHARDS:" + SaveData.realmShards, SW-4, 2, Graphics.RIGHT|Graphics.TOP);

        // Selected char mini badge
        g.setColor(GameData.CHAR_COLOR[SaveData.selectedChar]);
        g.drawString(GameData.CHAR_NAME[SaveData.selectedChar], 4, 2, Graphics.LEFT|Graphics.TOP);

        // Menu items - animated scrolling
        // Rank badge top-left
        if (rankedWins > 0 || isRankedMode) {
            g.setColor(RANK_COLS[rankedRank]); g.setFont(fSmall);
            g.drawString(RANK_NAMES[rankedRank] + " " + rankedWins + "W",
                         4, 2, Graphics.LEFT|Graphics.TOP);
        }

        String[] items = {
            "STORY MODE",
            "VS AI",
            "SURVIVAL",
            "MELEE BRAWL",
            "RANKED MODE",
            "TOURNAMENT",
            "SHOP: BAZAAR",
            "SKILLS",
            "HOW TO PLAY",
            "SETTINGS"
        };
        int[] itemCols = {
            C_GOLD,
            GameData.CHAR_COLOR[0],
            C_CYAN,
            0xFF8800,
            RANK_COLS[rankedRank],
            0xFF44FF,
            C_ORANGE,
            C_PURPLE,
            C_GREY,
            0x6688AA
        };

        // Smooth scroll interpolation (no float math in render - use fixed-point * 100)
        int scrollInt = (int)(menuScrollOffset * 100);
        int targetInt = (int)(menuScrollTarget * 100);
        if (scrollInt < targetInt) scrollInt = Math.min(targetInt, scrollInt + 18);
        else if (scrollInt > targetInt) scrollInt = Math.max(targetInt, scrollInt - 18);
        menuScrollOffset = scrollInt / 100f;

        // Clip menu list area
        g.setClip(0, 194, SW, MENU_VISIBLE * 21 + 4);
        for (int i = 0; i < MENU_ITEMS; i++) {
            float renderedOffset = menuScrollOffset;
            int iy = 200 + (int)((i - renderedOffset) * 21);
            if (iy < 190 || iy > 194 + MENU_VISIBLE * 21) continue; // outside visible window
            boolean sel = (i == menuSel);
            if (sel) {
                g.setColor(0x151525); g.fillRoundRect(20, iy-2, SW-40, 17, 4, 4);
                g.setColor(C_YELLOW); g.drawRoundRect(20, iy-2, SW-40, 17, 4, 4);
                g.setColor(C_YELLOW);
            } else {
                g.setColor(itemCols[i]);
            }
            g.setFont(fSmall);
            g.drawString((sel ? "> " : "  ") + items[i], SW/2, iy, Graphics.HCENTER|Graphics.TOP);
        }
        g.setClip(0, 0, SW, SH); // restore clip

        // Scroll indicators
        if (menuScrollTarget > 0) {
            g.setColor(C_GOLD); g.setFont(fSmall);
            g.drawString("^", SW/2, 196, Graphics.HCENTER|Graphics.TOP);
        }
        if (menuScrollTarget < MENU_ITEMS - MENU_VISIBLE) {
            g.setColor(C_GOLD); g.setFont(fSmall);
            g.drawString("v", SW/2, 194 + MENU_VISIBLE*21, Graphics.HCENTER|Graphics.TOP);
        }

        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString("4/8:nav  5:sel  #:char  *:music", SW/2, SH-9, Graphics.HCENTER|Graphics.TOP);
        // Music status dot
        g.setColor(SaveData.musicEnabled ? C_GREEN : C_RED);
        g.fillArc(4, SH-9, 5, 5, 0, 360);
    }

    // Realm silhouette layers for menu background depth
    private void drawRealmLayers(Graphics g) {
        // Far layer: mountain silhouette
        g.setColor(0x0D0D20);
        int[] mxs = {0,20,45,70,90,115,140,160,185,210,240};
        int[] mys = {170,155,145,160,140,150,155,142,158,148,170};
        for (int i=0;i<mxs.length-1;i++) {
            g.drawLine(mxs[i],mys[i],mxs[i+1],mys[i+1]);
            g.fillRect(mxs[i],mys[i],mxs[i+1]-mxs[i]+1,SH-mys[i]);
        }
        // Lava glow at base of mountains (subtle)
        g.setColor(0x1A0800);
        g.fillRect(0,168,SW,4);
    }

    // ==================================================================
    //  RENDER: Character Select
    // ==================================================================
    private void renderCharSel(Graphics g) {
        drawBg(g);
        g.setColor(0x0A0A1E); g.fillRect(0,0,SW,SH);
        drawBg(g);

        g.setFont(fMedium); g.setColor(C_GOLD);
        g.drawString("SELECT FIGHTER", SW/2, 6, Graphics.HCENTER|Graphics.TOP);

        // Big character art (procedural pose)
        int cx=SW/2, cy=110;
        int cid=charSel;
        int cc=GameData.CHAR_COLOR[cid];
        drawCharBig(g, cx, cy, cid, cc);

        // Name + title
        g.setFont(fMedium); g.setColor(cc);
        g.drawString(GameData.CHAR_NAME[cid], SW/2, cy+52, Graphics.HCENTER|Graphics.TOP);
        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString(GameData.CHAR_TITLE[cid], SW/2, cy+68, Graphics.HCENTER|Graphics.TOP);

        // Lore
        g.setColor(C_WHITE);
        g.drawString(GameData.CHAR_LORE_A[cid], SW/2, cy+82, Graphics.HCENTER|Graphics.TOP);
        g.drawString(GameData.CHAR_LORE_B[cid], SW/2, cy+94, Graphics.HCENTER|Graphics.TOP);

        // Stats bar
        renderStatBars(g, cid, 8, cy+108);

        // Passive
        g.setFont(fSmall); g.setColor(C_CYAN);
        g.drawString("PASSIVE: "+GameData.CHAR_PASSIVE[cid], SW/2, cy+144, Graphics.HCENTER|Graphics.TOP);
        g.setColor(C_GREY);
        g.drawString(GameData.CHAR_PASSIVE_DESC[cid], SW/2, cy+156, Graphics.HCENTER|Graphics.TOP);

        // Special move
        g.setFont(fSmall);
        g.setColor(GameData.CHAR_SPECIAL_COLOR[cid]);
        g.drawString("SPECIAL: "+GameData.CHAR_SPECIAL_NAME[cid], SW/2, cy+168, Graphics.HCENTER|Graphics.TOP);
        g.setColor(C_GREY);
        g.drawString(GameData.CHAR_SPECIAL_DESC[cid], SW/2, cy+180, Graphics.HCENTER|Graphics.TOP);
        g.setColor(C_WHITE);
        g.drawString(GameData.SPECIAL_UNLOCK_TEXT, SW/2, cy+192, Graphics.HCENTER|Graphics.TOP);

        // Lock/unlock status - full panel
        boolean locked = !SaveData.isCharUnlocked(cid);
        boolean isBoss2 = GameData.CHAR_IS_BOSS[cid];
        if (locked) {
            // Dark overlay
            g.setColor(0x88000000 & 0x1A0000);
            g.fillRect(0, 60, SW, 140);
            // Lock icon
            g.setColor(C_RED);
            g.drawRect(SW/2-10, cy-5, 20, 18);
            g.fillArc(SW/2-7, cy-12, 14, 14, 0, 180);
            g.setColor(C_YELLOW);
            g.setFont(fMedium);
            g.drawString("LOCKED", SW/2, cy+15, Graphics.HCENTER|Graphics.TOP);
            g.setFont(fSmall); g.setColor(C_GREY);
            int cost = GameData.CHAR_UNLOCK_COST[cid];
            if (cost == 0) {
                g.setColor(C_CYAN);
                g.drawString("Beat story to unlock!", SW/2, cy+32, Graphics.HCENTER|Graphics.TOP);
            } else if (SaveData.realmShards >= cost) {
                g.setColor(C_GREEN);
                g.drawString("Press 5 to UNLOCK! (" + cost + " shards)", SW/2, cy+32, Graphics.HCENTER|Graphics.TOP);
            } else {
                g.setColor(C_RED);
                g.drawString("Need " + cost + " shards (" + SaveData.realmShards + " owned)",
                    SW/2, cy+32, Graphics.HCENTER|Graphics.TOP);
                int need = cost - SaveData.realmShards;
                g.setColor(C_GREY);
                g.drawString("Missing " + need + " shards - play more!", SW/2, cy+46, Graphics.HCENTER|Graphics.TOP);
            }
        } else {
            // Owned - show equipped weapon
            g.setFont(fSmall); g.setColor(C_GREEN);
            g.drawString("UNLOCKED", SW/2, cy+192, Graphics.HCENTER|Graphics.TOP);
            if (SaveData.equippedWeapon != null && cid < SaveData.equippedWeapon.length
                && SaveData.equippedWeapon[cid] >= 0) {
                int wi = SaveData.equippedWeapon[cid];
                if (wi < GameData.SHOP_NAME.length) {
                    g.setColor(C_GOLD);
                    g.drawString("EQ: " + GameData.SHOP_NAME[wi], SW/2, cy+204, Graphics.HCENTER|Graphics.TOP);
                }
            }
        }

        // Nav arrows
        g.setColor(C_YELLOW); g.setFont(fMedium);
        g.drawString("<", 8, cy, Graphics.LEFT|Graphics.TOP);
        g.drawString(">", SW-18, cy, Graphics.LEFT|Graphics.TOP);

        // Character dots
        for (int i=0;i<GameData.CHAR_COUNT;i++) {
            boolean sel=(i==charSel);
            boolean unl=SaveData.isCharUnlocked(i);
            g.setColor(sel ? C_YELLOW : (unl ? GameData.CHAR_COLOR[i] : C_GREY));
            g.fillArc(SW/2-GameData.CHAR_COUNT*5+i*10, SH-18, sel?7:5, sel?7:5, 0,360);
        }

        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString("4/6:browse  5:select  0:back", SW/2, SH-8, Graphics.HCENTER|Graphics.TOP);
    }

    private void drawCharBig(Graphics g, int cx, int cy, int cid, int col) {
        // Draw a larger ~40px tall version
        cid = Math.max(0, Math.min(cid, GameData.CHAR_COUNT - 1)); // bounds safety
        boolean isBoss = GameData.CHAR_IS_BOSS[cid];
        int scale = isBoss ? 2 : 1;

        g.setColor(0x0A0A22); g.fillArc(cx-20,cy+10,40,12,0,360); // shadow

        // Boss: add aura
        if (isBoss) {
            g.setColor(0x331100);
            g.drawArc(cx-30,cy-52,60,60,0,360);
            g.drawArc(cx-28,cy-50,56,56,0,360);
        }

        // Head
        g.setColor(0x0E0E22); g.fillArc(cx-12,cy-48,24,24,0,360);
        g.setColor(col);      g.drawArc(cx-12,cy-48,24,24,0,360);
        g.drawArc(cx-11,cy-47,22,22,0,360);
        // Eye
        g.setColor(C_WHITE); g.fillRect(cx+3,cy-38,4,4);
        g.setColor(0x000000); g.fillRect(cx+4,cy-37,2,3);
        // Body
        g.setColor(col);
        g.drawLine(cx,cy-24,cx,cy+4); g.drawLine(cx+1,cy-24,cx+1,cy+4);
        // Arms
        g.drawLine(cx,cy-18,cx+22,cy-14); g.drawLine(cx+1,cy-18,cx+23,cy-13);
        g.drawLine(cx,cy-18,cx-14,cy-10); g.drawLine(cx+1,cy-18,cx-13,cy-9);
        // Fist
        g.fillRect(cx+21,cy-16,5,5);
        // Legs
        g.drawLine(cx,cy+4,cx-12,cy+22); g.drawLine(cx+1,cy+4,cx-11,cy+22);
        g.drawLine(cx,cy+4,cx+12,cy+22); g.drawLine(cx+1,cy+4,cx+13,cy+22);
        g.setColor(darken(col));
        g.fillRect(cx-14,cy+21,12,4); g.fillRect(cx+10,cy+21,12,4);
    }

    private void renderStatBars(Graphics g, int cid, int x, int y) {
        String[] labels = { "HP  ", "SPD ", "DMG ", "DEF " };
        int[] vals = GameData.CHAR_STATS[cid];
        int[] barCols = { C_GREEN, C_CYAN, C_RED, C_GOLD };
        int bw = (SW - x*2 - 50) / 4;
        for (int i=0;i<4;i++) {
            int bx = x + i*(bw+4);
            g.setFont(fSmall); g.setColor(C_GREY);
            g.drawString(labels[i], bx, y, Graphics.LEFT|Graphics.TOP);
            // Bar bg
            g.setColor(0x111122); g.fillRect(bx, y+12, bw, 6);
            // Bar fill
            int fw = bw * vals[i] / 160; // 160 = max stat
            g.setColor(barCols[i]); g.fillRect(bx, y+12, Math.min(fw, bw), 6);
            g.setColor(0x334455); g.drawRect(bx, y+12, bw, 6);
            g.setFont(fSmall); g.setColor(C_WHITE);
            g.drawString(String.valueOf(vals[i]), bx+bw/2, y+20, Graphics.HCENTER|Graphics.TOP);
        }
    }

    // ==================================================================
    //  RENDER: Arena Select
    // ==================================================================
    private void renderArenaSel(Graphics g) {
        drawBg(g);
        g.setColor(0x080814); g.fillRect(0,0,SW,SH);
        drawBg(g);

        g.setFont(fMedium); g.setColor(C_YELLOW);
        g.drawString("SELECT ARENA", SW/2, 6, Graphics.HCENTER|Graphics.TOP);

        int aid = arenaSel;
        boolean locked = !SaveData.isArenaUnlocked(aid);

        // Arena mini-preview (draw platforms scaled down)
        drawArenaPreview(g, aid, 20, 32, 200, 90);

        // Arena name + subtitle
        g.setFont(fMedium); g.setColor(locked ? C_GREY : C_CYAN);
        String[] aNames={"THE RING","SKY BRIDGE","INFERNAL CRUCIBLE","FROZEN CITADEL",
            "NEO ARCADIA","JUNGLE RUINS OF ZHARA","SPIKE PIT","LAVA CAVE",
            "THUNDER PEAK","VOID SANCTUM"};
        String[] aSubs={"Classic battleground","Fight in the clouds","Volcanic battlefield",
            "Eternal ice fortress","Neon city warzone","Ancient spirit temple",
            "Death from below","The molten depths","Fight in the storm","Nothing is permanent"};
        String[] aLoreA={"Where legends begin.","One wrong step -","Forged by the Fire Titan.",
            "The Frost Queen's curse.","Gravity anomalies warp","Spirit beasts guard",
            "Ancient trap-maze","The molten heart","Lightning never rests.","Platforms fade."};
        String[] aLoreB={"Fight to claim your name.","you fall forever.",
            "Lava rewards the bold.","Slippery. Deadly.","combat. Arcadia burns.",
            "these ruins.","of the old war.","of the world.","Neither should you.","Trust nothing."};
        String[] aHaz={"None","Falling hazard","Lava rivers","Slippery ice",
            "Gravity shifts","Poison plants","Spike traps","Lava floor",
            "Wind storms","Void abyss"};
        String aidName=aid<aNames.length?aNames[aid]:"UNKNOWN";
        g.drawString(aidName, SW/2, 130, Graphics.HCENTER|Graphics.TOP);
        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString(aid<aSubs.length?aSubs[aid]:"", SW/2, 148, Graphics.HCENTER|Graphics.TOP);
        g.setColor(C_WHITE);
        g.drawString(aid<aLoreA.length?aLoreA[aid]:"", SW/2, 162, Graphics.HCENTER|Graphics.TOP);
        g.drawString(aid<aLoreB.length?aLoreB[aid]:"", SW/2, 174, Graphics.HCENTER|Graphics.TOP);
        // Show FX type for new arenas
        String fxLabel=""; int curFX=Arena.ARENA_FX[aid];
        if (curFX==Arena.FX_WIND) fxLabel=" [WIND]";
        else if (curFX==Arena.FX_DARKNESS) fxLabel=" [DARK]";
        else if (curFX==Arena.FX_QUAKE) fxLabel=" [QUAKE]";
        g.setFont(fSmall); g.setColor(C_ORANGE);
        g.drawString("HAZARD: "+(aid<aHaz.length?aHaz[aid]:"?")+fxLabel, SW/2, 188, Graphics.HCENTER|Graphics.TOP);
        int hc=aid<GameData.ARENA_HOME_CHAR.length?GameData.ARENA_HOME_CHAR[aid]:0;
        g.setColor(GameData.CHAR_COLOR[hc]);
        g.drawString("HOME: "+GameData.CHAR_NAME[hc]+" (+bonus)",SW/2,200,Graphics.HCENTER|Graphics.TOP);

        // Lock
        if (locked) {
            g.setColor(C_RED);
            int cost=GameData.ARENA_UNLOCK[aid];
            g.drawString("LOCKED  "+cost+" SHARDS", SW/2, 214, Graphics.HCENTER|Graphics.TOP);
            // Buy option
            if (SaveData.realmShards >= cost) {
                g.setColor(C_YELLOW);
                g.drawString("[5] UNLOCK NOW", SW/2, 226, Graphics.HCENTER|Graphics.TOP);
            }
        }

        // Dots
        for (int i=0;i<Arena.ARENA_COUNT;i++) {
            boolean sel=(i==arenaSel);
            boolean unl=SaveData.isArenaUnlocked(i);
            g.setColor(sel ? C_YELLOW : (unl ? C_CYAN : C_GREY));
            g.fillArc(SW/2-GameData.ARENA_COUNT*5+i*10, SH-28, sel?7:5, sel?7:5, 0,360);
        }

        // Nav
        g.setColor(C_YELLOW); g.setFont(fMedium);
        g.drawString("<", 8, 100, Graphics.LEFT|Graphics.TOP);
        g.drawString(">", SW-18, 100, Graphics.LEFT|Graphics.TOP);

        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString("4/6:browse  5:fight  *:lore  0:back", SW/2, SH-10, Graphics.HCENTER|Graphics.TOP);
    }

    private void drawArenaPreview(Graphics g, int aid, int bx, int by, int bw, int bh) {
        // Scaled bounding box
        float sx=(float)bw/Arena.W, sy=(float)bh/Arena.H;
        // Background
        g.setColor(0x070712); g.fillRect(bx,by,bw,bh);
        g.setColor(0x334455); g.drawRect(bx,by,bw,bh);
        // Platforms from the correct set
        int[][][] allP = {Arena.P0,Arena.P1,Arena.P2,Arena.P3,Arena.P4,Arena.P5,Arena.P6,Arena.P7,Arena.P8,Arena.P9};
        int[][] plats = allP[aid];
        for (int i=0;i<plats.length;i++) {
            int px=(int)(plats[i][0]*sx)+bx;
            int py=(int)(plats[i][1]*sy)+by;
            int pw=Math.max(2,(int)(plats[i][2]*sx));
            int ph=Math.max(2,(int)(plats[i][3]*sy));
            int pt=plats[i][4];
            if      (pt==Arena.PT_LAVA)    g.setColor(C_LAVA_A);
            else if (pt==Arena.PT_SPIKE)   g.setColor(C_SPIKE);
            else if (pt==Arena.PT_ICE)     g.setColor(C_ICE_A);
            else if (pt==Arena.PT_BREAK)   g.setColor(C_BREAK_OK);
            else if (pt==Arena.PT_BOUNCE)  g.setColor(0x00AA44);
            else if (pt==Arena.PT_CRUMBLE) g.setColor(0xAA5500);
            else                           g.setColor(C_PLAT);
            g.fillRect(px,py,pw,ph);
            if (pt==Arena.PT_SOLID||pt==Arena.PT_ICE||pt==Arena.PT_BREAK) {
                g.setColor(C_PLAT_TOP); g.drawLine(px,py,px+pw-1,py);
            }
        }
        // Spawn dots
        int[] sp = {Arena.P0[0][0],Arena.P0[0][1]};
        try {
            int[][][] allSP={null,null,null,null,null,null,null,null};
            // Use static spawn arrays directly
            int[] s0=Arena.SP0,s1=Arena.SP1,s2=Arena.SP2,s3=Arena.SP3;
            int[] s4=Arena.SP4,s5=Arena.SP5,s6=Arena.SP6,s7=Arena.SP7;
            int[] s8=Arena.SP8,s9=Arena.SP9;
            int[][] allSpawns={s0,s1,s2,s3,s4,s5,s6,s7,s8,s9};
            int[] spn=allSpawns[aid];
            g.setColor(GameData.CHAR_COLOR[playerCharId]);
            g.fillArc((int)(spn[0]*sx)+bx-3,(int)(spn[1]*sy)+by-3,6,6,0,360);
            g.setColor(GameData.CHAR_COLOR[enemyCharId]);
            g.fillArc((int)(spn[2]*sx)+bx-3,(int)(spn[3]*sy)+by-3,6,6,0,360);
        } catch(Exception e2) {}
    }

    // ==================================================================
    //  RENDER: Lore screen
    // ==================================================================
    private void renderLoreScreen(Graphics g) {
        drawBg(g);
        g.setColor(0x060610); g.fillRect(0,0,SW,SH);
        drawBg(g);

        // Show either arena or char lore
        int idx = lorePage % (Arena.ARENA_COUNT + GameData.CHAR_COUNT);
        String title, sub, loreA, loreB, hazard;
        int col;
        if (idx < 8) {
            title  = GameData.ARENA_NAME[idx];
            sub    = GameData.ARENA_SUBTITLE[idx];
            loreA  = GameData.ARENA_LORE_A[idx];
            loreB  = GameData.ARENA_LORE_B[idx];
            hazard = "HAZARD: "+GameData.ARENA_HAZARD[idx];
            col    = C_CYAN;
        } else if (idx==8) {
            title="THUNDER PEAK"; sub="Fight in the storm";
            loreA="Lightning never rests."; loreB="Neither should you.";
            hazard="HAZARD: Wind storms"; col=0x4499FF;
        } else if (idx==9) {
            title="VOID SANCTUM"; sub="Nothing is permanent";
            loreA="Platforms fade. Fighters fall."; loreB="Trust nothing here.";
            hazard="HAZARD: Void abyss"; col=0x9966DD;
        } else {
            int ci=idx-GameData.ARENA_COUNT;
            title  = GameData.CHAR_NAME[ci];
            sub    = GameData.CHAR_TITLE[ci];
            loreA  = GameData.CHAR_LORE_A[ci];
            loreB  = GameData.CHAR_LORE_B[ci];
            hazard = "PASSIVE: "+GameData.CHAR_PASSIVE[ci];
            col    = GameData.CHAR_COLOR[ci];
        }

        g.setFont(fLarge); g.setColor(col);
        g.drawString(title, SW/2, 20, Graphics.HCENTER|Graphics.TOP);
        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString(sub,   SW/2, 52, Graphics.HCENTER|Graphics.TOP);
        g.setColor(C_WHITE);
        g.drawString(loreA, SW/2, 80, Graphics.HCENTER|Graphics.TOP);
        g.drawString(loreB, SW/2, 96, Graphics.HCENTER|Graphics.TOP);
        g.setFont(fSmall); g.setColor(C_ORANGE);
        g.drawString(hazard,SW/2,116, Graphics.HCENTER|Graphics.TOP);

        // Decorative separator
        g.setColor(0x223355);
        g.drawLine(20,136,SW-20,136);

        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString("Page "+(lorePage+1)+"/"+(Arena.ARENA_COUNT+GameData.CHAR_COUNT),
            SW/2,146, Graphics.HCENTER|Graphics.TOP);
        g.drawString("4/6:page  0:back", SW/2, SH-10, Graphics.HCENTER|Graphics.TOP);
    }

    // ==================================================================
    //  RENDER: Shop - Eternal Bazaar
    // ==================================================================
    private void renderShop(Graphics g) {
        drawBg(g);
        g.setColor(0x08080E); g.fillRect(0,0,SW,SH);
        drawBg(g);

        // Header
        g.setFont(fMedium); g.setColor(C_GOLD);
        g.drawString("ETERNAL BAZAAR", SW/2, 4, Graphics.HCENTER|Graphics.TOP);
        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString("Merchants outside of time", SW/2, 22, Graphics.HCENTER|Graphics.TOP);

        // Shard balance
        g.setColor(C_GOLD);
        g.drawString("SHARDS: "+SaveData.realmShards, SW-4, 4, Graphics.RIGHT|Graphics.TOP);

        // Tab bar
        String[] tabs = { "WEAPONS","ARMOR","COSMETIC","POWERUP" };
        int[] tabCols = { C_ORANGE, C_CYAN, C_PURPLE, C_GREEN };
        for (int t=0;t<4;t++) {
            int tx=4+t*59;
            boolean sel=(t==shopTab);
            g.setColor(sel?tabCols[t]:0x222233);
            g.fillRect(tx,36,57,13);
            g.setColor(sel?C_WHITE:C_GREY);
            g.setFont(fSmall);
            g.drawString(tabs[t], tx+28, 37, Graphics.HCENTER|Graphics.TOP);
        }

        // Item list in this tab
        int startIdx=0;
        for (int t=0;t<shopTab;t++) startIdx+=countItemsInTab(t);
        int tabCount=countItemsInTab(shopTab);

        int visStart = Math.max(0, shopSel-(shopSel%4));
        int drawY = 56;
        for (int rel=0;rel<4&&visStart+rel<tabCount;rel++) {
            int idx=startIdx+visStart+rel;
            boolean sel=(visStart+rel==shopSel);
            boolean owned=SaveData.isShopItemOwned(idx);
            int rar=GameData.SHOP_RARITY[idx];
            int rcol=GameData.RARITY_COLOR[rar];

            // Row bg
            g.setColor(sel?0x1A1A2E:0x0D0D1E);
            g.fillRect(4,drawY,SW-8,22);
            if (sel) { g.setColor(rcol); g.drawRect(4,drawY,SW-8,22); }

            // Icon placeholder
            g.setColor(rcol); g.fillRect(6,drawY+3,16,16);
            g.setColor(0x000000); g.drawString(GameData.rarityLabel(rar).substring(0,1), 14,drawY+4, Graphics.HCENTER|Graphics.TOP);

            // Name
            g.setFont(fSmall);
            g.setColor(owned?C_GREY:rcol);
            g.drawString(GameData.SHOP_NAME[idx], 26, drawY+3, Graphics.LEFT|Graphics.TOP);

            // Cost or OWNED
            if (owned) {
                int cid2 = SaveData.selectedChar;
                boolean equipped = false;
                if (GameData.SHOP_CATEGORY[idx] == GameData.SHOP_WEAPON) {
                    equipped = (SaveData.equippedWeapon != null && cid2 < SaveData.equippedWeapon.length
                                && SaveData.equippedWeapon[cid2] == idx);
                } else if (GameData.SHOP_CATEGORY[idx] == GameData.SHOP_ARMOR ||
                           GameData.SHOP_CATEGORY[idx] == GameData.SHOP_COSMETIC) {
                    equipped = (SaveData.activeSkin == idx);
                }
                g.setColor(equipped ? C_YELLOW : C_GREEN);
                g.drawString(equipped ? "[EQP]" : "OWNED", SW-6, drawY+3, Graphics.RIGHT|Graphics.TOP);
            } else {
                g.setColor(SaveData.realmShards>=GameData.SHOP_COST[idx]?C_GOLD:C_RED);
                g.drawString(GameData.SHOP_COST[idx]+"S", SW-6,drawY+3, Graphics.RIGHT|Graphics.TOP);
            }

            // Rarity label
            g.setColor(rcol); g.setFont(fSmall);
            g.drawString(GameData.rarityLabel(rar), 26, drawY+13, Graphics.LEFT|Graphics.TOP);

            drawY += 24;
        }

        // Item description for selected
        if (shopSel < tabCount) {
            int si=startIdx+shopSel;
            g.setColor(0x0A0A18); g.fillRect(4,152,SW-8,50);
            g.setColor(0x334455); g.drawRect(4,152,SW-8,50);
            g.setFont(fSmall); g.setColor(C_WHITE);
            g.drawString(GameData.SHOP_NAME[si], SW/2,155, Graphics.HCENTER|Graphics.TOP);
            g.setColor(C_GREY);
            g.drawString(GameData.SHOP_DESC_A[si], SW/2,168, Graphics.HCENTER|Graphics.TOP);
        }

        // Scroll indicator
        if (tabCount > 4) {
            g.setColor(C_GREY); g.setFont(fSmall);
            g.drawString((shopSel+1)+"/"+tabCount, SW-4,56, Graphics.RIGHT|Graphics.TOP);
        }

        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString("2/8:item 5:buy/equip 4/6:tab 0:back", SW/2, SH-10, Graphics.HCENTER|Graphics.TOP);
    }

    private int countItemsInTab(int tab) {
        int cnt=0;
        for (int i=0;i<GameData.SHOP_ITEM_COUNT;i++) if (GameData.SHOP_CATEGORY[i]==tab) cnt++;
        return cnt;
    }

    private void doShopBuy() {
        int startIdx=0;
        for (int t=0;t<shopTab;t++) startIdx+=countItemsInTab(t);
        int idx=startIdx+shopSel;
        if (idx>=GameData.SHOP_ITEM_COUNT) return;

        boolean owned = SaveData.isShopItemOwned(idx);
        int cat = GameData.SHOP_CATEGORY[idx];
        int cid = SaveData.selectedChar;

        // WEAPON tab (0): if owned, toggle equip/unequip
        if (cat == GameData.SHOP_WEAPON && owned) {
            int weaponSlot = idx; // slot 0-7 in shop
            int curEquipped = (SaveData.equippedWeapon != null && cid < SaveData.equippedWeapon.length)
                              ? SaveData.equippedWeapon[cid] : -1;
            if (curEquipped == weaponSlot) {
                // Unequip
                SaveData.unequipWeapon(cid);
                SaveData.save();
                showBigMsg("UNEQUIPPED!", C_GREY);
            } else {
                // Equip
                SaveData.equipWeapon(cid, weaponSlot);
                SaveData.save();
                showBigMsg("EQUIPPED!", C_GREEN);
            }
            return;
        }

        // ARMOR tab (1): if owned, toggle equip/unequip for armor items
        if (cat == GameData.SHOP_ARMOR && owned) {
            // Use equippedWeapon slot 8+ or a separate flag via activeSkin
            // For armor: slot 8-11 mapped to charId armor equipped flag
            // Toggle: if this armor is currently activeSkin, unequip; else equip
            int armorSlot = idx; // global idx
            if (SaveData.activeSkin == armorSlot) {
                SaveData.activeSkin = -1; // unequip
                SaveData.save();
                showBigMsg("ARMOR UNEQUIPPED", C_GREY);
            } else {
                SaveData.activeSkin = armorSlot;
                SaveData.save();
                showBigMsg("ARMOR EQUIPPED!", C_CYAN);
            }
            return;
        }

        // POWERUP: can re-buy to stock up
        if (cat == GameData.SHOP_POWERUP) {
            int cost=GameData.SHOP_COST[idx];
            if (!SaveData.spendShards(cost)) { showBigMsg("NEED MORE SHARDS!", C_RED); return; }
            if (!owned) SaveData.buyShopItem(idx);
            int pidx = idx - startIdx;
            if (pidx>=0 && pidx<4) SaveData.powerupCount[pidx]++;
            SaveData.save();
            showBigMsg("PURCHASED!", C_GOLD);
            return;
        }

        // COSMETIC: if owned, toggle
        if (cat == GameData.SHOP_COSMETIC && owned) {
            if (SaveData.activeSkin == idx) {
                SaveData.activeSkin = -1;
                showBigMsg("SKIN REMOVED", C_GREY);
            } else {
                SaveData.activeSkin = idx;
                showBigMsg("SKIN APPLIED!", C_PURPLE);
            }
            SaveData.save();
            return;
        }

        // Not owned: buy
        if (owned) { showBigMsg("PRESS 5:EQUIP/UNEQUIP", C_GREY); return; }
        int cost=GameData.SHOP_COST[idx];
        if (!SaveData.spendShards(cost)) { showBigMsg("NEED MORE SHARDS!", C_RED); return; }
        SaveData.buyShopItem(idx);
        SaveData.save();
        simSound(SSND_MENU);
        showBigMsg("PURCHASED!", C_GOLD);
    }

    // ==================================================================
    //  RENDER: Skill tree
    // ==================================================================
    private void renderSkills(Graphics g) {
        drawBg(g);
        g.setColor(0x070710); g.fillRect(0,0,SW,SH);
        drawBg(g);

        int cid=SaveData.selectedChar;
        int cc=GameData.CHAR_COLOR[cid];

        g.setFont(fMedium); g.setColor(cc);
        g.drawString("SKILL TREE", SW/2, 6, Graphics.HCENTER|Graphics.TOP);
        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString(GameData.CHAR_NAME[cid]+" - "+GameData.CHAR_TITLE[cid],
            SW/2, 24, Graphics.HCENTER|Graphics.TOP);

        g.setColor(C_GOLD); g.setFont(fSmall);
        g.drawString("SHARDS: "+SaveData.realmShards, SW-4, 6, Graphics.RIGHT|Graphics.TOP);

        for (int i=0;i<GameData.SK_COUNT;i++) {
            int sy=46+i*38;
            boolean sel=(i==skillSel);
            boolean unl=SaveData.isSkillUnlocked(cid,i);
            int cost=GameData.SK_COST[i];
            boolean canBuy=(!unl && SaveData.realmShards>=cost);

            // Row
            g.setColor(sel?0x1A1A30:0x0D0D1C); g.fillRect(6,sy,SW-12,34);
            if (sel) { g.setColor(cc); g.drawRect(6,sy,SW-12,34); }

            // Skill icon (simple shape)
            g.setColor(unl?cc:C_GREY); g.fillArc(12,sy+7,20,20,0,360);
            g.setColor(0x000000);
            g.drawString(String.valueOf(i+1), 22,sy+10, Graphics.HCENTER|Graphics.TOP);

            // Name
            g.setFont(fMedium); g.setColor(unl?cc:(canBuy?C_WHITE:C_GREY));
            g.drawString(GameData.SK_NAME[i], 38, sy+4, Graphics.LEFT|Graphics.TOP);

            // Desc
            g.setFont(fSmall); g.setColor(C_GREY);
            g.drawString(GameData.SK_DESC[i], 38, sy+18, Graphics.LEFT|Graphics.TOP);

            // Status
            if (unl) {
                g.setColor(C_GREEN); g.drawString("UNLOCKED", SW-8,sy+4, Graphics.RIGHT|Graphics.TOP);
            } else {
                g.setColor(canBuy?C_GOLD:C_RED);
                g.drawString(cost+"S", SW-8,sy+4, Graphics.RIGHT|Graphics.TOP);
            }
        }

        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString("2/8:skill  5:unlock  0:back", SW/2, SH-10, Graphics.HCENTER|Graphics.TOP);
    }

    private void doUnlockSkill() {
        int cid=SaveData.selectedChar;
        int sid=skillSel;
        if (SaveData.isSkillUnlocked(cid,sid)) { showBigMsg("ALREADY UNLOCKED",C_GREY); return; }
        if (!SaveData.spendShards(GameData.SK_COST[sid])) { showBigMsg("NEED MORE SHARDS!",C_RED); return; }
        SaveData.unlockSkill(cid,sid);
        SaveData.save();
        showBigMsg(GameData.SK_NAME[sid]+" UNLOCKED!",C_GOLD);
    }

    // ==================================================================
    //  RENDER: Story intro scroll
    // ==================================================================
    private void renderStoryIntro(Graphics g) {
        int ch = Math.min(storyChapter, GameData.CHAPTER_COUNT - 1);
        boolean isVoid = (ch == 4); // Chapter 5: Beyond the Void Gate

        // ---- BACKGROUND ----
        if (isVoid) {
            // Void chapter: animated dark vortex
            g.setColor(0x000000); g.fillRect(0, 0, SW, SH);
            // Pulsing void rings (use scrollY as animation counter)
            int vt = (int)(System.currentTimeMillis() % 4800L / 40);
            for (int r = 0; r < 8; r++) {
                int rad = ((vt + r * 15) % 120) * 2;
                int alpha = 255 - rad * 2;
                if (alpha < 0) alpha = 0;
                int col = 0x440066 | ((alpha / 4) << 16);
                g.setColor(col & 0xFFFFFF);
                if (rad > 4) {
                    g.drawArc(SW/2 - rad, SH/2 - rad - 40, rad*2, rad*2, 0, 360);
                }
            }
            // Void particles streaming in
            for (int p = 0; p < 16; p++) {
                int px = (p * 37 + vt * 3) % SW;
                int py = (p * 23 + vt * 2) % SH;
                g.setColor(0x9933FF);
                g.fillRect(px, py, 2, 2);
            }
            // Dark scanlines
            g.setColor(0x08000F);
            for (int sl = 0; sl < SH; sl += 4) g.drawLine(0, sl, SW, sl);
        } else {
            // Normal chapters
            int[] bgCols = { 0x1A0800, 0x0F0300, 0x000818, 0x060018 };
            g.setColor(bgCols[ch % bgCols.length]); g.fillRect(0, 0, SW, SH);
            drawBg(g);
            // Atmospheric lines
            int[] atCols = { 0xFF4400, 0xFF2200, 0x0044FF, 0x0000FF };
            g.setColor(atCols[ch % atCols.length] & 0x0F0F0F);
            for (int yl = 0; yl < SH; yl += 18) g.drawLine(0, yl, SW, yl);
        }

        // ---- TOP BANNER ----
        // Clamp topY so boss panel (topY+130+80) stays within screen height
        int topY = Math.max(8, Math.min(scrollY, SH - 220));
        // Chapter number pill
        g.setColor(isVoid ? 0x330044 : 0x0A1020);
        g.fillRoundRect(SW/2 - 60, topY - 2, 120, 20, 6, 6);
        g.setFont(fSmall);
        g.setColor(isVoid ? 0xCC44FF : C_GOLD);
        g.drawString("CHAPTER " + (ch + 1) + " OF " + GameData.CHAPTER_COUNT,
                     SW/2, topY, Graphics.HCENTER|Graphics.TOP);

        // Chapter title - glitch effect for void chapter
        g.setFont(fMedium);
        String chName = ch < GameData.CHAPTER_NAME.length ? GameData.CHAPTER_NAME[ch] : "???";
        if (isVoid && ((System.currentTimeMillis() % 800L) < 200L)) {
            // Glitch: draw offset in cyan/magenta then white
            g.setColor(0x00FFFF); g.drawString(chName, SW/2 + 2, topY + 22, Graphics.HCENTER|Graphics.TOP);
            g.setColor(0xFF00FF); g.drawString(chName, SW/2 - 2, topY + 22, Graphics.HCENTER|Graphics.TOP);
        }
        g.setColor(isVoid ? 0xDD88FF : C_WHITE);
        g.drawString(chName, SW/2, topY + 22, Graphics.HCENTER|Graphics.TOP);

        // ---- DIALOGUE LINES ----
        String[] introLines = (ch < STORY_DIALOGUE.length) ? STORY_DIALOGUE[ch] : new String[0];
        int lineY = topY + 52;
        int shown = 0;
        // Show up to 6 lines, animate them scrolling in
        int startLine = Math.max(0, scrollY < 80 ? 0 : (scrollY - 80) / 12);
        for (int i = startLine; i < introLines.length && shown < 5; i++) {
            String rawL = introLines[i];
            int colon = rawL.indexOf(':');
            boolean isNarr = colon < 0 || rawL.startsWith("NARRATOR");
            g.setFont(fSmall);
            int lineCol;
            if (isNarr) {
                lineCol = isVoid ? 0xCC88FF : C_GOLD;
            } else {
                String spk = colon > 0 ? rawL.substring(0, Math.min(colon, rawL.length())).trim() : "";
                lineCol = getSpeakerColor(spk, ch);
                if (isVoid) lineCol = (lineCol | 0x440044);
            }
            g.setColor(lineCol);
            // Clamp display length to fit 240px screen
            String disp = rawL.length() > 28 ? rawL.substring(0, 28) : rawL;
            g.drawString(disp, 4, lineY + shown * 14, Graphics.LEFT|Graphics.TOP);
            shown++;
        }

        // ---- VS BOSS PANEL ----
        int bossId = (ch < GameData.CHAPTER_BOSS_CHAR.length)
                     ? GameData.CHAPTER_BOSS_CHAR[ch] : GameData.CH_SHADOW;
        int panelY = Math.min(topY + 130, SH - 90);
        // Panel background
        g.setColor(isVoid ? 0x1A0033 : 0x0A0A1A);
        g.fillRoundRect(16, panelY, SW - 32, 80, 8, 8);
        g.setColor(isVoid ? 0xAA44FF : C_RED);
        g.drawRoundRect(16, panelY, SW - 32, 80, 8, 8);

        // VS label
        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString("YOUR OPPONENT:", SW/2, panelY + 4, Graphics.HCENTER|Graphics.TOP);

        // Boss stickman portrait (left side of panel)
        int bossCol = GameData.CHAR_COLOR[bossId];
        g.setColor(bossCol);
        drawCharBig(g, 48, panelY + 68, bossId, bossCol);

        // Boss name + title (right side)
        g.setFont(fMedium);
        g.setColor(isVoid ? 0xFF88FF : bossCol);
        g.drawString(GameData.CHAR_NAME[bossId], SW/2 + 10, panelY + 18, Graphics.LEFT|Graphics.TOP);
        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString(GameData.CHAR_TITLE[bossId], SW/2 + 10, panelY + 36, Graphics.LEFT|Graphics.TOP);
        // Boss passive
        g.setColor(C_CYAN);
        g.drawString("PWR: " + GameData.CHAR_PASSIVE[bossId], SW/2 + 10, panelY + 50, Graphics.LEFT|Graphics.TOP);
        // Boss stats mini bar
        int spd = GameData.CHAR_STATS[bossId][1];
        int hp  = GameData.CHAR_STATS[bossId][0];
        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString("HP:" + hp + " SPD:" + spd, SW/2 + 10, panelY + 64, Graphics.LEFT|Graphics.TOP);

        // ---- ANIMATED FLASH BORDER for void chapter ----
        if (isVoid) {
            boolean msFlash = (System.currentTimeMillis() % 600L) < 300L;
            if (msFlash) {
                g.setColor(0x6600AA);
                g.drawRect(0, 0, SW - 1, SH - 1);
                g.drawRect(1, 1, SW - 3, SH - 3);
            }
        }

        // ---- BOTTOM HINT ----
        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString("[5] START FIGHT   [0] BACK", SW/2, SH - 10, Graphics.HCENTER|Graphics.TOP);
    }

    // ==================================================================
    //  RENDER: Shard Reward screen
    // ==================================================================
    private void renderShardReward(Graphics g) {
        g.setColor(C_BG); g.fillRect(0,0,SW,SH);
        drawBg(g);

        g.setColor(0x0A1020); g.fillRoundRect(20,60,SW-40,200,10,10);
        g.setColor(C_GOLD);   g.drawRoundRect(20,60,SW-40,200,10,10);

        g.setFont(fLarge); g.setColor(C_GOLD);
        g.drawString("VICTORY!", SW/2, 72, Graphics.HCENTER|Graphics.TOP);

        g.setFont(fMedium); g.setColor(C_WHITE);
        g.drawString("REALM SHARDS EARNED", SW/2, 112, Graphics.HCENTER|Graphics.TOP);

        // Animated shard count
        g.setFont(fLarge); g.setColor(C_YELLOW);
        g.drawString("+"+pendingShards, SW/2, 132, Graphics.HCENTER|Graphics.TOP);

        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString("Total: "+SaveData.realmShards+" shards", SW/2, 168, Graphics.HCENTER|Graphics.TOP);

        // Unlock tip if close
        g.setColor(C_CYAN);
        g.drawString("Visit SHOP to upgrade!", SW/2, 186, Graphics.HCENTER|Graphics.TOP);

        // Final score
        g.setFont(fMedium); g.setColor(C_WHITE);
        g.drawString(playerScore+" - "+enemyScore, SW/2, 208, Graphics.HCENTER|Graphics.TOP);

        g.setFont(fSmall); g.setColor(C_YELLOW);
        g.drawString("[5] PLAY AGAIN   [0] MENU", SW/2, 238, Graphics.HCENTER|Graphics.TOP);
    }

    // ==================================================================
    //  RENDER: Game world
    // ==================================================================
    private void renderGame(Graphics g) {
        drawBg(g);

        // ---- Apply camera translation ????????????????????????????????
        int icx = (int) camX;
        int icy = (int) camY;
        g.translate(-icx, -icy);

        // ?? v7: Animated arena background (behind platforms) ??????????
        renderArenaBg(g);

        // ?? v5: Parallax background layers ????????????????????????????
        renderParallaxBg(g, camX, camY);

        // Platforms + character shadows + weapons with glow
        renderPlatforms(g);

        // ?? v6: Weapon glow aura under weapon icons ????????????????????
        renderWeaponGlow(g);
        renderGW(g);

        // ?? v6: Ground shadow under characters ????????????????????????
        renderCharShadow(g, player);
        renderCharShadow(g, enemy);

        renderParticles(g);

        // ?? v6: Ragdoll tumbling body parts ???????????????????????????
        renderRagdoll(g);

        renderBullets(g);

        // Characters
        if (player!=null) player.draw(g, GameData.CHAR_COLOR[playerCharId]);
        if (enemy !=null) enemy .draw(g, GameData.CHAR_COLOR[enemy.charId]);

        // ?? v6: Hit flash rings around damaged characters ??????????????
        renderHitFlash(g, player, true);
        renderHitFlash(g, enemy,  false);

        // ?? v5: Air-dash afterimage trail ?????????????????????????????
        renderDashTrail(g);

        // ?? v7: Score crown above winning character ????????????????????
        renderScoreCrown(g);

        renderSticktension(g);

        // ?? v5: Floating damage numbers (world space) ?????????????????
        renderDamageNumbers(g);

        // ?? v6: Charge attack ring around player ??????????????????????
        renderChargeIndicator(g);

        // ?? v5: Weather particles ?????????????????????????????????????
        renderWeather(g);

        // ?? v6: Confetti (round win) ??????????????????????????????????
        renderConfetti(g);

        // ?? v7: Kill cam zoom post-process (applied via translate trick)
        if (killCamTimer > 0 && killCamZoom > 1f) {
            // Draw zoom indicator outline
            g.setColor(0x330000);
            g.drawRect((int)(camX + 2), (int)(camY + 2),
                       SW - 4, SH - 4);
        }

        // ---- Restore to screen space ?????????????????????????????????
        g.translate(icx, icy);

        // ?? v7: Arena name splash (full-screen, drawn in screen space) ?
        renderArenaSplash(g);

        renderHUD(g);
        // Danger zone flash: red screen border when player on hazard
        renderDangerFlash(g);
        renderKillFeed(g);
        renderBigMsg(g);
        renderSimSound(g);
        renderGravityIndicator(g);
        renderWindIndicator(g);
        renderPowerupHUD(g);

        // ?? v5: Minimap corner ????????????????????????????????????????
        renderMinimap(g);

        // ?? v5: Stamina bar ????????????????????????????????????????????
        renderStaminaBar(g);

        // ?? v5: Level badge ????????????????????????????????????????????
        renderLevelBadge(g);

        // ?? v6: Rank badge (ranked mode only) ?????????????????????????
        renderRankBadge(g);

        // ?? v5: Off-screen enemy arrow ?????????????????????????????????
        renderEnemyArrow(g);

        // ?? v7: Daily challenge tracker ???????????????????????????????
        renderChallengeTracker(g);

        // ?? v5: Achievement popup toast ???????????????????????????????
        renderAchievementPopup(g);

        // ?? v5: Tooltip ???????????????????????????????????????????????
        renderTooltip(g);

        // Kill streak display
        if (killStreak >= 2 && killStreakTimer > 0) {
            g.setFont(fSmall);
            int[] scols = { 0,0, C_CYAN, C_ORANGE, C_PURPLE, C_GOLD, C_GOLD, C_GOLD };
            int sc = killStreak < scols.length ? scols[killStreak] : C_GOLD;
            g.setColor(sc);
            g.drawString(killStreak + " KILL STREAK", SW - 4, 18, Graphics.RIGHT|Graphics.TOP);
        }
        // Low-HP vignette (red border pulse)
        if (player != null && player.health > 0 && player.health <= player.maxHealth / 4) {
            boolean hbPulse = (heartbeatTimer > 400);
            g.setColor(hbPulse ? 0x550000 : 0x330000);
            g.drawRect(0,0,SW-1,SH-1);
            g.drawRect(1,1,SW-3,SH-3);
            g.drawRect(2,2,SW-5,SH-5);
        }
        // Sudden death red flash border
        if (suddenDeathActive) {
            boolean sdFlash = (System.currentTimeMillis() % 800L) < 200L;
            if (sdFlash) {
                g.setColor(0xFF0000);
                g.drawRect(0,0,SW-1,SH-1); g.drawRect(1,1,SW-3,SH-3);
            }
            g.setFont(fSmall); g.setColor(0xFF2200);
            g.drawString("SUDDEN DEATH", SW/2, 30, Graphics.HCENTER|Graphics.TOP);
        }
        // Slo-mo indicator
        if (slomoDur > 0) {
            g.setFont(fSmall); g.setColor(C_CYAN);
            g.drawString("ULTRA SLOW", SW/2, SH/2 - 40, Graphics.HCENTER|Graphics.TOP);
        }
        // Combo counter
        if (hudComboCount >= 3 && hudComboTimer > 0) {
            g.setFont(fMedium);
            boolean flash = (hudComboTimer / 200) % 2 == 0;
            g.setColor(flash ? C_GOLD : C_ORANGE);
            g.drawString(hudComboCount + "x COMBO!", SW/2, SH/2 - 60, Graphics.HCENTER|Graphics.TOP);
        }
        // Unlock notification banner
        if (unlockNotifTimer > 0 && unlockNotifMsg.length() > 0) {
            g.setColor(0x0A0818); g.fillRoundRect(20, 220, SW-40, 24, 6, 6);
            g.setColor(C_GOLD);   g.drawRoundRect(20, 220, SW-40, 24, 6, 6);
            g.setFont(fSmall);    g.setColor(C_GOLD);
            g.drawString("** " + unlockNotifMsg + " **", SW/2, 224, Graphics.HCENTER|Graphics.TOP);
        }
    }

    private void drawBg(Graphics g) {
        g.setColor(C_BG);  g.fillRect(0,0,SW,SH/2);
        g.setColor(C_BG2); g.fillRect(0,SH/2,SW,SH/2);
        for (int i=0;i<STAR_COUNT;i++) {
            g.setColor(starB[i]==2?0x445588:(starB[i]==1?0x7799BB:0x223355));
            g.fillRect(starX[i],starY[i],starB[i]==2?2:1,starB[i]==2?2:1);
        }
    }

    // -----------------------------------------------------------------
    //  Platforms
    // -----------------------------------------------------------------
    private void renderPlatforms(Graphics g) {
        int[][] plats=Arena.PLATFORMS;
        int quakeOff=Arena.isQuakeActive()?((int)(System.currentTimeMillis()/60)%3)-1:0;
        for (int i=0;i<plats.length;i++) {
            if (i<Arena.breakTimer.length && Arena.breakTimer[i]>0) continue;
            int px=plats[i][0],py=plats[i][1]+quakeOff,pw=plats[i][2],ph=plats[i][3],pt=plats[i][4];
            switch (pt) {
                case Arena.PT_LAVA:  renderLavaPlat(g,px,py,pw,ph); break;
                case Arena.PT_SPIKE: renderSpikePlat(g,px,py,pw,ph); break;
                case Arena.PT_ICE:   renderIcePlat(g,px,py,pw,ph); break;
                case Arena.PT_BREAK:   renderBreakPlat(g,i,px,py,pw,ph); break;
                case Arena.PT_BOUNCE:  renderBouncePlat(g,px,py,pw,ph); break;
                case Arena.PT_CRUMBLE: renderCrumblePlat(g,i,px,py,pw,ph); break;
                default:               renderSolidPlat(g,px,py,pw,ph); break;
            }
        }
    }

    private void renderSolidPlat(Graphics g, int px,int py,int pw,int ph) {
        g.setColor(C_PLAT_SHD); g.fillRect(px+3,py+3,pw,ph);
        g.setColor(C_PLAT);     g.fillRect(px,py,pw,ph);
        g.setColor(0x1A2733);   g.fillRect(px,py+ph-4,pw,4);
        g.setColor(C_PLAT_TOP); g.drawLine(px,py,px+pw-1,py); g.drawLine(px,py+1,px+pw-1,py+1);
        g.setColor(0x556677);   g.drawLine(px,py+2,px,py+ph-1); g.drawLine(px+pw-1,py+2,px+pw-1,py+ph-1);
        g.setColor(0x99BBCC);   g.fillRect(px,py,2,2); g.fillRect(px+pw-2,py,2,2);
        g.setColor(0x2A3A4A);
        for (int bx2=px+16;bx2<px+pw-2;bx2+=16) g.drawLine(bx2,py+2,bx2,py+ph-2);
    }

    private void renderLavaPlat(Graphics g, int px,int py,int pw,int ph) {
        py -= Arena.lavaRise; // lava rise offset
        for (int lx2=px;lx2<px+pw;lx2+=4) {
            boolean hot=((lx2/4+Arena.lavaFrame)%2==0);
            g.setColor(hot?C_LAVA_A:C_LAVA_B);
            g.fillRect(lx2,py,4,ph);
        }
        g.setColor(0xFFFF44); g.drawLine(px,py,px+pw-1,py);
        g.setColor(C_LAVA_B);
        int bub=px+(Arena.lavaFrame*22%pw);
        g.fillArc(bub,py-3,6,6,0,360);
    }

    private void renderIcePlat(Graphics g, int px,int py,int pw,int ph) {
        g.setColor(0x112233); g.fillRect(px+2,py+2,pw,ph);
        g.setColor(0x2244AA); g.fillRect(px,py,pw,ph);
        g.setColor(0x113366); g.fillRect(px,py+ph-3,pw,3);
        g.setColor(C_ICE_B);  g.drawLine(px,py,px+pw-1,py); g.drawLine(px,py+1,px+pw-1,py+1);
        g.setColor(C_ICE_A);
        for (int ix=px+8;ix<px+pw;ix+=12) { g.drawLine(ix,py,ix+3,py+ph-1); }
    }

    private void renderSpikePlat(Graphics g, int px,int py,int pw,int ph) {
        g.setColor(C_PLAT); g.fillRect(px,py,pw,ph);
        g.setColor(C_PLAT_TOP); g.drawLine(px,py+1,px+pw-1,py+1);
        g.setColor(C_SPIKE);
        for (int sp=px+2;sp<px+pw-2;sp+=6) {
            g.drawLine(sp,py,sp+3,py-8); g.drawLine(sp+3,py-8,sp+6,py);
            g.setColor(0xFF6666); g.fillRect(sp+2,py-9,3,3); g.setColor(C_SPIKE);
        }
    }

    private void renderBreakPlat(Graphics g, int idx,int px,int py,int pw,int ph) {
        int hp=idx<Arena.breakHealth.length?Arena.breakHealth[idx]:3;
        g.setColor(hp==3?C_BREAK_OK:C_BREAK_CK); g.fillRect(px,py,pw,ph);
        g.setColor(0xAA8866); g.drawLine(px,py,px+pw-1,py); g.drawLine(px,py+1,px+pw-1,py+1);
        if (hp<=2) { g.setColor(0x331100); g.drawLine(px+pw/3,py,px+pw/3+3,py+ph); }
        if (hp<=1) { g.setColor(0x220800); g.drawLine(px+pw*2/3,py,px+pw*2/3-4,py+ph); }
    }

    private void renderBouncePlat(Graphics g, int px,int py,int pw,int ph) {
        // Green trampoline with chevron stripes
        g.setColor(0x001A00); g.fillRect(px+2,py+2,pw,ph);
        g.setColor(0x00AA44); g.fillRect(px,py,pw,ph);
        g.setColor(0x00FF66); g.drawLine(px,py,px+pw-1,py); g.drawLine(px,py+1,px+pw-1,py+1);
        // Bounce chevrons
        g.setColor(0x00FF99);
        for (int bx2=px+4; bx2<px+pw-4; bx2+=10) {
            g.drawLine(bx2,py+ph-2,bx2+4,py+2);
            g.drawLine(bx2+4,py+2,bx2+8,py+ph-2);
        }
        g.setColor(0x55FFAA); g.fillRect(px,py,2,2); g.fillRect(px+pw-2,py,2,2);
    }

    private void renderCrumblePlat(Graphics g, int idx,int px,int py,int pw,int ph) {
        // Orange crumbling platform - intact but shows stress cracks
        g.setColor(0x331100); g.fillRect(px+2,py+2,pw,ph);
        g.setColor(0xAA5500); g.fillRect(px,py,pw,ph);
        g.setColor(0xFFAA44); g.drawLine(px,py,px+pw-1,py); g.drawLine(px,py+1,px+pw-1,py+1);
        // Crack lines showing instability
        g.setColor(0x331100);
        g.drawLine(px+pw/4, py, px+pw/4-2, py+ph);
        g.drawLine(px+pw*3/4, py, px+pw*3/4+2, py+ph);
        // Pulsing warning glow
        long t=System.currentTimeMillis();
        if ((t/400)%2==0) { g.setColor(0xFF6600); g.drawRect(px,py,pw-1,ph-1); }
    }

    // -----------------------------------------------------------------
    //  Ground weapon icons
    // -----------------------------------------------------------------
    private void renderGW(Graphics g) {
        for (int i=0;i<MAX_GW;i++) {
            if (!gwActive[i]) continue;
            int wx=(int)gwX[i], wy=(int)gwY[i];
            if (gwType[i]==Weapon.TYPE_MINE) {
                long mt=System.currentTimeMillis();
                g.setColor((mt/500)%2==0?0xDD0000:0xFF4400);
                g.fillRect(wx-8,wy-5,16,6);
                g.setColor(0xFF8800); g.drawRect(wx-8,wy-5,15,5);
                g.setColor(0xFFFF00); g.fillArc(wx-2,wy-9,5,5,0,360);
                continue;
            }
            int bob=((int)(System.currentTimeMillis()/300)+i*7)%4;
            wy -= bob/2;
            g.setColor(0x1A1100); g.fillArc(wx-12,wy-2,24,14,0,360);
            g.setColor(C_WEAPON); g.fillRect(wx-5,wy-3,10,5);
            int rar=Weapon.RARITY[gwType[i]<Weapon.RARITY.length?gwType[i]:0];
            g.setColor(GameData.RARITY_COLOR[rar<GameData.RARITY_COLOR.length?rar:0]);
            g.setFont(fSmall);
            g.drawString(Weapon.ICON[gwType[i]<Weapon.ICON.length?gwType[i]:0], wx, wy-11, Graphics.HCENTER|Graphics.TOP);
        }
    }

    // -----------------------------------------------------------------
    //  Bullets
    // -----------------------------------------------------------------
    private void renderBullets(Graphics g) {
        for (int i=0;i<MAX_BULLETS;i++) {
            Weapon.Bullet b=bullets[i]; if (!b.active) continue;
            // Railgun: full-screen piercing beam
            if (b.weaponType==Weapon.TYPE_RAILGUN) {
                int bex=(int)(b.x+(b.vx>0?Arena.W:-Arena.W));
                g.setColor(0xFF4499); g.drawLine((int)b.x,(int)b.y,bex,(int)b.y);
                g.setColor(0xFF88CC); g.drawLine((int)b.x,(int)b.y-1,bex,(int)b.y-1);
                g.setColor(0xFFCCEE); g.fillArc((int)b.x-3,(int)b.y-3,6,6,0,360);
                continue;
            }
            int bx=(int)b.x, by=(int)b.y;
            switch (b.weaponType) {
                case Weapon.TYPE_GRENADE:
                    g.setColor(0x44AA44); g.fillArc(bx-3,by-3,6,6,0,360); break;
                case Weapon.TYPE_ROCKET:
                    g.setColor(0xFF4400); g.fillArc(bx-4,by-3,8,6,0,360);
                    g.setColor(0xFF8800); g.fillRect(bx-(b.vx>0?8:0),by-1,7,3); break;
                case Weapon.TYPE_LASER:
                    g.setColor(0x00AAFF); g.fillRect(bx-6,by-1,12,3);
                    g.setColor(0xAAEEFF); g.fillRect(bx-4,by,8,1); break;
                case Weapon.TYPE_FLAMETHROWER:
                    g.setColor(0xFF6600-(((350-b.life)/60)*0x001100));
                    g.fillArc(bx-3,by-3,(int)(4+(350-b.life)*0.014f),(int)(4+(350-b.life)*0.014f),0,360); break;
                case Weapon.TYPE_SNIPER:
                    g.setColor(0x00FFFF); g.fillRect(bx-5,by-1,10,3); break;
                case Weapon.TYPE_BOOMERANG:
                    g.setColor(0xCC8833);
                    g.drawLine(bx-5,by+3,bx+2,by-4); g.drawLine(bx+2,by-4,bx+7,by+2); break;
                case Weapon.TYPE_CROSSBOW:
                    g.setColor(0xFFFFAA); g.fillRect(bx-5,by-1,10,2); break;
                default:
                    g.setColor(C_BULLET); g.fillRect(bx-2,by-2,4,4);
                    g.setColor(0xAA8833);
                    g.drawLine(bx,by,bx-(int)(b.vx*0.02f),by-(int)(b.vy*0.02f)); break;
            }
        }
    }

    // -----------------------------------------------------------------
    //  Particles
    // -----------------------------------------------------------------
    private void renderParticles(Graphics g) {
        // Draw blood pool decals first (under everything)
        for (int i=0;i<MAX_POOLS;i++) {
            if (poolR[i] <= 0) continue;
            g.setColor(poolCol[i]);
            g.fillArc(poolX[i]-poolR[i], poolY[i]-poolR[i]/2,
                      poolR[i]*2, poolR[i], 0, 360);
        }
        // Draw particles
        for (int i=0;i<MAX_PART;i++) {
            if (pLife[i]<=0) continue;
            int px=(int)pX[i], py=(int)pY[i], ps=pSize[i];
            switch (pType[i]) {
                case 1: // blood - round drops
                    g.setColor(pCol[i]);
                    g.fillArc(px, py, ps+1, ps+1, 0, 360);
                    // trailing tail for fast drops
                    if (pVY[i] > 100f) {
                        g.setColor(darken(pCol[i]));
                        g.drawLine(px, py, px-(int)(pVX[i]*0.02f), py-(int)(pVY[i]*0.03f));
                    }
                    break;
                case 2: // smoke - fading grey circle
                    int alpha = pLife[i] * 80 / 1100;
                    g.setColor(pCol[i] & 0x444444 | (alpha << 16 & 0xFF0000));
                    g.fillArc(px-ps, py-ps, ps*2, ps*2, 0, 360);
                    break;
                case 3: // ember - bright pixel with flicker
                    boolean lit = (pLife[i] / 60) % 2 == 0;
                    g.setColor(lit ? pCol[i] : darken(pCol[i]));
                    g.fillRect(px, py, ps, ps);
                    break;
                default: // sparks
                    g.setColor(pCol[i]);
                    g.fillRect(px, py, ps, ps);
                    break;
            }
        }
    }

    // -----------------------------------------------------------------
    //  HUD
    // -----------------------------------------------------------------
    private void renderDangerFlash(Graphics g) {
        if (player==null || !player.alive) return;
        // Check if player is standing on lava or spike platform
        int[] ltype={-1};
        Arena.checkLanding(player.x, player.y+2, 8f, 0.1f, ltype);
        boolean onHazard=(ltype[0]==Arena.PT_LAVA||ltype[0]==Arena.PT_SPIKE);
        // Also flash when health is critically low (<15%)
        boolean lowHp = player.maxHealth>0 && player.health*100/player.maxHealth<15;
        if (!onHazard && !lowHp) return;
        // Pulsing red border
        long t=System.currentTimeMillis();
        if ((t/250)%2==0) {
            g.setColor(onHazard ? 0xDD0000 : 0xAA0000);
            g.drawRect(1,1,SW-2,SH-2);
            g.drawRect(2,2,SW-4,SH-4);
            if (onHazard) {
                g.setFont(fSmall);
                g.drawString(ltype[0]==Arena.PT_LAVA?"!! LAVA !!":"!! SPIKES !!",SW/2,SH/2-6,Graphics.HCENTER|Graphics.TOP);
            }
        }
    }

    private void renderHUD(Graphics g) {
        // P1 health bar
        drawHPBar(g,4,4,84,10,player!=null?player.health:0,
            player!=null?player.maxHealth:100,
            GameData.CHAR_COLOR[playerCharId],
            GameData.CHAR_NAME[playerCharId]);

        // Enemy health bar
        int eid = enemy!=null ? enemy.charId : enemyCharId;
        drawHPBar(g,SW-88,4,84,10,enemy!=null?enemy.health:0,
            enemy!=null?enemy.maxHealth:100,
            GameData.CHAR_COLOR[eid],
            GameData.CHAR_NAME[eid]);

        // Timer / wave
        g.setFont(fSmall);
        if (gameState==GS_GAME||gameState==GS_ROUND_END) {
            int sec=roundTimeLeft/1000;
            String ts=(sec/60)+":"+(sec%60<10?"0":"")+sec%60;
            g.setColor(sec<20?C_RED:C_WHITE);
            g.drawString(ts, SW/2,4, Graphics.HCENTER|Graphics.TOP);
            g.setColor(C_YELLOW);
            g.drawString(playerScore+"-"+enemyScore, SW/2,16, Graphics.HCENTER|Graphics.TOP);
        } else if (gameState==GS_SURVIVAL||gameState==GS_WAVE_END) {
            g.setColor(C_CYAN);
            g.drawString("W"+survivalWave+"  K:"+survivalKills, SW/2,4, Graphics.HCENTER|Graphics.TOP);
        }

        // ============================================================
        //  BOTTOM HUD ??? three clean rows, no overlap
        //
        //  ROW A  SH-40..SH-30  Special bar (left 68px) | Ultra bar (right 56px)
        //  ROW B  SH-28..SH-18  Weapon+ammo (left)      | Shards (right)
        //  ROW C  SH-16..SH-6   Stamina bar (left 44px) | 3 power pills (right)
        //  FLASH  mid-screen    "SPECIAL READY!" only when full
        // ============================================================

        // ---- ROW A: Special bar (left) + Ultra bar (right) ----------
        if (player != null) {
            // Special bar ??? left side, 68px wide
            int sbX = 4, sbY = SH - 40;
            boolean spReady = player.specialReady;
            int spCol = GameData.CHAR_SPECIAL_COLOR[playerCharId];
            // Background + fill
            g.setColor(0x0A0A1C); g.fillRect(sbX, sbY, 68, 6);
            int spFill = spReady ? 68 :
                68 * Math.max(0, Stickman.SP_COOLDOWN_MS - player.specialCooldown)
                   / Stickman.SP_COOLDOWN_MS;
            g.setColor(spReady ? spCol : 0x334455);
            g.fillRect(sbX, sbY, spFill, 6);
            g.setColor(0x223344); g.drawRect(sbX, sbY, 68, 6);
            // Charge dot row inside bar ??? 5 dots
            int chargeMax = Stickman.SP_CHARGE_MAX;
            for (int ci = 0; ci < 5; ci++) {
                int dotX = sbX + 4 + ci * 12;
                boolean lit = player.specialCharge * 5 / chargeMax > ci;
                g.setColor(lit ? 0xFFDD00 : 0x1A1A2A);
                g.fillArc(dotX, sbY + 1, 4, 4, 0, 360);
            }
            // Label: special name abbreviated, or "SP!" when ready
            g.setFont(fSmall);
            if (spReady) {
                boolean fl = (System.currentTimeMillis() % 500L) < 250L;
                g.setColor(fl ? spCol : C_WHITE);
                g.drawString("SP!", sbX, sbY - 9, Graphics.LEFT|Graphics.TOP);
            } else {
                g.setColor(0x445566);
                // Show first 6 chars of special name to keep it short
                String spName = GameData.CHAR_SPECIAL_NAME[playerCharId];
                if (spName.length() > 6) spName = spName.substring(0, 6);
                g.drawString(spName, sbX, sbY - 9, Graphics.LEFT|Graphics.TOP);
            }

            // Ultra bar ??? right side, 52px wide
            int ubW = 52, ubX = SW - ubW - 4, ubY = SH - 40;
            g.setColor(0x0A0A1C); g.fillRect(ubX, ubY, ubW, 6);
            int uf = ubW - ubW * Math.max(player.ultraCooldown, 0) / 4000;
            g.setColor(uf >= ubW ? C_GOLD : 0x334466);
            g.fillRect(ubX, ubY, uf, 6);
            g.setColor(0x223344); g.drawRect(ubX, ubY, ubW, 6);
            g.setFont(fSmall);
            g.setColor(uf >= ubW ? C_GOLD : 0x445566);
            g.drawString("1:ULT", ubX, ubY - 9, Graphics.LEFT|Graphics.TOP);
        }

        // ---- ROW B: Weapon (left) + Shards (right) ------------------
        if (player != null) {
            String wl = player.weaponType == Weapon.TYPE_NONE
                ? "FIST"
                : Weapon.LABEL[player.weaponType] + ":" + player.weaponAmmo;
            g.setFont(fSmall);
            g.setColor(player.weaponType == Weapon.TYPE_NONE ? 0x556677 : C_YELLOW);
            g.drawString(wl, 4, SH - 28, Graphics.LEFT|Graphics.TOP);
        }
        g.setFont(fSmall); g.setColor(0x886600);
        g.drawString(SaveData.realmShards + "S", SW - 4, SH - 28, Graphics.RIGHT|Graphics.TOP);

        // ---- ROW C: Stamina bar (left) + Power pills (right) --------
        // Stamina bar ??? 44px
        if (player != null) {
            int stX = 4, stY = SH - 16;
            g.setColor(0x0A0A1C); g.fillRect(stX, stY, 44, 5);
            int stFw = 44 * playerStamina / 100;
            g.setColor(playerStamina > 60 ? 0x00AACC :
                       playerStamina > 30 ? 0xCC8800 : 0xCC2222);
            g.fillRect(stX, stY, stFw, 5);
            g.setColor(0x223344); g.drawRect(stX, stY, 44, 5);
            g.setFont(fSmall); g.setColor(0x334455);
            g.drawString("STA", stX + 46, stY - 1, Graphics.LEFT|Graphics.TOP);
        }

        // Power pills ??? right side, compact (show only if cooldown active)
        {
            int[] cds    = { power3Cooldown, power7Cooldown, power9Cooldown };
            int[] maxCds = { P3_CD, P7_CD, P9_CD };
            int[] cols   = { 0x00CC77, 0x0099CC, 0xCC8800 };
            String[] lbl = { "3", "7", "9" };
            int pillW = 18, pillH = 12, pillY = SH - 18;
            for (int pi = 0; pi < 3; pi++) {
                int pillX = SW - 62 + pi * (pillW + 2);
                boolean ready = cds[pi] <= 0;
                g.setColor(ready ? cols[pi] : 0x1A1A2A);
                g.fillRect(pillX, pillY, pillW, pillH);
                g.setColor(ready ? 0x000000 : cols[pi]);
                g.setFont(fSmall);
                String txt = ready ? lbl[pi] : (cds[pi] / 1000 + 1) + "s";
                g.drawString(txt, pillX + pillW/2, pillY + 1, Graphics.HCENTER|Graphics.TOP);
                if (!ready) {
                    // tiny progress bar at bottom of pill
                    int fill = pillW - pillW * cds[pi] / maxCds[pi];
                    g.setColor(cols[pi]); g.fillRect(pillX, pillY + pillH - 2, fill, 2);
                }
            }
        }

        // ---- Combo dots (just above row B, only when active) --------
        if (player != null && player.comboStep > 0 && player.comboTimer > 0) {
            for (int c = 0; c < player.comboStep; c++) {
                g.setColor(C_ORANGE); g.fillArc(4 + c*8, SH - 48, 5, 5, 0, 360);
            }
        }

        // ---- Special full flash (centre screen, brief) --------------
        if (player != null && player.specialFull) {
            boolean fl = (System.currentTimeMillis() / 350) % 2 == 0;
            if (fl) {
                g.setFont(fSmall);
                g.setColor(GameData.CHAR_SPECIAL_COLOR[playerCharId]);
                g.drawString("* SPECIAL READY *", SW/2, SH/2 - 50,
                             Graphics.HCENTER|Graphics.TOP);
            }
        }

        // ---- Controls hint ??? only if showControlsHint flag is on ----
        if (showControlsHint) {
            g.setFont(fSmall); g.setColor(0x1E2A38);
            // Two shorter lines to fit the screen width
            g.drawString("4/6:mv 2:jmp 5:atk 0:blk/dg #:ST", SW/2, SH - 6,
                         Graphics.HCENTER|Graphics.TOP);
        }
    }

    private void drawHPBar(Graphics g, int x,int y,int w,int h,int hp,int maxHp,int col,String label) {
        if (maxHp<=0) maxHp=100;
        g.setColor(0x0A0A20); g.fillRect(x,y,w,h);
        int fw=w*Math.max(hp,0)/maxHp;
        g.setColor(hp>maxHp*6/10?C_GREEN:(hp>maxHp*3/10?C_YELLOW:C_RED));
        g.fillRect(x,y,fw,h);
        // HP boost from armor shown as extra fill
        g.setColor(0x334455); g.drawRect(x,y,w,h);
        g.setColor(0x556677); g.drawLine(x+1,y+1,x+fw-1,y+1);
        g.setFont(fSmall); g.setColor(C_WHITE);
        g.drawString(label+":"+hp, x+2,y+h+1, Graphics.LEFT|Graphics.TOP);
    }

    private void renderPowerupHUD(Graphics g) {
        // Active powerup name ??? top right, below enemy HP bar
        if (SaveData.activePowerup >= 0 && SaveData.powerupTimer > 0) {
            String[] pnames = { "BLESSED", "CHRONO", "SURGE", "CLOAKED" };
            g.setColor(C_PURPLE); g.setFont(fSmall);
            g.drawString(pnames[SaveData.activePowerup], SW - 4, 32,
                         Graphics.RIGHT|Graphics.TOP);
        }
        // Power pills are now rendered in renderHUD ROW C (right side)
        // Nothing else here to avoid duplicate draw
    }

    private void renderWindIndicator(Graphics g) {
        int wf=Arena.getWindForce(); if(wf==0) return;
        boolean right=(wf>0);
        g.setColor(0x4499FF); g.setFont(fSmall);
        g.drawString(right?"WIND >>":"<< WIND", SW/2, SH-28, Graphics.HCENTER|Graphics.TOP);
        int bars=Math.min(Math.abs(wf)/20,4);
        for (int b=0;b<bars;b++) {
            g.setColor(0x2266DD+b*0x001100);
            int bx=right?(SW/2+24+b*6):(SW/2-26-b*6);
            g.fillRect(bx,SH-26,4,8);
        }
    }

    private void renderGravityIndicator(Graphics g) {
        if (Arena.gravityMult<0.8f) {
            boolean flash=(gravFlashTimer<150);
            if (flash) {
                g.setColor(C_CYAN); g.setFont(fSmall);
                g.drawString("GRAVITY ANOMALY!", SW/2, 30, Graphics.HCENTER|Graphics.TOP);
            }
        }
    }

    // -----------------------------------------------------------------
    //  Kill feed / big message
    // -----------------------------------------------------------------
    private void renderKillFeed(Graphics g) {
        g.setFont(fSmall); int ky=34;
        for (int i=0;i<4;i++) {
            int idx=(feedHead+4-1-i)%4;
            if (feedLife[idx]<=0) continue;
            g.setColor(feedLife[idx]>2000?0xFFCC44:0x887722);
            g.drawString(feedLines[idx], SW-3,ky, Graphics.RIGHT|Graphics.TOP);
            ky+=13;
        }
    }

    private void renderBigMsg(Graphics g) {
        if (bigMsgLife<=0||bigMsg.length()==0) return;
        g.setFont(fMedium);
        g.setColor(0x000000); g.drawString(bigMsg, SW/2+2,SH/2-18+2, Graphics.HCENTER|Graphics.TOP);
        g.setColor(bigMsgColor); g.drawString(bigMsg, SW/2,SH/2-18, Graphics.HCENTER|Graphics.TOP);
    }

    // -----------------------------------------------------------------
    //  Round-end banner
    // -----------------------------------------------------------------
    private void renderRoundBanner(Graphics g) {
        int by=bannerY;
        g.setColor(0x09091C); g.fillRoundRect(22,by,SW-44,88,8,8);
        g.setColor(C_YELLOW); g.drawRoundRect(22,by,SW-44,88,8,8);
        g.setFont(fLarge);
        if (roundWinner==0)      { g.setColor(GameData.CHAR_COLOR[playerCharId]); g.drawString("P1 WINS!",SW/2,by+8,Graphics.HCENTER|Graphics.TOP); }
        else if (roundWinner==1) { g.setColor(GameData.CHAR_COLOR[enemyCharId]);  g.drawString("AI WINS!",SW/2,by+8,Graphics.HCENTER|Graphics.TOP); }
        else                     { g.setColor(C_WHITE);                            g.drawString("DRAW!",   SW/2,by+8,Graphics.HCENTER|Graphics.TOP); }
        g.setFont(fMedium); g.setColor(C_WHITE);
        g.drawString(playerScore+" - "+enemyScore, SW/2,by+44, Graphics.HCENTER|Graphics.TOP);
        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString("Next in "+(roundEndTimer/1000+1)+"s", SW/2,by+66, Graphics.HCENTER|Graphics.TOP);
    }

    private void renderWaveBanner(Graphics g) {
        int by=bannerY;
        g.setColor(0x091509); g.fillRoundRect(22,by,SW-44,80,8,8);
        g.setColor(C_CYAN);   g.drawRoundRect(22,by,SW-44,80,8,8);
        g.setFont(fLarge); g.setColor(C_CYAN);
        g.drawString("WAVE "+(survivalWave-1)+" DONE!", SW/2,by+8, Graphics.HCENTER|Graphics.TOP);
        g.setFont(fSmall); g.setColor(C_GREEN);
        g.drawString("+25 HP restored", SW/2,by+44, Graphics.HCENTER|Graphics.TOP);
        g.setColor(C_GOLD);
        g.drawString("Wave "+survivalWave+" incoming...", SW/2,by+58, Graphics.HCENTER|Graphics.TOP);
    }

    // -----------------------------------------------------------------
    //  Game over
    // -----------------------------------------------------------------
    private void renderGameOver(Graphics g) {
        g.setColor(0x07070F); g.fillRect(0,0,SW,SH);
        drawBg(g);
        int bh=190, bx=18, bby=(SH-bh)/2;
        g.setColor(0x090918); g.fillRoundRect(bx,bby,SW-bx*2,bh,10,10);
        g.setColor(C_GOLD);   g.drawRoundRect(bx,bby,SW-bx*2,bh,10,10);

        boolean playerWon = (playerScore>=ROUNDS_TO_WIN);
        g.setFont(fLarge);
        g.setColor(playerWon?C_GREEN:C_RED);
        g.drawString(playerWon?"VICTORY!":"DEFEATED", SW/2,bby+12, Graphics.HCENTER|Graphics.TOP);

        // Character name of winner
        g.setFont(fMedium); g.setColor(C_WHITE);
        g.drawString(playerWon?
            GameData.CHAR_NAME[playerCharId]+" WINS":
            GameData.CHAR_NAME[enemyCharId]+" WINS",
            SW/2, bby+48, Graphics.HCENTER|Graphics.TOP);

        g.setFont(fSmall); g.setColor(C_GREY);
        if (gameState==GS_SURVIVAL) {
            g.drawString("Wave: "+survivalWave+"  Kills: "+survivalKills, SW/2,bby+74, Graphics.HCENTER|Graphics.TOP);
            g.setColor(C_GOLD);
            String[] hs=HighScore.getLines();
            if (hs.length>0) g.drawString("BEST: "+hs[0], SW/2,bby+92, Graphics.HCENTER|Graphics.TOP);
        } else {
            g.drawString(playerScore+" - "+enemyScore, SW/2,bby+74, Graphics.HCENTER|Graphics.TOP);
        }

        // Show unlock notification if any
        if (unlockNotifTimer > 0 && unlockNotifMsg.length() > 0) {
            g.setFont(fSmall); g.setColor(C_GOLD);
            g.drawString("** " + unlockNotifMsg + " **", SW/2, bby+bh-78, Graphics.HCENTER|Graphics.TOP);
        }
        g.setFont(fSmall); g.setColor(C_YELLOW);
        g.drawString("[5] PLAY AGAIN", SW/2,bby+bh-60, Graphics.HCENTER|Graphics.TOP);
        g.setColor(C_WHITE);
        g.drawString("[0] MAIN MENU",  SW/2,bby+bh-42, Graphics.HCENTER|Graphics.TOP);
        // Stats bar
        g.setFont(fSmall); g.setColor(C_GREY);
        g.drawString("WINS:" + SaveData.totalWins + " KILLS:" + SaveData.totalKills,
                     SW/2, bby+bh-24, Graphics.HCENTER|Graphics.TOP);
    }

    // -----------------------------------------------------------------
    //  How to play
    // -----------------------------------------------------------------
    private void renderHowTo(Graphics g) {
        drawBg(g);
        g.setColor(0x07070F); g.fillRect(0,0,SW,SH);
        drawBg(g);
        g.setFont(fMedium); g.setColor(C_YELLOW);
        g.drawString("HOW TO PLAY", SW/2,8, Graphics.HCENTER|Graphics.TOP);
        g.setFont(fSmall);
        String[] L={"4/6   Move","2     Jump x2","5     Attack","1     ULTRA!!","#     StickTension","0     Block",
                     "","STORY: beat bosses","unlock chars+arenas","earn Realm Shards","spend at SHOP"};
        String[] R={"PISTOL  15 shots","SHOTGUN 5pellet","AK-47  30 rnd","SNIPER  1-shot","ROCKET  homing",
                     "LASER   beam","FLAME   DoT","BOOMERANG returns","CROSSBOW pierce","ULTRA=1-hit KO!","COMBO=3-hit chain"};
        for (int i=0;i<L.length;i++) { g.setColor(C_WHITE); g.drawString(L[i],5,32+i*14,Graphics.LEFT|Graphics.TOP); }
        for (int i=0;i<R.length;i++) { g.setColor(C_CYAN);  g.drawString(R[i],SW-3,32+i*14,Graphics.RIGHT|Graphics.TOP); }
        g.setColor(C_GREY); g.drawString("[0] BACK",SW/2,SH-10,Graphics.HCENTER|Graphics.TOP);
    }

    // ==================================================================
    // ==================================================================
    //  MELEE BRAWL - 4 fighters free-for-all
    // ==================================================================
    private void initMelee() {
        setPendingBGM(SoundManager.BGM_COMBAT);
        isStoryFight = false;
        // Reset win counters only on round 1 (fresh start)
        if (meleeRound <= 1) { meleePlayerWins=0; meleeAIWins=0; meleeChampion=false; }
        meleeWinner   = -1;
        meleeEndTimer = 0;
        meleePlayerAlive = true;

        // Random arena each brawl
        arenaSel = rand.nextInt(Arena.ARENA_COUNT);
        Arena.setArena(arenaSel);
        SaveData.applySkills();

        // Player
        playerCharId = SaveData.selectedChar;
        player = makeChar(playerCharId, Arena.SPAWN[0], Arena.SPAWN[1], true);

        // 3 AI fighters - different characters
        int used0 = playerCharId;
        for (int i = 0; i < 3; i++) {
            int cid = (used0 + i + 1) % 8;
            meleeCharIds[i] = cid;
            float fx = (i==0) ? Arena.SPAWN[2] : (i==1 ? Arena.W*0.25f : Arena.W*0.75f);
            float fy = Arena.SPAWN[1];
            meleeAI[i] = makeChar(cid, fx, fy, false);
        }

        enemy = meleeAI[0]; // wire enemy ptr so updateBullets hit detection works
        enemyCharId = meleeCharIds[0];
        power3Cooldown=0; power7Cooldown=0; power9Cooldown=0;
        power7Timer=0; power9Timer=0;
        clearBullets(); clearGW(); clearParticles();
        for(int i=0;i<3;i++) meleeRespawnTimer[i]=0; // reset respawn timers
        weaponSpawnTimer = 3000;
        roundTimeLeft    = 180000; // 3 minute brawl
        bigMsg=""; bigMsgLife=0;
        spawnArenaWeapons();
        camX=Arena.W/2f-SW/2f; if(camX<0)camX=0;
        camY=Arena.H*0.55f-SH/2f; if(camY<0)camY=0;
        showBigMsg("MELEE BRAWL! LAST ONE STANDING!", C_GOLD);
        gameState = GS_MELEE;
    }

    private void updateMelee(int delta) {
        roundTimeLeft -= delta;
        weaponSpawnTimer -= delta;
        if (weaponSpawnTimer <= 0) {
            weaponSpawnTimer = 5000;
            spawnGWAt(randWeapon(0), (int)(Arena.W*0.3f)+rand.nextInt((int)(Arena.W*0.4f)), 30);
        }

        // Player input via getKeyStates
        int meleeKeys = getKeyStates();
        boolean lft  = (meleeKeys & LEFT_PRESSED)  != 0;
        boolean rgt  = (meleeKeys & RIGHT_PRESSED) != 0;
        boolean F    = (meleeKeys & FIRE_PRESSED)  != 0; // key 5 / centre
        boolean D    = (meleeKeys & DOWN_PRESSED)  != 0;
        boolean jmp  = jumpQueued;  jumpQueued  = false;
        boolean atk  = ultraQueued || F; ultraQueued = false;
        boolean thr  = throwQueued; throwQueued = false;
        boolean blk  = blockHeld || D;
        if (!blk) blockHeld = false; // BUG FIX: sync blockHeld with actual key state
        if (player != null && player.alive)
            player.update(delta, lft, rgt, jmp, atk||thr, blk, false, meleeFindTarget(player));
        applyBounce(player);

        // Update AI - each targets nearest enemy
        for (int i = 0; i < 3; i++) {
            if (meleeAI[i] == null || !meleeAI[i].alive) continue;
            Stickman tgt = meleeFindTarget(meleeAI[i]);
            meleeAI[i].update(delta, false, false, false, false, false, false, tgt);
            applyBounce(meleeAI[i]);
        }

        consumeShootRequests(); // FIXED: was missing - player can now shoot in melee
        Arena.update(delta);

        // Wind force on all melee fighters
        int windFM=Arena.getWindForce();
        if (windFM!=0) {
            float wp=windFM*(delta/1000f);
            if (player!=null&&player.alive) player.x+=wp;
            for(int i=0;i<3;i++) if(meleeAI[i]!=null&&meleeAI[i].alive) meleeAI[i].x+=wp;
        }
        if (Arena.isQuakeActive()&&(System.currentTimeMillis()/150)%2==0) triggerShake(3,80);
        int dangerYS=Arena.getDangerY(), dangerDmgS=Arena.getDangerDmgPerSec();
        if (dangerYS>0&&dangerDmgS>0&&player!=null&&player.alive&&player.y>=dangerYS)
            if ((roundTimeLeft%1000)<delta) player.takeDamage(dangerDmgS/4,0,0);
        if (Arena.isQuakeActive()&&(System.currentTimeMillis()/150)%2==0) triggerShake(3,80);

        updateBullets(delta);
        updateGW(delta);
        updateParticles(delta);
        if (power3Cooldown > 0) power3Cooldown -= delta;
        if (power7Cooldown > 0) power7Cooldown -= delta;
        if (power9Cooldown > 0) power9Cooldown -= delta;
        if (power7Timer > 0) { power7Timer -= delta; if (power7Timer<=0) { if(player!=null) player.speedBoost=false; } }
        if (power9Timer > 0) { power9Timer -= delta; if (power9Timer<=0) { if(player!=null) player.shieldWall=false; } }

        // Bullet hits: check vs all fighters
        meleeBulletHits();

        // Melee hit checks: every pair
        int plCol = player!=null ? GameData.CHAR_COLOR[playerCharId] : 0;
        for (int i = 0; i < 3; i++) {
            if (meleeAI[i]==null || !meleeAI[i].alive) continue;
            int aiCol = GameData.CHAR_COLOR[meleeCharIds[i]];
            if (player!=null && player.alive) {
                checkMeleeHit(player,     meleeAI[i], aiCol);
                checkMeleeHit(meleeAI[i], player,     plCol);
                if (player.alive) player.chargeSpecialOnHit(0);
            }
            for (int j = i+1; j < 3; j++) {
                if (meleeAI[j]==null || !meleeAI[j].alive) continue;
                checkMeleeHit(meleeAI[i], meleeAI[j], GameData.CHAR_COLOR[meleeCharIds[j]]);
                checkMeleeHit(meleeAI[j], meleeAI[i], aiCol);
            }
        }

        // Special FX for all fighters
        for (int i = 0; i < 3; i++) {
            if (meleeAI[i]==null) continue;
            if (player!=null) {
                dispatchSpecialFX(player,     meleeAI[i]);
                dispatchSpecialFX(meleeAI[i], player);
            }
            for (int j = 0; j < 3; j++) {
                if (i!=j && meleeAI[j]!=null)
                    dispatchSpecialFX(meleeAI[i], meleeAI[j]);
            }
        }

        // Melee respawn: dead fighters respawn after 4s with 35% HP
        for (int i=0;i<3;i++) {
            if (meleeAI[i]!=null && !meleeAI[i].alive && meleeRespawnTimer[i]>0) {
                meleeRespawnTimer[i]-=delta;
                if (meleeRespawnTimer[i]<=0) {
                    float rx=(i==0)?Arena.SPAWN[2]:(i==1?Arena.W*0.25f:Arena.W*0.75f);
                    meleeAI[i]=makeChar(meleeCharIds[i],rx,Arena.SPAWN[1]-30,false);
                    meleeAI[i].health=meleeAI[i].maxHealth*35/100;
                    spawnParticles((int)rx,(int)Arena.SPAWN[1]-30,GameData.CHAR_COLOR[meleeCharIds[i]],10);
                    addKillFeed(GameData.CHAR_NAME[meleeCharIds[i]]+" RESPAWNED!");
                    if (i==0) { enemy=meleeAI[0]; }
                }
            } else if (meleeAI[i]!=null && !meleeAI[i].alive && meleeRespawnTimer[i]==0
                       && meleeRound<=2) { // only respawn in rounds 1-2
                meleeRespawnTimer[i]=4000;
            }
        }
        // Count alive (for end condition only count as dead if no pending respawn)
        int alive = (player!=null && player.alive) ? 1 : 0;
        for (int i=0;i<3;i++) if (meleeAI[i]!=null && meleeAI[i].alive) alive++;
        // Also count pending respawns
        int pendRespawn=0; for(int i=0;i<3;i++) if(meleeRespawnTimer[i]>0) pendRespawn++;

        if ((alive + pendRespawn) <= 0 || roundTimeLeft <= 0) meleeEnd();
        else if (alive<=1 && pendRespawn==0) meleeEnd();
        updateCamera(delta);   // BUG FIX: camera tracks fighters in melee
    }

    private Stickman meleeFindTarget(Stickman src) {
        Stickman best = null;
        float   bestD = 9999999f;
        // Build candidate list: player + 3 AI
        Stickman[] all = new Stickman[4];
        all[0] = player;
        all[1] = meleeAI[0]; all[2] = meleeAI[1]; all[3] = meleeAI[2];
        for (int i=0;i<4;i++) {
            Stickman t = all[i];
            if (t==null || t==src || !t.alive) continue;
            float dx=t.x-src.x, dy=t.y-src.y;
            float d=dx*dx+dy*dy;
            if (d<bestD) { bestD=d; best=t; }
        }
        return best;
    }

    private void meleeBulletHits() {
        // updateBullets() already handled normal 1v1 hits (owner 0 vs enemy, owner 1 vs player).
        // In melee we extend it: bullets also hit extra AI fighters (ai index 2 and 3).
        for (int bi = 0; bi < MAX_BULLETS; bi++) {
            Weapon.Bullet b = bullets[bi];
            if (!b.active) continue;
            // Extra AI fighters (indices 1 and 2, i.e. meleeAI[1] and meleeAI[2])
            // meleeAI[0] = owner 1 is already handled by base updateBullets as "enemy"
            for (int ai2 = 1; ai2 < 3; ai2++) {
                if (meleeAI[ai2] == null || !meleeAI[ai2].alive) continue;
                if (b.owner == (ai2 + 1)) continue; // don't hit self
                int[] tb = meleeAI[ai2].getBounds();
                if (b.x > tb[0] && b.x < tb[0]+tb[2] && b.y > tb[1] && b.y < tb[1]+tb[3]) {
                    int hd = b.damage;
                    meleeAI[ai2].takeDamage(hd, b.vx * 0.35f, -150f);
                    spawnParticles((int)b.x, (int)b.y, GameData.CHAR_COLOR[meleeCharIds[ai2]], 5);
                    if (!meleeAI[ai2].alive) addKillFeed(GameData.CHAR_NAME[playerCharId]+" ELIMINATED "+GameData.CHAR_NAME[meleeCharIds[ai2]]);
                    if (b.weaponType == Weapon.TYPE_ROCKET) explodeBullet(b);
                    else b.active = false;
                    break;
                }
            }
        }
    }

    private void meleeEnd() {
        meleeEndTimer = 4000;
        // Determine winner this round
        if (player != null && player.alive) {
            meleeWinner = 0;
            playerWonRound = true;
            meleePlayerWins++;
            int reward = 60 + meleeRound * 30;
            SaveData.addShards(reward);
            if (player != null) player.setVictory(true);
            for (int i=0;i<3;i++)
                if (meleeAI[i]!=null) meleeAI[i].setVictory(!meleeAI[i].alive);
            showBigMsg("ROUND " + meleeRound + " WIN! +" + reward + " SHARDS!", C_GOLD);
            SoundManager.playSFX(SoundManager.SFX_WIN);
        } else {
            playerWonRound = false;
            meleeWinner = 1;
            meleeAIWins++;
            for (int i=0;i<3;i++) {
                if (meleeAI[i]!=null && meleeAI[i].alive) {
                    meleeAI[i].setVictory(true); meleeWinner = i+1;
                } else if (meleeAI[i]!=null) {
                    meleeAI[i].setVictory(false);
                }
            }
            if (player != null) player.setVictory(false);
            String wName = (meleeWinner>=1&&meleeWinner<=3) ?
                GameData.CHAR_NAME[meleeCharIds[meleeWinner-1]] : "AI";
            showBigMsg("ROUND " + meleeRound + " - " + wName + " WINS!", 0xFF4444);
            SoundManager.playSFX(SoundManager.SFX_DEATH);
        }
        // After 3 rounds determine overall champion
        if (meleeRound >= 3) {
            meleeChampion = (meleePlayerWins > meleeAIWins);
            if (meleeChampion) {
                SaveData.totalWins++;
                SaveData.checkAchievement(0);
                int bonus = 200;
                SaveData.addShards(bonus);
                showBigMsg("BRAWL CHAMPION! +" + bonus + " SHARDS!", C_GOLD);
                SoundManager.playSFX(SoundManager.SFX_UNLOCK);
            } else {
                showBigMsg("BRAWL OVER - DEFEATED!", 0xFF4444);
            }
        }
        meleeRound++;
        SaveData.save();
        prevGameState = GS_MELEE; gameState = GS_MELEE_END;
    }

    private void updateMeleeEnd(int delta) {
        meleeEndTimer -= delta;
        if (player!=null) player.update(delta,false,false,false,false,false,false,null);
        for (int i=0;i<3;i++)
            if (meleeAI[i]!=null) meleeAI[i].update(delta,false,false,false,false,false,false,null);
    }

    private void renderMelee(Graphics g) {
        drawBg(g);

        // CAMERA FIX: Apply world->screen translation BEFORE drawing any world-space objects.
        // This was the ROOT CAUSE - renderMelee never translated the graphics context,
        // so everything rendered at raw world coords (0-480) on a 240px screen.
        int icx = (int)camX, icy = (int)camY;
        g.translate(-icx, -icy);

        renderArenaBg(g);
        renderParallaxBg(g, camX, camY);
        renderPlatforms(g);
        renderWeaponGlow(g);
        renderGW(g);
        for (int i=0;i<3;i++) renderCharShadow(g, meleeAI[i]);
        renderCharShadow(g, player);
        renderParticles(g);
        renderRagdoll(g);
        renderBullets(g);

        // Draw all fighters
        if (player!=null) player.draw(g, GameData.CHAR_COLOR[playerCharId]);
        for (int i=0;i<3;i++)
            if (meleeAI[i]!=null) meleeAI[i].draw(g, GameData.CHAR_COLOR[meleeCharIds[i]]);
        renderDashTrail(g);
        renderSticktension(g);
        renderDamageNumbers(g);

        // Respawn countdown floating text (world space, before restore)
        for (int i=0;i<3;i++) {
            if (meleeAI[i]!=null && !meleeAI[i].alive && meleeRespawnTimer[i]>0) {
                float rx=(i==0)?Arena.SPAWN[2]:(i==1?Arena.W*0.25f:Arena.W*0.75f);
                int ry=(int)Arena.SPAWN[1]-40;
                g.setColor(0xFFAA00);
                g.setFont(fSmall);
                g.drawString("RESP "+(meleeRespawnTimer[i]/1000+1)+"s",(int)(rx-camX),ry-icy,Graphics.HCENTER|Graphics.TOP);
            }
        }

        // Restore to screen space before drawing HUD
        g.translate(icx, icy);

        // HUD top bar
        g.setColor(0x0D0D1A);
        g.fillRect(0, 0, SW, 20);

        // Draw 4 mini HP bars across top
        meleeMiniBar(g, 2,  2, player,     playerCharId,    "YOU");
        meleeMiniBar(g, 62, 2, meleeAI[0], meleeCharIds[0], null);
        meleeMiniBar(g, 122,2, meleeAI[1], meleeCharIds[1], null);
        meleeMiniBar(g, 182,2, meleeAI[2], meleeCharIds[2], null);

        // Timer / round badge
        g.setFont(fSmall); g.setColor(C_GOLD);
        int secs = roundTimeLeft/1000;
        String tstr = secs/60+":"+(secs%60<10?"0":"")+(secs%60);
        g.drawString("R"+meleeRound+" "+tstr, SW/2, 6, Graphics.HCENTER|Graphics.TOP);

        // End screen overlay
        if (gameState==GS_MELEE_END) {
            // Dark panel
            g.setColor(0x0A0A18);
            g.fillRect(0, SH/2-55, SW, 110);
            g.setColor(meleeWinner==0 ? C_GOLD : 0xFF4444);
            g.drawRect(0, SH/2-55, SW-1, 109);
            g.setFont(fLarge);
            g.drawString(meleeWinner==0 ? "VICTORY!" : "ELIMINATED!", SW/2, SH/2-50, Graphics.HCENTER|Graphics.TOP);

            // Winner name
            String winName;
            if (meleeWinner==0) winName = GameData.CHAR_NAME[playerCharId]+" (YOU)";
            else if (meleeWinner>=1&&meleeWinner<=3) winName = GameData.CHAR_NAME[meleeCharIds[meleeWinner-1]];
            else winName = "DRAW";
            g.setFont(fSmall); g.setColor(C_WHITE);
            g.drawString(winName+" WINS!", SW/2, SH/2-22, Graphics.HCENTER|Graphics.TOP);

            // Draw winner stickman big in centre
            int wCharId = (meleeWinner==0) ? playerCharId :
                         ((meleeWinner>=1&&meleeWinner<=3) ? meleeCharIds[meleeWinner-1] : 0);
            drawCharBig(g, wCharId, SW/2, SH/2+10, GameData.CHAR_COLOR[wCharId]);

            // Animated victory sparks (use time for animation)
            if (meleeWinner==0) {
                int frame = (int)(System.currentTimeMillis()/150)%8;
                for (int si=0;si<8;si++) {
                    int ang = si*45 + frame*15;
                    int rx  = (int)(Math.cos(ang*3.14/180)*40);
                    int ry  = (int)(Math.sin(ang*3.14/180)*20);
                    g.setColor((si%2==0)?C_GOLD:C_CYAN);
                    g.fillRect(SW/2+rx-2, SH/2+10+ry-2, 5, 5);
                }
            }

            // Score across rounds + next action
            g.setFont(fSmall); g.setColor(C_GREY);
            g.drawString("YOU " + meleePlayerWins + " - " + meleeAIWins + " AI",
                         SW/2, SH/2+36, Graphics.HCENTER|Graphics.TOP);
            if (meleeRound > 3) {
                // Series done - show champion banner
                g.setFont(fMedium);
                g.setColor(meleeChampion ? C_GOLD : 0xFF4444);
                g.drawString(meleeChampion ? "BRAWL CHAMPION!" : "BRAWL DEFEATED",
                             SW/2, SH/2+50, Graphics.HCENTER|Graphics.TOP);
                g.setFont(fSmall); g.setColor(C_GREY);
                g.drawString("5:MENU  0:MENU", SW/2, SH/2+68, Graphics.HCENTER|Graphics.TOP);
            } else {
                g.drawString("ROUND "+(meleeRound-1)+"/3 - 5:NEXT ROUND  0:MENU",
                             SW/2, SH/2+50, Graphics.HCENTER|Graphics.TOP);
            }
        }

        renderKillFeed(g);
        renderBigMsg(g);
    }

    private void meleeMiniBar(Graphics g, int x, int y, Stickman sm, int cid, String label) {
        int bw=56, bh=16;
        g.setColor(0x0A0A18); g.fillRect(x, y, bw, bh);
        if (sm==null || !sm.alive) {
            // Show respawn countdown if applicable
            int slotIdx = -1;
            for(int ii=0;ii<3;ii++) if(meleeAI[ii]==sm) slotIdx=ii;
            if(slotIdx>=0 && meleeRespawnTimer[slotIdx]>0) {
                g.setColor(0xFFAA00);
                g.setFont(fSmall);
                g.drawString("RESP "+(meleeRespawnTimer[slotIdx]/1000+1)+"s", x+bw/2, y+2, Graphics.HCENTER|Graphics.TOP);
                return;
            }
            g.setColor(0xFF3333); g.setFont(fSmall);
            g.drawString(label!=null?label+"X":"DEAD", x+2, y+1, Graphics.LEFT|Graphics.TOP);
            return;
        }
        int fill = sm.maxHealth>0 ? bw*sm.health/sm.maxHealth : 0;
        int col  = sm.health > sm.maxHealth/2 ? GameData.CHAR_COLOR[cid] :
                  (sm.health > sm.maxHealth/4 ? 0xFFCC00 : 0xFF3333);
        g.setColor(col); g.fillRect(x, y, fill, bh);
        g.setColor(C_WHITE); g.setFont(fSmall);
        String nm = label!=null ? label : GameData.CHAR_NAME[cid].substring(0,
                        Math.min(5,GameData.CHAR_NAME[cid].length()));
        g.drawString(nm, x+2, y+1, Graphics.LEFT|Graphics.TOP);
    }

    // ==================================================================
    //  Pixel text renderer (J2ME-safe, no binary literals)
    // ==================================================================
    private void drawPixelText(Graphics g, String text, int x, int y, int color, int scale) {
        // Pixel font: 3x5 grid per character. Bit 14=top-left, bit 0=bottom-right.
        // Each row is 3 bits, 5 rows = 15 bits total.
        final int[] CHARS_CODE = { 83,84,73,67,75,70,71,72,65,66,68,69,76,78,79,80,82,85,87,89,48,49,50,51,52,53,54,55,56,57,32,86,88,77,90,81,74,33,46 };
        final int[] PIXELS = { 31183,29842,29847,31015,23469,31140,31087,23533,31725,27566,27566,27502,31143,31143,18727,24557,31599,31716,31716,31661,23407,23421,23421,23186,31599,11415,29671,29647,23497,31183,31215,29257,31727,31695,0,23378,23213,24429,29351,31609,4719,9346,2 };
        int cx = x;
        for (int ci = 0; ci < text.length(); ci++) {
            int code = (int) Character.toUpperCase(text.charAt(ci));
            int bits = 0;
            for (int j = 0; j < CHARS_CODE.length; j++) {
                if (CHARS_CODE[j] == code) { bits = PIXELS[j]; break; }
            }
            g.setColor(color);
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 3; col++) {
                    if ((bits & (1 << (14 - row * 3 - col))) != 0) {
                        g.fillRect(cx + col * (scale + 1), y + row * (scale + 1), scale, scale);
                    }
                }
            }
            cx += 3 * (scale + 1) + scale + 1;
        }
    }

    // Demo stickman for menu
    private void drawDemoStick(Graphics g, int sx, int sy, boolean right, int color, int state) {
        int dir=right?1:-1;
        int fY=sy, wY=sy-12, cY=sy-22, hY=sy-32;
        g.setColor(color);
        g.drawArc(sx-5,hY-5,10,10,0,360);
        g.fillRect(sx+dir*2,hY-1,2,2);
        g.drawLine(sx,cY,sx,wY);
        g.drawLine(sx,wY,sx-6,fY+(menuAnimFrame%4<2?5:-5)/2);
        g.drawLine(sx,wY,sx+6,fY-(menuAnimFrame%4<2?5:-5)/2);
        switch(state) {
            case 1: g.drawLine(sx,cY,sx+dir*15,cY-2); g.drawLine(sx,cY,sx-dir*7,cY+7); break;
            case 2: g.drawLine(sx,cY,sx+dir*9,cY-5); g.setColor(0x8888FF); g.drawLine(sx+dir*8,cY-7,sx+dir*8,cY+5); break;
            case 3: g.drawLine(sx,cY,sx-10,cY-5); g.drawLine(sx,cY,sx+10,cY-5); break;
            default: int sw=(menuAnimFrame%4<2)?5:-5; g.drawLine(sx,cY,sx-8,cY+sw); g.drawLine(sx,cY,sx+8,cY-sw); break;
        }
    }

    // ==================================================================
    //  Key handling
    // ==================================================================
    protected void keyPressed(int keyCode) {
        int ga=getGameAction(keyCode);

        // LOGO: any key skips
        if (gameState==GS_LOGO) { gameState=GS_MENU; simBgmTimer=0; setPendingBGM(SoundManager.BGM_MENU); return; }

        // MENU
        if (gameState==GS_MENU) {
            if (ga==UP||keyCode==KEY_NUM4||keyCode==KEY_NUM2) {
                menuSel = (menuSel - 1 + MENU_ITEMS) % MENU_ITEMS;
                menuScrollTarget = Math.max(0, menuSel - MENU_VISIBLE + 1);
                simSound(SSND_MENU);
            } else if (ga==DOWN||keyCode==KEY_NUM8||keyCode==KEY_NUM6) {
                menuSel = (menuSel + 1) % MENU_ITEMS;
                menuScrollTarget = Math.max(0, menuSel - MENU_VISIBLE + 1);
                simSound(SSND_MENU);
            } else if ((ga==FIRE||keyCode==KEY_NUM5) && !isGuarded()) {
                armGuard(); menuAction();
                simSound(SSND_MENU);
            } else if (keyCode==KEY_POUND) {
                gameState=GS_CHAR_SEL; charSel=SaveData.selectedChar;
            } else if (keyCode==KEY_STAR) {
                // Toggle music on/off
                SaveData.musicEnabled = !SaveData.musicEnabled;
                simBgmTimer=0; // music toggle
                SaveData.save();
                showBigMsg(SaveData.musicEnabled ? "MUSIC: ON" : "MUSIC: OFF", C_CYAN);
            }
            return;
        }

        // CHAR SELECT
        if (gameState==GS_CHAR_SEL) {
            if (ga==LEFT||keyCode==KEY_NUM4)  charSel=(charSel-1+GameData.CHAR_COUNT)%GameData.CHAR_COUNT;
            if (ga==RIGHT||keyCode==KEY_NUM6) charSel=(charSel+1)%GameData.CHAR_COUNT;
            if ((ga==FIRE||keyCode==KEY_NUM5) && !isGuarded()) {
                armGuard();
                if (!GameData.CHAR_IS_BOSS[charSel]) {
                    if (SaveData.isCharUnlocked(charSel)) {
                        SaveData.selectedChar=charSel; playerCharId=charSel; SaveData.save();
                        gameState=GS_ARENA_SEL;
                    } else if (SaveData.realmShards>=GameData.CHAR_UNLOCK_COST[charSel]) {
                        SaveData.spendShards(GameData.CHAR_UNLOCK_COST[charSel]);
                        SaveData.unlockChar(charSel);
                        SaveData.selectedChar=charSel; playerCharId=charSel; SaveData.save();
                        showBigMsg(GameData.CHAR_NAME[charSel]+" UNLOCKED!",C_GOLD);
                        gameState=GS_ARENA_SEL;
                    } else { showBigMsg("NEED "+GameData.CHAR_UNLOCK_COST[charSel]+" SHARDS",C_RED); }
                }
            }
            if (keyCode==KEY_NUM0) gameState=GS_MENU;
            return;
        }

        // ARENA SELECT
        if (gameState==GS_ARENA_SEL) {
            if (ga==LEFT||keyCode==KEY_NUM4)  arenaSel=(arenaSel-1+GameData.ARENA_COUNT)%GameData.ARENA_COUNT;
            if (ga==RIGHT||keyCode==KEY_NUM6) arenaSel=(arenaSel+1)%GameData.ARENA_COUNT;
            if ((ga==FIRE||keyCode==KEY_NUM5) && !isGuarded()) {
                if (SaveData.isArenaUnlocked(arenaSel)) {
                    enemyCharId=1+rand.nextInt(GameData.CHAR_COUNT-1); // random from all non-Kael chars
                    pendingShards=0;
                    playerScore=0; enemyScore=0;
                    initRound();
                } else {
                    int cost=GameData.ARENA_UNLOCK[arenaSel];
                    if (SaveData.realmShards>=cost) {
                        SaveData.spendShards(cost); SaveData.unlockArena(arenaSel); SaveData.save();
                        showBigMsg(GameData.ARENA_NAME[arenaSel]+" UNLOCKED!",C_CYAN);
                    } else showBigMsg("NEED "+cost+" SHARDS",C_RED);
                }
            }
            if (keyCode==KEY_STAR) { lorePage=arenaSel; gameState=GS_LORE; }
            if (keyCode==KEY_NUM0) gameState=GS_CHAR_SEL;
            return;
        }

        // LORE SCREEN
        if (gameState==GS_LORE) {
            if (ga==LEFT||keyCode==KEY_NUM4)  lorePage=(lorePage-1+Arena.ARENA_COUNT+GameData.CHAR_COUNT)%(Arena.ARENA_COUNT+GameData.CHAR_COUNT);
            if (ga==RIGHT||keyCode==KEY_NUM6) lorePage=(lorePage+1)%(Arena.ARENA_COUNT+GameData.CHAR_COUNT);
            if (keyCode==KEY_NUM0) gameState=prevState;
            return;
        }

        // SHOP
        if (gameState==GS_SHOP) {
            if (ga==LEFT||keyCode==KEY_NUM4)  { shopTab=(shopTab-1+4)%4; shopSel=0; }
            if (ga==RIGHT||keyCode==KEY_NUM6) { shopTab=(shopTab+1)%4;   shopSel=0; }
            if (ga==UP||keyCode==KEY_NUM2) {
                int tc=countItemsInTab(shopTab);
                shopSel=(shopSel-1+tc)%tc;
            }
            if (ga==DOWN||keyCode==KEY_NUM8) {
                int tc=countItemsInTab(shopTab);
                shopSel=(shopSel+1)%tc;
            }
            if (ga==FIRE||keyCode==KEY_NUM5) doShopBuy();
            if (keyCode==KEY_NUM0) gameState=GS_MENU;
            return;
        }

        // SKILLS
        if (gameState==GS_SKILLS) {
            if (ga==UP||keyCode==KEY_NUM2)    skillSel=(skillSel-1+GameData.SK_COUNT)%GameData.SK_COUNT;
            if (ga==DOWN||keyCode==KEY_NUM8)  skillSel=(skillSel+1)%GameData.SK_COUNT;
            if (ga==FIRE||keyCode==KEY_NUM5)  doUnlockSkill();
            if (keyCode==KEY_NUM0) gameState=GS_MENU;
            return;
        }

        // HOWTO
        if (gameState==GS_HOWTO) {
            if (keyCode==KEY_NUM0||ga==FIRE) gameState=GS_MENU;
            return;
        }

        // SETTINGS
        if (gameState==GS_SETTINGS) {
            handleSettingsKey(keyCode);
            return;
        }

        // TOURNAMENT bracket
        if (gameState==GS_TOURNAMENT) {
            if ((keyCode==KEY_NUM5||ga==FIRE) && !isGuarded()) {
                armGuard();
                // Start the first available fight
                playerCharId = tournSlots[0];
                enemyCharId  = tournSlots[1];
                arenaSel     = rand.nextInt(Arena.ARENA_COUNT);
                isTournMode  = true; isRankedMode = false;
                playerScore  = 0; enemyScore = 0; pendingShards = 0;
                initRound();
            }
            if (keyCode==KEY_NUM0) { isTournMode=false; gameState=GS_MENU; }
            return;
        }
        if (gameState==GS_CINEMATIC) {
            if (keyCode==KEY_NUM5||ga==FIRE) {
                // Advance
                cinematicTimer = 99999; // force advance on next updateCinematic
                int ch = Math.min(storyChapter, GameData.CHAPTER_COUNT - 1);
                String[] lines = splitLines(playerWonRound ? STORY_VICTORY[ch][0]
                                                : STORY_VICTORY[ch][1]);
                cinematicLine++;
                cinematicTimer = 0;
                if (cinematicLine >= lines.length) finishCinematic();
            }
            if (keyCode==KEY_NUM0) finishCinematic(); // skip all
            return;
        }

        // STORY SELECT
        if (gameState==GS_STORY_SEL) {
            if (ga==UP  ||keyCode==KEY_NUM2) {
                if (storyChapter > 0) storyChapter--;
            }
            if (ga==DOWN||keyCode==KEY_NUM8) {
                int nextCh = storyChapter + 1;
                if (nextCh < GameData.CHAPTER_COUNT) {
                    // Can scroll to next chapter if current chapter is done (or ch0 always free)
                    boolean canAdvance = (storyChapter == 0)
                        || (storyChapter < storyDone.length && storyDone[storyChapter]);
                    if (canAdvance) storyChapter = nextCh;
                }
            }
            if ((keyCode==KEY_NUM5||ga==FIRE) && !isGuarded()) {
                // Go to story intro - guard so keyRepeated can't fire startStoryFight
                scrollY = SH;
                gameState = GS_STORY_INTRO;
                armGuard();
            }
            if (keyCode==KEY_NUM0 && !isGuarded()) { gameState = GS_MENU; armGuard(); }
            return;
        }

        // VICTORY ANIM - press to skip to cinematic
        if (gameState==GS_VICTORY_ANIM) {
            if ((keyCode==KEY_NUM5||ga==FIRE) && !isGuarded()) { armGuard(); victoryAnimTimer = 0; }
            return;
        }

        // SHARD REWARD
        if (gameState==GS_SHARD_REWARD) {
            if ((keyCode==KEY_NUM5||ga==FIRE) && !isGuarded()) { armGuard(); playerScore=0; enemyScore=0; initRound(); }
            if (keyCode==KEY_NUM0 && !isGuarded()) { armGuard(); gameState=GS_MENU; simBgmTimer=0; }
            return;
        }

        // GAME OVER
        if (gameState==GS_GAME_OVER) {
            if ((keyCode==KEY_NUM5||ga==FIRE) && !isGuarded()) {
                armGuard(); playerScore=0; enemyScore=0; pendingShards=0;
                if (prevGameState==GS_SURVIVAL) { initSurvival(); }
                else if (prevGameState==GS_MELEE) { initMelee(); }
                else if (isStoryFight) { initRound(); }
                else { isStoryFight=false; initRound(); }
            }
            if (keyCode==KEY_NUM0 && !isGuarded()) {
                armGuard(); playerScore=0; enemyScore=0; isStoryFight=false;
                gameState=GS_MENU; simBgmTimer=0;
            }
            return;
        }

        // STORY INTRO
        if (gameState==GS_STORY_INTRO) {
            if ((keyCode==KEY_NUM5||ga==FIRE) && !isGuarded()) {
                armGuard(); startStoryFight(); return;
            }
            if (keyCode==KEY_NUM0 && !isGuarded()) {
                armGuard(); gameState=GS_STORY_SEL; return;
            }
            return;
        }

        // PAUSE OVERLAY
        if (gameState==GS_PAUSE) {
            if (ga==UP||keyCode==KEY_NUM2||keyCode==KEY_NUM4)
                pauseSel = (pauseSel-1+2)%2;
            if (ga==DOWN||keyCode==KEY_NUM8||keyCode==KEY_NUM6)
                pauseSel = (pauseSel+1)%2;
            if ((ga==FIRE||keyCode==KEY_NUM5) && !isGuarded()) {
                armGuard();
                if (pauseSel==0) {
                    // Resume - restore correct game mode
                    gamePaused=false;
                    gameState = (prevGameState == GS_MELEE) ? GS_MELEE :
                                (prevGameState == GS_SURVIVAL) ? GS_SURVIVAL : GS_GAME;
                    simSound(SSND_MENU);
                } else {
                    // Exit to menu
                    gamePaused=false;
                    playerScore=0; enemyScore=0;
                    gameState=GS_MENU;
                    simBgmTimer=0;
                    simSound(SSND_MENU);
                }
            }
            // LSK or RSK also resumes
            // BUG FIX: restore correct mode on resume
            if (keyCode==-6||keyCode==-21||keyCode==-1||keyCode==-7||keyCode==KEY_NUM0) {
                gamePaused=false;
                gameState = (prevGameState==GS_MELEE)?GS_MELEE:(prevGameState==GS_SURVIVAL)?GS_SURVIVAL:GS_GAME;
            }
            return;
        }

        // IN-GAME
        if (gameState==GS_MELEE_END) {
            if ((keyCode==KEY_NUM5||ga==FIRE) && !isGuarded()) {
                armGuard();
                if (meleeRound > 3) {
                    meleeRound=1; meleePlayerWins=0; meleeAIWins=0;
                    gameState=GS_MENU; setPendingBGM(SoundManager.BGM_MENU);
                } else {
                    initMelee();
                }
            }
            if (keyCode==KEY_NUM0 && !isGuarded()) {
                armGuard(); meleeRound=1; meleePlayerWins=0; meleeAIWins=0;
                gameState=GS_MENU; setPendingBGM(SoundManager.BGM_MENU);
            }
            return;
        }
        if (gameState==GS_GAME||gameState==GS_SURVIVAL||gameState==GS_MELEE) {
            // LSK (left soft key) = pause - never UP/DOWN dpad
            if (ga != UP && ga != DOWN && (keyCode == -6 || keyCode == -21 || keyCode == -1)) {
                gamePaused = true; pauseSel = 0;
                prevGameState = gameState;
                gameState = GS_PAUSE;
                simSound(SSND_MENU);
                return;
            }
            if (keyCode==KEY_NUM2) { jumpQueued=true; SoundManager.playSFX(SoundManager.SFX_JUMP); }
            // TAUNT: press 8 while grounded
            if (keyCode==KEY_NUM8 && player!=null && player.alive && player.onGround) {
                showBigMsg(GameData.CHAR_NAME[playerCharId]+" TAUNTS!", GameData.CHAR_COLOR[playerCharId]);
                SoundManager.playSFX(SoundManager.SFX_POWERUP);
                spawnParticles((int)player.x,(int)(player.y-20),GameData.CHAR_COLOR[playerCharId],12);
                triggerShake(3, 150);
            }
            if (keyCode==KEY_NUM1)         ultraQueued=true;
            // KEY_NUM0: DODGE ROLL when airborne, BLOCK when on ground
            if (keyCode==KEY_NUM0) {
                if (player!=null && player.alive && !player.onGround) {
                    tryDodgeRoll(0);   // v7: air dodge
                } else {
                    blockHeld=true;    // ground block (unchanged behaviour)
                }
            }
            if (keyCode==KEY_STAR && player!=null && player.specialReady) {
                player.fireSpecial();
                showBigMsg(GameData.CHAR_SPECIAL_NAME[playerCharId]+"!", GameData.CHAR_SPECIAL_COLOR[playerCharId]);
                simSound(SSND_SPECIAL);
            } else if (keyCode==KEY_STAR && (player==null || !player.specialReady)) {
                // Show controls tooltip when * pressed but special not ready
                showTooltip("2:jump 5:atk 1:ultra 0:blk/dodge(air)", "#:StickTension  *:special");
            }
            // STICKTENSION - # key. Check this BEFORE throwQueued so grenade
            // doesn't accidentally drop+explode on the player when activating.
            if (keyCode==KEY_POUND && player!=null && player.alive) {
                if (stPhase==ST_IDLE) {
                    boolean specialFull = player.specialCharge >= Stickman.SP_CHARGE_MAX && player.specialReady;
                    boolean ultraFull   = player.ultraCooldown <= 0;
                    if (specialFull && ultraFull) {
                        activateSticktension();
                        // Do NOT fall through to throw - domain is activating
                    } else {
                        if (!specialFull && !ultraFull) showBigMsg("NEED FULL SPECIAL + ULTRA!", 0xFF8800);
                        else if (!specialFull) showBigMsg("NEED FULL SPECIAL!", 0xFF8800);
                        else                  showBigMsg("NEED FULL ULTRA!", 0xFF8800);
                        throwQueued=true; // not activating, treat # as throw/drop
                    }
                } else {
                    // Domain already active, # = throw
                    throwQueued=true;
                }
            }
            // Power keys 3/7/9 - combat abilities with cooldowns
            if (keyCode==KEY_NUM3 && player!=null && player.alive) {
                if (power3Cooldown <= 0) {
                    // HEAL BURST: restore 25% max HP
                    int heal = player.maxHealth / 4;
                    player.health = Math.min(player.maxHealth, player.health + heal);
                    power3Cooldown = P3_CD;
                    spawnParticles((int)player.x, (int)(player.y-20), 0x00FF88, 12);
                    showBigMsg("HEAL BURST! +" + (heal/10) + " HP", 0x00FF88);
                    SoundManager.playSFX(SoundManager.SFX_POWERUP);
                } else {
                    showBigMsg("HEAL: " + (power3Cooldown/1000+1) + "s", 0x336633);
                }
            }
            if (keyCode==KEY_NUM7 && player!=null && player.alive) {
                if (power7Cooldown <= 0) {
                    // SPEED BOOST: double speed for 5s
                    player.speedBoost = true;
                    power7Cooldown = P7_CD;
                    power7Timer    = 5000;
                    spawnParticles((int)player.x, (int)(player.y-10), 0x00CCFF, 10);
                    showBigMsg("SPEED BOOST! 5s", 0x00CCFF);
                    SoundManager.playSFX(SoundManager.SFX_POWERUP);
                } else {
                    showBigMsg("SPEED: " + (power7Cooldown/1000+1) + "s", 0x224466);
                }
            }
            if (keyCode==KEY_NUM9 && player!=null && player.alive) {
                if (power9Cooldown <= 0) {
                    // SHIELD WALL: blocks all damage for 3s
                    player.shieldWall = true;
                    power9Cooldown = P9_CD;
                    power9Timer    = 3000;
                    spawnParticles((int)player.x, (int)(player.y-14), 0xFFAA00, 14);
                    showBigMsg("SHIELD WALL! 3s", 0xFFAA00);
                } else {
                    showBigMsg("SHIELD: " + (power9Cooldown/1000+1) + "s", 0x443300);
                }
            }
            // Legacy powerup slot activation
            if (keyCode==KEY_NUM7 && power7Cooldown > 0) {} // handled above
            if (keyCode==KEY_NUM9 && power9Cooldown > 0) {} // handled above
        }
    }

    /** Suppress ALL key repeats - on real J2ME phones keyRepeated fires
     *  for every held key and routes to keyPressed if not overridden.
     *  We only want LEFT/RIGHT to repeat for menu scroll; everything else
     *  must be a clean single press. */
    protected void keyRepeated(int keyCode) {
        // Allow LEFT/RIGHT/UP/DOWN repeat in menus for smooth scrolling
        int ga = getGameAction(keyCode);
        if (ga == LEFT || ga == RIGHT) {
            keyPressed(keyCode);  // allow scroll repeat
        }
        // All other keys (FIRE, NUM5, # etc) - swallow the repeat
        // so state transitions don't fire twice
    }

    protected void keyReleased(int keyCode) {
        if (keyCode==KEY_NUM0) blockHeld=false;
    }

    private void menuAction() {
        switch (menuSel) {
            case 0: // Story
                storyChapter = Math.max(0, Math.min(SaveData.chapterProgress,
                               GameData.CHAPTER_COUNT - 1));
                scrollY = SH;
                setPendingBGM(SoundManager.BGM_STORY);
                gameState = GS_STORY_SEL; break;
            case 1: // VS AI
                playerCharId=SaveData.selectedChar;
                enemyCharId=1; arenaSel=0;
                isRankedMode=false; isTournMode=false;
                gameState=GS_CHAR_SEL; break;
            case 2: // Survival
                playerCharId=SaveData.selectedChar;
                isRankedMode=false; isTournMode=false;
                initSurvival(); break;
            case 3: // Melee Brawl
                playerCharId=SaveData.selectedChar;
                isRankedMode=false; isTournMode=false;
                initMelee(); break;
            case 4: // Ranked Mode
                playerCharId=SaveData.selectedChar;
                gameState=GS_CHAR_SEL;
                isRankedMode=true; isTournMode=false;
                showBigMsg("RANKED MODE! Rank: " + RANK_NAMES[rankedRank], RANK_COLS[rankedRank]);
                break;
            case 5: // Tournament
                initTournament(); break;
            case 6: // Shop
                shopTab=0; shopSel=0; gameState=GS_SHOP; break;
            case 7: // Skills
                gameState=GS_SKILLS; break;
            case 8: // How to play
                gameState=GS_HOWTO; break;
            case 9: // Settings
                settingsSel=0; gameState=GS_SETTINGS; break;
        }
    }
}