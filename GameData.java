/**
 * GameData — master lore, character, shop, and progression data.
 * StickFight v3.0 ETERNAL REALMS
 *
 * All strings SHORT (J2ME heap friendly).
 */
public class GameData {

    // ==================================================================
    //  CHARACTERS
    // ==================================================================
    public static final int CHAR_COUNT = 12;  // 8 original + 4 new

    public static final int CH_KAEL   = 0;
    public static final int CH_IGNIS  = 1;
    public static final int CH_FROST  = 2;
    public static final int CH_NEXUS  = 3;
    public static final int CH_ZHARA  = 4;
    public static final int CH_SHADOW = 5;
    public static final int CH_TITAN  = 6;
    public static final int CH_QUEEN  = 7;
    public static final int CH_BLADE  = 8;   // Bladestorm — spin attack specialist
    public static final int CH_VOLT   = 9;   // Voltra — electric dash fighter
    public static final int CH_GOLEM  = 10;  // Stone Golem — heavy armor bruiser
    public static final int CH_WRAITH = 11;  // Wraith — teleport & illusion rogue

    public static final String[] CHAR_NAME = {
        "KAEL", "IGNIS", "FROSTBANE", "NEXUS",
        "ZHARA", "SHADOWBORN", "TITAN", "FROST QUEEN",
        "BLADESTORM", "VOLTRA", "GOLEM", "WRAITH"
    };

    public static final String[] CHAR_TITLE = {
        "The Wanderer",    "Ember Champion",
        "Ice Knight",      "Cyber Soldier",
        "Spirit Caller",   "Void Rogue",
        "Fire Colossus",   "Ice Sovereign",
        "Wind Dancer",     "Lightning Striker",
        "Mountain Born",   "Phantom"
    };

    // Lore — split on \n for rendering
    public static final String[] CHAR_LORE_A = {
        "No realm claims him.",
        "Born in lava, forged",
        "Cursed by the Queen.",
        "Augmented in Arcadia.",
        "The jungle chose her.",
        "Shadow-touched outcast.",
        "The Crucible's guardian.",
        "She froze a thousand",
        "A thousand blades become",
        "Struck by lightning,",
        "Carved from the mountain",
        "Neither alive nor dead."
    };
    public static final String[] CHAR_LORE_B = {
        "He fights for no king.",
        "in the Crucible's fire.",
        "Cold to the bone.",
        "Half man, half machine.",
        "Spirits answer her call.",
        "Strikes unseen.",
        "Immovable. Unstoppable.",
        "warriors without a word.",
        "one with the wind.",
        "born with purpose.",
        "core, heart of stone.",
        "Passes through walls."
    };

    // Base stats: [HP, speed%, dmg%, def%]  (100 = normal)
    public static final int[][] CHAR_STATS = {
        { 100, 100, 100, 100 },
        { 110,  90, 120,  90 },
        { 120,  80, 100, 120 },
        {  90, 120, 110,  80 },
        {  95, 100, 100, 100 },
        {  85, 130,  90,  70 },
        { 160,  60, 140, 150 },
        { 130,  90, 130, 130 },
        {  95, 140, 130,  75 },
        {  80, 150, 140,  65 },
        { 180,  55, 145, 160 },
        {  75, 145,  95,  60 }
    };

    public static final int[] CHAR_HOME_ARENA    = { 0, 2, 3, 4, 5, 0, 2, 3, 1, 4, 7, 5 };
    public static final int[] CHAR_UNLOCK_COST   = { 0, 0, 0, 0, 180, 250, 0, 0, 220, 280, 300, 320 };
    public static final boolean[] CHAR_IS_BOSS   = {
        false, false, false, false, false, false, true, true,
        false, false, false, false
    };

    public static final int[] CHAR_COLOR = {
        0xFF3333, 0xFF6600, 0x66DDFF, 0x00FF99,
        0x88FF44, 0xAA44FF, 0xFF4400, 0x88EEFF,
        0xFFCC00, 0x00EEFF, 0xBB8844, 0x9966DD
    };

    public static final String[] CHAR_PASSIVE = {
        "Adaptable",     "Ember Aura",    "Permafrost",  "Overclock",
        "Thorn Skin",    "Ghost Step",    "Magma Core",  "Blizzard Veil",
        "Blade Dance",   "Static Charge", "Iron Hide",   "Phase Shift"
    };

    public static final String[] CHAR_PASSIVE_DESC = {
        "+5 dmg after each kill",
        "+15 dmg in lava arenas",
        "Slow attackers on hit",
        "Sprint never slows",
        "Punch-back 5 dmg",
        "Invisible while blocking",
        "Immune to lava",
        "Ice under every step",
        "Spin hits all around",
        "Stun after 3 hits",
        "50% less knockback",
        "10% dodge on hit"
    };

    public static final int[] CHAR_START_WEAPON = {
        1, 10, 3, 9, 11, 3, 3, 6, 3, 9, 3, 3
    };

