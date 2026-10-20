package tw.nekomimi.nekogram.autofish;

import android.util.Log;

import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.LaunchActivity;

import java.util.Locale;

/**
 * Verifikasi Fish it? (fishid.online / Turnstile).
 *
 * Di klien Android:
 * - Klik tombol URL/WebApp → buka WebView Telegram (user centang captcha).
 * - Bypass otomatis ala CloakBrowser+proxy VPS TIDAK di-port di sini
 *   (butuh engine browser + proxy di device; beda arsitektur).
 *
 * Setelah user selesai, engine lanjut dari pesan bot berikutnya / /mancing.
 */
public final class VerifyHelper {

    private static final String TAG = "AutofishVerify";
    private static long lastAt;

    private VerifyHelper() {
    }

    public static boolean isVerifyText(String low) {
        if (low == null) return false;
        return low.contains("verifikasi diperlukan")
                || low.contains("verifikasi sekarang");
    }

    public static void handle(TLRPC.Message message) {
        long now = System.currentTimeMillis();
        if (now - lastAt < 60_000) {
            Log.i(TAG, "cooldown skip");
            return;
        }
        lastAt = now;

        // Coba klik tombol yang mengandung verifikasi / buka url
        boolean clicked = AutofishActions.clickButtonExactOrContains(
                message, "verifikasi", "verify", "lanjutkan");
        Log.i(TAG, "open verify UI clicked=" + clicked
                + " — selesaikan Turnstile manual di WebView");

        // Jika ada URL di markup, log saja (Browser.openUrl butuh Activity)
        String url = extractUrl(message);
        if (url != null) {
            Log.i(TAG, "verify url: " + url);
        }
    }

    public static String extractUrl(TLRPC.Message message) {
        if (message == null || message.reply_markup == null) return null;
        if (!(message.reply_markup instanceof TLRPC.TL_replyInlineMarkup)) return null;
        TLRPC.TL_replyInlineMarkup m = (TLRPC.TL_replyInlineMarkup) message.reply_markup;
        for (TLRPC.TL_keyboardButtonRow row : m.rows) {
            for (TLRPC.KeyboardButton b : row.buttons) {
                if (b instanceof TLRPC.TL_keyboardButtonUrl) {
                    String u = ((TLRPC.TL_keyboardButtonUrl) b).url;
                    if (u != null && (u.contains("fishid") || u.contains("verify") || u.startsWith("http"))) {
                        return u;
                    }
                }
            }
        }
        return null;
    }
}
