package joker.john.com.joker.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import joker.john.com.joker.Card;
import joker.john.com.joker.CardColor;
import joker.john.com.joker.CardsUtility;

public class SimpleShengJiGame {
    public static final int PLAYER_SELF = 0;
    public static final int PLAYER_LEFT = 1;
    public static final int PLAYER_TOP = 2;
    public static final int PLAYER_RIGHT = 3;

    private static final int[] RANK_PATH = {5, 10, 13, 1, 2, 3, 4, 6, 7, 8, 9, 11, 12};

    private final AiPlayer aiPlayer = new AiPlayer();
    private final int[] teamLevels = new int[]{5, 5};
    private RuleConfig baseRuleConfig;
    private GameState state;
    private int revealCursor;
    private int preferredDealerPlayer = PLAYER_SELF;
    private int pendingNextDealerPlayer = PLAYER_SELF;
    private int pendingNextDealerTeam = -1;
    private String lastRoundSummary = "";
    private int revealPassCount;

    public SimpleShengJiGame(RuleConfig ruleConfig) {
        baseRuleConfig = ruleConfig.copy();
        teamLevels[0] = baseRuleConfig.getRankValue();
        teamLevels[1] = baseRuleConfig.getRankValue();
        startRound(false);
    }

    public void updateBaseRuleConfig(RuleConfig ruleConfig) {
        if (ruleConfig != null) {
            baseRuleConfig = ruleConfig.copy();
        }
    }

    public void startNextRound() {
        startRound(true);
    }

    private void startRound(boolean nextRound) {
        RuleConfig configSnapshot = baseRuleConfig.copy();
        if (nextRound) {
            configSnapshot.setRankValue(teamLevels[getPendingDealerTeamOrDefault()]);
        }

        GameState newState = new GameState(configSnapshot);
        if (state != null && nextRound) {
            newState.setRoundNumber(state.getRoundNumber() + 1);
        }
        newState.setCurrentRankValue(configSnapshot.getRankValue());
        newState.setPhase(GamePhase.REVEAL_TRUMP);

        ArrayList<Card> deck = CardsUtility.getSortedCardList();
        Collections.shuffle(deck, CardsUtility.rand);
        for (int i = 0; i < 25; i++) {
            for (int player = 0; player < 4; player++) {
                newState.getHands()[player].add(deck.remove(0));
            }
        }
        newState.getKitty().addAll(deck);

        for (int player = 0; player < 4; player++) {
            PlayValidator.sortForDisplay(newState.getHands()[player], configSnapshot);
        }

        newState.setTurnState(new TurnState(PLAYER_SELF, PLAYER_SELF));
        newState.setDealerPlayer(-1);
        newState.setDealerTeam(-1);

        if (nextRound) {
            preferredDealerPlayer = pendingNextDealerPlayer;
        }
        state = newState;
        revealPassCount = 0;
        revealCursor = resolveRevealCursor();
        updateRevealStatus();
    }

    public ArrayList<Card> getHumanHand() {
        return state.getHands()[PLAYER_SELF];
    }

    public int getHandCount(int player) {
        return state.getHands()[player].size();
    }

    public ArrayList<Card> getKitty() {
        return state.getKitty();
    }

    public List<PlayedHand> getCurrentTrick() {
        return state.getCurrentTrick();
    }

    public List<PlayedTrick> getCompletedTricks() {
        return state.getCompletedTricks();
    }

    public RuleConfig getRuleConfig() {
        return state.getRuleConfig();
    }

    public boolean isHumanTurn() {
        if (state.getPhase() == GamePhase.REVEAL_TRUMP) {
            return revealCursor == PLAYER_SELF && canPlayerReveal(PLAYER_SELF);
        }
        if (state.getPhase() == GamePhase.BURY_KITTY) {
            return state.getDealerPlayer() == PLAYER_SELF;
        }
        return state.getPhase() == GamePhase.PLAYING
                && !state.isRoundFinished()
                && state.getTurnState().getPendingWinner() < 0
                && state.getTurnState().getCurrentPlayer() == PLAYER_SELF;
    }

    public boolean isHumanDealerBuryPhase() {
        return state.getPhase() == GamePhase.BURY_KITTY && state.getDealerPlayer() == PLAYER_SELF;
    }

    public boolean hasPendingTrickResolution() {
        return state.getPhase() == GamePhase.PLAYING
                && state.getTurnState().getPendingWinner() >= 0
                && !state.isRoundFinished();
    }

