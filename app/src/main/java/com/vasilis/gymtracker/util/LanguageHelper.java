package com.vasilis.gymtracker.util;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vasilis.gymtracker.R;

import java.util.Locale;

/**
 * In-app language switch (English / Greek).
 *
 * AppCompatDelegate.setApplicationLocales() changes the language of the whole app,
 * recreates the open screens automatically and remembers the choice
 * (on Android 12 and older thanks to the "autoStoreLocales" service in the manifest).
 *
 * If the user never picks a language, the app follows the phone's language:
 * English by default, Greek if the phone is set to Greek.
 */
public final class LanguageHelper {

    private static final String[] LANGUAGE_TAGS = {"en", "el"};
    // Language names are shown in their own language, so they are not translated.
    private static final String[] LANGUAGE_NAMES = {"English", "Ελληνικά"};

    private LanguageHelper() {
    }

    public static void showLanguageDialog(Context context) {
        int current = currentLanguageIndex();
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.language)
                .setSingleChoiceItems(LANGUAGE_NAMES, current, (dialog, which) -> {
                    dialog.dismiss();
                    if (which != current) {
                        AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags(LANGUAGE_TAGS[which]));
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** 0 = English, 1 = Greek. */
    private static int currentLanguageIndex() {
        LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
        Locale locale = appLocales.isEmpty() ? Locale.getDefault() : appLocales.get(0);
        String language = locale == null ? "en" : locale.getLanguage();
        return "el".equals(language) ? 1 : 0;
    }
}
