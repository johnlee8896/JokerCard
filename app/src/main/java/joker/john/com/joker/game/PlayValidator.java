package joker.john.com.joker.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import joker.john.com.joker.Card;
import joker.john.com.joker.CardColor;
import joker.john.com.joker.CardsUtility;

public class PlayValidator {
    public static final int GROUP_SPADE = 0;
    public static final int GROUP_HEART = 1;
    public static final int GROUP_CLUB = 2;
    public static final int GROUP_DIAMOND = 3;
    public static final int GROUP_TRUMP = 100;
    public static final int GROUP_MIXED = -100;

    public static ValidationResult validateLead(List<Card> selectedCards, RuleConfig config) {
        PlayPattern pattern = analyzePattern(selectedCards, config);
        if (pattern.getType() == PlayPattern.Type.INVALID) {
            return ValidationResult.fail(pattern.getMessage());
        }
        if (pattern.getType() == PlayPattern.Type.MIXED) {
            return ValidationResult.fail("先手只支持单张、对子和拖拉机。");
        }
        if (pattern.getType() == PlayPattern.Type.TRACTOR && !config.isTractorEnabled()) {
            return ValidationResult.fail("当前设置未开启拖拉机。");
        }
        return ValidationResult.success(pattern);
    }

    public static ValidationResult validateFollow(List<Card> hand, List<Card> selectedCards, PlayPattern leadPattern, RuleConfig config) {
        PlayPattern selectedPattern = analyzePattern(selectedCards, config);
        if (selectedPattern.getType() == PlayPattern.Type.INVALID) {
            return ValidationResult.fail(selectedPattern.getMessage());
        }

        if (selectedCards.size() != leadPattern.getCardCount()) {
            return ValidationResult.fail("本轮需要跟 " + leadPattern.getCardCount() + " 张牌。");
        }

        int leadGroup = leadPattern.getGroup();
        int handLeadGroupCount = countCardsInGroup(hand, leadGroup, config);
        int requiredLeadGroupCount = Math.min(handLeadGroupCount, leadPattern.getCardCount());
        int selectedLeadGroupCount = countCardsInGroup(selectedCards, leadGroup, config);
        if (selectedLeadGroupCount != requiredLeadGroupCount) {
            if (handLeadGroupCount >= leadPattern.getCardCount()) {
                return ValidationResult.fail("需要优先跟同门或主牌。");
            }
            return ValidationResult.fail("同门不够时需要先把手里的同门出完。");
        }

        if (handLeadGroupCount >= leadPattern.getCardCount()) {
            List<Card> sameGroupCards = getCardsInGroup(hand, leadGroup, config);
            if (leadPattern.getType() == PlayPattern.Type.PAIR
                    && hasExactPair(sameGroupCards)
                    && selectedPattern.getType() != PlayPattern.Type.PAIR) {
                return ValidationResult.fail("手里有对子时，需要跟对子。");
            }
            if (leadPattern.getType() == PlayPattern.Type.TRACTOR
                    && canFormTractor(sameGroupCards, leadPattern.getCardCount(), config)
                    && selectedPattern.getType() != PlayPattern.Type.TRACTOR) {
                return ValidationResult.fail("手里能跟拖拉机时，需要按拖拉机跟牌。");
            }
        }

        return ValidationResult.success(selectedPattern);
    }

    public static PlayPattern analyzePattern(List<Card> selectedCards, RuleConfig config) {
        if (selectedCards == null || selectedCards.isEmpty()) {
            return PlayPattern.invalid("请先点选要出的牌。");
        }

        ArrayList<Card> cards = new ArrayList<>(selectedCards);
        sortByStrengthDescending(cards, config);
        int group = getCommonGroup(cards, config);
        int topSequence = getSequenceIndex(cards.get(0), config);

        if (cards.size() == 1) {
            return new PlayPattern(PlayPattern.Type.SINGLE, getGroup(cards.get(0), config), 1, 0, topSequence, "单张");
        }

        if (cards.size() == 2) {
            if (isExactPair(cards.get(0), cards.get(1))) {
                return new PlayPattern(PlayPattern.Type.PAIR, getGroup(cards.get(0), config), 2, 1, topSequence, "对子");
            }
            if (group != GROUP_MIXED) {
                return new PlayPattern(PlayPattern.Type.MIXED, group, 2, 0, topSequence, "散牌");
            }
            return new PlayPattern(PlayPattern.Type.MIXED, GROUP_MIXED, 2, 0, topSequence, "散牌");
        }

        if (cards.size() % 2 != 0) {
            if (group != GROUP_MIXED) {
                return new PlayPattern(PlayPattern.Type.MIXED, group, cards.size(), 0, topSequence, "散牌");
            }
            return new PlayPattern(PlayPattern.Type.MIXED, GROUP_MIXED, cards.size(), 0, topSequence, "散牌");
        }

        if (group != GROUP_MIXED && isTractor(cards, config)) {
            return new PlayPattern(PlayPattern.Type.TRACTOR, group, cards.size(), cards.size() / 2, topSequence, "拖拉机");
        }

        if (group != GROUP_MIXED) {
            return new PlayPattern(PlayPattern.Type.MIXED, group, cards.size(), 0, topSequence, "散牌");
        }
        return new PlayPattern(PlayPattern.Type.MIXED, GROUP_MIXED, cards.size(), 0, topSequence, "散牌");
    }

