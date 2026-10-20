package tw.nekomimi.nekogram.autofish;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.telegram.tgnet.TLRPC;

import java.util.Locale;

/**
 * Klaim misi @fish_it_market_bot:
 * notif selesai → /mission → Harian & Mingguan (klaim semua) → Kembali
 * → Spesial (klaim semua).
 */
public final class MissionHelper {

    private static final String TAG = "AutofishMission";
    private static final Handler H = new Handler(Looper.getMainLooper());

    public static final String PHASE_IDLE = "idle";
    public static final String PHASE_DAILY = "daily";
    public static final String PHASE_DAILY_LIST = "daily_list";
    public static final String PHASE_AFTER_DAILY = "after_daily";
    public static final String PHASE_SPECIAL = "special";
    public static final String PHASE_SPECIAL_LIST = "special_list";

    private MissionHelper() {
    }

    public static boolean isReadyNotif(String low) {
        if (low == null) return false;
        return low.contains("misi selesai")
                || low.contains("siap diklaim")
                || low.contains("dapat diklaim")
                || low.contains("mission complete")
                || (low.contains("klaim") && low.contains("misi"));
    }

    public static boolean isCategoryMenu(String low) {
        if (low == null) return false;
        return low.contains("pilih jenis misi")
                || (low.contains("misi harian") && low.contains("spesial"))
                || low.contains("jenis misi");
    }

    public static void onMarketMessage(TLRPC.Message message, String low) {
        if (!AutofishConfig.isMissionEnabled()) return;
        if (message == null) return;

        String phase = AutofishConfig.getMissionPhase();

        if (isReadyNotif(low)) {
            AutofishConfig.setMissionPhase(PHASE_DAILY);
            AutofishConfig.setMissionNeedDaily(true);
            AutofishConfig.setMissionNeedSpecial(true);
            H.postDelayed(AutofishActions::sendMission,
                    AutofishActions.humanDelayMs(2500, 5500));
            return;
        }

        if (isCategoryMenu(low)) {
            boolean needD = AutofishConfig.isMissionNeedDaily();
            boolean needS = AutofishConfig.isMissionNeedSpecial();
            if (needD && (PHASE_DAILY.equals(phase) || PHASE_IDLE.equals(phase)
                    || phase.isEmpty() || "both".equals(phase))) {
                if (AutofishActions.clickButtonExactOrContains(message,
                        "misi harian & mingguan", "harian & mingguan", "harian")) {
                    AutofishConfig.setMissionPhase(PHASE_DAILY_LIST);
                    Log.i(TAG, "→ Harian & Mingguan");
                }
                return;
            }
            if (needS && (PHASE_SPECIAL.equals(phase) || PHASE_AFTER_DAILY.equals(phase)
                    || PHASE_SPECIAL_LIST.equals(phase))) {
                if (AutofishActions.clickButtonExactOrContains(message,
                        "misi spesial", "spesial")) {
                    AutofishConfig.setMissionPhase(PHASE_SPECIAL_LIST);
                    Log.i(TAG, "→ Spesial");
                }
                return;
            }
        }

        // Klik semua klaim/claim
        if (clickAllKlaim(message)) {
            H.postDelayed(() -> afterKlaimRound(message), 4000);
        }
    }

    private static boolean clickAllKlaim(TLRPC.Message message) {
        // satu klik per pesan; engine edit message akan panggil lagi
        return AutofishActions.clickButtonExactOrContains(message, "klaim", "claim");
    }

    private static void afterKlaimRound(TLRPC.Message message) {
        String phase = AutofishConfig.getMissionPhase();
        if (PHASE_DAILY_LIST.equals(phase) || PHASE_DAILY.equals(phase)) {
            if (AutofishConfig.isMissionNeedSpecial()) {
                AutofishActions.clickButtonExactOrContains(message, "kembali", "back");
                AutofishConfig.setMissionPhase(PHASE_AFTER_DAILY);
                H.postDelayed(AutofishActions::sendMission,
                        AutofishActions.humanDelayMs(2500, 5000));
            } else {
                clearFlags();
            }
        } else if (PHASE_SPECIAL_LIST.equals(phase) || PHASE_SPECIAL.equals(phase)
                || PHASE_AFTER_DAILY.equals(phase)) {
            clearFlags();
        }
    }

    private static void clearFlags() {
        AutofishConfig.setMissionPhase(PHASE_IDLE);
        AutofishConfig.setMissionNeedDaily(false);
        AutofishConfig.setMissionNeedSpecial(false);
        Log.i(TAG, "misi selesai flags clear");
    }
}
