import javax.microedition.rms.*;

/**
 * SaveData — persistent player profile.
 * StickFight v3.0 ETERNAL REALMS
 *
 * Stores to RecordStore "SFPRO3" as a compact CSV string:
 *   shards,selectedChar,chapterProgress,
 *   unlockedChars(bitmask),unlockedArenas(bitmask),ownedItems(bitmask),
 *   skill0..skill7(bitmask each),
 *   survivalBest,totalKills,totalWins,
 *   powerup0..powerup3(count each)
 *
 * All operations are silent (no crash if RMS unavailable).
 */
public class SaveData {

    private static final String RS_NAME = "SFPRO3";

    // ---- In-memory state ------------------------------------------------
    public static int   realmShards      = 500;  // starting bonus
    public static int   selectedChar     = GameData.CH_KAEL;
    public static int   chapterProgress  = 0;    // highest chapter reached
    public static int   storyDoneMask    = 0;    // bitmask: bit i = chapter i complete

    // Bitmask: bit i set = item/arena/char unlocked or owned
    public static int   unlockedChars    = 0x01; // Kael free
    public static int   unlockedArenas   = 0x03; // Ring + SkyBridge free + index2/3 locked
    public static int   ownedShopItems   = 0;    // bitmask of shop items bought

    // Skill trees: one int per character, each bit = skill unlocked
    public static int[] charSkills       = new int[GameData.CHAR_COUNT];

    // Stat boosts from skills applied in-session
    public static int   skillDmgBonus    = 0;   // flat dmg added
    public static int   skillSpeedBonus  = 0;   // % added
    public static int   skillHPBonus     = 0;   // flat HP added

    // Active power-up inventory (count per type)
    public static int[] powerupCount     = new int[4];

    // Active power-up in play this round
    public static int   activePowerup    = -1;  // -1 = none
    public static int   powerupTimer     = 0;   // ms remaining

    // Session stats
    public static int   survivalBest     = 0;
    public static int   totalKills       = 0;
    public static int   totalWins        = 0;
    public static int   totalDeaths      = 0;
    public static int   comboRecord      = 0;  // best combo chain ever
    public static int   dailyBonus       = 0;  // shards from daily bonus today
    public static long  lastDayMs        = 0;  // timestamp of last daily bonus
    // Achievement flags (bitmask)
    public static int   achievements     = 0;
    // ACH: 0=first win 1=10kills 2=survive5waves 3=all chapters 4=beat all chars 5=100 total wins

    // Cosmetic: which skin is active (index into shop cosmetics section)
    public static int   activeSkin       = -1;  // -1 = default

    // Settings
    public static boolean sfxEnabled    = true;
    public static boolean musicEnabled  = true;
    public static int     volume        = 80;    // 0-100
    public static int     brightness    = 50;    // 0-100 (simulated overlay)
    // Equipped weapon slot per character (-1 = default)
    public static int[]   equippedWeapon = new int[12]; // per char, index into shop weapons 0-7

    // Passive boost tracking
    public static int   killComboBonus   = 0;   // Kael passive: per-kill stack

    private static boolean loaded = false;

    // ==================================================================
    //  Load / Save
    // ==================================================================

    /** Returns the weapon type (Weapon.TYPE_*) equipped for charId, or -1 for default. */
    // --- Story progress helpers (canvas calls these instead of direct field access) ---
    /** Returns true if chapter ch has been completed. */
    public static boolean isChapterDone(int ch) {
        return (storyDoneMask & (1 << ch)) != 0;
    }
    /** Mark chapter ch as completed and persist it. */
    public static void setChapterDone(int ch) {
        storyDoneMask |= (1 << ch);
    }

    public static int getEquippedWeaponType(int charId) {
        if (equippedWeapon == null || charId < 0 || charId >= equippedWeapon.length) return -1;
        return equippedWeapon[charId]; // index 0-7 = shop weapon slot, -1 = default
    }

    public static void equipWeapon(int charId, int shopWeaponIdx) {
        if (equippedWeapon == null) equippedWeapon = new int[12];
        if (charId >= 0 && charId < equippedWeapon.length)
            equippedWeapon[charId] = shopWeaponIdx;
    }

    public static void unequipWeapon(int charId) {
        if (equippedWeapon != null && charId >= 0 && charId < equippedWeapon.length)
            equippedWeapon[charId] = -1;
    }

    public static void load() {
        if (loaded) return;
        loaded = true;
        try {
            RecordStore rs = RecordStore.openRecordStore(RS_NAME, false);
            RecordEnumeration en = rs.enumerateRecords(null, null, false);
            if (en.hasNextElement()) {
                String s = new String(en.nextRecord(), "UTF-8");
                parse(s);
            }
            en.destroy();
            rs.closeRecordStore();
        } catch (Exception e) {
            // First launch or RMS unavailable — use defaults
            resetDefaults();
        }
    }

