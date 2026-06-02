package joker.john.com.joker.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import joker.john.com.joker.Card;
import joker.john.com.joker.CardColor;
import joker.john.com.joker.CardsUtility;

/**
 * 出牌规则校验与牌型分析工具。
 * 创建时间：2026-06-02
 * 最近修改：2026-06-02
 * by john
 */
public class PlayValidator {
    public static final int GROUP_SPADE = 0;
    public static final int GROUP_HEART = 1;
    public static final int GROUP_CLUB = 2;
    public static final int GROUP_DIAMOND = 3;
    public static final int GROUP_TRUMP = 100;
    public static final int GROUP_MIXED = -100;

    /**
     * 校验先手出牌是否合法，并识别牌型。
     */
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

    /**
     * 校验跟牌是否合法，重点保证同门、对子和拖拉机结构优先被跟满。
     */
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

        List<Card> sameGroupCards = getCardsInGroup(hand, leadGroup, config);
        if (leadPattern.getType() == PlayPattern.Type.PAIR
                && handLeadGroupCount >= leadPattern.getCardCount()
                && hasExactPair(sameGroupCards)
                && selectedPattern.getType() != PlayPattern.Type.PAIR) {
            return ValidationResult.fail("手里有对子时，需要跟对子。");
        }

        if (leadPattern.getType() == PlayPattern.Type.TRACTOR) {
            int requiredPairCount = Math.min(countPairUnits(sameGroupCards, leadGroup, config), leadPattern.getPairCount());
            int selectedPairCount = countPairUnits(getCardsInGroup(selectedCards, leadGroup, config), leadGroup, config);
            if (selectedPairCount < requiredPairCount) {
                return ValidationResult.fail("拖拉机跟牌时，手里有对子必须尽量先跟对子。");
            }
            if (handLeadGroupCount >= leadPattern.getCardCount()
                    && canFormTractor(sameGroupCards, leadPattern.getCardCount(), config)
                    && selectedPattern.getType() != PlayPattern.Type.TRACTOR) {
                return ValidationResult.fail("手里能跟拖拉机时，需要按拖拉机跟牌。");
            }
        }

