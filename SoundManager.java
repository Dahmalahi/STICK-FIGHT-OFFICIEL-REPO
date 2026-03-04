import javax.microedition.media.*;
import javax.microedition.media.control.*;
import java.io.*;

/**
 * SoundManager v4 — 100% SYNTHESIZED audio for J2ME CLDC 1.1 / MIDP 2.0
 * NO WAV FILES NEEDED. Generates real PCM audio in memory using math.
 */
public class SoundManager {

    public static final int SFX_CLICK   = 0;
    public static final int SFX_ATTACK  = 1;
    public static final int SFX_HIT     = 2;
    public static final int SFX_ULTRA   = 3;
    public static final int SFX_SPECIAL = 4;
    public static final int SFX_WIN     = 5;
    public static final int SFX_DEATH   = 6;
    public static final int SFX_SHOOT   = 7;
    public static final int SFX_UNLOCK  = 8;
    public static final int SFX_JUMP      = 9;
    public static final int SFX_BLOCK     = 10;  // parry/block clang
    public static final int SFX_LAND      = 11;  // landing thud
    public static final int SFX_EXPLODE   = 12;  // explosion boom
    public static final int SFX_PICKUP    = 13;  // weapon pickup
    public static final int SFX_HEARTBEAT = 14;  // low HP pulse
    public static final int SFX_CRIT      = 15;  // critical hit
    public static final int SFX_HEADSHOT  = 16;  // headshot ding
    public static final int SFX_STREAK    = 17;  // kill streak fanfare
    public static final int SFX_DEATH2    = 18;  // player death wail
    public static final int SFX_POWERUP   = 19;  // power-up activate
    // Aliases
    public static final int SFX_GENERIC = SFX_CLICK;
    public static final int SFX_BLOOD   = SFX_HIT;

    public static final int BGM_MENU   = 0;
    public static final int BGM_COMBAT = 1;
    public static final int BGM_STORY  = 2;
    public static final int BGM_VOID   = 3;

    private static final int SR = 22050;  // sample rate 22kHz

    private static Player   bgmPlayer  = null;
    private static int      currentBGM = -1;
    private static volatile boolean bgmRunning = false;

    public static void init() {}

    public static void playSFX(final int id) {
        if (!SaveData.sfxEnabled) return;
        Thread t = new Thread() {
            public void run() { doPlaySFX(id); }
        };
        t.start();
    }

    private static void doPlaySFX(int id) {
        Player p = null;
        try {
            byte[] wav = makeSFX(id);
            if (wav == null) return;
            p = Manager.createPlayer(new ByteArrayInputStream(wav), "audio/x-wav");
            p.realize();
            p.prefetch();
            setVol(p, SaveData.volume);
            p.start();
            Thread.sleep(800);
        } catch (Exception e) {
        } finally { closeP(p); }
    }

    public static void playBGM(final int id) {
        if (id == currentBGM && bgmPlayer != null) return;
        stopBGM();
        currentBGM = id;
        if (!SaveData.musicEnabled) return;
        bgmRunning = true;
        Thread t = new Thread() {
            public void run() {
                while (bgmRunning && SaveData.musicEnabled) {
                    Player p = null;
                    try {
                        byte[] wav = makeBGM(id);
                        if (wav == null) { Thread.sleep(2000); continue; }
                        p = Manager.createPlayer(new ByteArrayInputStream(wav), "audio/x-wav");
                        p.realize();
                        p.prefetch();
                        setVol(p, SaveData.volume * 65 / 100);
                        bgmPlayer = p;
                        p.start();
                        long dur = p.getDuration();
                        Thread.sleep(dur > 0 ? dur / 1000 : 5000);
                    } catch (Exception e) {
                        try { Thread.sleep(1000); } catch (Exception ex) {}
                    } finally {
                        closeP(p);
                        if (bgmPlayer == p) bgmPlayer = null;
                    }
                }
            }
        };
        t.start();
    }