    public static void save() {
        try {
            try { RecordStore.deleteRecordStore(RS_NAME); } catch (Exception ignore) {}
            RecordStore rs = RecordStore.openRecordStore(RS_NAME, true);
            String s = serialize();
            byte[] data = s.getBytes("UTF-8");
            rs.addRecord(data, 0, data.length);
            rs.closeRecordStore();
        } catch (Exception e) {
            // Silent fail
        }
    }

    public static void resetDefaults() {
        realmShards     = 500;
        selectedChar    = 0;
        chapterProgress = 0;
        storyDoneMask   = 0;
        unlockedChars   = 0x01;
        unlockedArenas  = 0x43; // bits 0,1,6 = Ring, SkyBridge, SpikePit free
        ownedShopItems  = 0;
        charSkills      = new int[GameData.CHAR_COUNT];
        powerupCount    = new int[4];
        activePowerup   = -1;
        powerupTimer    = 0;
        survivalBest    = 0;
        totalKills      = 0;
        totalWins       = 0;
        activeSkin      = -1;
        killComboBonus  = 0;
        totalDeaths     = 0;
        comboRecord     = 0;
        lastDayMs       = 0;
        achievements    = 0;
        sfxEnabled      = true;
        musicEnabled    = true;
        volume          = 80;
        brightness      = 50;
        equippedWeapon  = new int[12];
        for (int i = 0; i < 12; i++) equippedWeapon[i] = -1;
    }

    // ==================================================================
    //  Shard economy
    // ==================================================================

    public static void addShards(int amount) {
        realmShards += amount;
        if (realmShards < 0) realmShards = 0;
    }

    public static boolean spendShards(int cost) {
        if (realmShards < cost) return false;
        realmShards -= cost;
        return true;
    }

    // ==================================================================
    //  Daily bonus
    // ==================================================================
    /** Returns shards awarded today (0 if already claimed). */
    public static int claimDailyBonus() {
        long now = System.currentTimeMillis();
        // 20 hours = 72000000ms (lenient so device clock drift ok)
        if (now - lastDayMs > 72000000L) {
            lastDayMs = now;
            int bonus = 50 + (totalWins > 10 ? 50 : 0) + (survivalBest > 5 ? 30 : 0);
            addShards(bonus);
            dailyBonus = bonus;
            save();
            return bonus;
        }
        return 0;
    }

    public static void checkAchievement(int bit) {
        if ((achievements & (1<<bit)) == 0) {
            achievements |= (1<<bit);
            save();
        }
    }

    public static boolean hasAchievement(int bit) { return (achievements & (1<<bit)) != 0; }

    // ==================================================================
    //  Unlock helpers
    // ==================================================================

    public static boolean isCharUnlocked(int id) {
        return (unlockedChars & (1 << id)) != 0;
    }

    public static boolean isArenaUnlocked(int id) {
        return (unlockedArenas & (1 << id)) != 0;
    }

    public static boolean isShopItemOwned(int id) {
        return (ownedShopItems & (1 << id)) != 0;
    }

    public static boolean isSkillUnlocked(int charId, int skillId) {
        return (charSkills[charId] & (1 << skillId)) != 0;
    }

    public static void unlockChar(int id) {
        unlockedChars |= (1 << id);
    }

    public static void unlockArena(int id) {
        unlockedArenas |= (1 << id);
    }

    public static void buyShopItem(int id) {
        ownedShopItems |= (1 << id);
    }

    public static void unlockSkill(int charId, int skillId) {
        charSkills[charId] |= (1 << skillId);
    }

    // ==================================================================
    //  Apply skill stats (call before each match)
    // ==================================================================

    public static void applySkills() {
        int cid = selectedChar;
        skillDmgBonus   = isSkillUnlocked(cid, GameData.SK_DMG_UP)   ? 15 : 0;
        skillSpeedBonus = isSkillUnlocked(cid, GameData.SK_SPEED_UP)  ? 20 : 0;
        skillHPBonus    = isSkillUnlocked(cid, GameData.SK_HP_UP)     ? 25 : 0;
    }

    // ==================================================================
    //  Power-ups
    // ==================================================================

    /** Activate a power-up for this session. Returns false if none owned. */
    public static boolean activatePowerup(int slot) {
        if (slot < 0 || slot >= 4) return false;
        if (powerupCount[slot] <= 0) return false;
        powerupCount[slot]--;
        activePowerup = slot;
        switch (slot) {
            case 0: powerupTimer = 10000; break; // Blessing: 10s
            case 1: powerupTimer =  8000; break; // Chrono: 8s
            case 2: powerupTimer =     1; break; // Realm Surge: instant
            case 3: powerupTimer =  5000; break; // Void Cloak: 5s
        }
        return true;
    }

    public static void updatePowerup(int delta) {
        if (activePowerup < 0) return;
        powerupTimer -= delta;
        if (powerupTimer <= 0) {
            activePowerup = -1;
            powerupTimer  = 0;
        }
    }

