package com.sendbird.desk.android.sample.utils;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class SoftInputUtils {
    public static void hideSoftKeyboard(View view) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) view.getContext().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(
                    view.getWindowToken(), 0);
        }
    }

    public static void showSoftKeyboard(final EditText editText) {
        final InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        editText.postDelayed(() -> {
            editText.requestFocus();
            if (imm != null) {
                imm.showSoftInput(editText, 0);
            }
        }, 100);
    }
}
