package joker.john.com.joker.game;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.Serializable;

import joker.john.com.joker.CardColor;

public class RuleConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String PREFS_NAME = "shengji_rule_config";
    private static final String KEY_RANK_VALUE = "rank_value";
    private static final String KEY_TRUMP_SUIT = "trump_suit";
    private static final String KEY_TRACTOR_ENABLED = "tractor_enabled";
    private static final String KEY_HISTORY_LIMIT = "history_limit";
    private static final String KEY_HINT_ENABLED = "hint_enabled";

    public static final int[] AVAILABLE_RANK_VALUES = {5, 10, 13, 1, 2, 3, 4, 6, 7, 8, 9, 11, 12};
    public static final CardColor[] AVAILABLE_TRUMP_SUITS = {
            CardColor.Spade, CardColor.Heart, CardColor.Club, CardColor.Diamond
    };

    private int rankValue = 5;
    private CardColor trumpSuit = CardColor.Spade;
    private boolean tractorEnabled = true;
    private int historyLimit = 6;
    private boolean hintEnabled = true;

    public RuleConfig copy() {
        RuleConfig config = new RuleConfig();
        config.rankValue = rankValue;
        config.trumpSuit = trumpSuit;
        config.tractorEnabled = tractorEnabled;
        config.historyLimit = historyLimit;
        config.hintEnabled = hintEnabled;
        return config;
    }

    public static RuleConfig load(Context context) {
        RuleConfig config = new RuleConfig();
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        config.rankValue = preferences.getInt(KEY_RANK_VALUE, 5);
        try {
            config.trumpSuit = CardColor.valueOf(preferences.getString(KEY_TRUMP_SUIT, CardColor.Spade.name()));
        } catch (Exception e) {
            config.trumpSuit = CardColor.Spade;
        }
        config.tractorEnabled = preferences.getBoolean(KEY_TRACTOR_ENABLED, true);
        config.historyLimit = preferences.getInt(KEY_HISTORY_LIMIT, 6);
        config.hintEnabled = preferences.getBoolean(KEY_HINT_ENABLED, true);
        config.historyLimit = Math.max(3, Math.min(10, config.historyLimit));
        return config;
    }

    public void save(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit()
                .putInt(KEY_RANK_VALUE, rankValue)
                .putString(KEY_TRUMP_SUIT, trumpSuit.name())
                .putBoolean(KEY_TRACTOR_ENABLED, tractorEnabled)
                .putInt(KEY_HISTORY_LIMIT, historyLimit)
                .putBoolean(KEY_HINT_ENABLED, hintEnabled)
                .apply();
    }

    public int getRankValue() {
        return rankValue;
    }

    public void setRankValue(int rankValue) {
        this.rankValue = rankValue;
    }

    public CardColor getTrumpSuit() {
        return trumpSuit;
    }

    public void setTrumpSuit(CardColor trumpSuit) {
        this.trumpSuit = trumpSuit;
    }

    public boolean isTractorEnabled() {
        return tractorEnabled;
    }

    public void setTractorEnabled(boolean tractorEnabled) {
        this.tractorEnabled = tractorEnabled;
    }

    public int getHistoryLimit() {
        return historyLimit;
    }

    public void setHistoryLimit(int historyLimit) {
        this.historyLimit = Math.max(3, Math.min(10, historyLimit));
    }

    public boolean isHintEnabled() {
        return hintEnabled;
    }

    public void setHintEnabled(boolean hintEnabled) {
        this.hintEnabled = hintEnabled;
    }

    public String getRankLabel() {
        return formatCardValue(rankValue);
    }

    public String getTrumpSuitLabel() {
        if (trumpSuit == CardColor.Spade) {
            return "黑桃";
        }
        if (trumpSuit == CardColor.Heart) {
            return "红桃";
        }
        if (trumpSuit == CardColor.Club) {
            return "梅花";
        }
        return "方块";
    }

    public static String formatCardValue(int value) {
        switch (value) {
            case 1:
                return "A";
            case 11:
                return "J";
            case 12:
                return "Q";
            case 13:
                return "K";
            default:
                return String.valueOf(value);
        }
    }
}