    public boolean isRoundFinished() {
        return state.isRoundFinished();
    }

    public boolean shouldAutoStartNextRound() {
        return state.getPhase() == GamePhase.ROUND_END && state.isRoundFinished();
    }

    public String getStatusMessage() {
        return state.getStatusMessage();
    }

    public int getUsScore() {
        return state.getTeamScores()[0];
    }

    public int getThemScore() {
        return state.getTeamScores()[1];
    }

    public int getNonDealerScore() {
        if (state.getDealerTeam() < 0) {
            return 0;
        }
        return state.getTeamScores()[1 - state.getDealerTeam()];
    }

    public int getTrickNumber() {
        return state.getTrickNumber();
    }

    public int getRoundNumber() {
        return state.getRoundNumber();
    }

    public int getDealerPlayer() {
        return state.getDealerPlayer();
    }

    public int getDealerTeam() {
        return state.getDealerTeam();
    }

    public GamePhase getPhase() {
        return state.getPhase();
    }

    public String getTrumpLabel() {
        if (state.getTrumpRevealLabel().length() > 0) {
            return "主牌: " + state.getTrumpRevealLabel();
        }
        return "主牌: 待亮主";
    }

    public String getLevelSummary() {
        return "我方级牌 " + RuleConfig.formatCardValue(teamLevels[0]) + " / 对方级牌 " + RuleConfig.formatCardValue(teamLevels[1]);
    }

    public String getLastRoundSummary() {
        return lastRoundSummary;
    }

    public String getPrimaryActionText() {
        if (state.getPhase() == GamePhase.REVEAL_TRUMP) {
            return "亮主";
        }
        if (state.getPhase() == GamePhase.BURY_KITTY) {
            return "放底牌";
        }
        return "出牌";
    }

    public boolean canHumanUsePrimaryAction() {
        if (state.getPhase() == GamePhase.REVEAL_TRUMP) {
            return canPlayerReveal(PLAYER_SELF);
        }
        if (state.getPhase() == GamePhase.BURY_KITTY) {
            return state.getDealerPlayer() == PLAYER_SELF;
        }
        return isHumanTurn();
    }

    public MoveResult handleHumanPrimaryAction(List<Integer> selectedIndexes) {
        if (state.getPhase() == GamePhase.REVEAL_TRUMP) {
            return revealTrump(PLAYER_SELF);
        }
        if (state.getPhase() == GamePhase.BURY_KITTY) {
            return buryKitty(PLAYER_SELF, selectedIndexes);
        }
        return playHuman(selectedIndexes);
    }

    public MoveResult passHumanReveal() {
        if (state.getPhase() != GamePhase.REVEAL_TRUMP) {
            return MoveResult.fail("当前不是亮主阶段。");
        }
        if (revealCursor != PLAYER_SELF) {
            return MoveResult.fail("当前还没轮到你决定是否亮主。");
        }
        advanceRevealTurn(false);
        return continueRevealOrFinalize();
    }

    public MoveResult playHuman(List<Integer> selectedIndexes) {
        if (!isHumanTurn()) {
            return MoveResult.fail("还没轮到你出牌。");
        }
        if (selectedIndexes == null || selectedIndexes.isEmpty()) {
            return MoveResult.fail("请先点选要出的牌。");
        }

        ArrayList<Integer> normalizedIndexes = normalizeIndexes(selectedIndexes);
        ArrayList<Card> hand = state.getHands()[PLAYER_SELF];
        ArrayList<Card> selectedCards = new ArrayList<>();
        for (Integer index : normalizedIndexes) {
            if (index < 0 || index >= hand.size()) {
                return MoveResult.fail("选中的牌已经变化，请重新选择。");
            }
            selectedCards.add(hand.get(index));
        }

        MoveResult validation = validatePlay(hand, selectedCards);
        if (!validation.success) {
            return validation;
        }

        removeCardsByIndex(hand, normalizedIndexes);
        applyPlay(PLAYER_SELF, selectedCards, validation.pattern);
        return MoveResult.success(state.getStatusMessage(), validation.pattern);
    }