    public static void applyRuleConfigToUtility(RuleConfig config) {
        CardsUtility.pubMainValue = config.getRankValue();
        CardsUtility.colorMain = config.getTrumpSuit();
    }

    public static void sortByStrengthDescending(List<Card> cards, final RuleConfig config) {
        Collections.sort(cards, new Comparator<Card>() {
            @Override
            public int compare(Card o1, Card o2) {
                return getSequenceIndex(o2, config) - getSequenceIndex(o1, config);
            }
        });
    }

    public static void sortForDisplay(List<Card> cards, final RuleConfig config) {
        Collections.sort(cards, new Comparator<Card>() {
            @Override
            public int compare(Card first, Card second) {
                int groupCompare = getDisplayGroupOrder(first, config) - getDisplayGroupOrder(second, config);
                if (groupCompare != 0) {
                    return groupCompare;
                }
                return getSequenceIndex(second, config) - getSequenceIndex(first, config);
            }
        });
    }

    public static ArrayList<Card> suggestLead(List<Card> hand, RuleConfig config) {
        ArrayList<Card> result = null;
        if (config.isTractorEnabled()) {
            result = findLowestTractor(hand, 4, config);
            if (result != null) {
                return result;
            }
        }
        result = findLowestPair(hand, GROUP_MIXED, config);
        if (result != null) {
            return result;
        }
        result = new ArrayList<>();
        ArrayList<Card> copy = new ArrayList<>(hand);
        sortByStrengthAscending(copy, config);
        result.add(copy.get(0));
        return result;
    }

    public static ArrayList<Card> suggestFollow(List<Card> hand, PlayPattern leadPattern, RuleConfig config) {
        ArrayList<Card> sameGroupCards = getCardsInGroup(hand, leadPattern.getGroup(), config);
        sortByStrengthAscending(sameGroupCards, config);

        int requiredCount = leadPattern.getCardCount();
        if (sameGroupCards.size() >= requiredCount) {
            if (leadPattern.getType() == PlayPattern.Type.SINGLE) {
                ArrayList<Card> result = new ArrayList<>();
                result.add(sameGroupCards.get(0));
                return result;
            }
            if (leadPattern.getType() == PlayPattern.Type.PAIR) {
                ArrayList<Card> pair = findLowestPair(sameGroupCards, leadPattern.getGroup(), config);
                if (pair != null) {
                    return pair;
                }
                return takeFirstCards(sameGroupCards, requiredCount);
            }
            if (leadPattern.getType() == PlayPattern.Type.TRACTOR) {
                ArrayList<Card> tractor = findLowestTractor(sameGroupCards, requiredCount, config);
                if (tractor != null) {
                    return tractor;
                }
                return takeFirstCards(sameGroupCards, requiredCount);
            }
            return takeFirstCards(sameGroupCards, requiredCount);
        }

        ArrayList<Card> result = new ArrayList<>(sameGroupCards);
        ArrayList<Card> rest = new ArrayList<>(hand);
        for (Card card : sameGroupCards) {
            rest.remove(card);
        }
        sortByStrengthAscending(rest, config);
        while (result.size() < requiredCount && !rest.isEmpty()) {
            result.add(rest.remove(0));
        }
        return result;
    }

    public static ArrayList<Card> findLowestPair(List<Card> cards, int restrictedGroup, RuleConfig config) {
        ArrayList<PairUnit> pairUnits = buildPairUnits(cards, restrictedGroup, config);
        if (pairUnits.isEmpty()) {
            return null;
        }
        Collections.sort(pairUnits, new Comparator<PairUnit>() {
            @Override
            public int compare(PairUnit first, PairUnit second) {
                return first.sequence - second.sequence;
            }
        });
        return new ArrayList<>(pairUnits.get(0).cards);
    }

