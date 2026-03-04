import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

/**
 * StickFight v2.0 PRO MAX
 * J2ME MIDP 2.0 / CLDC 1.1  — Itel 5615 (240x320)
 *
 * Entry point MIDlet.
 */
public class StickFightMIDlet extends MIDlet {

    private StickFightCanvas canvas;

    public void startApp() {
        if (canvas == null) {
            canvas = new StickFightCanvas(this);
        }
        Display.getDisplay(this).setCurrent(canvas);
        canvas.start();
    }

    public void pauseApp() {
        if (canvas != null) canvas.stop();
    }

    public void destroyApp(boolean unconditional) {
        if (canvas != null) canvas.stop();
    }

    public void exitApp() {
        destroyApp(true);
        notifyDestroyed();
    }
}
