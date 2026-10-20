package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.RadioCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

import tw.nekomimi.nekogram.autofish.AutofishConfig;
import tw.nekomimi.nekogram.autofish.AutofishEngine;

/**
 * Pengaturan Autofish / FishLimit / Daily / Misi / Mancing Grup.
 * Tanpa sistem sewa. Engine auto = fase berikutnya.
 */
public class NekoAutofishSettingsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter adapter;

    private int rowCount;
    private int headerModeRow;
    private int autofishRow;
    private int fishLimitRow;
    private int statusRow;

    private int headerExtraRow;
    private int dailyRow;
    private int missionRow;

    private int headerGroupRow;
    private int groupEnableRow;
    private int groupIdRow;
    private int groupClearRow;

    private int botHeaderRow;
    private int botStartRow;
    private int botEndRow;
    private int infoRow;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        if (getParentActivity() != null) {
            AutofishConfig.init(getParentActivity());
        }
        updateRows();
        return true;
    }

    private void updateRows() {
        rowCount = 0;
        headerModeRow = rowCount++;
        autofishRow = rowCount++;
        fishLimitRow = rowCount++;
        statusRow = rowCount++;

        headerExtraRow = rowCount++;
        dailyRow = rowCount++;
        missionRow = rowCount++;

        headerGroupRow = rowCount++;
        groupEnableRow = rowCount++;
        groupIdRow = rowCount++;
        groupClearRow = rowCount++;

        botHeaderRow = rowCount++;
        botStartRow = rowCount;
        rowCount += AutofishConfig.FISH_BOTS.length;
        botEndRow = rowCount;
        infoRow = rowCount++;
    }

    @Override
    public View createView(Context context) {
        AutofishConfig.init(context);

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Autofish / Mancing");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frame = (FrameLayout) fragmentView;
        frame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        adapter = new ListAdapter(context);
        listView.setAdapter(adapter);
        frame.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position == autofishRow) {
                AutofishConfig.setAutofishEnabled(!AutofishConfig.isAutofishEnabled());
                notifyAllRows();
                AutofishEngine.onSettingsChanged();
            } else if (position == fishLimitRow) {
                AutofishConfig.setFishLimitEnabled(!AutofishConfig.isFishLimitEnabled());
                notifyAllRows();
                AutofishEngine.onSettingsChanged();
            } else if (position == dailyRow) {
                AutofishConfig.setDailyEnabled(!AutofishConfig.isDailyEnabled());
                notifyAllRows();
            } else if (position == missionRow) {
                AutofishConfig.setMissionEnabled(!AutofishConfig.isMissionEnabled());
                notifyAllRows();
            } else if (position == groupEnableRow) {
                AutofishConfig.setGroupEnabled(!AutofishConfig.isGroupEnabled());
                notifyAllRows();
            } else if (position == groupIdRow) {
                showGroupIdDialog();
            } else if (position == groupClearRow) {
                AutofishConfig.clearGroupChatId();
                AutofishConfig.setGroupEnabled(false);
                notifyAllRows();
            } else if (position >= botStartRow && position < botEndRow) {
                AutofishConfig.setBotIndex(position - botStartRow);
                notifyAllRows();
            }
        });

        return fragmentView;
    }

    private void notifyAllRows() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showGroupIdDialog() {
        if (getParentActivity() == null) {
            return;
        }
        Context context = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Target Group ID");

        final EditTextBoldCursor edit = new EditTextBoldCursor(context);
        edit.setTextSize(18);
        edit.setHint("-100xxxxxxxxxx");
        edit.setSingleLine(true);
        long cur = AutofishConfig.getGroupChatId();
        if (cur != 0L) {
            edit.setText(String.valueOf(cur));
        }
        edit.setPadding(
                AndroidUtilities.dp(16),
                AndroidUtilities.dp(12),
                AndroidUtilities.dp(16),
                AndroidUtilities.dp(12)
        );
        builder.setView(edit);

        builder.setPositiveButton("Simpan", (dialog, which) -> {
            String raw = edit.getText() != null ? edit.getText().toString().trim() : "";
            if (raw.isEmpty()) {
                AutofishConfig.clearGroupChatId();
                notifyAllRows();
                return;
            }
            try {
                long id = Long.parseLong(raw);
                AutofishConfig.setGroupChatId(id);
                notifyAllRows();
            } catch (NumberFormatException ignored) {
            }
        });
        builder.setNegativeButton("Batal", null);
        builder.show();
        AndroidUtilities.showKeyboard(edit);
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context context;

        ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int t = holder.getItemViewType();
            return t == 1 || t == 2 || t == 3;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new HeaderCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1:
                    view = new TextCheckCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
                    view = new RadioCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new TextSettingsCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 4:
                default:
                    view = new TextInfoPrivacyCell(context);
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == headerModeRow) {
                        cell.setText("Mode mancing PM");
                    } else if (position == headerExtraRow) {
                        cell.setText("Market · Daily & Misi");
                    } else if (position == headerGroupRow) {
                        cell.setText("Mancing grup");
                    } else if (position == botHeaderRow) {
                        cell.setText("Bot Fish it? (PM)");
                    }
                    break;
                }
                case 1: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (position == autofishRow) {
                        cell.setTextAndCheck("Autofish (loop sesi)", AutofishConfig.isAutofishEnabled(), true);
                    } else if (position == fishLimitRow) {
                        cell.setTextAndCheck("FishLimit (~40 menit)", AutofishConfig.isFishLimitEnabled(), true);
                    } else if (position == dailyRow) {
                        cell.setTextAndCheck("Auto Daily (@market)", AutofishConfig.isDailyEnabled(), true);
                    } else if (position == missionRow) {
                        cell.setTextAndCheck("Auto Klaim Misi (@market)", AutofishConfig.isMissionEnabled(), true);
                    } else if (position == groupEnableRow) {
                        cell.setTextAndCheck("Auto daftar di grup", AutofishConfig.isGroupEnabled(), true);
                    }
                    break;
                }
                case 2: {
                    RadioCell cell = (RadioCell) holder.itemView;
                    int idx = position - botStartRow;
                    cell.setText(
                            "@" + AutofishConfig.FISH_BOTS[idx],
                            AutofishConfig.getBotIndex() == idx,
                            position + 1 < botEndRow
                    );
                    break;
                }
                case 3: {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    if (position == statusRow) {
                        cell.setTextAndValue("Status", AutofishConfig.getStatusLabel(), true);
                    } else if (position == groupIdRow) {
                        cell.setTextAndValue("Target Group ID", AutofishConfig.getGroupChatIdDisplay(), true);
                    } else if (position == groupClearRow) {
                        cell.setText("Hapus Group ID", true);
                    }
                    break;
                }
                case 4: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == infoRow) {
                        cell.setText(
                                "Autofish: loop /mancing + inventory → favorite rare → extract → jual.\n\n"
                                        + "FishLimit: /mancing tiap ~40 menit; setelah sesi tetap scan → jual.\n\n"
                                        + "Daily / Misi: lewat @fish_it_market_bot (jadwal daily ~00:03 WIB di engine nanti).\n\n"
                                        + "Grup: isi ID supergrup (contoh -100…), akun harus sudah join; auto klik daftar saat tombol muncul.\n\n"
                                        + "Verifikasi captcha: manual di WebView bila muncul.\n"
                                        + "Engine pengirim perintah = fase berikutnya."
                        );
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerModeRow || position == headerExtraRow
                    || position == headerGroupRow || position == botHeaderRow) {
                return 0;
            }
            if (position == autofishRow || position == fishLimitRow
                    || position == dailyRow || position == missionRow
                    || position == groupEnableRow) {
                return 1;
            }
            if (position >= botStartRow && position < botEndRow) {
                return 2;
            }
            if (position == statusRow || position == groupIdRow || position == groupClearRow) {
                return 3;
            }
            return 4;
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> list = new ArrayList<>();
        list.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR,
                new Class[]{HeaderCell.class, TextCheckCell.class, RadioCell.class, TextSettingsCell.class},
                null, null, null, Theme.key_windowBackgroundWhite));
        list.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null,
                Theme.key_windowBackgroundGray));
        return list;
    }
}
