package tw.nekomimi.nekogram.autofish;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

import java.util.Locale;

/**
 * Entry point pesan masuk. Hook dari MessagesController / update UI.
 */
public final class AutofishEngine {

    private static final String TAG = "AutofishEngine";
    private static final Handler H = new Handler(Looper.getMainLooper());

    private AutofishEngine() {
    }

    public static void onMessageObject(MessageObject mo) {
        if (mo == null || mo.messageOwner == null) return;
        onNewMessage(mo.messageOwner);
    }

    public static void onNewMessage(TLRPC.Message message) {
        if (message == null || message.out) return;
        if (!anyOn()) return;

        long dialogId = MessageDialogId.get(message);
        String text = message.message != null ? message.message : "";
        String low = text.toLowerCase(Locale.ROOT);

        long fishId = AutofishActions.fishBotDialogId();
        long marketId = AutofishActions.marketBotDialogId();
        long groupId = AutofishConfig.getGroupChatId();

        boolean isFish = fishId != 0 && dialogId == fishId;
        boolean isMarket = marketId != 0 && dialogId == marketId;
        boolean isGroup = groupId != 0 && dialogId == groupId;

        // --- Grup ---
        if (isGroup && AutofishConfig.isGroupEnabled()) {
            GroupDaftarHelper.handle(message, low);
            return;
        }

        // --- Market: daily + misi ---
        if (isMarket) {
            if (AutofishConfig.isDailyEnabled()) {
                if (low.contains("daily reward")
                        || low.contains("hadiah yang bisa")
                        || low.contains("celestial bait")) {
                    H.postDelayed(() -> AutofishActions.clickButtonExactOrContains(
                            message, "klaim", "claim"),
                            AutofishActions.humanDelayMs(2000, 4500));
                }
            }
            if (AutofishConfig.isMissionEnabled()) {
                MissionHelper.onMarketMessage(message, low);
            }
            return;
        }

        // --- PM bot Fish ---
        if (!isFish) return;

        boolean af = AutofishConfig.isAutofishEnabled();
        boolean lim = AutofishConfig.isFishLimitEnabled();
        if (!af && !lim) return;

        if (VerifyHelper.isVerifyText(low)) {
            VerifyHelper.handle(message);
            return;
        }

        if (InventoryStateMachine.isHarapBersabar(low)) {
            if (lim) {
                FishLimitScheduler.applyBersabarCooldown(
                        AutofishConfig.getLimitIntervalSec());
            }
            return;
        }

        if (low.contains("sedang memancing") || low.contains("auto mancing")
                || low.contains("mancing otomatis akan dimulai")) {
            AutofishConfig.setSessionBusy(true);
            return;
        }

        if (InventoryStateMachine.isSessionEnd(low)) {
            AutofishConfig.setSessionBusy(false);
            if (lim) {
                InventoryStateMachine.startScanAfterSession();
            } else if (af) {
                H.postDelayed(AutofishActions::sendMancing,
                        AutofishActions.humanDelayMs(6000, 15000));
            }
            return;
        }

        InventoryStateMachine.handle(message);
    }

    private static boolean anyOn() {
        return AutofishConfig.isAutofishEnabled()
                || AutofishConfig.isFishLimitEnabled()
                || AutofishConfig.isDailyEnabled()
                || AutofishConfig.isMissionEnabled()
                || AutofishConfig.isGroupEnabled();
    }

    public static void onSettingsChanged() {
        FishLimitScheduler.onConfigChanged();
        DailyScheduler.onConfigChanged();
        if (AutofishConfig.isAutofishEnabled() && !AutofishConfig.isFishLimitEnabled()) {
            H.postDelayed(AutofishActions::sendMancing, 1500);
        }
    }
}