    public static void stopBGM() {
        bgmRunning = false; currentBGM = -1;
        // Don't call closeP here — it can block on real J2ME devices
        // The BGM thread will exit on its own when bgmRunning=false
        // Just null the reference so we don't start it again
        bgmPlayer = null;
    }
    public static void pauseBGM()  { if (bgmPlayer!=null) try { bgmPlayer.stop();  } catch(Exception e){} }
    public static void resumeBGM() { if (!SaveData.musicEnabled) return; if (bgmPlayer!=null) try { bgmPlayer.start(); } catch(Exception e){} }
    public static void onMusicToggle() {
        if (SaveData.musicEnabled) { if (currentBGM>=0){int t=currentBGM;stopBGM();playBGM(t);} }
        else pauseBGM();
    }
    public static void applyVolumeAll() { setVol(bgmPlayer, SaveData.volume * 65 / 100); }
    public static void releaseAll()     { stopBGM(); }

    // ---- SFX synthesis -------------------------------------------------
    private static byte[] makeSFX(int id) {
        switch (id) {
            case SFX_CLICK:   return tone(880,  60, 1);
            case SFX_ATTACK:  return thud(180,  100);
            case SFX_HIT:     return noise(70,  80);
            case SFX_ULTRA:   return sweep(200, 1000, 300, 2);
            case SFX_SPECIAL: return sweep(400, 1800, 280, 0);
            case SFX_WIN:     return fanfare();
            case SFX_DEATH:   return sweep(700, 100,  500, 2);
            case SFX_SHOOT:   return shootSnd();
            case SFX_UNLOCK:  return chime();
            case SFX_JUMP:      return tone(550,  80, 0);
            case SFX_BLOCK:     return blockClang();
            case SFX_LAND:      return thud(90,  70);
            case SFX_EXPLODE:   return explosion();
            case SFX_PICKUP:    return pickupBleep();
            case SFX_HEARTBEAT: return heartbeat();
            case SFX_CRIT:      return critHit();
            case SFX_HEADSHOT:  return headshotDing();
            case SFX_STREAK:    return streakFanfare();
            case SFX_DEATH2:    return deathWail();
            case SFX_POWERUP:   return powerupRise();
            default:            return tone(440,  80, 0);
        }
    }

    private static byte[] makeBGM(int id) {
        switch (id) {
            case BGM_MENU:   return melody(MENU_NOTES,   MENU_DURS,   true);
            case BGM_COMBAT: return melody(COMBAT_NOTES, COMBAT_DURS, false);
            case BGM_STORY:  return melody(STORY_NOTES,  STORY_DURS,  true);
            case BGM_VOID:   return melody(VOID_NOTES,   VOID_DURS,   false);
            default:         return melody(MENU_NOTES,   MENU_DURS,   true);
        }
    }

    // Waveform shapes: 0=sin-approx 1=square 2=sawtooth
    private static int wave(float ph, int shape) {
        switch (shape) {
            case 1: return (ph < 0.5f) ? 70 : -70;
            case 2: return (int)((ph * 2f - 1f) * 70f);
            default:
                // fast sin approximation  (Bhaskara I)
                float x = ph < 0.5f ? ph * 2f : (ph - 0.5f) * 2f;
                float s = 4f * x * (1f - x);
                return (int)((ph < 0.5f ? s : -s) * 68f);
        }
    }

    private static byte[] tone(int hz, int ms, int shape) {
        int n = SR * ms / 1000;
        byte[] b = new byte[n];
        float per = (float)SR / hz;
        int rel = n / 4;
        for (int i = 0; i < n; i++) {
            int s = wave((i % (int)per) / per, shape);
            if (i > n - rel) s = s * (n - i) / rel;
            b[i] = (byte)(128 + s);
        }
        return wav(b);
    }

    private static byte[] thud(int startHz, int ms) {
        int n = SR * ms / 1000; byte[] b = new byte[n];
        float ph = 0f;
        long seed = 31415926L;
        for (int i = 0; i < n; i++) {
            float prog = (float)i / n;
            float hz = startHz * (1f - prog * 0.8f);
            ph += hz / SR; if (ph >= 1f) ph -= 1f;
            int s = wave(ph, 0) * (n - i) / n;
            if (i < n / 5) {
                seed = seed * 1664525L + 1013904223L;
                int noise = (int)((seed >> 8) & 0xFF) - 128;
                s += noise * (n/5 - i) / (n/5) / 3;
            }
            b[i] = (byte)Math.max(0, Math.min(255, 128 + s));
        }
        return wav(b);
    }