    public static boolean isPowerupActive(int slot) {
        return activePowerup == slot && powerupTimer > 0;
    }

    // ==================================================================
    //  Serialization
    // ==================================================================

    private static String serialize() {
        StringBuffer sb = new StringBuffer();
        sb.append(realmShards).append(',');
        sb.append(selectedChar).append(',');
        sb.append(chapterProgress).append(',');
        sb.append(unlockedChars).append(',');
        sb.append(unlockedArenas).append(',');
        sb.append(ownedShopItems).append(',');
        for (int i = 0; i < GameData.CHAR_COUNT; i++) {
            sb.append(charSkills[i]).append(',');
        }
        sb.append(survivalBest).append(',');
        sb.append(totalKills).append(',');
        sb.append(totalWins).append(',');
        for (int i = 0; i < 4; i++) {
            sb.append(powerupCount[i]).append(',');
        }
        sb.append(totalDeaths).append(',');
        sb.append(comboRecord).append(',');
        sb.append((int)(lastDayMs/1000)).append(',');
        sb.append(achievements).append(',');
        sb.append(activeSkin).append(',');
        sb.append(sfxEnabled?1:0).append(',');
        sb.append(musicEnabled?1:0).append(',');
        sb.append(volume).append(',');
        sb.append(brightness).append(',');
        for (int i = 0; i < 12; i++) sb.append(equippedWeapon[i]).append(',');
        sb.append(storyDoneMask).append(',');
        sb.append(0); // padding
        return sb.toString();
    }

    private static void parse(String s) {
        try {
            // split on comma
            int pos = 0, next;
            realmShards     = nextInt(s, pos); pos = s.indexOf(',', pos) + 1;
            selectedChar    = nextInt(s, pos); pos = s.indexOf(',', pos) + 1;
            chapterProgress = nextInt(s, pos); pos = s.indexOf(',', pos) + 1;
            unlockedChars   = nextInt(s, pos); pos = s.indexOf(',', pos) + 1;
            unlockedArenas  = nextInt(s, pos); pos = s.indexOf(',', pos) + 1;
            ownedShopItems  = nextInt(s, pos); pos = s.indexOf(',', pos) + 1;
            charSkills = new int[GameData.CHAR_COUNT];
            for (int i = 0; i < GameData.CHAR_COUNT; i++) {
                charSkills[i] = nextInt(s, pos);
                pos = s.indexOf(',', pos) + 1;
                if (pos <= 0) break;
            }
            survivalBest = nextInt(s, pos); pos = s.indexOf(',', pos) + 1;
            totalKills   = nextInt(s, pos); pos = s.indexOf(',', pos) + 1;
            totalWins    = nextInt(s, pos); pos = s.indexOf(',', pos) + 1;
            powerupCount = new int[4];
            for (int i = 0; i < 4; i++) {
                powerupCount[i] = nextInt(s, pos);
                pos = s.indexOf(',', pos) + 1;
                if (pos <= 0) break;
            }
            if (pos > 0 && pos < s.length()) {
                totalDeaths = nextInt(s, pos); pos = s.indexOf(',', pos) + 1;
            }
            if (pos > 0 && pos < s.length()) {
                comboRecord = nextInt(s, pos); pos = s.indexOf(',', pos) + 1;
            }
            if (pos > 0 && pos < s.length()) {
                // lastDayMs stored as int seconds (simplified, safe for RMS)
                lastDayMs   = (long)nextInt(s, pos) * 1000L; pos = s.indexOf(',', pos) + 1;
            }
            if (pos > 0 && pos < s.length()) {
                achievements = nextInt(s, pos); pos = s.indexOf(',', pos) + 1;
            }
            if (pos > 0 && pos < s.length()) {
                activeSkin = nextInt(s, pos);
                pos = s.indexOf(',', pos) + 1;
            }
            if (pos > 0 && pos < s.length()) {
                sfxEnabled   = nextInt(s, pos) != 0; pos = s.indexOf(',', pos) + 1;
                musicEnabled = nextInt(s, pos) != 0; pos = s.indexOf(',', pos) + 1;
                volume       = nextInt(s, pos);      pos = s.indexOf(',', pos) + 1;
                brightness   = nextInt(s, pos);      pos = s.indexOf(',', pos) + 1;
                equippedWeapon = new int[12];
                for (int i = 0; i < 12 && pos > 0 && pos < s.length(); i++) {
                    equippedWeapon[i] = nextInt(s, pos);
                    pos = s.indexOf(',', pos) + 1;
                }
            }
            if (pos > 0 && pos < s.length()) {
                storyDoneMask = nextInt(s, pos); pos = s.indexOf(',', pos) + 1;
            }
        } catch (Exception e) {
            resetDefaults();
        }
    }

    private static int nextInt(String s, int start) {
        int end = s.indexOf(',', start);
        if (end < 0) end = s.length();
        String tok = s.substring(start, end).trim();
        if (tok.length() == 0) return 0;
        return Integer.parseInt(tok);
    }
}