        if (leadPattern.getType() == PlayPattern.Type.SHUAI
                && handLeadGroupCount >= leadPattern.getCardCount()
                && selectedPattern.getGroup() != leadGroup) {
            return ValidationResult.fail("甩牌时有同门必须完整跟同门。");
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
                return new PlayPattern(PlayPattern.Type.SHUAI, group, cards.size(), 0, topSequence, "甩牌");
            }
            return new PlayPattern(PlayPattern.Type.MIXED, GROUP_MIXED, cards.size(), 0, topSequence, "散牌");
        }

        if (group != GROUP_MIXED && isTractor(cards, config)) {
            return new PlayPattern(PlayPattern.Type.TRACTOR, group, cards.size(), cards.size() / 2, topSequence, "拖拉机");
        }

        if (group != GROUP_MIXED) {
            return new PlayPattern(PlayPattern.Type.SHUAI, group, cards.size(), 0, topSequence, "甩牌");
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
                PairUnit previous = pairUnits.get(i + j - 1);
                Card previousRepresentative = previous.cards.get(0);
                Card currentRepresentative = next.cards.get(0);
                if (next.group != start.group
                        || !areTractorPairsAdjacent(currentRepresentative, previousRepresentative,
                        next.sequence, previous.sequence, config)) {
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
                || (!config.isNoTrump() && card.getColor() == config.getTrumpSuit());
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
        if (!config.isNoTrump() && card.getColor() == config.getTrumpSuit()) {
            return 294 - getDescendingRankIndex(card.getValue(), config.getRankValue());
        }
        return 100 - getDescendingRankIndex(card.getValue(), config.getRankValue());
    }

    public static boolean canShuaiSucceed(List<Card> selectedCards, List<Card>[] hands, int leadPlayer, RuleConfig config) {
        if (selectedCards == null || selectedCards.size() <= 1) {
            return true;
        }
        PlayPattern pattern = analyzePattern(selectedCards, config);
        if (pattern.getType() != PlayPattern.Type.SHUAI) {
            return true;
        }
        if (isPrivilegedThreeCardShuai(selectedCards, config)) {
            return true;
        }
        if (isPrivilegedTopSplitShuai(selectedCards, hands[leadPlayer], config)) {
            return true;
        }
        int group = pattern.getGroup();
        ArrayList<Card> sameGroupCards = getCardsInGroup(selectedCards, group, config);
        ArrayList<PairUnit> selectedPairs = buildPairUnits(sameGroupCards, group, config);
        boolean pairOnlyShuai = selectedCards.size() % 2 == 0 && selectedPairs.size() * 2 == selectedCards.size();
        int weakest = pairOnlyShuai ? getWeakestPairSequence(selectedPairs) : getWeakestSequence(selectedCards, config);
        for (int player = 0; player < hands.length; player++) {
            if (player == leadPlayer) {
                continue;
            }
            ArrayList<Card> playerGroupCards = getCardsInGroup(hands[player], group, config);
            if (pairOnlyShuai) {
                ArrayList<PairUnit> playerPairs = buildPairUnits(playerGroupCards, group, config);
                for (PairUnit pairUnit : playerPairs) {
                    if (pairUnit.sequence > weakest) {
                        return false;
                    }
                }
                continue;
            }
            for (Card card : playerGroupCards) {
                if (getSequenceIndex(card, config) > weakest) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 按公开信息判断半甩是否成立，不读取其他玩家的未公开手牌。
     */
    public static boolean canSemiShuaiByPublicInfo(List<Card> selectedCards, GameState state) {
        if (state == null || selectedCards == null || selectedCards.size() <= 1) {
            return false;
        }
        RuleConfig config = state.getRuleConfig();
        if (!config.isPublicInfoSemiShuaiEnabled()) {
            return false;
        }
        PlayPattern pattern = analyzePattern(selectedCards, config);
        if (pattern.getType() != PlayPattern.Type.SHUAI || pattern.getGroup() == GROUP_MIXED) {
            return false;
        }

        if (isPairOnlySemiShuai(selectedCards, pattern.getGroup(), config)) {
            return canPairSemiShuaiByPublicInfo(selectedCards, state, config);
        }
        return canSingleSemiShuaiByPublicInfo(selectedCards, state, config);
    }

    /**
     * 半甩失败时，按公开信息缩成必须先落出来的最小子集。
     */
    public static ArrayList<Card> reduceFailedSemiShuai(List<Card> selectedCards, GameState state) {
        if (state == null || selectedCards == null || selectedCards.isEmpty()) {
            return new ArrayList<>();
        }
        RuleConfig config = state.getRuleConfig();
        PlayPattern pattern = analyzePattern(selectedCards, config);
        if (pattern.getType() != PlayPattern.Type.SHUAI || pattern.getGroup() == GROUP_MIXED) {
            return reduceFailedShuai(selectedCards, config);
        }

        if (isPairOnlySemiShuai(selectedCards, pattern.getGroup(), config)) {
            ArrayList<Card> reducedPairs = reduceFailedPairSemiShuai(selectedCards, state, config);
            if (!reducedPairs.isEmpty()) {
                return reducedPairs;
            }
        } else {
            ArrayList<Card> reducedSingles = reduceFailedSingleSemiShuai(selectedCards, state, config);
            if (!reducedSingles.isEmpty()) {
                return reducedSingles;
            }
        }
        return reduceFailedShuai(selectedCards, config);
    }

    public static ArrayList<Card> reduceFailedShuai(List<Card> selectedCards, RuleConfig config) {
        ArrayList<Card> copy = new ArrayList<>(selectedCards);
        sortByStrengthAscending(copy, config);
        int group = getCommonGroup(copy, config);
        if (group != GROUP_MIXED && copy.size() % 2 == 0) {
            ArrayList<PairUnit> pairUnits = buildPairUnits(copy, group, config);
            if (!pairUnits.isEmpty() && pairUnits.size() * 2 == copy.size()) {
                Collections.sort(pairUnits, new Comparator<PairUnit>() {
                    @Override
                    public int compare(PairUnit first, PairUnit second) {
                        return first.sequence - second.sequence;
                    }
                });
                return new ArrayList<>(pairUnits.get(0).cards);
            }
        }
        ArrayList<Card> reduced = new ArrayList<>();
        if (!copy.isEmpty()) {
            reduced.add(copy.get(0));
        }
        return reduced;
    }

    public static ArrayList<Card> buildPublicInfoSemiShuaiCandidates(List<Card> hand, GameState state) {
        ArrayList<Card> candidates = new ArrayList<>();
        if (hand == null || state == null || hand.size() < 3) {
            return candidates;
        }
        RuleConfig config = state.getRuleConfig();
        int[] groups = {GROUP_TRUMP, GROUP_SPADE, GROUP_HEART, GROUP_CLUB, GROUP_DIAMOND};
        for (int group : groups) {
            ArrayList<Card> groupCards = getCardsInGroup(hand, group, config);
            if (groupCards.size() < 3) {
                continue;
            }
            sortByStrengthDescending(groupCards, config);
            int maxSize = Math.min(groupCards.size(), 6);
            for (int size = maxSize; size >= 3; size--) {
                ArrayList<Card> candidate = new ArrayList<>(groupCards.subList(0, size));
                ValidationResult validation = validateLead(candidate, config);
                if (!validation.isSuccess() || validation.getPattern().getType() != PlayPattern.Type.SHUAI) {
                    continue;
                }
                if (canSemiShuaiByPublicInfo(candidate, state)) {
                    candidates.addAll(candidate);
                    return candidates;
                }
            }
        }
        return candidates;
    }

    public static boolean isAllTrump(List<Card> cards, RuleConfig config) {
        if (cards == null || cards.isEmpty()) {
            return false;
        }
        for (Card card : cards) {
            if (!isTrump(card, config)) {
                return false;
            }
        }
        return true;
    }

    public static int getWeakestSequence(List<Card> cards, RuleConfig config) {
        int weakest = Integer.MAX_VALUE;
        for (Card card : cards) {
            weakest = Math.min(weakest, getSequenceIndex(card, config));
        }
        return weakest == Integer.MAX_VALUE ? -1 : weakest;
    }

    private static boolean canSingleSemiShuaiByPublicInfo(List<Card> selectedCards, GameState state, RuleConfig config) {
        int weakestSequence = getWeakestSequence(selectedCards, config);
        return !hasHiddenStrongerSingles(weakestSequence, selectedCards, state, config);
    }

    private static boolean canPairSemiShuaiByPublicInfo(List<Card> selectedCards, GameState state, RuleConfig config) {
        ArrayList<PairUnit> pairUnits = buildPairUnits(selectedCards, getCommonGroup(selectedCards, config), config);
        int weakestPairSequence = getWeakestPairSequence(pairUnits);
        return !hasHiddenStrongerPairs(weakestPairSequence, selectedCards, state, config);
    }

    private static ArrayList<Card> reduceFailedSingleSemiShuai(List<Card> selectedCards, GameState state, RuleConfig config) {
        ArrayList<Card> copy = new ArrayList<>(selectedCards);
        sortByStrengthAscending(copy, config);
        int weakestSequence = copy.isEmpty() ? -1 : getSequenceIndex(copy.get(0), config);
        ArrayList<Card> reduced = new ArrayList<>();
        for (Card card : copy) {
            if (getSequenceIndex(card, config) == weakestSequence) {
                reduced.add(card);
            }
        }
        return reduced;
    }

    private static ArrayList<Card> reduceFailedPairSemiShuai(List<Card> selectedCards, GameState state, RuleConfig config) {
        ArrayList<PairUnit> pairUnits = buildPairUnits(selectedCards, getCommonGroup(selectedCards, config), config);
        if (pairUnits.isEmpty()) {
            return new ArrayList<>();
        }
        Collections.sort(pairUnits, new Comparator<PairUnit>() {
            @Override
            public int compare(PairUnit first, PairUnit second) {
                return first.sequence - second.sequence;
            }
        });
        return new ArrayList<>(pairUnits.get(0).cards);
    }

    private static boolean hasHiddenStrongerSingles(int weakestSequence, List<Card> selectedCards, GameState state, RuleConfig config) {
        int group = getCommonGroup(selectedCards, config);
        HashMap<String, Integer> publicCounts = state.buildPublicCardCountMap();
        HashMap<String, Integer> selectedCounts = buildIdentityCountMap(selectedCards);
        for (Card card : getAllPossibleCardsInGroup(group, config)) {
            if (getSequenceIndex(card, config) <= weakestSequence) {
                continue;
            }
            String key = buildCardKey(card);
            int knownCount = getCount(publicCounts, key) + getCount(selectedCounts, key);
            if (knownCount < getTotalCopiesForIdentity(card)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasHiddenStrongerPairs(int weakestPairSequence, List<Card> selectedCards, GameState state, RuleConfig config) {
        int group = getCommonGroup(selectedCards, config);
        HashMap<String, Integer> publicCounts = state.buildPublicCardCountMap();
        HashMap<String, Integer> selectedCounts = buildIdentityCountMap(selectedCards);
        for (Card card : getAllPossibleCardsInGroup(group, config)) {
            if (getSequenceIndex(card, config) <= weakestPairSequence) {
                continue;
            }
            String key = buildCardKey(card);
            int knownCount = getCount(publicCounts, key) + getCount(selectedCounts, key);
            if (knownCount < getTotalCopiesForIdentity(card)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPairOnlySemiShuai(List<Card> selectedCards, int group, RuleConfig config) {
        if (selectedCards.size() < 4 || selectedCards.size() % 2 != 0) {
            return false;
        }
        ArrayList<PairUnit> pairUnits = buildPairUnits(selectedCards, group, config);
        return !pairUnits.isEmpty() && pairUnits.size() * 2 == selectedCards.size();
    }

    private static HashMap<String, Integer> buildIdentityCountMap(List<Card> cards) {
        HashMap<String, Integer> counts = new HashMap<>();
        for (Card card : cards) {
            String key = buildCardKey(card);
            Integer count = counts.get(key);
            counts.put(key, count == null ? 1 : count + 1);
        }
        return counts;
    }

    private static int getCount(HashMap<String, Integer> counts, String key) {
        Integer value = counts.get(key);
        return value == null ? 0 : value;
    }

    private static String buildCardKey(Card card) {
        return card.getValue() + "_" + card.getColor().name();
    }

    private static int getTotalCopiesForIdentity(Card card) {
        return 2;
    }

    private static ArrayList<Card> getAllPossibleCardsInGroup(int group, RuleConfig config) {
        ArrayList<Card> cards = new ArrayList<>();
        CardColor[] colors = {
                CardColor.Spade, CardColor.Heart, CardColor.Club, CardColor.Diamond,
                CardColor.SmallJoker, CardColor.BigJoker
        };
        for (CardColor color : colors) {
            for (int value = 1; value <= 15; value++) {
                if ((value == 14 && color != CardColor.SmallJoker)
                        || (value == 15 && color != CardColor.BigJoker)
                        || (value <= 13 && (color == CardColor.SmallJoker || color == CardColor.BigJoker))) {
                    continue;
                }
                Card card = new Card();
                card.setValue(value);
                card.setColor(color);
                if (getGroup(card, config) == group) {
                    cards.add(card);
                }
            }
        }
        return cards;
    }

    private static int getWeakestPairSequence(List<PairUnit> pairUnits) {
        int weakest = Integer.MAX_VALUE;
        for (PairUnit pairUnit : pairUnits) {
            weakest = Math.min(weakest, pairUnit.sequence);
        }
        return weakest == Integer.MAX_VALUE ? -1 : weakest;
    }

    private static boolean isPrivilegedThreeCardShuai(List<Card> cards, RuleConfig config) {
        if (!config.isPublicInfoSemiShuaiEnabled() || !config.isSpecialThreeCardShuaiEnabled()) {
            return false;
        }
        if (cards == null || cards.size() != 3) {
            return false;
        }
        int group = getCommonGroup(cards, config);
        if (group == GROUP_MIXED) {
            return false;
        }
        int aceCount = 0;
        int kingCount = 0;
        for (Card card : cards) {
            if (card.getValue() == 1) {
                aceCount++;
            } else if (card.getValue() == 13) {
                kingCount++;
            } else {
                return false;
            }
        }
        return (aceCount == 2 && kingCount == 1) || (aceCount == 1 && kingCount == 2);
    }

    private static boolean isPrivilegedTopSplitShuai(List<Card> cards, List<Card> fullHand, RuleConfig config) {
        if (!config.isPublicInfoSemiShuaiEnabled() || !config.isSpecialTopSplitEnabled()) {
            return false;
        }
        if (cards == null || fullHand == null || cards.size() != 3) {
            return false;
        }
        int group = getCommonGroup(cards, config);
        if (group == GROUP_MIXED) {
            return false;
        }
        int aceCount = 0;
        int queenCount = 0;
        for (Card card : cards) {
            if (card.getValue() == 1) {
                aceCount++;
            } else if (card.getValue() == 12) {
                queenCount++;
            } else {
                return false;
            }
        }
        if (aceCount != 1 || queenCount != 2) {
            return false;
        }
        for (Card card : fullHand) {
            if (cards.contains(card)) {
                continue;
            }
            if (card.getValue() == 13 && getGroup(card, config) == group) {
                return true;
            }
        }
        return false;
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
                if (!areTractorPairsAdjacent(copy.get(i - 2), copy.get(i), previousSequence, currentSequence, config)) {
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

    private static int countPairUnits(List<Card> cards, int restrictedGroup, RuleConfig config) {
        return buildPairUnits(cards, restrictedGroup, config).size();
    }

    private static boolean areTractorPairsAdjacent(Card previousRepresentative, Card currentRepresentative,
                                                   int previousSequence, int currentSequence, RuleConfig config) {
        if (isSpecialCarryTractor(previousRepresentative, currentRepresentative, config)) {
            return config.isTractorCarryAceEnabled();
        }
        if (previousSequence - currentSequence == 1) {
            return true;
        }
        return false;
    }

    private static boolean isSpecialCarryTractor(Card first, Card second, RuleConfig config) {
        if (config.getRankValue() != 5) {
            return false;
        }
        if (first.getColor() != second.getColor()) {
            return false;
        }
        int high = Math.max(first.getValue(), second.getValue());
        int low = Math.min(first.getValue(), second.getValue());
        return high == 6 && low == 4;
    }

    private static int getDescendingRankIndex(int value, int rankValue) {
        int[] descending = {1, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
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
