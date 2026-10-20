package tw.nekomimi.nekogram.autofish;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

/** Ambil dialogId dari TLRPC.Message. */
public final class MessageDialogId {
    private MessageDialogId() {}

    public static long get(TLRPC.Message message) {
        if (message == null) {
            return 0;
        }
        try {
            return MessageObject.getDialogId(message);
        } catch (Throwable t) {
            if (message.peer_id != null) {
                if (message.peer_id.user_id != 0) {
                    return message.peer_id.user_id;
                }
                if (message.peer_id.channel_id != 0) {
                    return -message.peer_id.channel_id;
                }
                if (message.peer_id.chat_id != 0) {
                    return -message.peer_id.chat_id;
                }
            }
            return 0;
        }
    }
}
