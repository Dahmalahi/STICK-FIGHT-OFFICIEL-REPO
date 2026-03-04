import javax.microedition.rms.*;

/**
 * HighScore — RecordStore-backed top-10 for Survival mode.
 * StickFight v2.0 PRO MAX
 *
 * Storage format per record: "wavesReached:killCount" as UTF-8 string.
 * Records sorted by wavesReached DESC, then kills DESC.
 * Max 10 entries stored; oldest/lowest dropped when full.
 */
public class HighScore {

    private static final String RS_NAME   = "SFScores";
    private static final int    MAX_ENTRY = 10;

    // In-memory cache (loaded once on demand)
    private static int[]    cachedWaves  = null;
    private static int[]    cachedKills  = null;
    private static int      cachedCount  = 0;

    // ------------------------------------------------------------------
    //  Save a new score
    // ------------------------------------------------------------------

    /**
     * Submits a new score. If it beats any entry in the top-10, it is
     * inserted; the lowest entry is dropped if the list is full.
     *
     * @return rank (1-based) of the new entry, or -1 if not in top-10.
     */
    public static int save(int waves, int kills) {
        load();   // refresh cache

        // Find insertion point (sorted descending by waves, then kills)
        int insertAt = -1;
        for (int i = 0; i < cachedCount; i++) {
            if (waves > cachedWaves[i] ||
                (waves == cachedWaves[i] && kills > cachedKills[i])) {
                insertAt = i;
                break;
            }
        }
        if (insertAt == -1) {
            if (cachedCount < MAX_ENTRY) insertAt = cachedCount;
            else return -1; // not good enough
        }

        // Shift down
        int newCount = Math.min(cachedCount + 1, MAX_ENTRY);
        for (int i = newCount - 1; i > insertAt; i--) {
            cachedWaves[i] = cachedWaves[i - 1];
            cachedKills[i] = cachedKills[i - 1];
        }
        cachedWaves[insertAt] = waves;
        cachedKills[insertAt] = kills;
        cachedCount           = newCount;

        persist();
        return insertAt + 1; // rank
    }

    // ------------------------------------------------------------------
    //  Read top-10 as formatted lines
    // ------------------------------------------------------------------

    /** Returns up to 10 display lines: "#1 Wave:25 Kills:47" */
    public static String[] getLines() {
        load();
        if (cachedCount == 0) return new String[]{ "No scores yet!" };
        String[] lines = new String[cachedCount];
        for (int i = 0; i < cachedCount; i++) {
            lines[i] = (i + 1) + ".  Wave:" + cachedWaves[i] + "  Kills:" + cachedKills[i];
        }
        return lines;
    }

    public static int getTopWave() {
        load();
        return (cachedCount > 0) ? cachedWaves[0] : 0;
    }

    // ------------------------------------------------------------------
    //  Internal helpers
    // ------------------------------------------------------------------

    private static void load() {
        if (cachedWaves != null) return; // already loaded
        cachedWaves = new int[MAX_ENTRY];
        cachedKills = new int[MAX_ENTRY];
        cachedCount = 0;

        try {
            RecordStore rs = RecordStore.openRecordStore(RS_NAME, true);
            RecordEnumeration en = rs.enumerateRecords(null, null, false);
            while (en.hasNextElement() && cachedCount < MAX_ENTRY) {
                byte[] data = en.nextRecord();
                String s    = new String(data, "UTF-8");
                int sep = s.indexOf(':');
                if (sep > 0) {
                    cachedWaves[cachedCount] = Integer.parseInt(s.substring(0, sep));
                    cachedKills[cachedCount] = Integer.parseInt(s.substring(sep + 1));
                    cachedCount++;
                }
            }
            en.destroy();
            rs.closeRecordStore();
        } catch (Exception e) {
            // First run or corrupt: start fresh
            cachedCount = 0;
        }
    }

    private static void persist() {
        try {
            // Delete old store, rewrite
            try { RecordStore.deleteRecordStore(RS_NAME); } catch (Exception e2) { /* ok */ }
            RecordStore rs = RecordStore.openRecordStore(RS_NAME, true);
            for (int i = 0; i < cachedCount; i++) {
                String s    = cachedWaves[i] + ":" + cachedKills[i];
                byte[] data = s.getBytes("UTF-8");
                rs.addRecord(data, 0, data.length);
            }
            rs.closeRecordStore();
        } catch (Exception e) {
            // Silently ignore on devices without RMS
        }
    }

    /** Force reload on next access (call after a new round). */
    public static void invalidate() { cachedWaves = null; }
}
