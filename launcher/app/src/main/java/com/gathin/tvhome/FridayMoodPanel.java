package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Small Friday-only mood card. Kept in its own slot above the prayer panel to prevent overlap. */
final class FridayMoodPanel {
    private final View card;
    private final Context context;

    FridayMoodPanel(Context context, FrameLayout root) {
        this.context = context;
        LinearLayout body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xE90C2416, 0xE907110C});
        background.setCornerRadius(dp(15));
        background.setStroke(dp(1), 0x55D7B56D);
        body.setBackground(background);
        body.setFocusable(false);
        body.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        TextView kicker = new TextView(context);
        kicker.setText("FRIDAY MOOD");
        kicker.setTextColor(0xFFD7B56D);
        kicker.setTextSize(9);
        kicker.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        kicker.setLetterSpacing(0.18f);
        body.addView(kicker, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(context);
        title.setText("Jumat Santai  ✳");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        title.setPadding(0, dp(3), 0, 0);
        body.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView subtitle = new TextView(context);
        subtitle.setText("Pelan-pelan, tetap jalan · stay in balance");
        subtitle.setTextColor(0xFFB0C4B6);
        subtitle.setTextSize(10);
        subtitle.setPadding(0, dp(2), 0, 0);
        body.addView(subtitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        card = body;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                dp(390), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT);
        lp.rightMargin = dp(56);
        lp.topMargin = dp(82);
        root.addView(body, lp);
        update();
    }

    void update() {
        card.setVisibility(LocalDate.now().getDayOfWeek() == DayOfWeek.FRIDAY
                ? View.VISIBLE : View.GONE);
    }

    private int dp(float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