    public MoveResult playNextAiTurn() {
        if (state.getPhase() == GamePhase.REVEAL_TRUMP) {
            return handleAiReveal();
        }
        if (state.getPhase() == GamePhase.BURY_KITTY) {
            return handleAiBuryKitty();
        }
        if (state.isRoundFinished()) {
            return MoveResult.fail(state.getStatusMessage());
        }
        if (state.getTurnState().getPendingWinner() >= 0) {
            return MoveResult.fail(state.getStatusMessage());
        }
        int player = state.getTurnState().getCurrentPlayer();
        if (player == PLAYER_SELF) {
            return MoveResult.fail("等待玩家出牌。");
        }

        ArrayList<Card> selectedCards = aiPlayer.choosePlay(state, player);
        MoveResult validation = validatePlay(state.getHands()[player], selectedCards);
        if (!validation.success) {
            return validation;
        }

        removeCardsByReference(state.getHands()[player], selectedCards);
        applyPlay(player, selectedCards, validation.pattern);
        return MoveResult.success(getPlayerName(player) + "出牌。", validation.pattern);
    }

    public MoveSuggestion getSuggestedHumanMove() {
        if (state.getPhase() == GamePhase.BURY_KITTY) {
            if (state.getDealerPlayer() != PLAYER_SELF) {
                return MoveSuggestion.fail("当前不是你放底牌。");
            }
            ArrayList<Card> suggestedCards = pickLowestCards(state.getHands()[PLAYER_SELF], 8);
            return MoveSuggestion.success(mapCardsToIndexes(state.getHands()[PLAYER_SELF], suggestedCards), suggestedCards, "建议把这 8 张放到底牌。");
        }
        if (!isHumanTurn()) {
            return MoveSuggestion.fail("当前不是你的回合。");
        }
        if (!state.getRuleConfig().isHintEnabled()) {
            return MoveSuggestion.fail("当前规则未开启提示。");
        }

        ArrayList<Card> suggestedCards = aiPlayer.choosePlay(state, PLAYER_SELF);
        if (suggestedCards == null || suggestedCards.isEmpty()) {
            return MoveSuggestion.fail("当前没有可用提示。");
        }

        ArrayList<Integer> indexes = mapCardsToIndexes(state.getHands()[PLAYER_SELF], suggestedCards);
        state.getActionLog().add(new GameAction(GameAction.Type.HINT, PLAYER_SELF, state.getTrickNumber(), suggestedCards, "提示"));
        state.getEventLog().add(new GameEvent(GameEvent.Type.HINT_READY, PLAYER_SELF, "已生成提示"));
        return MoveSuggestion.success(indexes, suggestedCards, "已为你选中建议牌。");
    }

    public MoveSuggestion getForcedHumanMove() {
        if (state.getPhase() != GamePhase.PLAYING || !isHumanTurn()) {
            return MoveSuggestion.fail("当前还不能自动选牌。");
        }
        if (state.getCurrentTrick().isEmpty() || state.getTurnState().getLeadPattern() == null) {
            return MoveSuggestion.fail("当前是先手，不自动代选。");
        }

        PlayPattern leadPattern = state.getTurnState().getLeadPattern();
        ArrayList<Card> hand = state.getHands()[PLAYER_SELF];
        int leadGroup = leadPattern.getGroup();
        ArrayList<Card> sameGroupCards = PlayValidator.getCardsInGroup(hand, leadGroup, state.getRuleConfig());
        int requiredCount = leadPattern.getCardCount();

        ArrayList<Card> forcedCards = null;
        if (sameGroupCards.size() == requiredCount) {
            forcedCards = new ArrayList<>(sameGroupCards);
        } else if (sameGroupCards.isEmpty() && hand.size() == requiredCount) {
            forcedCards = new ArrayList<>(hand);
        } else if (sameGroupCards.size() < requiredCount && hand.size() == requiredCount) {
            forcedCards = new ArrayList<>(hand);
        }

        if (forcedCards == null) {
            return MoveSuggestion.fail("当前还有多种跟牌选择。");
        }

        ArrayList<Integer> indexes = mapCardsToIndexes(hand, forcedCards);
        if (indexes.size() != requiredCount) {
            return MoveSuggestion.fail("当前还有多种跟牌选择。");
        }
        return MoveSuggestion.success(indexes, forcedCards, "当前只有一种合法跟牌，已自动帮你选中。");
    }

    public void beginNextTrick() {
        if (!hasPendingTrickResolution()) {
            return;
        }
        TurnState turnState = state.getTurnState();
        state.getCurrentTrick().clear();
        turnState.setLeadPattern(null);
        turnState.setLeadPlayer(turnState.getPendingWinner());
        turnState.setCurrentPlayer(turnState.getPendingWinner());
        turnState.setPendingWinner(-1);
        turnState.setPendingPoints(0);
        state.setTrickNumber(state.getTrickNumber() + 1);
        state.setStatusMessage(getPlayerName(turnState.getCurrentPlayer()) + "先手。");
    }

