package tw.nekomimi.nekogram.autofish;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Auto Daily @fish_it_market_bot — target ~00:03 WIB, catch-up 1x/hari.
 * Klaim tombol ditangani AutofishEngine saat pesan Daily Reward.
 */
public final class DailyScheduler {

    private static final String TAG = "AutofishDaily";
    private static final Handler H = new Handler(Looper.getMainLooper());
    private static final TimeZone WIB = TimeZone.getTimeZone("Asia/Jakarta");

    private static final int HOUR = 0;
    private static final int MINUTE = 3;

    private static boolean running;
    private static final Runnable TICK = DailyScheduler::tick;

    private DailyScheduler() {
    }

    public static void start() {
        if (running) return;
        running = true;
        Log.i(TAG, "start");
        H.postDelayed(TICK, 3000);
    }

    public static void stop() {
        running = false;
        H.removeCallbacks(TICK);
        Log.i(TAG, "stop");
    }

    public static void onConfigChanged() {
        if (AutofishConfig.isDailyEnabled()) start();
        else stop();
    }

    private static void tick() {
        if (!running || !AutofishConfig.isDailyEnabled()) return;

        Calendar now = Calendar.getInstance(WIB);
        String today = String.format("%04d-%02d-%02d",
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH));

        String last = AutofishConfig.getDailyLastDate();
        boolean already = today.equals(last);

        Calendar target = (Calendar) now.clone();
        target.set(Calendar.HOUR_OF_DAY, HOUR);
        target.set(Calendar.MINUTE, MINUTE);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (now.after(target) || now.equals(target)) {
            if (!already) {
                Log.i(TAG, "kirim /daily (catch-up atau jendela 00:03) " + today);
                AutofishActions.sendDaily();
                AutofishConfig.setDailyLastDate(today);
            }
            target.add(Calendar.DAY_OF_MONTH, 1);
        }

        long delay = target.getTimeInMillis() - System.currentTimeMillis();
        if (delay < 5000) delay = 5000;
        H.removeCallbacks(TICK);
        H.postDelayed(TICK, delay);
        Log.i(TAG, "next check in ~" + (delay / 60000) + " mnt");
    }
}