    private static byte[] noise(int ms, int amp) {
        int n = SR * ms / 1000; byte[] b = new byte[n];
        long seed = 271828L;
        for (int i = 0; i < n; i++) {
            seed = seed * 1664525L + 1013904223L;
            int s = (int)((seed >> 8) & 0xFF) - 128;
            s = s * amp / 100 * (n - i) / n;
            b[i] = (byte)Math.max(0, Math.min(255, 128 + s));
        }
        return wav(b);
    }

    private static byte[] sweep(int f0, int f1, int ms, int shape) {
        int n = SR * ms / 1000; byte[] b = new byte[n];
        float ph = 0f; int rel = n / 5;
        for (int i = 0; i < n; i++) {
            float prog = (float)i / n;
            float hz = f0 + (f1 - f0) * prog;
            ph += hz / SR; if (ph >= 1f) ph -= 1f;
            int s = wave(ph, shape);
            if (i > n - rel) s = s * (n - i) / rel;
            b[i] = (byte)Math.max(0, Math.min(255, 128 + s));
        }
        return wav(b);
    }

    private static byte[] shootSnd() {
        int ms = 90, n = SR * ms / 1000; byte[] b = new byte[n];
        long seed = 98765L; float ph = 0f; int nEnd = n / 3;
        for (int i = 0; i < n; i++) {
            seed = seed * 1664525L + 1013904223L;
            float hz = 2200f - 1700f * (float)i / n;
            ph += hz / SR; if (ph >= 1f) ph -= 1f;
            int t = wave(ph, 1) * (n - i) / n;
            int no = 0;
            if (i < nEnd) { no = (int)((seed >> 8) & 0xFF) - 128; no = no * (nEnd - i) / nEnd; }
            b[i] = (byte)Math.max(0, Math.min(255, 128 + t * 2/3 + no / 3));
        }
        return wav(b);
    }

    private static byte[] fanfare() {
        int[] notes = { 2616, 3296, 3920, 0, 5233, 3920, 5233 };
        int nd = 110;
        return noteSeq(notes, nd);
    }

    private static byte[] chime() {
        int[] notes = { 2616, 3296, 3920, 5233, 7840 };
        return noteSeq(notes, 90);
    }

    private static byte[] noteSeq(int[] notes, int durMs) {
        int perNote = SR * durMs / 1000;
        byte[] b = new byte[notes.length * perNote];
        int pos = 0;
        for (int ni = 0; ni < notes.length; ni++) {
            int hz = notes[ni];
            for (int i = 0; i < perNote; i++) {
                if (hz == 0) { b[pos++] = (byte)128; continue; }
                float ph2 = (i % (SR/hz + 1)) / ((float)SR / hz);
                int s = wave(ph2, 0) * 55 / 100;
                int rel = perNote / 4;
                if (i > perNote - rel) s = s * (perNote - i) / rel;
                b[pos++] = (byte)Math.max(0, Math.min(255, 128 + s));
            }
        }
        return wav(b);
    }

    // ---- BLOCK CLANG: metallic impact ------------------------------------
    private static byte[] blockClang() {
        int n = SR * 80 / 1000; byte[] b = new byte[n];
        float ph1=0f, ph2=0f;
        for (int i=0;i<n;i++) {
            float prog=(float)i/n;
            ph1 += 900f/SR; if(ph1>=1f)ph1-=1f;
            ph2 += 1350f/SR; if(ph2>=1f)ph2-=1f;
            int s = (wave(ph1,1) + wave(ph2,1)) / 2;
            // metallic decay
            s = s * (n-i) / n;
            long seed2 = i * 12345L + 9876L;
            seed2 = seed2 * 1664525L + 1013904223L;
            int click = (int)((seed2>>9)&0xFF)-128;
            s = s * 3/4 + click*(n/8>i?1:0)/8;
            b[i] = (byte)Math.max(0,Math.min(255,128+s));
        }
        return wav(b);
    }