    public String describeCards(List<Card> cards) {
        return PlayValidator.describeCards(cards, state.getRuleConfig());
    }

    private MoveResult handleAiReveal() {
        if (state.getPhase() != GamePhase.REVEAL_TRUMP) {
            return MoveResult.fail("当前不是亮主阶段。");
        }
        for (int guard = 0; guard < 12 && state.getPhase() == GamePhase.REVEAL_TRUMP; guard++) {
            if (state.getDealerPlayer() >= 0 && revealPassCount >= 3) {
                return finalizeRevealStage();
            }
            if (state.getDealerPlayer() < 0 && revealPassCount >= 4) {
                return fallbackRevealTrump();
            }

            int player = revealCursor;
            if (canPlayerReveal(player)) {
                revealTrump(player);
            } else {
                advanceRevealTurn(false);
                continueRevealOrFinalize();
            }
        }
        return state.getPhase() == GamePhase.REVEAL_TRUMP
                ? MoveResult.fail("亮主流程异常，已停止自动亮主。")
                : MoveResult.success(state.getStatusMessage(), null);
    }

    private MoveResult revealTrump(int player) {
        ArrayList<Card> revealCards = getRevealCards(state.getHands()[player], state.getCurrentRankValue());
        if (revealCards.isEmpty()) {
            return MoveResult.fail("当前没有可亮的级牌。");
        }
        Card revealCard = revealCards.get(0);
        RuleConfig config = state.getRuleConfig();
        config.setTrumpSuit(revealCard.getColor());
        PlayValidator.applyRuleConfigToUtility(config);

        state.setDealerPlayer(player);
        state.setDealerTeam(getTeamIndex(player));
        state.setTrumpRevealLabel(config.getTrumpSuitLabel() + " " + RuleConfig.formatCardValue(state.getCurrentRankValue()));
        state.getActionLog().add(new GameAction(GameAction.Type.DEAL, player, state.getTrickNumber(), revealCards, "亮主"));
        state.setStatusMessage(getPlayerName(player) + "亮主成功，当前主牌改为" + config.getTrumpSuitLabel() + "。");
        advanceRevealTurn(true);
        return continueRevealOrFinalize();
    }

    private MoveResult fallbackRevealTrump() {
        int fallbackPlayer = preferredDealerPlayer;
        RuleConfig config = state.getRuleConfig();
        config.setTrumpSuit(baseRuleConfig.getTrumpSuit());
        PlayValidator.applyRuleConfigToUtility(config);

        state.setDealerPlayer(fallbackPlayer);
        state.setDealerTeam(getTeamIndex(fallbackPlayer));
        state.setTrumpRevealLabel(config.getTrumpSuitLabel() + " " + RuleConfig.formatCardValue(state.getCurrentRankValue()) + "（默认）");
        state.setStatusMessage(getPlayerName(fallbackPlayer) + "无人亮主，按默认主花色坐庄并拿底牌。");
        state.getActionLog().add(new GameAction(GameAction.Type.DEAL, fallbackPlayer, state.getTrickNumber(), null, "默认坐庄"));
        return finalizeRevealStage();
    }

    private void advanceRevealTurn(boolean revealed) {
        revealCursor = (revealCursor + 1) % 4;
        revealPassCount = revealed ? 0 : revealPassCount + 1;
    }

    private MoveResult continueRevealOrFinalize() {
        if (state.getDealerPlayer() >= 0 && revealPassCount >= 3) {
            return finalizeRevealStage();
        }
        if (state.getDealerPlayer() < 0 && revealPassCount >= 4) {
            return fallbackRevealTrump();
        }
        updateRevealStatus();
        return MoveResult.success(state.getStatusMessage(), null);
    }

    private MoveResult finalizeRevealStage() {
        int dealerPlayer = state.getDealerPlayer();
        if (dealerPlayer < 0) {
            dealerPlayer = preferredDealerPlayer;
            state.setDealerPlayer(dealerPlayer);
            state.setDealerTeam(getTeamIndex(dealerPlayer));
        }
        state.getHands()[dealerPlayer].addAll(state.getKitty());
        PlayValidator.sortForDisplay(state.getHands()[dealerPlayer], state.getRuleConfig());
        state.getKitty().clear();
        state.setPhase(GamePhase.BURY_KITTY);
        state.setStatusMessage(getPlayerName(dealerPlayer) + "抢到庄并拿到底牌。");
        if (dealerPlayer == PLAYER_SELF) {
            return MoveResult.success("你抢到庄，主牌已更新，请选 8 张放底牌。", null);
        }
        return handleAiBuryKitty();
    }

