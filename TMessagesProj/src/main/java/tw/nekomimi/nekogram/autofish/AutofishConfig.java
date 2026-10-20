package tw.nekomimi.nekogram.autofish;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Preferensi Autofish / FishLimit / Daily / Misi / Grup (tanpa sistem sewa).
 */
public final class AutofishConfig {

    private static final String PREF = "nekogram_autofish";

    public static final String[] FISH_BOTS = {
            "fish_it_vip_bot",
            "fish_it_vip3_bot",
            "fish_it_vip4_bot",
            "fish_it_vip5_bot"
    };

    public static final String MARKET_BOT = "fish_it_market_bot";

    /** Interval FishLimit default = 40 menit (detik). */
    public static final int DEFAULT_LIMIT_INTERVAL_SEC = 40 * 60;

    private static SharedPreferences prefs;

    private AutofishConfig() {
    }

    public static void init(Context context) {
        if (prefs == null) {
            prefs = context.getApplicationContext()
                    .getSharedPreferences(PREF, Context.MODE_PRIVATE);
        }
    }

    private static SharedPreferences p() {
        if (prefs == null) {
            throw new IllegalStateException("AutofishConfig.init(context) dulu");
        }
        return prefs;
    }

    // ----- Autofish / FishLimit (mutual exclusive) -----

    public static boolean isAutofishEnabled() {
        return p().getBoolean("autofish_enabled", false);
    }

    public static void setAutofishEnabled(boolean on) {
        SharedPreferences.Editor e = p().edit();
        e.putBoolean("autofish_enabled", on);
        if (on) {
            e.putBoolean("fishlimit_enabled", false);
        }
        e.apply();
    }

    public static boolean isFishLimitEnabled() {
        return p().getBoolean("fishlimit_enabled", false);
    }

    public static void setFishLimitEnabled(boolean on) {
        SharedPreferences.Editor e = p().edit();
        e.putBoolean("fishlimit_enabled", on);
        if (on) {
            e.putBoolean("autofish_enabled", false);
        }
        e.apply();
    }

    // ----- Daily & Mission (market bot) -----

    public static boolean isDailyEnabled() {
        return p().getBoolean("daily_enabled", false);
    }

    public static void setDailyEnabled(boolean on) {
        p().edit().putBoolean("daily_enabled", on).apply();
    }

    public static boolean isMissionEnabled() {
        return p().getBoolean("mission_enabled", false);
    }

    public static void setMissionEnabled(boolean on) {
        p().edit().putBoolean("mission_enabled", on).apply();
    }

    // ----- Mancing grup -----

    public static boolean isGroupEnabled() {
        return p().getBoolean("group_enabled", false);
    }

    public static void setGroupEnabled(boolean on) {
        p().edit().putBoolean("group_enabled", on).apply();
    }

    /**
     * Target group id, contoh -1001234567890.
     * 0 = belum di-set.
     */
    public static long getGroupChatId() {
        return p().getLong("group_chat_id", 0L);
    }

    public static void setGroupChatId(long id) {
        p().edit().putLong("group_chat_id", id).apply();
    }

    public static void clearGroupChatId() {
        p().edit().remove("group_chat_id").apply();
    }

    public static String getGroupChatIdDisplay() {
        long id = getGroupChatId();
        if (id == 0L) {
            return "Belum di-set";
        }
        return String.valueOf(id);
    }

    // ----- bot Fish it? (PM) -----

    public static int getBotIndex() {
        return p().getInt("bot_index", 0);
    }

    public static void setBotIndex(int index) {
        if (index < 0) index = 0;
        if (index >= FISH_BOTS.length) index = FISH_BOTS.length - 1;
        p().edit().putInt("bot_index", index).apply();
    }

    public static String getBotUsername() {
        return FISH_BOTS[getBotIndex()];
    }

    // ----- FishLimit interval -----

    public static int getLimitIntervalSec() {
        return p().getInt("limit_interval_sec", DEFAULT_LIMIT_INTERVAL_SEC);
    }

    public static void setLimitIntervalSec(int sec) {
        if (sec < 60) sec = 60;
        p().edit().putInt("limit_interval_sec", sec).apply();
    }

    // ----- runtime (engine) -----

    public static long getLastMancingAt() {
        return p().getLong("last_mancing_at", 0L);
    }

    public static void setLastMancingAt(long epochMs) {
        p().edit().putLong("last_mancing_at", epochMs).apply();
    }

    public static long getCooldownUntil() {
        return p().getLong("cooldown_until", 0L);
    }

    public static void setCooldownUntil(long epochMs) {
        p().edit().putLong("cooldown_until", epochMs).apply();
    }

    public static String getScanPhase() {
        return p().getString("scan_phase", "idle");
    }

    public static void setScanPhase(String phase) {
        p().edit().putString("scan_phase", phase == null ? "idle" : phase).apply();
    }

    public static boolean isSessionBusy() {
        return p().getBoolean("session_busy", false);
    }

    public static void setSessionBusy(boolean busy) {
        p().edit().putBoolean("session_busy", busy).apply();
    }

    public static String getDailyLastDate() {
        return p().getString("daily_last_date", "");
    }

    public static void setDailyLastDate(String ymd) {
        p().edit().putString("daily_last_date", ymd == null ? "" : ymd).apply();
    }

    public static String getMissionPhase() {
        return p().getString("mission_phase", "idle");
    }

    public static void setMissionPhase(String phase) {
        p().edit().putString("mission_phase", phase == null ? "idle" : phase).apply();
    }

    public static boolean isMissionNeedDaily() {
        return p().getBoolean("mission_need_daily", false);
    }

    public static void setMissionNeedDaily(boolean v) {
        p().edit().putBoolean("mission_need_daily", v).apply();
    }

    public static boolean isMissionNeedSpecial() {
        return p().getBoolean("mission_need_special", false);
    }

    public static void setMissionNeedSpecial(boolean v) {
        p().edit().putBoolean("mission_need_special", v).apply();
    }





    public static String getStatusLabel() {
        StringBuilder sb = new StringBuilder();
        if (isFishLimitEnabled()) {
            long cool = getCooldownUntil();
            long now = System.currentTimeMillis();
            if (cool > now) {
                long left = (cool - now) / 1000L;
                sb.append("Limit ~").append(left / 60).append("m");
            } else {
                String phase = getScanPhase();
                sb.append(!"idle".equals(phase) ? "Limit·" + phase : "Limit ON");
            }
        } else if (isAutofishEnabled()) {
            String phase = getScanPhase();
            if (!"idle".equals(phase)) {
                sb.append("AF·").append(phase);
            } else {
                sb.append(isSessionBusy() ? "AF·sesi" : "AF ON");
            }
        } else {
            sb.append("Mancing OFF");
        }
        if (isDailyEnabled()) sb.append(" · Daily");
        if (isMissionEnabled()) sb.append(" · Misi");
        if (isGroupEnabled()) sb.append(" · Grup");
        return sb.toString();
    }
}
