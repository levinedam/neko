package tw.nekomimi.nekogram.autofish;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Alur: inventory page → favorite rare → next → /extract → 🟢 / Ya → /jual semua → Ya jual.
 * Port logika dari autofish.py (tanpa log grup).
 */
public final class InventoryStateMachine {

    private static final String TAG = "AutofishInv";
    private static final Pattern LINE_NUM = Pattern.compile("^\\s*(\\d+)\\.");

    private static final Handler H = new Handler(Looper.getMainLooper());

    private InventoryStateMachine() {
    }

    public static boolean isInventoryPage(String textLower, TLRPC.Message message) {
        if (textLower == null) textLower = "";
        if (textLower.contains("halaman:") || textLower.contains("slot terisi:")
                || textLower.contains("total nilai inventory")) {
            return true;
        }
        if (textLower.contains("extract -") || textLower.contains("pilih artefak")
                || textLower.contains("konfirmasi penjualan")
                || textLower.contains("konfirmasi extract")
                || textLower.contains("ya, jual semua")
                || textLower.contains("jumlah ikan")) {
            return false;
        }
        if (textLower.contains("inventory") || textLower.contains("slot terisi")) {
            return true;
        }
        // baris "1. ..."
        if (message != null && message.message != null) {
            for (String line : message.message.split("\n")) {
                if (LINE_NUM.matcher(line.trim()).find()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isInventoryFull(String textLower) {
        return textLower != null && textLower.contains("inventory penuh");
    }

    public static boolean isSessionEnd(String textLower) {
        if (textLower == null) return false;
        return textLower.contains("sesi mancing selesai")
                || textLower.contains("mancing selesai")
                || textLower.contains("waktu memancing telah habis")
                || textLower.contains("selesai memancing")
                || textLower.contains("terima kasih sudah memancing");
    }

    public static boolean isHarapBersabar(String textLower) {
        if (textLower == null) return false;
        return textLower.contains("harap bersabar")
                || (textLower.contains("tunggu beberapa") && textLower.contains("menit"))
                || (textLower.contains("tunggu") && textLower.contains("menit lagi"));
    }

    public static boolean isVerify(String textLower) {
        if (textLower == null) return false;
        return textLower.contains("verifikasi diperlukan")
                || textLower.contains("verifikasi sekarang");
    }

    /** Proses satu pesan di work chat (bot fish). */
    public static void handle(TLRPC.Message message) {
        if (message == null) return;
        String text = message.message != null ? message.message : "";
        String low = text.toLowerCase(Locale.ROOT);
        String phase = AutofishConfig.getScanPhase();

        // Inventory penuh → mulai scan
        if (isInventoryFull(low)) {
            AutofishConfig.setScanPhase("scan");
            AutofishConfig.setSessionBusy(true);
            postDelayed(() -> AutofishActions.sendInventory(),
                    AutofishActions.humanDelayMs(2500, 6000));
            return;
        }

        // Halaman inventory
        if (!"extract".equals(phase) && !"sell".equals(phase) && isInventoryPage(low, message)) {
            AutofishConfig.setScanPhase("scan");
            AutofishConfig.setSessionBusy(true);

            ArrayList<String> found = collectRareNums(text);
            if (!found.isEmpty()) {
                String joined = join(found);
                postDelayed(() -> AutofishActions.sendFavorite(joined),
                        AutofishActions.humanDelayMs(3000, 7000));
            }

            // coba next
            boolean hasNext = AutofishActions.clickButtonExactOrContains(message, "next", "➡️");
            if (hasNext) {
                Log.i(TAG, "next page");
                return;
            }

            // habis page → extract
            AutofishConfig.setScanPhase("extract");
            postDelayed(AutofishActions::sendExtract,
                    AutofishActions.humanDelayMs(4500, 10000));
            return;
        }

        // Extract flow
        if (low.contains("extract") || low.contains("pilih artefak") || "extract".equals(phase)) {
            if (low.contains("konfirmasi penjualan") || low.contains("jual semua ikan")) {
                // fall through ke jual
            } else {
                if ((low.contains("konfirmasi") && low.contains("artefak"))
                        || low.contains("yakin betul")
                        || (low.contains("konfirmasi") && low.contains("ya, extract"))) {
                    postDelayed(() -> AutofishActions.clickButtonExactOrContains(
                                    message, "ya, extract", "ya,"),
                            AutofishActions.humanDelayMs(3000, 6500));
                    return;
                }
                // tombol 🟢
                if (AutofishActions.clickButtonContaining(message, "🟢")) {
                    Log.i(TAG, "klik 🟢");
                    return;
                }
                if (AutofishActions.clickButtonContaining(message, "inventory")) {
                    return;
                }
                // fallback jual
                AutofishConfig.setScanPhase("sell");
                postDelayed(AutofishActions::sendJualSemua,
                        AutofishActions.humanDelayMs(3500, 8000));
                return;
            }
        }

        if (low.contains("di-extract:")) {
            AutofishConfig.setScanPhase("sell");
            postDelayed(AutofishActions::sendJualSemua,
                    AutofishActions.humanDelayMs(3500, 8000));
            return;
        }

        if (low.contains("konfirmasi penjualan") || low.contains("jual semua ikan")) {
            postDelayed(() -> {
                boolean ok = AutofishActions.clickButtonExactOrContains(
                        message, "ya, jual semua", "jual semua");
                if (ok) {
                    finishClean();
                }
            }, AutofishActions.humanDelayMs(3000, 6000));
            return;
        }

        if (low.contains("penjualan berhasil")
                || low.contains("inventory sudah dikosongkan")
                || low.contains("ikan terjual")
                || low.contains("berhasil menjual")) {
            finishClean();
        }
    }

    private static void finishClean() {
        AutofishConfig.setScanPhase("idle");
        AutofishConfig.setSessionBusy(false);
        Log.i(TAG, "inventory bersih");

        // Autofish: lanjut /mancing. FishLimit: diam sampai scheduler.
        if (AutofishConfig.isAutofishEnabled() && !AutofishConfig.isFishLimitEnabled()) {
            postDelayed(AutofishActions::sendMancing,
                    AutofishActions.humanDelayMs(6000, 15000));
        }
    }

    /** Setelah sesi selesai (FishLimit): paksa mulai scan. */
    public static void startScanAfterSession() {
        AutofishConfig.setScanPhase("scan");
        AutofishConfig.setSessionBusy(true);
        long cool = System.currentTimeMillis()
                + AutofishConfig.getLimitIntervalSec() * 1000L;
        AutofishConfig.setCooldownUntil(cool);
        postDelayed(AutofishActions::sendInventory,
                AutofishActions.humanDelayMs(3500, 8000));
    }

    private static ArrayList<String> collectRareNums(String text) {
        ArrayList<String> out = new ArrayList<>();
        if (text == null) return out;
        for (String line : text.split("\n")) {
            Matcher m = LINE_NUM.matcher(line.trim());
            if (m.find() && RareFish.isRareLine(line)) {
                out.add(m.group(1));
            }
        }
        return out;
    }

    private static String join(ArrayList<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    private static void postDelayed(Runnable r, long delayMs) {
        H.postDelayed(r, delayMs);
    }
}