    public static ArrayList<Card> findLowestTractor(List<Card> cards, int cardCount, RuleConfig config) {
        if (cardCount < 4 || cardCount % 2 != 0) {
            return null;
        }
        ArrayList<PairUnit> pairUnits = buildPairUnits(cards, GROUP_MIXED, config);
        Collections.sort(pairUnits, new Comparator<PairUnit>() {
            @Override
            public int compare(PairUnit first, PairUnit second) {
                return first.sequence - second.sequence;
            }
        });

        int needPairs = cardCount / 2;
        for (int i = 0; i <= pairUnits.size() - needPairs; i++) {
            PairUnit start = pairUnits.get(i);
            boolean ok = true;
            for (int j = 1; j < needPairs; j++) {
                PairUnit next = pairUnits.get(i + j);
                if (next.group != start.group || next.sequence - pairUnits.get(i + j - 1).sequence != 1) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                ArrayList<Card> result = new ArrayList<>();
                for (int j = 0; j < needPairs; j++) {
                    result.addAll(pairUnits.get(i + j).cards);
                }
                sortByStrengthDescending(result, config);
                return result;
            }
        }
        return null;
    }

    public static boolean canFormTractor(List<Card> cards, int cardCount, RuleConfig config) {
        return findLowestTractor(cards, cardCount, config) != null;
    }

    public static boolean hasExactPair(List<Card> cards) {
        return !buildPairUnits(cards, GROUP_MIXED, null).isEmpty();
    }

    public static int getGroup(Card card, RuleConfig config) {
        if (isTrump(card, config)) {
            return GROUP_TRUMP;
        }
        if (card.getColor() == CardColor.Spade) {
            return GROUP_SPADE;
        }
        if (card.getColor() == CardColor.Heart) {
            return GROUP_HEART;
        }
        if (card.getColor() == CardColor.Club) {
            return GROUP_CLUB;
        }
        return GROUP_DIAMOND;
    }

    public static boolean isTrump(Card card, RuleConfig config) {
        return card.getValue() == 14
                || card.getValue() == 15
                || card.getValue() == config.getRankValue()
                || card.getColor() == config.getTrumpSuit();
    }

    public static int getSequenceIndex(Card card, RuleConfig config) {
        if (card.getValue() == 15) {
            return 300;
        }
        if (card.getValue() == 14) {
            return 299;
        }
        if (card.getValue() == config.getRankValue() && card.getColor() == config.getTrumpSuit()) {
            return 298;
        }
        if (card.getValue() == config.getRankValue()) {
            return 297 - getOffSuitRankOrder(card.getColor(), config.getTrumpSuit());
        }
        if (card.getColor() == config.getTrumpSuit()) {
            return 294 - getDescendingRankIndex(card.getValue(), config.getRankValue());
        }
        return 100 - getDescendingRankIndex(card.getValue(), config.getRankValue());
    }

    public static int getCardPoints(Card card) {
        if (card.getValue() == 5) {
            return 5;
        }
        if (card.getValue() == 10 || card.getValue() == 13) {
            return 10;
        }
        return 0;
    }

