package tw.nekomimi.nekogram.autofish;

import android.util.Log;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Kirim perintah & klik tombol inline ke bot Fish it? / market.
 * Account = UserConfig.selectedAccount (akun yang sedang aktif di app).
 */
public final class AutofishActions {

    private static final String TAG = "AutofishActions";

    private AutofishActions() {
    }

    public static int account() {
        return UserConfig.selectedAccount;
    }

    /** Resolve username bot → user_id (dialogId positif untuk user/bot). */
    public static long resolveBotDialogId(String username) {
        if (username == null || username.isEmpty()) {
            return 0;
        }
        String u = username.startsWith("@") ? username.substring(1) : username;
        u = u.toLowerCase(Locale.ROOT);
        try {
            TLRPC.User user = MessagesController.getInstance(account()).getUser(
                    MessagesController.getInstance(account()).getUserName(u)
            );
            // fallback: search in memory by username
            if (user == null) {
                for (TLRPC.User candidate : MessagesController.getInstance(account()).getUsers().values()) {
                    if (candidate != null && candidate.username != null
                            && candidate.username.equalsIgnoreCase(u)) {
                        user = candidate;
                        break;
                    }
                }
            }
            if (user != null) {
                return user.id;
            }
        } catch (Throwable t) {
            Log.w(TAG, "resolveBotDialogId: " + t.getMessage());
        }
        return 0;
    }

    public static long fishBotDialogId() {
        return resolveBotDialogId(AutofishConfig.getBotUsername());
    }

    public static long marketBotDialogId() {
        return resolveBotDialogId(AutofishConfig.MARKET_BOT);
    }

    /**
     * Kirim teks ke dialog (PM bot / grup).
     * Harus dipanggil dari UI thread atau via AndroidUtilities.runOnUIThread.
     */
    public static void sendText(long dialogId, String text) {
        if (dialogId == 0 || text == null || text.isEmpty()) {
            Log.w(TAG, "sendText skip dialogId=" + dialogId);
            return;
        }
        try {
            int acc = account();
            SendMessagesHelper.SendMessageParams params =
                    SendMessagesHelper.SendMessageParams.of(
                            text,
                            dialogId,
                            null,
                            null,
                            null,
                            true,
                            null,
                            null,
                            null,
                            true,
                            0,
                            null,
                            false
                    );
            SendMessagesHelper.getInstance(acc).sendMessage(params);
            Log.i(TAG, "send → " + dialogId + " : " + text);
            AutofishConfig.setLastMancingAt(System.currentTimeMillis());
        } catch (Throwable t) {
            Log.e(TAG, "sendText error", t);
            // Fallback API lama bila signature SendMessageParams beda di fork
            try {
                SendMessagesHelper.getInstance(account()).sendMessage(
                        text, dialogId, null, null, null, true, null, null, null, true, 0, null, false
                );
            } catch (Throwable t2) {
                Log.e(TAG, "sendText fallback error", t2);
            }
        }
    }

    public static void sendMancing() {
        long id = fishBotDialogId();
        if (id == 0) {
            Log.w(TAG, "Peer bot belum dikenal — buka PM @" + AutofishConfig.getBotUsername() + " /start dulu");
            return;
        }
        sendText(id, "/mancing");
    }

    public static void sendInventory() {
        long id = fishBotDialogId();
        if (id != 0) sendText(id, "/inventory");
    }

    public static void sendExtract() {
        long id = fishBotDialogId();
        if (id != 0) sendText(id, "/extract");
    }

    public static void sendJualSemua() {
        long id = fishBotDialogId();
        if (id != 0) sendText(id, "/jual semua");
    }

    public static void sendFavorite(String numbersJoined) {
        long id = fishBotDialogId();
        if (id != 0) sendText(id, "/favorite " + numbersJoined);
    }

    public static void sendDaily() {
        long id = marketBotDialogId();
        if (id != 0) sendText(id, "/daily");
    }

    public static void sendMission() {
        long id = marketBotDialogId();
        if (id != 0) sendText(id, "/mission");
    }

    /**
     * Klik tombol inline yang teksnya mengandung needle (case-insensitive).
     * @return true jika request callback dikirim
     */
    public static boolean clickButtonContaining(TLRPC.Message message, String needle) {
        if (message == null || needle == null || needle.isEmpty()) {
            return false;
        }
        if (message.reply_markup == null || !(message.reply_markup instanceof TLRPC.TL_replyInlineMarkup)) {
            return false;
        }
        String want = needle.toLowerCase(Locale.ROOT);
        TLRPC.TL_replyInlineMarkup markup = (TLRPC.TL_replyInlineMarkup) message.reply_markup;
        for (TLRPC.TL_keyboardButtonRow row : markup.rows) {
            for (TLRPC.KeyboardButton btn : row.buttons) {
                String t = btn.text != null ? btn.text.toLowerCase(Locale.ROOT) : "";
                if (!t.contains(want)) {
                    continue;
                }
                return clickButton(message, btn);
            }
        }
        return false;
    }

    public static boolean clickButtonExactOrContains(TLRPC.Message message, String... labels) {
        for (String label : labels) {
            if (clickButtonContaining(message, label)) {
                return true;
            }
        }
        return false;
    }

    public static boolean clickButton(TLRPC.Message message, TLRPC.KeyboardButton button) {
        if (message == null || button == null) {
            return false;
        }
        try {
            int acc = account();
            long dialogId = MessageDialogId.get(message);

            // URL / WebApp — buka lewat browser internal (verifikasi manual)
            if (button instanceof TLRPC.TL_keyboardButtonUrl) {
                String url = ((TLRPC.TL_keyboardButtonUrl) button).url;
                Log.i(TAG, "URL button: " + url);
                // Engine bisa set flag; user selesaikan di WebView
                return true;
            }
            if (button instanceof TLRPC.TL_keyboardButtonWebView
                    || button instanceof TLRPC.TL_keyboardButtonSimpleWebView) {
                Log.i(TAG, "WebView button — user selesaikan captcha manual");
                return true;
            }

            // Callback data
            if (button instanceof TLRPC.TL_keyboardButtonCallback) {
                byte[] data = ((TLRPC.TL_keyboardButtonCallback) button).data;
                if (data == null) {
                    return false;
                }
                TLRPC.TL_messages_getBotCallbackAnswer req = new TLRPC.TL_messages_getBotCallbackAnswer();
                req.peer = MessagesController.getInstance(acc).getInputPeer(dialogId);
                req.msg_id = message.id;
                req.data = data;
                req.game = false;
                ConnectionsManager.getInstance(acc).sendRequest(req, (response, error) -> {
                    if (error != null) {
                        Log.w(TAG, "callback error: " + error.text);
                    } else {
                        Log.i(TAG, "callback OK: " + (button.text != null ? button.text : ""));
                    }
                });
                return true;
            }
        } catch (Throwable t) {
            Log.e(TAG, "clickButton", t);
        }
        return false;
    }

    /** Delay acak manusiawi (ms). */
    public static long humanDelayMs(long lo, long hi) {
        if (hi <= lo) return lo;
        return ThreadLocalRandom.current().nextLong(lo, hi + 1);
    }
}