    private MoveResult handleAiBuryKitty() {
        if (state.getPhase() != GamePhase.BURY_KITTY) {
            return MoveResult.fail("当前不是放底牌阶段。");
        }
        int dealerPlayer = state.getDealerPlayer();
        if (dealerPlayer == PLAYER_SELF) {
            return MoveResult.fail("等待你放底牌。");
        }
        ArrayList<Card> buryCards = pickLowestCards(state.getHands()[dealerPlayer], 8);
        removeCardsByReference(state.getHands()[dealerPlayer], buryCards);
        state.getKitty().addAll(buryCards);
        state.setPhase(GamePhase.PLAYING);
        state.setTurnState(new TurnState(dealerPlayer, dealerPlayer));
        state.setStatusMessage(getPlayerName(dealerPlayer) + "已经放好底牌，开始出牌。");
        return MoveResult.success(state.getStatusMessage(), null);
    }

    private MoveResult buryKitty(int player, List<Integer> selectedIndexes) {
        if (state.getPhase() != GamePhase.BURY_KITTY || player != state.getDealerPlayer()) {
            return MoveResult.fail("当前不是你放底牌。");
        }
        if (selectedIndexes == null || selectedIndexes.size() != 8) {
            return MoveResult.fail("请准确选择 8 张底牌。");
        }

        ArrayList<Integer> normalized = normalizeIndexes(selectedIndexes);
        if (normalized.size() != 8) {
            return MoveResult.fail("请准确选择 8 张不同的底牌。");
        }

        ArrayList<Card> hand = state.getHands()[player];
        ArrayList<Card> buryCards = new ArrayList<>();
        for (Integer index : normalized) {
            if (index < 0 || index >= hand.size()) {
                return MoveResult.fail("底牌选择已变化，请重新选择。");
            }
            buryCards.add(hand.get(index));
        }

        removeCardsByIndex(hand, normalized);
        state.getKitty().clear();
        state.getKitty().addAll(buryCards);
        PlayValidator.sortForDisplay(hand, state.getRuleConfig());
        state.setPhase(GamePhase.PLAYING);
        state.setTurnState(new TurnState(player, player));
        state.setStatusMessage("底牌已放好，你先出牌。");
        return MoveResult.success(state.getStatusMessage(), null);
    }

    private MoveResult validatePlay(List<Card> hand, List<Card> selectedCards) {
        RuleConfig config = state.getRuleConfig();
        TurnState turnState = state.getTurnState();
        PlayValidator.ValidationResult validation;
        if (state.getCurrentTrick().isEmpty() || turnState.getLeadPattern() == null) {
            validation = PlayValidator.validateLead(selectedCards, config);
        } else {
            validation = PlayValidator.validateFollow(hand, selectedCards, turnState.getLeadPattern(), config);
        }
        if (!validation.isSuccess()) {
            return MoveResult.fail(validation.getMessage());
        }
        return MoveResult.success("", validation.getPattern());
    }

    private void applyPlay(int player, List<Card> selectedCards, PlayPattern pattern) {
        RuleConfig config = state.getRuleConfig();
        ArrayList<Card> playedCards = new ArrayList<>(selectedCards);
        PlayValidator.sortByStrengthDescending(playedCards, config);

        TurnState turnState = state.getTurnState();
        if (state.getCurrentTrick().isEmpty()) {
            turnState.setLeadPlayer(player);
            turnState.setLeadPattern(pattern);
        }

        PlayedHand playedHand = new PlayedHand(player, playedCards, pattern);
        state.getCurrentTrick().add(playedHand);
        state.getActionLog().add(new GameAction(GameAction.Type.PLAY, player, state.getTrickNumber(), playedCards, "出牌"));
        state.getEventLog().add(new GameEvent(GameEvent.Type.PLAY_ACCEPTED, player, getPlayerName(player) + "出牌"));

        if (state.getCurrentTrick().size() == 4) {
            resolveCurrentTrick();
            return;
        }

        int nextPlayer = (player + 1) % 4;
        turnState.setCurrentPlayer(nextPlayer);
        if (nextPlayer == PLAYER_SELF) {
            state.setStatusMessage("轮到你跟牌。");
        } else {
            state.setStatusMessage(getPlayerName(nextPlayer) + "思考中。");
        }
    }

