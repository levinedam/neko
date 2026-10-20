package tw.nekomimi.nekogram.autofish;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * FishLimit: /mancing tiap interval (~40 menit).
 * Hormati cooldown "Harap bersabar".
 */
public final class FishLimitScheduler {

    private static final String TAG = "FishLimitSched";
    private static final Handler H = new Handler(Looper.getMainLooper());

    private static final Runnable TICK = new Runnable() {
        @Override
        public void run() {
            tick();
        }
    };

    private static boolean running;

    private FishLimitScheduler() {
    }

    public static void start() {
        if (running) return;
        running = true;
        Log.i(TAG, "start");
        // /mancing pertama segera (jika ON)
        H.postDelayed(() -> {
            if (!AutofishConfig.isFishLimitEnabled()) return;
            if (AutofishConfig.isSessionBusy()) return;
            if (System.currentTimeMillis() < AutofishConfig.getCooldownUntil()) return;
            AutofishActions.sendMancing();
            long until = System.currentTimeMillis()
                    + AutofishConfig.getLimitIntervalSec() * 1000L;
            AutofishConfig.setCooldownUntil(until);
            scheduleNext();
        }, 2000);
    }

    public static void stop() {
        running = false;
        H.removeCallbacks(TICK);
        Log.i(TAG, "stop");
    }

    public static void onConfigChanged() {
        if (AutofishConfig.isFishLimitEnabled()) {
            start();
        } else {
            stop();
        }
    }

    private static void scheduleNext() {
        if (!running || !AutofishConfig.isFishLimitEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        long cool = AutofishConfig.getCooldownUntil();
        long interval = AutofishConfig.getLimitIntervalSec() * 1000L;
        long next = Math.max(cool, now + interval);
        long delay = Math.max(5000, next - now);
        H.removeCallbacks(TICK);
        H.postDelayed(TICK, delay);
        Log.i(TAG, "next tick in ~" + (delay / 1000) + "s");
    }

    private static void tick() {
        if (!running || !AutofishConfig.isFishLimitEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < AutofishConfig.getCooldownUntil()) {
            scheduleNext();
            return;
        }
        if (AutofishConfig.isSessionBusy()) {
            Log.i(TAG, "sesi/scan busy — tunda 30s");
            H.postDelayed(TICK, 30_000);
            return;
        }
        String phase = AutofishConfig.getScanPhase();
        if (!"idle".equals(phase)) {
            Log.i(TAG, "phase=" + phase + " — tunda 30s");
            H.postDelayed(TICK, 30_000);
            return;
        }
        AutofishActions.sendMancing();
        AutofishConfig.setCooldownUntil(now + AutofishConfig.getLimitIntervalSec() * 1000L);
        scheduleNext();
    }

    /** Dipanggil saat teks Harap bersabar. */
    public static void applyBersabarCooldown(long waitSec) {
        long sec = Math.max(waitSec, AutofishConfig.getLimitIntervalSec());
        long until = System.currentTimeMillis() + sec * 1000L;
        AutofishConfig.setCooldownUntil(until);
        AutofishConfig.setSessionBusy(false);
        Log.i(TAG, "bersabar cooldown " + sec + "s");
        if (running) {
            scheduleNext();
        }
    }
}
