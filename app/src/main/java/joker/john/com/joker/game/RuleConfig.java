package joker.john.com.joker.game;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.Serializable;

import joker.john.com.joker.CardColor;

/**
 * 双升规则配置。
 * 创建时间：2026-06-02
 * 最近修改：2026-06-02
 * by john
 */
public class RuleConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String PREFS_NAME = "shengji_rule_config";
    private static final String KEY_RANK_VALUE = "rank_value";
    private static final String KEY_TRUMP_SUIT = "trump_suit";
    private static final String KEY_TRACTOR_ENABLED = "tractor_enabled";
    private static final String KEY_HISTORY_LIMIT = "history_limit";
    private static final String KEY_HINT_ENABLED = "hint_enabled";
    private static final String KEY_REVEAL_STEAL_ENABLED = "reveal_steal_enabled";
    private static final String KEY_SPECIAL_THREE_CARD_SHUAI_ENABLED = "special_three_card_shuai_enabled";
    private static final String KEY_TRACTOR_CARRY_ACE_ENABLED = "tractor_carry_ace_enabled";
    private static final String KEY_SPECIAL_TOP_SPLIT_ENABLED = "special_top_split_enabled";
    private static final String KEY_PUBLIC_INFO_SEMI_SHUAI_ENABLED = "public_info_semi_shuai_enabled";
    private static final String KEY_COUNTER_TRUMP_SINGLE_JOKER_PAIR_ENABLED = "counter_trump_single_joker_pair_enabled";
    private static final String KEY_COUNTER_TRUMP_DOUBLE_JOKER_COPY_ENABLED = "counter_trump_double_joker_copy_enabled";
    private static final String KEY_COUNTER_NO_TRUMP_ENABLED = "counter_no_trump_enabled";
    private static final String KEY_SETTLEMENT_DELAY_SECONDS = "settlement_delay_seconds";
    private static final String KEY_UNDO_CHANCES_PER_ROUND = "undo_chances_per_round";

    public static final int[] AVAILABLE_RANK_VALUES = {5, 10, 13, 1, 2, 3, 4, 6, 7, 8, 9, 11, 12};
    public static final CardColor[] AVAILABLE_TRUMP_SUITS = {
            CardColor.Spade, CardColor.Heart, CardColor.Club, CardColor.Diamond
    };
    public static final int[] AVAILABLE_SETTLEMENT_DELAY_SECONDS = {0, 2, 4, 8};
    public static final int[] AVAILABLE_UNDO_CHANCES = {0, 1, 2};

    private int rankValue = 5;
    private CardColor trumpSuit = CardColor.Spade;
    private boolean tractorEnabled = true;
    private int historyLimit = 6;
    private boolean hintEnabled = true;
    private boolean revealStealEnabled = true;
    private boolean specialThreeCardShuaiEnabled = true;
    private boolean tractorCarryAceEnabled = true;
    private boolean specialTopSplitEnabled = true;
    private boolean publicInfoSemiShuaiEnabled = true;
    private boolean counterTrumpSingleJokerPairEnabled = true;
    private boolean counterTrumpDoubleJokerCopyEnabled = true;
    private boolean counterNoTrumpEnabled = true;
    private boolean noTrump;
    private int settlementDelaySeconds = 4;
    private int undoChancesPerRound = 1;

    public RuleConfig copy() {
        RuleConfig config = new RuleConfig();
        config.rankValue = rankValue;
        config.trumpSuit = trumpSuit;
        config.tractorEnabled = tractorEnabled;
        config.historyLimit = historyLimit;
        config.hintEnabled = hintEnabled;
        config.revealStealEnabled = revealStealEnabled;
        config.specialThreeCardShuaiEnabled = specialThreeCardShuaiEnabled;
        config.tractorCarryAceEnabled = tractorCarryAceEnabled;
        config.specialTopSplitEnabled = specialTopSplitEnabled;
        config.publicInfoSemiShuaiEnabled = publicInfoSemiShuaiEnabled;
        config.counterTrumpSingleJokerPairEnabled = counterTrumpSingleJokerPairEnabled;
        config.counterTrumpDoubleJokerCopyEnabled = counterTrumpDoubleJokerCopyEnabled;
        config.counterNoTrumpEnabled = counterNoTrumpEnabled;
        config.noTrump = noTrump;
        config.settlementDelaySeconds = settlementDelaySeconds;
        config.undoChancesPerRound = undoChancesPerRound;
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
        config.revealStealEnabled = preferences.getBoolean(KEY_REVEAL_STEAL_ENABLED, true);
        config.specialThreeCardShuaiEnabled = preferences.getBoolean(KEY_SPECIAL_THREE_CARD_SHUAI_ENABLED, true);
        config.tractorCarryAceEnabled = preferences.getBoolean(KEY_TRACTOR_CARRY_ACE_ENABLED, true);
        config.specialTopSplitEnabled = preferences.getBoolean(KEY_SPECIAL_TOP_SPLIT_ENABLED, true);
        config.publicInfoSemiShuaiEnabled = preferences.getBoolean(KEY_PUBLIC_INFO_SEMI_SHUAI_ENABLED, true);
        config.counterTrumpSingleJokerPairEnabled = preferences.getBoolean(KEY_COUNTER_TRUMP_SINGLE_JOKER_PAIR_ENABLED, true);
        config.counterTrumpDoubleJokerCopyEnabled = preferences.getBoolean(KEY_COUNTER_TRUMP_DOUBLE_JOKER_COPY_ENABLED, true);
        config.counterNoTrumpEnabled = preferences.getBoolean(KEY_COUNTER_NO_TRUMP_ENABLED, true);
        config.settlementDelaySeconds = preferences.getInt(KEY_SETTLEMENT_DELAY_SECONDS, 4);
        config.undoChancesPerRound = preferences.getInt(KEY_UNDO_CHANCES_PER_ROUND, 1);
        config.historyLimit = Math.max(3, Math.min(10, config.historyLimit));
        if (config.settlementDelaySeconds != 0 && config.settlementDelaySeconds != 2
                && config.settlementDelaySeconds != 4 && config.settlementDelaySeconds != 8) {
            config.settlementDelaySeconds = 4;
        }
        if (config.undoChancesPerRound != 0 && config.undoChancesPerRound != 1 && config.undoChancesPerRound != 2) {
            config.undoChancesPerRound = 1;
        }
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
                .putBoolean(KEY_REVEAL_STEAL_ENABLED, revealStealEnabled)
                .putBoolean(KEY_SPECIAL_THREE_CARD_SHUAI_ENABLED, specialThreeCardShuaiEnabled)
                .putBoolean(KEY_TRACTOR_CARRY_ACE_ENABLED, tractorCarryAceEnabled)
                .putBoolean(KEY_SPECIAL_TOP_SPLIT_ENABLED, specialTopSplitEnabled)
                .putBoolean(KEY_PUBLIC_INFO_SEMI_SHUAI_ENABLED, publicInfoSemiShuaiEnabled)
                .putBoolean(KEY_COUNTER_TRUMP_SINGLE_JOKER_PAIR_ENABLED, counterTrumpSingleJokerPairEnabled)
                .putBoolean(KEY_COUNTER_TRUMP_DOUBLE_JOKER_COPY_ENABLED, counterTrumpDoubleJokerCopyEnabled)
                .putBoolean(KEY_COUNTER_NO_TRUMP_ENABLED, counterNoTrumpEnabled)
                .putInt(KEY_SETTLEMENT_DELAY_SECONDS, settlementDelaySeconds)
                .putInt(KEY_UNDO_CHANCES_PER_ROUND, undoChancesPerRound)
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

    public boolean isRevealStealEnabled() {
        return revealStealEnabled;
    }

    public void setRevealStealEnabled(boolean revealStealEnabled) {
        this.revealStealEnabled = revealStealEnabled;
    }

    public boolean isSpecialThreeCardShuaiEnabled() {
        return specialThreeCardShuaiEnabled;
    }

    public void setSpecialThreeCardShuaiEnabled(boolean specialThreeCardShuaiEnabled) {
        this.specialThreeCardShuaiEnabled = specialThreeCardShuaiEnabled;
    }

    public boolean isTractorCarryAceEnabled() {
        return tractorCarryAceEnabled;
    }

    public void setTractorCarryAceEnabled(boolean tractorCarryAceEnabled) {
        this.tractorCarryAceEnabled = tractorCarryAceEnabled;
    }

    public boolean isSpecialTopSplitEnabled() {
        return specialTopSplitEnabled;
    }

    public void setSpecialTopSplitEnabled(boolean specialTopSplitEnabled) {
        this.specialTopSplitEnabled = specialTopSplitEnabled;
    }

    public boolean isPublicInfoSemiShuaiEnabled() {
        return publicInfoSemiShuaiEnabled;
    }

    public void setPublicInfoSemiShuaiEnabled(boolean publicInfoSemiShuaiEnabled) {
        this.publicInfoSemiShuaiEnabled = publicInfoSemiShuaiEnabled;
    }

    public boolean isCounterTrumpSingleJokerPairEnabled() {
        return counterTrumpSingleJokerPairEnabled;
    }

    public void setCounterTrumpSingleJokerPairEnabled(boolean counterTrumpSingleJokerPairEnabled) {
        this.counterTrumpSingleJokerPairEnabled = counterTrumpSingleJokerPairEnabled;
    }

    public boolean isCounterTrumpDoubleJokerCopyEnabled() {
        return counterTrumpDoubleJokerCopyEnabled;
    }

    public void setCounterTrumpDoubleJokerCopyEnabled(boolean counterTrumpDoubleJokerCopyEnabled) {
        this.counterTrumpDoubleJokerCopyEnabled = counterTrumpDoubleJokerCopyEnabled;
    }

    public boolean isCounterNoTrumpEnabled() {
        return counterNoTrumpEnabled;
    }

    public void setCounterNoTrumpEnabled(boolean counterNoTrumpEnabled) {
        this.counterNoTrumpEnabled = counterNoTrumpEnabled;
    }

    public boolean isNoTrump() {
        return noTrump;
    }

    public void setNoTrump(boolean noTrump) {
        this.noTrump = noTrump;
    }

    public int getSettlementDelaySeconds() {
        return settlementDelaySeconds;
    }

    public void setSettlementDelaySeconds(int settlementDelaySeconds) {
        if (settlementDelaySeconds == 0 || settlementDelaySeconds == 2
                || settlementDelaySeconds == 4 || settlementDelaySeconds == 8) {
            this.settlementDelaySeconds = settlementDelaySeconds;
        }
    }

    public String getRankLabel() {
        return formatCardValue(rankValue);
    }

    public String getTrumpSuitLabel() {
        if (noTrump) {
            return "无主";
        }
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

    public String getSettlementDelayLabel() {
        if (settlementDelaySeconds <= 0) {
            return "手动";
        }
        return settlementDelaySeconds + "秒";
    }

    public int getUndoChancesPerRound() {
        return undoChancesPerRound;
    }

    public void setUndoChancesPerRound(int undoChancesPerRound) {
        if (undoChancesPerRound == 0 || undoChancesPerRound == 1 || undoChancesPerRound == 2) {
            this.undoChancesPerRound = undoChancesPerRound;
        }
    }

    public String getUndoChancesLabel() {
        return String.valueOf(undoChancesPerRound);
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