    // ---- EXPLOSION: big boom with rumble ----------------------------------
    private static byte[] explosion() {
        int ms=350, n=SR*ms/1000; byte[] b=new byte[n];
        long seed=55555L; float ph=0f;
        for (int i=0;i<n;i++) {
            seed=seed*1664525L+1013904223L;
            float prog=(float)i/n;
            float hz=180f*(1f-prog*0.9f);
            ph+=hz/SR; if(ph>=1f)ph-=1f;
            int tone2=wave(ph,2)*(n-i)/n;
            int nz=(int)((seed>>8)&0xFF)-128;
            nz=nz*(int)(80*(1f-prog));
            int s=tone2*2/3+nz/3;
            b[i]=(byte)Math.max(0,Math.min(255,128+s));
        }
        return wav(b);
    }

    // ---- PICKUP BLEEP: cheerful ascending blip ---------------------------
    private static byte[] pickupBleep() {
        int[] notes={494,587,740}; return noteSeq(notes,55);
    }

    // ---- HEARTBEAT: low double-thump pulse -------------------------------
    private static byte[] heartbeat() {
        int n=SR*300/1000; byte[] b=new byte[n];
        int beat1=SR*60/1000, beat2=SR*120/1000;
        float ph=0f;
        for (int i=0;i<n;i++) {
            float hz=60f*(1f-(float)i/n*0.5f);
            ph+=hz/SR; if(ph>=1f)ph-=1f;
            int s=wave(ph,0);
            int env=0;
            int d1=i-beat1, d2=i-beat2;
            if (d1>=0&&d1<40) env=80*(40-d1)/40;
            else if (d2>=0&&d2<30) env=55*(30-d2)/30;
            s=s*env/80;
            b[i]=(byte)Math.max(0,Math.min(255,128+s));
        }
        return wav(b);
    }

    // ---- CRITICAL HIT: sharp crack + ring --------------------------------
    private static byte[] critHit() {
        int n=SR*150/1000; byte[] b=new byte[n];
        long seed=77777L; float ph=0f;
        for (int i=0;i<n;i++) {
            seed=seed*1664525L+1013904223L;
            float prog=(float)i/n;
            float hz=1800f-1200f*prog;
            ph+=hz/SR; if(ph>=1f)ph-=1f;
            int t=wave(ph,1)*(n-i)/n;
            int nz=(int)((seed>>8)&0xFF)-128;
            nz=nz*(i<n/5?1:0)*60/(n/5+1);
            b[i]=(byte)Math.max(0,Math.min(255,128+t*3/4+nz/4));
        }
        return wav(b);
    }

    // ---- HEADSHOT DING: bright ping --------------------------------------
    private static byte[] headshotDing() {
        int n=SR*200/1000; byte[] b=new byte[n];
        float ph1=0f, ph2=0f;
        for (int i=0;i<n;i++) {
            ph1+=1047f/SR; if(ph1>=1f)ph1-=1f;
            ph2+=2093f/SR; if(ph2>=1f)ph2-=1f;
            int s=(wave(ph1,0)*2+wave(ph2,0))/3;
            s=s*(n-i)/n;
            b[i]=(byte)Math.max(0,Math.min(255,128+s));
        }
        return wav(b);
    }

    // ---- KILL STREAK FANFARE: triumphant burst ---------------------------
    private static byte[] streakFanfare() {
        int[] notes={523,659,784,1047}; return noteSeq(notes,80);
    }

    // ---- DEATH WAIL: descending moan ------------------------------------
    private static byte[] deathWail() {
        int ms=600, n=SR*ms/1000; byte[] b=new byte[n];
        float ph=0f;
        for (int i=0;i<n;i++) {
            float prog=(float)i/n;
            float hz=440f*(1f-prog*0.75f);
            ph+=hz/SR; if(ph>=1f)ph-=1f;
            int s=wave(ph,0);
            int env=i<n/10 ? s*i/(n/10) : s*(n-i)/(n*9/10);
            b[i]=(byte)Math.max(0,Math.min(255,128+env));
        }
        return wav(b);
    }

    // ---- POWER-UP RISE: ascending sweep + chime -------------------------
    private static byte[] powerupRise() {
        // Sweep then ding
        int sn=SR*180/1000, cn=SR*120/1000;
        byte[] b=new byte[sn+cn]; float ph=0f;
        for (int i=0;i<sn;i++) {
            float hz=300f+900f*(float)i/sn;
            ph+=hz/SR; if(ph>=1f)ph-=1f;
            b[i]=(byte)Math.max(0,Math.min(255,128+wave(ph,0)*70/100));
        }
        ph=0f;
        for (int i=0;i<cn;i++) {
            ph+=1047f/SR; if(ph>=1f)ph-=1f;
            int s=wave(ph,0)*(cn-i)/cn;
            b[sn+i]=(byte)Math.max(0,Math.min(255,128+s));
        }
        return wav(b);
    }