    // ==================================================================
    //  ARENAS
    // ==================================================================
    public static final int ARENA_COUNT = 8;

    public static final String[] ARENA_NAME = {
        "THE RING",
        "SKY BRIDGE",
        "INFERNAL CRUCIBLE",
        "FROZEN CITADEL",
        "NEO ARCADIA",
        "JUNGLE RUINS OF ZHARA",
        "SPIKE PIT",
        "LAVA CAVE"
    };

    public static final String[] ARENA_SUBTITLE = {
        "Classic battleground",
        "Fight in the clouds",
        "Volcanic battlefield",
        "Eternal ice fortress",
        "Neon city warzone",
        "Ancient spirit temple",
        "Death from below",
        "The molten depths"
    };

    public static final String[] ARENA_LORE_A = {
        "Where legends begin.",
        "One wrong step -",
        "Forged by the Fire Titan.",
        "The Frost Queen's curse.",
        "Gravity anomalies warp",
        "Spirit beasts guard",
        "Ancient trap-maze",
        "The molten heart"
    };
    public static final String[] ARENA_LORE_B = {
        "Fight to claim your name.",
        "you fall forever.",
        "Lava rewards the bold.",
        "Slippery. Deadly.",
        "combat. Arcadia burns.",
        "these ruins. Don't linger.",
        "of the old war.",
        "of the world. Don't fall."
    };

    // Unlock: 0=free, positive=shard cost
    public static final int[] ARENA_UNLOCK = { 0, 0, 100, 120, 200, 180, 0, 80 };

    public static final String[] ARENA_HAZARD = {
        "None",           "Falling hazard",
        "Lava rivers",    "Slippery ice floor",
        "Gravity shifts", "Poison plants",
        "Spike traps",    "Lava floor (instant kill)"
    };

    public static final int[] ARENA_HOME_CHAR = { 0, 0, 1, 2, 3, 4, 0, 0 };

    // Special arena effect IDs
    public static final int FX_NONE      = 0;
    public static final int FX_LAVA_RISE = 1;  // periodic lava flood
    public static final int FX_ICE_FLOOR = 2;  // reduced friction
    public static final int FX_GRAVITY   = 3;  // random gravity dips
    public static final int FX_POISON    = 4;  // standing on ground hurts slowly

    public static final int[] ARENA_FX = {
        FX_NONE, FX_NONE, FX_LAVA_RISE, FX_ICE_FLOOR,
        FX_GRAVITY, FX_POISON, FX_NONE, FX_NONE
    };

    // ==================================================================
    //  SHOP — ETERNAL BAZAAR
    // ==================================================================
    public static final int SHOP_WEAPON   = 0;
    public static final int SHOP_ARMOR    = 1;
    public static final int SHOP_COSMETIC = 2;
    public static final int SHOP_POWERUP  = 3;

    public static final int SHOP_ITEM_COUNT = 20;

    public static final String[] SHOP_NAME = {
        "Soulbound Blade",    "Plasma Rifle",     "Enchanted Staff",  "Magma Cannon",
        "Frostfang Sword",    "Arc Disruptor",    "Spirit Spear",     "Voidpiercer",
        "Dragonhide Suit",    "Cyber Plating",    "Frost Shield",     "Shadowweave",
        "Shadowborn Knight",  "Cyber Samurai",    "Ember Lord",       "Frost Valkyrie",
        "Blessing:Ancients",  "Chrono Shift",     "Realm Surge",      "Void Cloak"
    };

    public static final String[] SHOP_DESC_A = {
        "+10 dmg per combo hit",  "Plasma pierces shields",  "Fires homing orbs",  "Lava blobs burn floor",
        "Hits slow the enemy",    "Stuns on hit",            "Pierces+returns",     "Void tips ignore armor",
        "25% bullet reduction",   "+20 HP, fast reload",     "Blocks 1 free hit",  "Invis while still",
        "Dark armor skin",        "Neon katana skin",        "Molten body glow",   "Ice crystal trim",
        "+50% dmg for 10s",       "+80% speed for 8s",       "Full HP + ULTRA now", "Invis 5s, shadow atk"
    };

    public static final int[] SHOP_CATEGORY = {
        0,0,0,0, 0,0,0,0,
        1,1,1,1,
        2,2,2,2,
        3,3,3,3
    };

    public static final int[] SHOP_COST = {
        200,280,320,400,
        300,380,260,350,
        180,220,250,300,
        80,100,120,90,
        50,60,80,70
    };

    // Maps to Weapon.TYPE_* (-1 = special effect, no weapon assigned)
    public static final int[] SHOP_WEAPON_ID = {
        3,9,2,10, 3,9,11,11,
        -1,-1,-1,-1,
        -1,-1,-1,-1,
        -1,-1,-1,-1
    };

    // 0=common 1=uncommon 2=rare 3=legendary
    public static final int[] SHOP_RARITY = {
        2,2,3,3, 2,2,2,3,
        1,2,2,3,
        0,1,2,1,
        1,1,3,2
    };

