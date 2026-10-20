package tw.nekomimi.nekogram.autofish;

import android.util.Log;

import org.telegram.tgnet.TLRPC;

import java.util.Locale;

/**
 * Auto klik "Daftar mancing" di grup target.
 * Inventory/verifikasi tetap di PM bot (abaikan di grup).
 */
public final class GroupDaftarHelper {

    private static final String TAG = "AutofishGroup";
    private static long lastClickAt;

    private GroupDaftarHelper() {
    }

    public static boolean isRegMessage(String low, TLRPC.Message message) {
        if (low == null) low = "";
        if (low.contains("pendaftaran dibuka")) return true;
        if (low.contains("daftar mancing")) return true;
        if (low.contains("auto mancing") && low.contains("peserta")) return true;
        return hasDaftarButton(message);
    }

    public static boolean hasDaftarButton(TLRPC.Message message) {
        if (message == null || message.reply_markup == null) return false;
        if (!(message.reply_markup instanceof TLRPC.TL_replyInlineMarkup)) return false;
        TLRPC.TL_replyInlineMarkup m = (TLRPC.TL_replyInlineMarkup) message.reply_markup;
        for (TLRPC.TL_keyboardButtonRow row : m.rows) {
            for (TLRPC.KeyboardButton b : row.buttons) {
                if (b.text != null && b.text.toLowerCase(Locale.ROOT).contains("daftar")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void handle(TLRPC.Message message, String low) {
        if (!AutofishConfig.isGroupEnabled()) return;
        long gid = AutofishConfig.getGroupChatId();
        if (gid == 0) return;
        if (MessageDialogId.get(message) != gid) return;

        // maintenance di grup → biarkan PM
        if (low.contains("inventory penuh") || low.contains("verifikasi")
                || low.contains("halaman:") || low.contains("extract")) {
            return;
        }

        if (!isRegMessage(low, message)) return;

        long now = System.currentTimeMillis();
        if (now - lastClickAt < 15_000) {
            return;
        }
        lastClickAt = now;
        boolean ok = AutofishActions.clickButtonExactOrContains(
                message, "daftar mancing", "daftar");
        Log.i(TAG, "klik daftar → " + ok);
    }
}