    // ---- MELODIES -------------------------------------------------------
    // Note freqs in Hz, duration in ms, 0=rest, -1=end
    private static final int[] MENU_NOTES  = { 523,0,587,659,784,880,784,659,523,587,659,523,-1 };
    private static final int[] MENU_DURS   = { 200,200,200,200,200,400,200,200,200,200,400,600,0 };
    private static final int[] COMBAT_NOTES= { 440,0,440,0,392,0,330,440,0,392,0,330,0,466,466,440,392,330,294,-1 };
    private static final int[] COMBAT_DURS = { 120,60,120,60,120,60,120,240,120,120,60,120,120,120,120,120,120,120,300,0 };
    private static final int[] STORY_NOTES = { 262,0,311,392,330,262,0,294,392,440,392,330,-1 };
    private static final int[] STORY_DURS  = { 500,250,500,500,500,500,500,500,500,500,500,1000,0 };
    private static final int[] VOID_NOTES  = { 262,0,294,277,262,247,262,0,415,392,330,311,262,-1 };
    private static final int[] VOID_DURS   = { 300,150,300,300,600,300,300,300,300,300,300,300,600,0 };

    private static byte[] melody(int[] notes, int[] durs, boolean gentle) {
        int total = 0;
        for (int i = 0; i < notes.length; i++) { if (notes[i]==-1) break; total += SR * durs[i] / 1000; }
        if (total <= 0) return null;
        byte[] b = new byte[total];
        int pos = 0;
        for (int ni = 0; ni < notes.length && pos < total; ni++) {
            if (notes[ni] == -1) break;
            int hz = notes[ni], durMs = durs[ni];
            int sLen = SR * durMs / 1000;
            if (hz == 0) {
                for (int i = 0; i < sLen && pos < total; i++) b[pos++] = (byte)128;
            } else {
                float per = (float)SR / hz;
                int atk = Math.max(1, sLen/15), rel = Math.max(1, sLen/6);
                int vol = gentle ? 50 : 65;
                for (int i = 0; i < sLen && pos < total; i++) {
                    float ph2 = (i % (int)per) / per;
                    int s = wave(ph2, gentle ? 0 : 1) * vol / 100;
                    if (i < atk) s = s * i / atk;
                    else if (i > sLen - rel) s = s * (sLen - i) / rel;
                    b[pos++] = (byte)Math.max(0, Math.min(255, 128 + s));
                }
            }
        }
        return wav(b);
    }

    // ---- WAV header builder --------------------------------------------
    private static byte[] wav(byte[] pcm) {
        byte[] out = new byte[pcm.length + 44];
        out[0]='R'; out[1]='I'; out[2]='F'; out[3]='F';
        i32(out, 4, pcm.length + 36);
        out[8]='W'; out[9]='A'; out[10]='V'; out[11]='E';
        out[12]='f'; out[13]='m'; out[14]='t'; out[15]=' ';
        i32(out,16,16); i16(out,20,1); i16(out,22,1);
        i32(out,24,SR); i32(out,28,SR); i16(out,32,1); i16(out,34,8);
        out[36]='d'; out[37]='a'; out[38]='t'; out[39]='a';
        i32(out,40,pcm.length);
        System.arraycopy(pcm, 0, out, 44, pcm.length);
        return out;
    }
    private static void i32(byte[] b,int o,int v){b[o]=(byte)v;b[o+1]=(byte)(v>>8);b[o+2]=(byte)(v>>16);b[o+3]=(byte)(v>>24);}
    private static void i16(byte[] b,int o,int v){b[o]=(byte)v;b[o+1]=(byte)(v>>8);}
    private static void setVol(Player p, int l){if(p==null)return;try{VolumeControl v=(VolumeControl)p.getControl("VolumeControl");if(v!=null)v.setLevel(Math.max(0,Math.min(100,l)));}catch(Exception e){}}
    private static void closeP(Player p){if(p==null)return;try{p.stop();}catch(Exception e){}try{p.close();}catch(Exception e){}}
}
