package tw.nekomimi.nekogram.autofish;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Daftar ikan rare untuk /favorite saat scan inventory.
 * Port dari rare_fish.py — tanpa log grup.
 */
public final class RareFish {

    private RareFish() {
    }

    public static final List<String> NAMES = Collections.unmodifiableList(Arrays.asList(
            // Ubur-ubur
            "ubur-ubur alien kristal",
            "ubur-ubur alien",
            "ubur-ubur hantu",
            "ubur-ubur arwah kristal",
            "dewi ubur-ubur",

            // Naga laut
            "dewa naga laut",
            "dewi naga laut",
            "naga laut emas",
            "naga laut",
            "naga laut terkutuk",
            "naga kutukan abadi",

            // Kraken
            "kraken",
            "kraken mata iblis",
            "kraken terkutuk",

            // Megalodon / Lochness
            "megalodon",
            "megalodon purba",
            "megalodon terkutuk",
            "lochness",
            "mega lochness",

            // Legenda / dewa
            "ratu laut mutiara",
            "poseidon",
            "duyung aurora",
            "raja atlantis",
            "siren",
            "jormungandr",
            "karkinos",

            // Obsidian / terkutuk
            "lumba-lumba obsidian",
            "hiu obsidian",
            "hiu terkutuk",
            "kepiting obsidian",
            "lumba-lumba nimbus",
            "lumba-lumba terkutuk",
            "kepiting terkutuk",

            // Event Halloween
            "anglerfish labu",
            "cupang penyihir",
            "buntal labu",
            "pari drakula",
            "martil frankenstein",
            "penyu mumi",
            "jack o kraken",

            // Lainnya
            "anglerfish",
            "mini bake kujira",
            "bake kujira",
            "tralalelo tralala"
    ));

    /**
     * true jika baris inventory mengandung nama rare (abaikan trisula).
     */
    public static boolean isRareLine(String line) {
        if (line == null || line.isEmpty()) {
            return false;
        }
        String low = line.toLowerCase(Locale.ROOT);
        if (low.contains("trisula")) {
            return false;
        }
        for (int i = 0; i < NAMES.size(); i++) {
            if (low.contains(NAMES.get(i))) {
                return true;
            }
        }
        return false;
    }
}
