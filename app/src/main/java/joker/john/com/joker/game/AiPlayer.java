package joker.john.com.joker.game;

import java.util.ArrayList;
import java.util.List;

import joker.john.com.joker.Card;

/**
 * 电脑出牌决策。
 * 创建时间：2026-06-02
 * 最近修改：2026-06-02
 * by john
 */
public class AiPlayer {
    /**
     * AI 出牌主入口，只使用公开信息和当前牌桌状态做选择。
     */
    public ArrayList<Card> choosePlay(GameState state, int player) {
        ArrayList<Card> hand = state.getHands()[player];
        TurnState turnState = state.getTurnState();
        if (state.getCurrentTrick().isEmpty() || turnState.getLeadPattern() == null) {
            return chooseLead(hand, state, player);
        }
        return chooseFollow(hand, state, player);
    }

    private ArrayList<Card> chooseLead(ArrayList<Card> hand, GameState state, int player) {
        ArrayList<ArrayList<Card>> candidates = buildLeadCandidates(hand, state);
        if (candidates.isEmpty()) {
            return PlayValidator.suggestLead(hand, state.getRuleConfig());
        }

        ArrayList<Card> best = candidates.get(0);
        int bestScore = Integer.MIN_VALUE;
        for (ArrayList<Card> candidate : candidates) {
            int score = evaluateLeadCandidate(candidate, hand, state, player);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private ArrayList<Card> chooseFollow(ArrayList<Card> hand, GameState state, int player) {
        PlayPattern leadPattern = state.getTurnState().getLeadPattern();
        ArrayList<Card> fallback = PlayValidator.suggestFollow(hand, leadPattern, state.getRuleConfig());
        if (leadPattern == null) {
            return fallback;
        }

        ArrayList<ArrayList<Card>> candidates = buildFollowCandidates(hand, leadPattern, state.getRuleConfig());
        if (candidates.isEmpty()) {
            return fallback;
        }

        PlayedHand currentWinner = resolveCurrentWinner(state);
        if (currentWinner == null) {
            return fallback;
        }

        ArrayList<Card> best = candidates.get(0);
        int bestScore = Integer.MIN_VALUE;
        for (ArrayList<Card> candidate : candidates) {
            PlayPattern pattern = PlayValidator.analyzePattern(candidate, state.getRuleConfig());
            if (pattern.getType() == PlayPattern.Type.INVALID) {
                continue;
            }
            int score = evaluateFollowCandidate(candidate, pattern, hand, state, player, currentWinner, leadPattern);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private ArrayList<ArrayList<Card>> buildLeadCandidates(ArrayList<Card> hand, GameState state) {
        RuleConfig config = state.getRuleConfig();
        ArrayList<ArrayList<Card>> candidates = new ArrayList<>();
        for (Card card : hand) {
            ArrayList<Card> single = new ArrayList<>();
            single.add(card);
            if (PlayValidator.validateLead(single, config).isSuccess()) {
                candidates.add(single);
            }
        }

        for (int i = 0; i < hand.size(); i++) {
            for (int j = i + 1; j < hand.size(); j++) {
                Card first = hand.get(i);
                Card second = hand.get(j);
                if (first.getValue() != second.getValue() || first.getColor() != second.getColor()) {
                    continue;
                }
                ArrayList<Card> pair = new ArrayList<>();
                pair.add(first);
                pair.add(second);
                if (PlayValidator.validateLead(pair, config).isSuccess()) {
                    candidates.add(pair);
                }
            }
        }

        for (int size = 4; size <= 6; size += 2) {
            ArrayList<Card> tractor = PlayValidator.findLowestTractor(hand, size, config);
            if (tractor != null && PlayValidator.validateLead(tractor, config).isSuccess()) {
                candidates.add(tractor);
            }
        }

        ArrayList<Card> semiShuai = PlayValidator.buildPublicInfoSemiShuaiCandidates(hand, state);
        if (!semiShuai.isEmpty()) {
            candidates.add(semiShuai);
        }

        return candidates;
    }

    private ArrayList<ArrayList<Card>> buildFollowCandidates(ArrayList<Card> hand, PlayPattern leadPattern, RuleConfig config) {
        ArrayList<ArrayList<Card>> candidates = new ArrayList<>();
        if (leadPattern.getType() == PlayPattern.Type.SINGLE) {
            for (Card card : hand) {
                ArrayList<Card> single = new ArrayList<>();
                single.add(card);
                if (PlayValidator.validateFollow(hand, single, leadPattern, config).isSuccess()) {
                    candidates.add(single);
                }
            }
            return candidates;
        }

        if (leadPattern.getType() == PlayPattern.Type.PAIR) {
            for (int i = 0; i < hand.size(); i++) {
                for (int j = i + 1; j < hand.size(); j++) {
                    Card first = hand.get(i);
                    Card second = hand.get(j);
                    if (first.getValue() != second.getValue() || first.getColor() != second.getColor()) {
                        continue;
                    }
                    ArrayList<Card> pair = new ArrayList<>();
                    pair.add(first);
                    pair.add(second);
                    if (PlayValidator.validateFollow(hand, pair, leadPattern, config).isSuccess()) {
                        candidates.add(pair);
                    }
                }
            }
            return candidates;
        }

        ArrayList<Card> fallback = PlayValidator.suggestFollow(hand, leadPattern, config);
        if (fallback != null && !fallback.isEmpty()) {
            candidates.add(fallback);
        }
        return candidates;
    }

    private int evaluateLeadCandidate(ArrayList<Card> candidate, ArrayList<Card> fullHand, GameState state, int player) {
        RuleConfig config = state.getRuleConfig();
        PlayPattern pattern = PlayValidator.analyzePattern(candidate, config);
        int team = getTeamIndex(player);
        int dealerTeam = state.getDealerTeam();
        boolean defender = dealerTeam >= 0 && team != dealerTeam;
        int handSize = fullHand.size();
        int trickPoints = getCardPoints(candidate);
        int topSequence = pattern.getTopSequence();
        boolean earlyStage = handSize >= 18;
        boolean lateStage = handSize <= 8;
        boolean openingLead = state.getTrickNumber() <= 2;
        boolean dealerSide = dealerTeam >= 0 && team == dealerTeam;
        int score = 0;

        score -= getCardPoints(candidate) * (defender ? 35 : 110);
        score -= getTrumpCount(candidate, config) * (defender ? 10 : 70);
        score -= topSequence / (defender ? 8 : 4);
        score -= getPatternWeight(pattern) * 30;
        score += getVoidSuitBonus(candidate, fullHand, config, defender);
        score += getLowSuitDumpBonus(candidate, fullHand, config, defender);
        score += getPatternLeadBias(pattern, earlyStage, lateStage, defender);
        score += getOpeningConserveBias(candidate, pattern, config, earlyStage, defender);
        score += getBottomPreparationBias(candidate, fullHand, config, dealerSide, lateStage);
        score += getOpeningLeadShapeBias(candidate, pattern, config, openingLead, defender, dealerSide);

        if (defender) {
            if (canLikelyCashPoints(candidate, pattern, config)) {
                score += trickPoints * 160;
                score += getPatternWeight(pattern) * 40;
                score += topSequence / 3;
            }
            if (handSize <= 10 && isStrongControlCandidate(candidate, pattern, config)) {
                score += 160;
            }
        } else {
            if (handSize <= 10) {
                score -= trickPoints * 120;
                if (isWeakSafeDump(candidate, pattern, config)) {
                    score += 180;
                }
            } else {
                score += getVoidSuitBonus(candidate, fullHand, config, false);
            }
        }

        if (pattern.getType() == PlayPattern.Type.SINGLE && isVeryHighCard(candidate.get(0), config)) {
            score += defender ? 180 : 120;
        }
        if (pattern.getType() == PlayPattern.Type.PAIR && handSize <= 12) {
            score += defender ? 60 : -80;
        }
        if (pattern.getType() == PlayPattern.Type.SINGLE && earlyStage && isHonorNonTrump(candidate.get(0), config)) {
            score -= defender ? 180 : 240;
        }
        if (pattern.getType() == PlayPattern.Type.SINGLE && earlyStage && trickPoints > 0) {
            score -= defender ? 140 : 260;
        }
        if (pattern.getType() == PlayPattern.Type.SINGLE && openingLead && isVeryLowNonTrump(candidate.get(0), config)) {
            score -= dealerSide ? 520 : 360;
        }

        return score;
    }

    private int evaluateFollowCandidate(ArrayList<Card> candidate, PlayPattern pattern, ArrayList<Card> fullHand, GameState state,
                                        int player, PlayedHand currentWinner, PlayPattern leadPattern) {
        RuleConfig config = state.getRuleConfig();
        int team = getTeamIndex(player);
        int dealerTeam = state.getDealerTeam();
        boolean defender = dealerTeam >= 0 && team != dealerTeam;
        boolean teammateWinning = getTeamIndex(currentWinner.getPlayer()) == team;
        boolean candidateWins = beats(candidate, pattern, currentWinner, leadPattern, config);
        int trickPoints = getTrickPoints(state.getCurrentTrick());
        int remainingHandAfterPlay = Math.max(0, fullHand.size() - candidate.size());
        boolean lateStage = remainingHandAfterPlay <= 8;
        boolean dealerSide = dealerTeam >= 0 && team == dealerTeam;
        int score = 0;

        score -= getCardPoints(candidate) * (defender ? 60 : 150);
        score -= getTrumpCount(candidate, config) * (defender ? 20 : 90);
        score -= pattern.getTopSequence() / 5;
        score += getVoidSuitBonus(candidate, fullHand, config, defender);
        score += getLowSuitDumpBonus(candidate, fullHand, config, defender);
        score += getBottomPreparationBias(candidate, fullHand, config, dealerSide, lateStage);

        if (teammateWinning) {
            score += candidateWins ? -500 : 180;
            score -= getCardPoints(candidate) * 60;
        } else if (candidateWins) {
            score += evaluateWinningOpportunity(candidate, state, player, trickPoints, remainingHandAfterPlay);
        } else {
            score += evaluateLosingDump(candidate, state, player, trickPoints, remainingHandAfterPlay);
        }

        if (!defender && remainingHandAfterPlay <= 8) {
            score -= getCardPoints(candidate) * 120;
            if (candidateWins) {
                score += trickPoints * 35;
            }
        }
        if (defender && remainingHandAfterPlay <= 8 && candidateWins) {
            score += 120;
        }
        if (defender && !candidateWins && trickPoints > 0 && getCardPoints(candidate) == 0) {
            score += 90;
        }
        if (!defender && !candidateWins && lateStage && getCardPoints(candidate) > 0) {
            score -= 180;
        }
        return score;
    }

    private int evaluateWinningOpportunity(List<Card> candidate, GameState state, int player, int trickPoints, int remainingHandAfterPlay) {
        boolean defender = state.getDealerTeam() >= 0 && getTeamIndex(player) != state.getDealerTeam();
        int score = 220;
        score -= getCardPoints(candidate) * (defender ? 10 : 40);
        score -= getTrumpCount(candidate, state.getRuleConfig()) * (defender ? 15 : 60);
        score += trickPoints * (defender ? 90 : 65);
        if (!defender && remainingHandAfterPlay <= 8) {
            score += 110;
        }
        if (defender && getCardPoints(candidate) > 0) {
            score += 80;
        }
        return score;
    }

    private int evaluateLosingDump(List<Card> candidate, GameState state, int player, int trickPoints, int remainingHandAfterPlay) {
        boolean defender = state.getDealerTeam() >= 0 && getTeamIndex(player) != state.getDealerTeam();
        int score = 60;
        score -= getCardPoints(candidate) * (defender ? 50 : 120);
        score -= getTrumpCount(candidate, state.getRuleConfig()) * (defender ? 10 : 80);
        if (!defender && remainingHandAfterPlay <= 8) {
            score -= trickPoints * 55;
        }
        if (defender && trickPoints > 0) {
            score -= 80;
        }
        return score;
    }

    private int getVoidSuitBonus(List<Card> candidate, ArrayList<Card> fullHand, RuleConfig config, boolean defender) {
        if (candidate.isEmpty()) {
            return 0;
        }
        int group = PlayValidator.getGroup(candidate.get(0), config);
        if (group == PlayValidator.GROUP_TRUMP) {
            return defender ? -10 : -30;
        }

        int beforeCount = countGroup(fullHand, group, config);
        int afterCount = beforeCount - candidate.size();
        if (afterCount == 0) {
            return defender ? 70 : 160;
        }
        if (beforeCount <= 3) {
            return defender ? 25 : 70;
        }
        return 0;
    }

    private int getLowSuitDumpBonus(List<Card> candidate, ArrayList<Card> fullHand, RuleConfig config, boolean defender) {
        if (candidate.isEmpty()) {
            return 0;
        }
        int group = PlayValidator.getGroup(candidate.get(0), config);
        if (group == PlayValidator.GROUP_TRUMP) {
            return 0;
        }
        int beforeCount = countGroup(fullHand, group, config);
        if (beforeCount > 4) {
            return 0;
        }
        int bonus = 0;
        for (Card card : candidate) {
            if (!isVeryHighCard(card, config) && PlayValidator.getCardPoints(card) == 0) {
                bonus += defender ? 20 : 45;
            }
        }
        return bonus;
    }

    private boolean canLikelyCashPoints(List<Card> candidate, PlayPattern pattern, RuleConfig config) {
        if (getCardPoints(candidate) <= 0) {
            return false;
        }
        if (pattern.getType() == PlayPattern.Type.PAIR || pattern.getType() == PlayPattern.Type.TRACTOR) {
            return true;
        }
        return !candidate.isEmpty() && (PlayValidator.isTrump(candidate.get(0), config) || isVeryHighCard(candidate.get(0), config));
    }

    private boolean isStrongControlCandidate(List<Card> candidate, PlayPattern pattern, RuleConfig config) {
        if (pattern.getType() == PlayPattern.Type.TRACTOR) {
            return true;
        }
        if (pattern.getType() == PlayPattern.Type.PAIR && !candidate.isEmpty()) {
            return PlayValidator.isTrump(candidate.get(0), config) || isVeryHighCard(candidate.get(0), config);
        }
        return !candidate.isEmpty() && (PlayValidator.isTrump(candidate.get(0), config) || isVeryHighCard(candidate.get(0), config));
    }

    private boolean isWeakSafeDump(List<Card> candidate, PlayPattern pattern, RuleConfig config) {
        if (pattern.getType() == PlayPattern.Type.TRACTOR) {
            return false;
        }
        for (Card card : candidate) {
            if (PlayValidator.getCardPoints(card) > 0 || PlayValidator.isTrump(card, config) || isVeryHighCard(card, config)) {
                return false;
            }
        }
        return true;
    }

    private boolean isVeryHighCard(Card card, RuleConfig config) {
        int value = card.getValue();
        return value == 1 || value == 15 || value == 14
                || (value == config.getRankValue() && PlayValidator.isTrump(card, config));
    }

    private boolean isHonorNonTrump(Card card, RuleConfig config) {
        return !PlayValidator.isTrump(card, config)
                && (card.getValue() == 1 || card.getValue() == 13 || card.getValue() == 12 || card.getValue() == 11);
    }

    private boolean isVeryLowNonTrump(Card card, RuleConfig config) {
        return !PlayValidator.isTrump(card, config) && card.getValue() >= 4 && card.getValue() <= 9;
    }

    private int getPatternLeadBias(PlayPattern pattern, boolean earlyStage, boolean lateStage, boolean defender) {
        if (earlyStage) {
            if (pattern.getType() == PlayPattern.Type.TRACTOR) {
                return defender ? 220 : 120;
            }
            if (pattern.getType() == PlayPattern.Type.PAIR) {
                return defender ? 120 : 50;
            }
            if (pattern.getType() == PlayPattern.Type.SINGLE) {
                return defender ? -80 : 0;
            }
        }
        if (lateStage && pattern.getType() == PlayPattern.Type.SINGLE) {
            return defender ? 30 : 70;
        }
        return 0;
    }

    private int getOpeningConserveBias(List<Card> candidate, PlayPattern pattern, RuleConfig config,
                                       boolean earlyStage, boolean defender) {
        if (!earlyStage) {
            return 0;
        }
        int score = 0;
        for (Card card : candidate) {
            if (PlayValidator.getCardPoints(card) > 0) {
                score -= defender ? 120 : 210;
            }
            if (isHonorNonTrump(card, config)) {
                score -= defender ? 80 : 140;
            }
            if (PlayValidator.isTrump(card, config)) {
                score -= defender ? 40 : 120;
            }
        }
        if (pattern.getType() == PlayPattern.Type.PAIR || pattern.getType() == PlayPattern.Type.TRACTOR) {
            score += defender ? 80 : 30;
        }
        return score;
    }

    private int getOpeningLeadShapeBias(List<Card> candidate, PlayPattern pattern, RuleConfig config,
                                        boolean openingLead, boolean defender, boolean dealerSide) {
        if (!openingLead) {
            return 0;
        }
        int score = 0;
        if (pattern.getType() == PlayPattern.Type.TRACTOR) {
            score += defender ? 320 : 240;
        } else if (pattern.getType() == PlayPattern.Type.PAIR) {
            score += defender ? 180 : 120;
        } else if (pattern.getType() == PlayPattern.Type.SINGLE && !candidate.isEmpty()) {
            Card card = candidate.get(0);
            if (isVeryLowNonTrump(card, config)) {
                score -= dealerSide ? 560 : 380;
            }
            if (isHonorNonTrump(card, config)) {
                score -= dealerSide ? 220 : 120;
            }
            if (PlayValidator.isTrump(card, config) || isVeryHighCard(card, config)) {
                score += defender ? 220 : 160;
            }
        }
        return score;
    }

    private int getBottomPreparationBias(List<Card> candidate, ArrayList<Card> fullHand, RuleConfig config,
                                         boolean dealerSide, boolean lateStage) {
        int score = 0;
        if (dealerSide) {
            if (lateStage) {
                score -= getCardPoints(candidate) * 90;
                score -= getTrumpCount(candidate, config) * 45;
                if (createsVoidSuit(candidate, fullHand, config)) {
                    score += 70;
                }
            }
            return score;
        }
        if (lateStage) {
            PlayPattern pattern = PlayValidator.analyzePattern(candidate, config);
            if (isStrongControlCandidate(candidate, pattern, config)) {
                score += 120;
            }
            if (createsVoidSuit(candidate, fullHand, config)) {
                score += 90;
            }
        }
        return score;
    }

    private boolean createsVoidSuit(List<Card> candidate, ArrayList<Card> fullHand, RuleConfig config) {
        if (candidate.isEmpty()) {
            return false;
        }
        int group = PlayValidator.getGroup(candidate.get(0), config);
        if (group == PlayValidator.GROUP_TRUMP) {
            return false;
        }
        return countGroup(fullHand, group, config) == candidate.size();
    }

    private int getPatternWeight(PlayPattern pattern) {
        if (pattern.getType() == PlayPattern.Type.TRACTOR) {
            return pattern.getCardCount() + 4;
        }
        if (pattern.getType() == PlayPattern.Type.PAIR) {
            return 4;
        }
        if (pattern.getType() == PlayPattern.Type.SHUAI) {
            return pattern.getCardCount() + 2;
        }
        return 1;
    }

    private int countGroup(ArrayList<Card> cards, int group, RuleConfig config) {
        int count = 0;
        for (Card card : cards) {
            if (PlayValidator.getGroup(card, config) == group) {
                count++;
            }
        }
        return count;
    }

    private PlayedHand resolveCurrentWinner(GameState state) {
        List<PlayedHand> trick = state.getCurrentTrick();
        if (trick.isEmpty()) {
            return null;
        }
        PlayPattern leadPattern = state.getTurnState().getLeadPattern();
        PlayedHand winner = trick.get(0);
        for (int i = 1; i < trick.size(); i++) {
            PlayedHand candidate = trick.get(i);
            if (beats(candidate.getCards(), candidate.getPattern(), winner, leadPattern, state.getRuleConfig())) {
                winner = candidate;
            }
        }
        return winner;
    }

    private boolean beats(List<Card> candidateCards, PlayPattern candidatePattern, PlayedHand currentWinner,
                          PlayPattern leadPattern, RuleConfig config) {
        if (candidatePattern == null || currentWinner == null || leadPattern == null) {
            return false;
        }
        if (!candidatePattern.canBeatLead() || leadPattern.getType() != candidatePattern.getType()) {
            return false;
        }
        if (leadPattern.getType() == PlayPattern.Type.SHUAI) {
            boolean candidateAllTrump = PlayValidator.isAllTrump(candidateCards, config);
            boolean winnerAllTrump = PlayValidator.isAllTrump(currentWinner.getCards(), config);
            if (leadPattern.getGroup() == PlayValidator.GROUP_TRUMP) {
                if (!candidateAllTrump) {
                    return false;
                }
                if (!winnerAllTrump) {
                    return true;
                }
                return candidatePattern.getTopSequence() > currentWinner.getPattern().getTopSequence();
            }
            if (candidateAllTrump && !winnerAllTrump) {
                return true;
            }
            if (!candidateAllTrump) {
                return false;
            }
            if (!winnerAllTrump) {
                return true;
            }
            return candidatePattern.getTopSequence() > currentWinner.getPattern().getTopSequence();
        }

        int candidateGroup = candidatePattern.getGroup();
        int winnerGroup = currentWinner.getPattern().getGroup();
        int leadGroup = leadPattern.getGroup();
        if (leadGroup == PlayValidator.GROUP_TRUMP) {
            if (candidateGroup != PlayValidator.GROUP_TRUMP) {
                return false;
            }
            if (winnerGroup != PlayValidator.GROUP_TRUMP) {
                return true;
            }
            return candidatePattern.getTopSequence() > currentWinner.getPattern().getTopSequence();
        }
        if (candidateGroup == PlayValidator.GROUP_TRUMP && winnerGroup != PlayValidator.GROUP_TRUMP) {
            return true;
        }
        if (winnerGroup == PlayValidator.GROUP_TRUMP && candidateGroup != PlayValidator.GROUP_TRUMP) {
            return false;
        }
        if (candidateGroup != leadGroup) {
            return false;
        }
        if (winnerGroup != leadGroup) {
            return true;
        }
        return candidatePattern.getTopSequence() > currentWinner.getPattern().getTopSequence();
    }

    private int getCardPoints(List<Card> cards) {
        int points = 0;
        for (Card card : cards) {
            points += PlayValidator.getCardPoints(card);
        }
        return points;
    }

    private int getTrumpCount(List<Card> cards, RuleConfig config) {
        int count = 0;
        for (Card card : cards) {
            if (PlayValidator.isTrump(card, config)) {
                count++;
            }
        }
        return count;
    }

    private int getTrickPoints(List<PlayedHand> trick) {
        int points = 0;
        for (PlayedHand playedHand : trick) {
            points += getCardPoints(playedHand.getCards());
        }
        return points;
    }

    private int getTeamIndex(int player) {
        return player % 2 == 0 ? 0 : 1;
    }
}