    public static final int[] RARITY_COLOR = {
        0x888888, 0x44CC44, 0x4488FF, 0xFFAA00
    };

    // ==================================================================
    //  SKILL TREE (6 skills, unlocked per character)
    // ==================================================================
    public static final int SK_COUNT    = 6;
    public static final int SK_DMG_UP   = 0;
    public static final int SK_SPEED_UP = 1;
    public static final int SK_HP_UP    = 2;
    public static final int SK_COMBO4   = 3;
    public static final int SK_ULTRA_CD = 4;
    public static final int SK_PASSIVE2 = 5;

    public static final String[] SK_NAME = {
        "Power Up", "Swift Feet", "Iron Body",
        "Ext. Combo", "Ultra Master", "Second Wind"
    };
    public static final String[] SK_DESC = {
        "+15% damage",      "+20% speed",    "+25 max HP",
        "4-hit combo kick", "Ultra CD -1.5s","Passive Lv2"
    };
    public static final int[] SK_COST = { 80, 80, 100, 150, 200, 250 };

    // ==================================================================
    //  PROGRESSION
    // ==================================================================
    public static final int SHARDS_PER_WIN      = 20;
    public static final int SHARDS_SURVIVAL_WAVE = 5;
    public static final int SHARDS_BOSS_BONUS    = 100;

    public static final int[] CHAPTER_BOSS_CHAR = { 1, 6, 7, 3, 5 };
    // Aliases used by story select screen
    public static final int[] CHAPTER_BOSS  = CHAPTER_BOSS_CHAR;
    public static final int[] CHAPTER_ARENA = { 2, 2, 3, 4, 5 };  // Crucible,Crucible,FrozenCitadel,NeoArcadia,JungleRuins
    public static final int CHAPTER_COUNT = 5;
    public static final String[] CHAPTER_NAME   = {
        "WANDERER'S TRIAL",
        "FIRES OF THE CRUCIBLE",
        "FROZEN CITADEL SIEGE",
        "NEO ARCADIA UPRISING",
        "BEYOND THE VOID GATE"
    };


    // ==================================================================
    //  SPECIAL MOVES (8 character-specific ultimates)
    // ==================================================================
    public static final int SP_NONE          = 0;
    public static final int SP_DRAGON_BREATH = 1;
    public static final int SP_LIGHTNING     = 2;
    public static final int SP_SOUL_REAP     = 3;
    public static final int SP_PERMAFROST    = 4;
    public static final int SP_GRAVITY_WELL  = 5;
    public static final int SP_VINE_CAGE     = 6;
    public static final int SP_MAGMA_SLAM    = 7;
    public static final int SP_BLIZZARD      = 8;

    // Special name per character
    public static final String[] CHAR_SPECIAL_NAME = {
        "LIGHTNING STORM",
        "DRAGON BREATH",
        "PERMAFROST",
        "GRAVITY WELL",
        "VINE CAGE",
        "SOUL REAP",
        "MAGMA SLAM",
        "BLIZZARD",
        "BLADE TORNADO",
        "VOLT SURGE",
        "STONE CRASH",
        "PHANTOM STEP"
    };

    // Short desc
    public static final String[] CHAR_SPECIAL_DESC = {
        "6 bolts rain. Chain lightning.",
        "Flamethrower nova. Burns all.",
        "Freeze field. Stops enemies.",
        "Drop well. Sucks foes in.",
        "Root + poison in range.",
        "Dash + lifesteal 50% HP.",
        "Slam down. 90dmg shockwave.",
        "Screen blizzard. Slow + DoT.",
        "Spin storm. 360 hit+knockback.",
        "Electro dash. 3x chain stun.",
        "Rock slam. 110dmg. Shatter.",
        "Teleport behind. Backstab."
    };

    // Color to flash when special fires
    public static final int[] CHAR_SPECIAL_COLOR = {
        0x0088FF, 0xFF4400, 0x88CCFF, 0x00FF88,
        0x44CC44, 0xAA44FF, 0xFF6600, 0xCCEEFF,
        0xFFCC00, 0x00CCFF, 0x886633, 0xCC88FF
    };

    // Unlock condition text
    public static final String SPECIAL_UNLOCK_TEXT = "2 KILLS -> SPECIAL READY [HOLD 5+2]";

    // ==================================================================
    //  HELPERS
    // ==================================================================
    public static String rarityLabel(int r) {
        if (r == 3) return "LEGENDARY";
        if (r == 2) return "RARE";
        if (r == 1) return "UNCOMMON";
        return "COMMON";
    }

    public static int getCharHP(int id)    { return CHAR_STATS[id][0]; }
    public static int getCharSpeed(int id) { return CHAR_STATS[id][1]; }
    public static int getCharDmg(int id)   { return CHAR_STATS[id][2]; }
    public static int getCharDef(int id)   { return CHAR_STATS[id][3]; }
}