    private void resolveCurrentTrick() {
        PlayedHand winner = state.getCurrentTrick().get(0);
        PlayPattern leadPattern = state.getTurnState().getLeadPattern();
        for (int i = 1; i < state.getCurrentTrick().size(); i++) {
            PlayedHand candidate = state.getCurrentTrick().get(i);
            if (beats(candidate, winner, leadPattern)) {
                winner = candidate;
            }
        }

        int points = 0;
        for (PlayedHand playedHand : state.getCurrentTrick()) {
            for (Card card : playedHand.getCards()) {
                points += PlayValidator.getCardPoints(card);
            }
        }

        state.getCompletedTricks().add(new PlayedTrick(state.getTrickNumber(), state.getCurrentTrick(), winner.getPlayer(), points));
        state.getTeamScores()[getTeamIndex(winner.getPlayer())] += points;
        state.getTurnState().setPendingWinner(winner.getPlayer());
        state.getTurnState().setPendingPoints(points);
        state.getTurnState().setCurrentPlayer(winner.getPlayer());
        state.getActionLog().add(new GameAction(GameAction.Type.TRICK_END, winner.getPlayer(), state.getTrickNumber(), winner.getCards(), "赢墩"));
        state.getEventLog().add(new GameEvent(GameEvent.Type.TRICK_WON, winner.getPlayer(), getPlayerName(winner.getPlayer()) + "赢下一墩"));

        if (state.getHands()[PLAYER_SELF].isEmpty()) {
            finishRound();
        } else {
            state.setStatusMessage(getPlayerName(winner.getPlayer()) + "赢下第 " + state.getTrickNumber() + " 墩，收 " + points + " 分。");
        }
    }

    private void finishRound() {
        int dealerTeam = state.getDealerTeam();
        int defenderScore = state.getTeamScores()[1 - dealerTeam];
        int dealerDelta = 0;
        int defenderDelta = 0;
        boolean dealerKeepsSeat = defenderScore < 80;

        if (defenderScore == 0) {
            dealerDelta = 3;
        } else if (defenderScore < 40) {
            dealerDelta = 2;
        } else if (defenderScore < 80) {
            dealerDelta = 1;
        } else if (defenderScore < 120) {
            dealerKeepsSeat = false;
        } else if (defenderScore < 160) {
            defenderDelta = 2;
            dealerKeepsSeat = false;
        } else {
            defenderDelta = 3;
            dealerKeepsSeat = false;
        }

        if (!dealerKeepsSeat && (teamLevels[dealerTeam] == 10 || teamLevels[dealerTeam] == 13)) {
            teamLevels[dealerTeam] = 5;
        }
        if (dealerDelta > 0) {
            teamLevels[dealerTeam] = advanceRank(teamLevels[dealerTeam], dealerDelta);
        }
        if (defenderDelta > 0) {
            teamLevels[1 - dealerTeam] = advanceRank(teamLevels[1 - dealerTeam], defenderDelta);
        }

        pendingNextDealerTeam = dealerKeepsSeat ? dealerTeam : 1 - dealerTeam;
        pendingNextDealerPlayer = chooseNextDealerPlayer(dealerKeepsSeat);

        state.setRoundFinished(true);
        state.setPhase(GamePhase.ROUND_END);
        lastRoundSummary = buildRoundSummary(defenderScore, dealerDelta, defenderDelta, dealerKeepsSeat);
        state.setStatusMessage(lastRoundSummary);
        state.getActionLog().add(new GameAction(GameAction.Type.ROUND_END, state.getDealerPlayer(), state.getTrickNumber(), null, lastRoundSummary));
        state.getEventLog().add(new GameEvent(GameEvent.Type.ROUND_FINISHED, state.getDealerPlayer(), lastRoundSummary));
    }

    private String buildRoundSummary(int defenderScore, int dealerDelta, int defenderDelta, boolean dealerKeepsSeat) {
        StringBuilder builder = new StringBuilder();
        builder.append("本局结束，");
        builder.append(dealerKeepsSeat ? getTeamLabel(state.getDealerTeam()) : getTeamLabel(1 - state.getDealerTeam()));
        builder.append("获胜，");
        builder.append(dealerKeepsSeat ? "庄家方守住" : "庄家下台");
        builder.append("。闲家得分 ").append(defenderScore).append("。");
        if (dealerDelta > 0) {
            builder.append("庄家方升 ").append(dealerDelta).append(" 级。");
        }
        if (defenderDelta > 0) {
            builder.append("闲家方升 ").append(defenderDelta).append(" 级。");
        }
        builder.append("下一局级牌：我方 ").append(RuleConfig.formatCardValue(teamLevels[0]))
                .append(" / 对方 ").append(RuleConfig.formatCardValue(teamLevels[1])).append("。");
        builder.append(getPlayerName(pendingNextDealerPlayer)).append(" 方优先亮主。");
        return builder.toString();
    }