    public static String describeCards(List<Card> cards, RuleConfig config) {
        ArrayList<Card> copy = new ArrayList<>(cards);
        sortByStrengthDescending(copy, config);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < copy.size(); i++) {
            if (i > 0) {
                builder.append(" ");
            }
            builder.append(getCardLabel(copy.get(i), config));
        }
        return builder.toString();
    }

    public static String getCardLabel(Card card, RuleConfig config) {
        if (card.getValue() == 15) {
            return "大王";
        }
        if (card.getValue() == 14) {
            return "小王";
        }
        return getSuitLabel(card.getColor()) + RuleConfig.formatCardValue(card.getValue()) + (isTrump(card, config) ? "*" : "");
    }

    public static String getSuitLabel(CardColor color) {
        if (color == CardColor.Spade) {
            return "黑";
        }
        if (color == CardColor.Heart) {
            return "红";
        }
        if (color == CardColor.Club) {
            return "梅";
        }
        if (color == CardColor.Diamond) {
            return "方";
        }
        return "";
    }

    public static int countCardsInGroup(List<Card> cards, int group, RuleConfig config) {
        int count = 0;
        for (Card card : cards) {
            if (getGroup(card, config) == group) {
                count++;
            }
        }
        return count;
    }

    public static ArrayList<Card> getCardsInGroup(List<Card> cards, int group, RuleConfig config) {
        ArrayList<Card> result = new ArrayList<>();
        for (Card card : cards) {
            if (group == GROUP_MIXED || getGroup(card, config) == group) {
                result.add(card);
            }
        }
        return result;
    }

    private static boolean isTractor(List<Card> cards, RuleConfig config) {
        if (cards.size() < 4 || cards.size() % 2 != 0) {
            return false;
        }
        int group = getCommonGroup(cards, config);
        if (group == GROUP_MIXED) {
            return false;
        }
        ArrayList<Card> copy = new ArrayList<>(cards);
        sortByStrengthDescending(copy, config);
        for (int i = 0; i < copy.size(); i += 2) {
            if (!isExactPair(copy.get(i), copy.get(i + 1))) {
                return false;
            }
            if (i > 0) {
                int previousSequence = getSequenceIndex(copy.get(i - 2), config);
                int currentSequence = getSequenceIndex(copy.get(i), config);
                if (previousSequence - currentSequence != 1) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int getCommonGroup(List<Card> cards, RuleConfig config) {
        int group = getGroup(cards.get(0), config);
        for (int i = 1; i < cards.size(); i++) {
            if (getGroup(cards.get(i), config) != group) {
                return GROUP_MIXED;
            }
        }
        return group;
    }

    private static boolean isExactPair(Card first, Card second) {
        return first.getValue() == second.getValue() && first.getColor() == second.getColor();
    }

    private static int getDisplayGroupOrder(Card card, RuleConfig config) {
        int group = getGroup(card, config);
        if (group == GROUP_TRUMP) {
            return 0;
        }
        if (group == GROUP_SPADE) {
            return 1;
        }
        if (group == GROUP_HEART) {
            return 2;
        }
        if (group == GROUP_CLUB) {
            return 3;
        }
        return 4;
    }

    private static void sortByStrengthAscending(List<Card> cards, final RuleConfig config) {
        Collections.sort(cards, new Comparator<Card>() {
            @Override
            public int compare(Card o1, Card o2) {
                return getSequenceIndex(o1, config) - getSequenceIndex(o2, config);
            }
        });
    }

    private static ArrayList<PairUnit> buildPairUnits(List<Card> cards, int restrictedGroup, RuleConfig config) {
        HashMap<String, ArrayList<Card>> pairMap = new HashMap<>();
        for (Card card : cards) {
            if (restrictedGroup != GROUP_MIXED && config != null && getGroup(card, config) != restrictedGroup) {
                continue;
            }
            String key = card.getValue() + "_" + card.getColor().name();
            ArrayList<Card> list = pairMap.get(key);
            if (list == null) {
                list = new ArrayList<>();
                pairMap.put(key, list);
            }
            list.add(card);
        }

        ArrayList<PairUnit> result = new ArrayList<>();
        for (ArrayList<Card> sameCards : pairMap.values()) {
            if (sameCards.size() >= 2) {
                ArrayList<Card> pairCards = new ArrayList<>();
                pairCards.add(sameCards.get(0));
                pairCards.add(sameCards.get(1));
                Card representative = pairCards.get(0);
                int group = config == null ? GROUP_MIXED : getGroup(representative, config);
                int sequence = config == null ? 0 : getSequenceIndex(representative, config);
                result.add(new PairUnit(group, sequence, pairCards));
            }
        }
        return result;
    }

    private static int getDescendingRankIndex(int value, int rankValue) {
        int[] descending = {2, 1, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3};
        int index = 0;
        for (int candidate : descending) {
            if (candidate == rankValue) {
                continue;
            }
            if (candidate == value) {
                return index;
            }
            index++;
        }
        return 99;
    }

    private static int getOffSuitRankOrder(CardColor color, CardColor trumpSuit) {
        CardColor[] suits = {CardColor.Spade, CardColor.Heart, CardColor.Club, CardColor.Diamond};
        int offset = 0;
        for (CardColor suit : suits) {
            if (suit == trumpSuit) {
                continue;
            }
            if (suit == color) {
                return offset;
            }
            offset++;
        }
        return 0;
    }

    private static ArrayList<Card> takeFirstCards(List<Card> cards, int count) {
        ArrayList<Card> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, cards.size()); i++) {
            result.add(cards.get(i));
        }
        return result;
    }

    public static class ValidationResult {
        private final boolean success;
        private final String message;
        private final PlayPattern pattern;

        private ValidationResult(boolean success, String message, PlayPattern pattern) {
            this.success = success;
            this.message = message;
            this.pattern = pattern;
        }

        public static ValidationResult success(PlayPattern pattern) {
            return new ValidationResult(true, "", pattern);
        }

        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public PlayPattern getPattern() {
            return pattern;
        }
    }

    private static class PairUnit {
        final int group;
        final int sequence;
        final ArrayList<Card> cards;

        PairUnit(int group, int sequence, ArrayList<Card> cards) {
            this.group = group;
            this.sequence = sequence;
            this.cards = cards;
        }
    }
}