    private boolean beats(PlayedHand candidate, PlayedHand currentWinner, PlayPattern leadPattern) {
        if (!candidate.getPattern().canBeatLead()) {
            return false;
        }
        if (leadPattern.getType() != candidate.getPattern().getType()) {
            return false;
        }

        int candidateGroup = candidate.getPattern().getGroup();
        int winnerGroup = currentWinner.getPattern().getGroup();
        int leadGroup = leadPattern.getGroup();

        if (leadGroup == PlayValidator.GROUP_TRUMP) {
            if (candidateGroup != PlayValidator.GROUP_TRUMP) {
                return false;
            }
            if (winnerGroup != PlayValidator.GROUP_TRUMP) {
                return true;
            }
            return candidate.getPattern().getTopSequence() > currentWinner.getPattern().getTopSequence();
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
        return candidate.getPattern().getTopSequence() > currentWinner.getPattern().getTopSequence();
    }

    private int advanceRank(int current, int steps) {
        int index = 0;
        for (int i = 0; i < RANK_PATH.length; i++) {
            if (RANK_PATH[i] == current) {
                index = i;
                break;
            }
        }
        int nextIndex = Math.min(RANK_PATH.length - 1, index + steps);
        return RANK_PATH[nextIndex];
    }

    private int chooseNextDealerPlayer(boolean dealerKeepsSeat) {
        if (dealerKeepsSeat) {
            return state.getDealerPlayer();
        }
        return state.getDealerTeam() == 0 ? PLAYER_LEFT : PLAYER_SELF;
    }

    private int resolveRevealCursor() {
        ArrayList<Integer> revealOrder = getRevealOrder();
        for (Integer player : revealOrder) {
            if (canPlayerReveal(player)) {
                return player;
            }
        }
        return preferredDealerPlayer;
    }

    private ArrayList<Integer> getRevealOrder() {
        ArrayList<Integer> order = new ArrayList<>();
        if (pendingNextDealerTeam < 0) {
            order.add(PLAYER_SELF);
            order.add(PLAYER_LEFT);
            order.add(PLAYER_TOP);
            order.add(PLAYER_RIGHT);
            return order;
        }

        int preferredTeam = getTeamIndex(preferredDealerPlayer);
        addTeamRevealOrder(order, preferredTeam, preferredDealerPlayer);
        addTeamRevealOrder(order, 1 - preferredTeam, (preferredDealerPlayer + 1) % 4);
        return order;
    }

    private void addTeamRevealOrder(ArrayList<Integer> order, int team, int preferredPlayer) {
        if (getTeamIndex(preferredPlayer) == team && order.indexOf(preferredPlayer) < 0) {
            order.add(preferredPlayer);
        }
        for (int player = 0; player < 4; player++) {
            if (getTeamIndex(player) == team && order.indexOf(player) < 0) {
                order.add(player);
            }
        }
    }

    private ArrayList<Card> getRevealCards(List<Card> hand, int rankValue) {
        Card bigJoker = null;
        Card smallJoker = null;
        ArrayList<Card> blackRankCards = new ArrayList<>();
        ArrayList<Card> redRankCards = new ArrayList<>();

        for (Card card : hand) {
            if (card.getValue() == 15) {
                bigJoker = card;
            } else if (card.getValue() == 14) {
                smallJoker = card;
            } else if (card.getValue() == rankValue) {
                if (card.getColor() == CardColor.Heart || card.getColor() == CardColor.Diamond) {
                    redRankCards.add(card);
                } else if (card.getColor() == CardColor.Spade || card.getColor() == CardColor.Club) {
                    blackRankCards.add(card);
                }
            }
        }

        ArrayList<Card> result = new ArrayList<>();
        if (bigJoker != null && !redRankCards.isEmpty()) {
            result.add(bigJoker);
            result.add(redRankCards.get(0));
            return result;
        }
        if (smallJoker != null && !blackRankCards.isEmpty()) {
            result.add(smallJoker);
            result.add(blackRankCards.get(0));
        }
        return result;
    }

    private boolean canPlayerReveal(int player) {
        return !getRevealCards(state.getHands()[player], state.getCurrentRankValue()).isEmpty();
    }

    private void updateRevealStatus() {
        if (state.getDealerPlayer() >= 0) {
            state.setStatusMessage("当前已亮 " + state.getTrumpRevealLabel() + "，系统正在自动判定是否有人继续抢亮。");
            return;
        }
        ArrayList<Integer> revealOrder = getRevealOrder();
        for (Integer player : revealOrder) {
            if (!canPlayerReveal(player)) {
                continue;
            }
            revealCursor = player;
            state.setStatusMessage("系统正在自动判定亮主。当前级牌 " + RuleConfig.formatCardValue(state.getCurrentRankValue()) + "。");
            return;
        }
        state.setStatusMessage("当前无人可亮主，将按默认主花色兜底。");
    }

    private ArrayList<Card> pickLowestCards(List<Card> cards, int count) {
        ArrayList<Card> copy = new ArrayList<>(cards);
        Collections.sort(copy, new java.util.Comparator<Card>() {
            @Override
            public int compare(Card first, Card second) {
                return PlayValidator.getSequenceIndex(first, state.getRuleConfig()) - PlayValidator.getSequenceIndex(second, state.getRuleConfig());
            }
        });
        ArrayList<Card> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, copy.size()); i++) {
            result.add(copy.get(i));
        }
        return result;
    }

    private ArrayList<Integer> normalizeIndexes(List<Integer> selectedIndexes) {
        ArrayList<Integer> normalized = new ArrayList<>();
        for (Integer index : selectedIndexes) {
            if (index != null && normalized.indexOf(index) < 0) {
                normalized.add(index);
            }
        }
        Collections.sort(normalized);
        return normalized;
    }

    private ArrayList<Integer> mapCardsToIndexes(List<Card> hand, List<Card> cards) {
        ArrayList<Integer> indexes = new ArrayList<>();
        ArrayList<Card> copy = new ArrayList<>(cards);
        for (Card card : copy) {
            for (int i = 0; i < hand.size(); i++) {
                if (hand.get(i) == card && indexes.indexOf(i) < 0) {
                    indexes.add(i);
                    break;
                }
            }
        }
        Collections.sort(indexes);
        return indexes;
    }

    private void removeCardsByReference(ArrayList<Card> hand, List<Card> selectedCards) {
        for (Card card : selectedCards) {
            hand.remove(card);
        }
    }

    private void removeCardsByIndex(ArrayList<Card> hand, List<Integer> normalizedIndexes) {
        for (int i = normalizedIndexes.size() - 1; i >= 0; i--) {
            hand.remove((int) normalizedIndexes.get(i));
        }
    }

    private int getPendingDealerTeamOrDefault() {
        if (pendingNextDealerTeam >= 0) {
            return pendingNextDealerTeam;
        }
        return 0;
    }

    private int getTeamIndex(int player) {
        return player % 2 == 0 ? 0 : 1;
    }

    public String getPlayerName(int player) {
        switch (player) {
            case PLAYER_LEFT:
                return "左家";
            case PLAYER_TOP:
                return "对家";
            case PLAYER_RIGHT:
                return "右家";
            default:
                return "你";
        }
    }

    private String getTeamLabel(int teamIndex) {
        return teamIndex == 0 ? "我方" : "对方";
    }

    public static class MoveResult {
        public final boolean success;
        public final String message;
        public final PlayPattern pattern;

        private MoveResult(boolean success, String message, PlayPattern pattern) {
            this.success = success;
            this.message = message;
            this.pattern = pattern;
        }

        public static MoveResult success(String message, PlayPattern pattern) {
            return new MoveResult(true, message, pattern);
        }

        public static MoveResult fail(String message) {
            return new MoveResult(false, message, null);
        }
    }

    public static class MoveSuggestion {
        public final boolean success;
        public final String message;
        public final ArrayList<Integer> indexes;
        public final ArrayList<Card> cards;

        private MoveSuggestion(boolean success, String message, ArrayList<Integer> indexes, ArrayList<Card> cards) {
            this.success = success;
            this.message = message;
            this.indexes = indexes;
            this.cards = cards;
        }

        public static MoveSuggestion success(ArrayList<Integer> indexes, ArrayList<Card> cards, String message) {
            return new MoveSuggestion(true, message, indexes, cards);
        }

        public static MoveSuggestion fail(String message) {
            return new MoveSuggestion(false, message, new ArrayList<Integer>(), new ArrayList<Card>());
        }
    }
}
