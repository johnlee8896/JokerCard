package joker.john.com.joker.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import joker.john.com.joker.Card;
import joker.john.com.joker.CardColor;
import joker.john.com.joker.CardsUtility;

/**
 * 双升单机主流程控制器。
 * 创建时间：2026-06-02
 * 最近修改：2026-06-02
 * by john
 */
public class SimpleShengJiGame {
    public static final int PLAYER_SELF = 0;
    public static final int PLAYER_LEFT = 1;
    public static final int PLAYER_TOP = 2;
    public static final int PLAYER_RIGHT = 3;

    private static final int[] RANK_PATH = {5, 10, 13, 1, 2, 3, 4, 6, 7, 8, 9, 11, 12};

    private final AiPlayer aiPlayer = new AiPlayer();
    private final int[] teamLevels = new int[]{5, 5};
    private final boolean[] teamPassedGate = new boolean[]{false, false};
    private RuleConfig baseRuleConfig;
    private GameState state;
    private int revealCursor;
    private int preferredDealerPlayer = PLAYER_SELF;
    private int pendingNextDealerPlayer = PLAYER_SELF;
    private int pendingNextDealerTeam = -1;
    private String lastRoundSummary = "";
    private int revealPassCount;
    private boolean autoRevealEnabled = true;
    private int kittyOwnerPlayer = -1;
    private int leadPlayerAfterBury = PLAYER_SELF;
    private int currentRevealLevel;
    private int[] undoRemaining = new int[]{1, 1, 1, 1};
    private HumanUndoState lastHumanUndoState;
    private RevealChoice pendingHumanSpecialChoice;

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
        kittyOwnerPlayer = -1;
        leadPlayerAfterBury = preferredDealerPlayer;
        currentRevealLevel = 0;
        pendingHumanSpecialChoice = null;
        int undoCount = configSnapshot.getUndoChancesPerRound();
        undoRemaining = new int[]{undoCount, undoCount, undoCount, undoCount};
        lastHumanUndoState = null;
        clearSettlementState();
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
            return canHumanRevealNow();
        }
        if (state.getPhase() == GamePhase.BURY_KITTY) {
            return kittyOwnerPlayer == PLAYER_SELF;
        }
        return state.getPhase() == GamePhase.PLAYING
                && !state.isRoundFinished()
                && state.getTurnState().getPendingWinner() < 0
                && state.getTurnState().getCurrentPlayer() == PLAYER_SELF;
    }

    public boolean isHumanDealerBuryPhase() {
        return state.getPhase() == GamePhase.BURY_KITTY && kittyOwnerPlayer == PLAYER_SELF;
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

    public String getDefenderScoreLabel() {
        if (state.getDealerTeam() < 0) {
            return "闲家得分: 0";
        }
        return getTeamLabel(1 - state.getDealerTeam()) + "抓分: " + getNonDealerScore();
    }

    public String getNextDealerLabel() {
        if (pendingNextDealerPlayer < 0) {
            return "下局庄家: 待定";
        }
        return "下局庄家: " + getPlayerName(pendingNextDealerPlayer);
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

    public ArrayList<Card> getLastRevealCards() {
        return state.getLastRevealCards();
    }

    public String getLastRevealSummary() {
        return state.getLastRevealSummary();
    }

    public int getKittyBasePoints() {
        return state.getKittyBasePoints();
    }

    public int getKittyMultiplier() {
        return state.getKittyMultiplier();
    }

    public int getKittyBonusPoints() {
        return state.getKittyBonusPoints();
    }

    public int getLastTrickWinner() {
        return state.getLastTrickWinner();
    }

    public String getKittyWinTypeLabel() {
        return state.getKittyWinTypeLabel();
    }

    public String getRoundSettlementSummary() {
        return state.getRoundSettlementSummary();
    }

    public String getPrimaryActionText() {
        if (pendingHumanSpecialChoice != null) {
            return pendingHumanSpecialChoice.copyKitty ? "抄底" : "反主";
        }
        if (state.getPhase() == GamePhase.REVEAL_TRUMP) {
            return canHumanRevealNow() ? "我亮" : "亮主";
        }
        if (state.getPhase() == GamePhase.BURY_KITTY) {
            return kittyOwnerPlayer == PLAYER_SELF ? "放底牌" : "等待";
        }
        return "出牌";
    }

    public boolean canHumanUsePrimaryAction() {
        if (pendingHumanSpecialChoice != null && pendingHumanSpecialChoice.player == PLAYER_SELF) {
            return true;
        }
        if (state.getPhase() == GamePhase.REVEAL_TRUMP) {
            return canHumanRevealNow();
        }
        if (state.getPhase() == GamePhase.BURY_KITTY) {
            return kittyOwnerPlayer == PLAYER_SELF;
        }
        return isHumanTurn();
    }

    public MoveResult handleHumanPrimaryAction(List<Integer> selectedIndexes) {
        if (pendingHumanSpecialChoice != null && pendingHumanSpecialChoice.player == PLAYER_SELF) {
            return applyPendingHumanSpecialChoice();
        }
        if (state.getPhase() == GamePhase.REVEAL_TRUMP) {
            if (!canHumanRevealNow()) {
                return MoveResult.fail("当前没有可抢亮的级牌。");
            }
            MoveResult revealResult = revealTrump(PLAYER_SELF);
            if (!revealResult.success) {
                return revealResult;
            }
            return finalizeRevealStage();
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
        return resolveRevealImmediately();
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

        HumanUndoState undoState = captureHumanUndoState();
        NormalizedLeadPlay normalizedPlay = normalizeLeadPlayIfNeeded(PLAYER_SELF, selectedCards, validation.pattern);
        removeCardsByReference(hand, normalizedPlay.cards);
        applyPlay(PLAYER_SELF, normalizedPlay.cards, normalizedPlay.pattern);
        rememberHumanUndoState(undoState);
        appendLeadAdjustmentStatus(normalizedPlay.message);
        return MoveResult.success(normalizedPlay.message.length() > 0 ? normalizedPlay.message : state.getStatusMessage(), normalizedPlay.pattern);
    }

    public MoveResult playNextAiTurn() {
        lastHumanUndoState = null;
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

        NormalizedLeadPlay normalizedPlay = normalizeLeadPlayIfNeeded(player, selectedCards, validation.pattern);
        removeCardsByReference(state.getHands()[player], normalizedPlay.cards);
        applyPlay(player, normalizedPlay.cards, normalizedPlay.pattern);
        appendLeadAdjustmentStatus(normalizedPlay.message);
        return MoveResult.success(normalizedPlay.message.length() > 0 ? normalizedPlay.message : getPlayerName(player) + "出牌。", normalizedPlay.pattern);
    }

    public MoveSuggestion getSuggestedHumanMove() {
        if (state.getPhase() == GamePhase.BURY_KITTY) {
            if (kittyOwnerPlayer != PLAYER_SELF) {
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

    public boolean canUndoHumanMove() {
        return lastHumanUndoState != null && undoRemaining[PLAYER_SELF] > 0;
    }

    public int getHumanUndoRemaining() {
        return undoRemaining[PLAYER_SELF];
    }

    public MoveResult undoHumanMove() {
        if (!canUndoHumanMove()) {
            return MoveResult.fail("当前没有可撤销的出牌。");
        }
        restoreHumanUndoState(lastHumanUndoState);
        undoRemaining[PLAYER_SELF] = Math.max(0, undoRemaining[PLAYER_SELF] - 1);
        lastHumanUndoState = null;
        return MoveResult.success("已撤销上一次出牌。", null);
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
        } else if (leadPattern.getType() == PlayPattern.Type.PAIR) {
            ArrayList<ArrayList<Card>> pairCandidates = findFollowPairCandidates(hand, leadPattern);
            if (pairCandidates.size() == 1) {
                forcedCards = pairCandidates.get(0);
            }
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

    private ArrayList<ArrayList<Card>> findFollowPairCandidates(ArrayList<Card> hand, PlayPattern leadPattern) {
        ArrayList<ArrayList<Card>> candidates = new ArrayList<>();
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
                if (PlayValidator.validateFollow(hand, pair, leadPattern, state.getRuleConfig()).isSuccess()) {
                    candidates.add(pair);
                }
            }
        }
        return candidates;
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
        return resolveRevealImmediately();
    }

    private MoveResult revealTrump(int player) {
        RevealChoice choice = getInitialRevealChoice(state.getHands()[player], state.getCurrentRankValue(), player);
        if (choice == null) {
            return MoveResult.fail("当前没有可亮的级牌。");
        }
        applyRevealChoice(choice, false, true);
        state.setDealerPlayer(player);
        state.setDealerTeam(getTeamIndex(player));
        state.getActionLog().add(new GameAction(GameAction.Type.DEAL, player, state.getTrickNumber(), choice.cards, "亮主"));
        state.setStatusMessage(getPlayerName(player) + "亮主成功，当前主牌改为" + state.getRuleConfig().getTrumpSuitLabel() + "。");
        return MoveResult.success(state.getStatusMessage(), null);
    }

    private MoveResult fallbackRevealTrump() {
        RevealChoice kittyChoice = findKittyRevealChoice();
        if (kittyChoice != null) {
            RuleConfig config = state.getRuleConfig();
            config.setTrumpSuit(kittyChoice.trumpSuit);
            PlayValidator.applyRuleConfigToUtility(config);
            state.setDealerPlayer(kittyChoice.player);
            state.setDealerTeam(getTeamIndex(kittyChoice.player));
            state.setTrumpRevealLabel(config.getTrumpSuitLabel() + " " + RuleConfig.formatCardValue(state.getCurrentRankValue()) + "（底牌配亮）");
            resortAllHands();
            updateLastReveal(kittyChoice.player, kittyChoice.cards, getPlayerName(kittyChoice.player) + "用底牌配亮 "
                    + PlayValidator.describeCards(kittyChoice.cards, config) + "。");
            state.setStatusMessage(getPlayerName(kittyChoice.player) + "通过底牌配亮成为庄家。");
            state.getActionLog().add(new GameAction(GameAction.Type.DEAL, kittyChoice.player, state.getTrickNumber(), kittyChoice.cards, "底牌配亮"));
            return finalizeRevealStage();
        }

        int fallbackPlayer = preferredDealerPlayer;
        RuleConfig config = state.getRuleConfig();
        config.setNoTrump(false);
        config.setTrumpSuit(baseRuleConfig.getTrumpSuit());
        PlayValidator.applyRuleConfigToUtility(config);

        state.setDealerPlayer(fallbackPlayer);
        state.setDealerTeam(getTeamIndex(fallbackPlayer));
        state.setTrumpRevealLabel(config.getTrumpSuitLabel() + " " + RuleConfig.formatCardValue(state.getCurrentRankValue()) + "（默认）");
        resortAllHands();
        updateLastReveal(fallbackPlayer, new ArrayList<Card>(), getPlayerName(fallbackPlayer) + "无人亮主，按默认主花色"
                + config.getTrumpSuitLabel() + "坐庄。");
        state.setStatusMessage(getPlayerName(fallbackPlayer) + "无人亮主，按默认主花色坐庄并拿底牌。");
        state.getActionLog().add(new GameAction(GameAction.Type.DEAL, fallbackPlayer, state.getTrickNumber(), null, "默认坐庄"));
        return finalizeRevealStage();
    }

    private MoveResult resolveRevealImmediately() {
        if (pendingHumanSpecialChoice != null && pendingHumanSpecialChoice.player == PLAYER_SELF) {
            return MoveResult.success(state.getStatusMessage(), null);
        }
        RevealChoice bestChoice = findBestRevealChoice();
        if (bestChoice != null) {
            MoveResult result = revealTrump(bestChoice.player);
            if (!result.success) {
                return result;
            }
            RevealChoice counterChoice = findImmediateCounterRevealChoice();
            if (counterChoice != null) {
                if (counterChoice.player == PLAYER_SELF) {
                    pendingHumanSpecialChoice = counterChoice;
                    state.setStatusMessage("你可用 " + describeCards(counterChoice.cards) + " 反主，点“反主”确认。");
                    return MoveResult.success(state.getStatusMessage(), null);
                }
                applyRevealChoice(counterChoice, true, false);
            }
            return finalizeRevealStage();
        }
        return fallbackRevealTrump();
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
        kittyOwnerPlayer = dealerPlayer;
        leadPlayerAfterBury = dealerPlayer;
        state.getHands()[kittyOwnerPlayer].addAll(state.getKitty());
        PlayValidator.sortForDisplay(state.getHands()[kittyOwnerPlayer], state.getRuleConfig());
        state.getKitty().clear();
        state.setPhase(GamePhase.BURY_KITTY);
        state.setStatusMessage(getPlayerName(kittyOwnerPlayer) + "抢到庄并拿到底牌。");
        if (kittyOwnerPlayer == PLAYER_SELF) {
            return MoveResult.success("你抢到庄，主牌已更新，请选 8 张放底牌。", null);
        }
        return handleAiBuryKitty();
    }

    private MoveResult handleAiBuryKitty() {
        if (state.getPhase() != GamePhase.BURY_KITTY) {
            return MoveResult.fail("当前不是放底牌阶段。");
        }
        if (kittyOwnerPlayer == PLAYER_SELF) {
            return MoveResult.fail("等待你放底牌。");
        }
        ArrayList<Card> buryCards = pickLowestCards(state.getHands()[kittyOwnerPlayer], 8);
        removeCardsByReference(state.getHands()[kittyOwnerPlayer], buryCards);
        state.getKitty().addAll(buryCards);
        PlayValidator.sortForDisplay(state.getHands()[kittyOwnerPlayer], state.getRuleConfig());
        return continueAfterBury(kittyOwnerPlayer);
    }

    private MoveResult buryKitty(int player, List<Integer> selectedIndexes) {
        if (state.getPhase() != GamePhase.BURY_KITTY || player != kittyOwnerPlayer) {
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
        return continueAfterBury(player);
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

    /**
     * 结算当前牌墩，判定赢家并累计本墩分数。
     */
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
        state.setLastTrickWinner(winner.getPlayer());
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
        applyKittySettlementIfNeeded();
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
            dealerKeepsSeat = false;
        } else {
            dealerKeepsSeat = false;
        }

        int dealerLevelBeforeRound = teamLevels[dealerTeam];
        if (dealerDelta > 0) {
            teamLevels[dealerTeam] = advanceRank(teamLevels[dealerTeam], dealerDelta);
            if (!teamPassedGate[dealerTeam] && hasPassedGate(teamLevels[dealerTeam])) {
                teamPassedGate[dealerTeam] = true;
            }
        }
        if (!dealerKeepsSeat) {
            if (!teamPassedGate[dealerTeam]) {
                teamLevels[dealerTeam] = 5;
            }
            if (!teamPassedGate[1 - dealerTeam]) {
                teamLevels[1 - dealerTeam] = 5;
            }
        }

        pendingNextDealerTeam = dealerKeepsSeat ? dealerTeam : 1 - dealerTeam;
        pendingNextDealerPlayer = chooseNextDealerPlayer(dealerKeepsSeat);

        state.setRoundFinished(true);
        state.setPhase(GamePhase.ROUND_END);
        lastRoundSummary = buildRoundSummary(defenderScore, dealerDelta, defenderDelta, dealerKeepsSeat, dealerLevelBeforeRound);
        state.setRoundSettlementSummary(lastRoundSummary);
        state.setStatusMessage(lastRoundSummary);
        state.getActionLog().add(new GameAction(GameAction.Type.ROUND_END, state.getDealerPlayer(), state.getTrickNumber(), null, lastRoundSummary));
        state.getEventLog().add(new GameEvent(GameEvent.Type.ROUND_FINISHED, state.getDealerPlayer(), lastRoundSummary));
    }

    private String buildRoundSummary(int defenderScore, int dealerDelta, int defenderDelta, boolean dealerKeepsSeat, int dealerLevelBeforeRound) {
        StringBuilder builder = new StringBuilder();
        builder.append("本局结束，");
        builder.append(dealerKeepsSeat ? getTeamLabel(state.getDealerTeam()) : getTeamLabel(1 - state.getDealerTeam()));
        builder.append("获胜，");
        builder.append(dealerKeepsSeat ? "庄家方守住" : "庄家下台");
        builder.append("。闲家抓分 ").append(defenderScore).append("。");
        if (state.getKittyBasePoints() > 0 || state.getKittyWinTypeLabel().length() > 0) {
            builder.append("底牌 ").append(state.getKittyBasePoints()).append(" 分，")
                    .append(state.getKittyWinTypeLabel()).append("。");
            if (state.getKittyBonusPoints() > 0) {
                builder.append("本次加 ").append(state.getKittyBonusPoints()).append(" 分。");
            }
        }
        if (dealerDelta > 0) {
            builder.append("庄家方升 ").append(dealerDelta).append(" 级。");
        }
        if (defenderDelta > 0) {
            builder.append("闲家方升 ").append(defenderDelta).append(" 级。");
        } else if (!dealerKeepsSeat && !teamPassedGate[state.getDealerTeam()] && dealerLevelBeforeRound != 5) {
            builder.append("庄家方未连着通关，下次回到 5。");
        }
        builder.append("下一局级牌：我方 ").append(RuleConfig.formatCardValue(teamLevels[0]))
                .append(" / 对方 ").append(RuleConfig.formatCardValue(teamLevels[1])).append("。");
        builder.append("下局庄家是 ").append(getPlayerName(pendingNextDealerPlayer)).append("。");
        return builder.toString();
    }

    /**
     * 比较当前候选出牌是否能压过本墩暂时最大牌。
     */
    private boolean beats(PlayedHand candidate, PlayedHand currentWinner, PlayPattern leadPattern) {
        if (!candidate.getPattern().canBeatLead()) {
            return false;
        }
        if (leadPattern.getType() != candidate.getPattern().getType()) {
            return false;
        }

        if (leadPattern.getType() == PlayPattern.Type.SHUAI) {
            return beatsShuai(candidate, currentWinner, leadPattern);
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

    private boolean beatsShuai(PlayedHand candidate, PlayedHand currentWinner, PlayPattern leadPattern) {
        RuleConfig config = state.getRuleConfig();
        boolean candidateAllTrump = PlayValidator.isAllTrump(candidate.getCards(), config);
        boolean winnerAllTrump = PlayValidator.isAllTrump(currentWinner.getCards(), config);
        int leadGroup = leadPattern.getGroup();

        if (leadGroup == PlayValidator.GROUP_TRUMP) {
            if (!candidateAllTrump) {
                return false;
            }
            if (!winnerAllTrump) {
                return true;
            }
            return candidate.getPattern().getTopSequence() > currentWinner.getPattern().getTopSequence();
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
        return candidate.getPattern().getTopSequence() > currentWinner.getPattern().getTopSequence();
    }

    /**
     * 先处理普通甩牌，再处理公开信息半甩，最后按最小子集自动收缩。
     */
    private NormalizedLeadPlay normalizeLeadPlayIfNeeded(int player, List<Card> selectedCards, PlayPattern pattern) {
        ArrayList<Card> normalizedCards = new ArrayList<>(selectedCards);
        if (!state.getCurrentTrick().isEmpty() || pattern.getType() != PlayPattern.Type.SHUAI) {
            return new NormalizedLeadPlay(normalizedCards, pattern, "");
        }
        if (PlayValidator.canShuaiSucceed(normalizedCards, state.getHands(), player, state.getRuleConfig())) {
            return new NormalizedLeadPlay(normalizedCards, pattern, "");
        }
        if (PlayValidator.canSemiShuaiByPublicInfo(normalizedCards, state)) {
            return new NormalizedLeadPlay(normalizedCards, pattern,
                    getPlayerName(player) + "半甩成功，按公开信息允许连出" + PlayValidator.describeCards(normalizedCards, state.getRuleConfig()) + "。");
        }

        ArrayList<Card> reducedCards = state.getRuleConfig().isPublicInfoSemiShuaiEnabled()
                ? PlayValidator.reduceFailedSemiShuai(normalizedCards, state)
                : PlayValidator.reduceFailedShuai(normalizedCards, state.getRuleConfig());
        PlayValidator.ValidationResult reducedValidation = PlayValidator.validateLead(reducedCards, state.getRuleConfig());
        PlayPattern reducedPattern = reducedValidation.isSuccess() ? reducedValidation.getPattern() : pattern;
        String message = getPlayerName(player) + "半甩失败，只落成" + PlayValidator.describeCards(reducedCards, state.getRuleConfig()) + "。";
        return new NormalizedLeadPlay(reducedCards, reducedPattern, message);
    }

    private void appendLeadAdjustmentStatus(String message) {
        if (message == null || message.length() == 0) {
            return;
        }
        state.setStatusMessage(message + " " + state.getStatusMessage());
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

    private boolean hasPassedGate(int level) {
        for (int i = 0; i < RANK_PATH.length; i++) {
            if (RANK_PATH[i] == level) {
                return i >= 3;
            }
        }
        return false;
    }

    private int chooseNextDealerPlayer(boolean dealerKeepsSeat) {
        if (dealerKeepsSeat) {
            return state.getDealerPlayer();
        }
        return chooseDefenderLeadPlayer(state.getDealerPlayer());
    }

    private int chooseDefenderLeadPlayer(int dealerPlayer) {
        int defenderTeam = 1 - state.getDealerTeam();
        for (int offset = 1; offset < 4; offset++) {
            int player = (dealerPlayer + offset) % 4;
            if (getTeamIndex(player) == defenderTeam) {
                return player;
            }
        }
        return dealerPlayer;
    }

    private void clearSettlementState() {
        state.setKittyBasePoints(0);
        state.setKittyMultiplier(1);
        state.setKittyBonusPoints(0);
        state.setLastTrickWinner(-1);
        state.setKittyWinTypeLabel("");
        state.setRoundSettlementSummary("");
        state.setLastRevealPlayer(-1);
        state.setLastRevealSummary("");
        state.getLastRevealCards().clear();
        state.getPublicRevealCards().clear();
    }

    private void updateLastReveal(int player, List<Card> cards, String summary) {
        state.setLastRevealPlayer(player);
        state.setLastRevealSummary(summary);
        state.getLastRevealCards().clear();
        if (cards != null) {
            state.getLastRevealCards().addAll(cards);
            state.getPublicRevealCards().addAll(cards);
        }
    }

    private void resortAllHands() {
        for (int player = 0; player < 4; player++) {
            PlayValidator.sortForDisplay(state.getHands()[player], state.getRuleConfig());
        }
    }

    private void applyKittySettlementIfNeeded() {
        int kittyBasePoints = 0;
        for (Card card : state.getKitty()) {
            kittyBasePoints += PlayValidator.getCardPoints(card);
        }
        state.setKittyBasePoints(kittyBasePoints);
        state.setKittyMultiplier(1);
        state.setKittyBonusPoints(0);

        if (state.getCompletedTricks().isEmpty()) {
            state.setKittyWinTypeLabel("未收底");
            return;
        }

        PlayedTrick lastTrick = state.getCompletedTricks().get(state.getCompletedTricks().size() - 1);
        PlayedHand winningHand = lastTrick.getWinningHand();
        if (winningHand == null) {
            state.setKittyWinTypeLabel("未收底");
            return;
        }

        int winner = lastTrick.getWinner();
        state.setLastTrickWinner(winner);
        if (getTeamIndex(winner) == state.getDealerTeam()) {
            state.setKittyWinTypeLabel("庄家收底");
            return;
        }

        int multiplier = resolveKittyMultiplier(winningHand.getPattern());
        int bonusPoints = kittyBasePoints * multiplier;
        state.getTeamScores()[getTeamIndex(winner)] += bonusPoints;
        state.setKittyMultiplier(multiplier);
        state.setKittyBonusPoints(bonusPoints);
        state.setKittyWinTypeLabel(resolveKittyWinTypeLabel(winningHand.getPattern(), multiplier));
    }

    private int resolveKittyMultiplier(PlayPattern pattern) {
        if (pattern == null) {
            return 2;
        }
        if (pattern.getType() == PlayPattern.Type.PAIR) {
            return 4;
        }
        if (pattern.getType() == PlayPattern.Type.TRACTOR) {
            return (int) Math.pow(2, pattern.getPairCount() + 1);
        }
        return 2;
    }

    private String resolveKittyWinTypeLabel(PlayPattern pattern, int multiplier) {
        if (pattern == null) {
            return "单扣 ×" + multiplier;
        }
        if (pattern.getType() == PlayPattern.Type.PAIR) {
            return "对扣 ×" + multiplier;
        }
        if (pattern.getType() == PlayPattern.Type.TRACTOR) {
            return "拖拉机扣 ×" + multiplier;
        }
        return "单扣 ×" + multiplier;
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
        RevealChoice choice = getInitialRevealChoice(hand, rankValue, -1);
        return choice == null ? new ArrayList<Card>() : new ArrayList<Card>(choice.cards);
    }

    private boolean canHumanRevealNow() {
        if (state.getPhase() != GamePhase.REVEAL_TRUMP) {
            return false;
        }
        if (!state.getRuleConfig().isRevealStealEnabled() && preferredDealerPlayer != PLAYER_SELF) {
            return false;
        }
        return getInitialRevealChoice(state.getHands()[PLAYER_SELF], state.getCurrentRankValue(), PLAYER_SELF) != null;
    }

    private boolean canPlayerReveal(int player) {
        return !getRevealCards(state.getHands()[player], state.getCurrentRankValue()).isEmpty();
    }

    private void updateRevealStatus() {
        state.setStatusMessage("系统正在自动判定亮主。当前级牌 " + RuleConfig.formatCardValue(state.getCurrentRankValue()) + "。");
    }

    private RevealChoice findBestRevealChoice() {
        if (!state.getRuleConfig().isRevealStealEnabled()) {
            return getInitialRevealChoice(state.getHands()[preferredDealerPlayer], state.getCurrentRankValue(), preferredDealerPlayer);
        }
        RevealChoice bestChoice = null;
        int bestScore = Integer.MIN_VALUE;
        for (int player = 0; player < 4; player++) {
            RevealChoice choice = getInitialRevealChoice(state.getHands()[player], state.getCurrentRankValue(), player);
            if (choice == null) {
                continue;
            }
            int score = scoreRevealChoice(player, choice);
            if (score > bestScore) {
                bestScore = score;
                bestChoice = choice;
            }
        }
        return bestChoice;
    }

    private RevealChoice findImmediateCounterRevealChoice() {
        if (!state.getRuleConfig().isCounterTrumpSingleJokerPairEnabled() || state.getDealerPlayer() < 0) {
            return null;
        }
        RevealChoice bestChoice = null;
        int bestScore = Integer.MIN_VALUE;
        for (int player = 0; player < 4; player++) {
            RevealChoice choice = getSingleJokerPairCounterChoice(state.getHands()[player], state.getCurrentRankValue(), player);
            if (choice == null) {
                continue;
            }
            int score = scoreRevealChoice(player, choice);
            if (score > bestScore) {
                bestScore = score;
                bestChoice = choice;
            }
        }
        return bestChoice;
    }

    private RevealChoice findPostBuryCounterChoice() {
        RevealChoice bestChoice = null;
        int bestScore = Integer.MIN_VALUE;
        for (int player = 0; player < 4; player++) {
            if (player == kittyOwnerPlayer) {
                continue;
            }
            RevealChoice choice = getDoubleJokerCounterChoice(state.getHands()[player], state.getCurrentRankValue(), player);
            if (choice == null || choice.level <= currentRevealLevel) {
                continue;
            }
            int score = scoreRevealChoice(player, choice);
            if (score > bestScore) {
                bestScore = score;
                bestChoice = choice;
            }
        }
        return bestChoice;
    }

    private int scoreRevealChoice(int player, RevealChoice choice) {
        int score = 0;
        if (pendingNextDealerTeam >= 0 && getTeamIndex(player) == pendingNextDealerTeam) {
            score += 600;
        }
        if (player == preferredDealerPlayer) {
            score += 120;
        }

        ArrayList<Card> hand = state.getHands()[player];
        RuleConfig simulated = state.getRuleConfig().copy();
        simulated.setNoTrump(choice.noTrump);
        if (!choice.noTrump) {
            simulated.setTrumpSuit(choice.trumpSuit);
        }
        score += countTrumpCards(hand, simulated) * 14;
        score += countPairs(hand) * 18;
        score += countPointCards(hand) * 2;
        if (containsBigJoker(hand)) {
            score += 90;
        }
        if (containsSmallJoker(hand)) {
            score += 55;
        }
        if (PlayValidator.findLowestTractor(hand, 4, simulated) != null) {
            score += 65;
        }
        return score;
    }

    private RevealChoice getInitialRevealChoice(List<Card> hand, int rankValue, int player) {
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

        if (bigJoker != null && !redRankCards.isEmpty()) {
            ArrayList<Card> cards = new ArrayList<>();
            cards.add(bigJoker);
            cards.add(redRankCards.get(0));
            return new RevealChoice(player, redRankCards.get(0).getColor(), false, false, 1, cards,
                    getPlayerName(player) + "亮出 " + PlayValidator.describeCards(cards, state.getRuleConfig()) + "。");
        }
        if (smallJoker != null && !blackRankCards.isEmpty()) {
            ArrayList<Card> cards = new ArrayList<>();
            cards.add(smallJoker);
            cards.add(blackRankCards.get(0));
            return new RevealChoice(player, blackRankCards.get(0).getColor(), false, false, 1, cards,
                    getPlayerName(player) + "亮出 " + PlayValidator.describeCards(cards, state.getRuleConfig()) + "。");
        }
        return null;
    }

    private RevealChoice getSingleJokerPairCounterChoice(List<Card> hand, int rankValue, int player) {
        Card bigJoker = null;
        Card smallJoker = null;
        ArrayList<ArrayList<Card>> redPairs = new ArrayList<>();
        ArrayList<ArrayList<Card>> blackPairs = new ArrayList<>();
        collectRankPairs(hand, rankValue, redPairs, blackPairs);
        for (Card card : hand) {
            if (card.getValue() == 15) {
                bigJoker = card;
            } else if (card.getValue() == 14) {
                smallJoker = card;
            }
        }
        if (bigJoker != null && !redPairs.isEmpty()) {
            ArrayList<Card> cards = new ArrayList<>();
            cards.add(bigJoker);
            cards.addAll(redPairs.get(0));
            CardColor suit = redPairs.get(0).get(0).getColor();
            return new RevealChoice(player, suit, false, false, 2, cards,
                    getPlayerName(player) + "用" + PlayValidator.describeCards(cards, state.getRuleConfig()) + "反主为" + suitLabel(suit) + "。");
        }
        if (smallJoker != null && !blackPairs.isEmpty()) {
            ArrayList<Card> cards = new ArrayList<>();
            cards.add(smallJoker);
            cards.addAll(blackPairs.get(0));
            CardColor suit = blackPairs.get(0).get(0).getColor();
            return new RevealChoice(player, suit, false, false, 2, cards,
                    getPlayerName(player) + "用" + PlayValidator.describeCards(cards, state.getRuleConfig()) + "反主为" + suitLabel(suit) + "。");
        }
        return null;
    }

    private RevealChoice getDoubleJokerCounterChoice(List<Card> hand, int rankValue, int player) {
        if (!state.getRuleConfig().isCounterTrumpDoubleJokerCopyEnabled() && !state.getRuleConfig().isCounterNoTrumpEnabled()) {
            return null;
        }
        ArrayList<Card> bigJokers = new ArrayList<>();
        ArrayList<Card> smallJokers = new ArrayList<>();
        ArrayList<Card> redRanks = new ArrayList<>();
        ArrayList<Card> blackRanks = new ArrayList<>();
        for (Card card : hand) {
            if (card.getValue() == 15) {
                bigJokers.add(card);
            } else if (card.getValue() == 14) {
                smallJokers.add(card);
            } else if (card.getValue() == rankValue) {
                if (card.getColor() == CardColor.Heart || card.getColor() == CardColor.Diamond) {
                    redRanks.add(card);
                } else if (card.getColor() == CardColor.Spade || card.getColor() == CardColor.Club) {
                    blackRanks.add(card);
                }
            }
        }
        boolean hasBigPair = bigJokers.size() >= 2;
        boolean hasSmallPair = smallJokers.size() >= 2;
        if (!hasBigPair && !hasSmallPair) {
            return null;
        }

        RevealChoice bestChoice = null;
        if (state.getRuleConfig().isCounterNoTrumpEnabled()) {
            ArrayList<Card> cards = new ArrayList<>();
            if (hasBigPair) {
                cards.add(bigJokers.get(0));
                cards.add(bigJokers.get(1));
            } else {
                cards.add(smallJokers.get(0));
                cards.add(smallJokers.get(1));
            }
            bestChoice = new RevealChoice(player, state.getRuleConfig().getTrumpSuit(), true, true, 3, cards,
                    getPlayerName(player) + "用" + PlayValidator.describeCards(cards, state.getRuleConfig()) + "反成无主并抄底。");
        }
        if (state.getRuleConfig().isCounterTrumpDoubleJokerCopyEnabled()) {
            if (hasBigPair && !redRanks.isEmpty()) {
                ArrayList<Card> cards = new ArrayList<>();
                cards.add(bigJokers.get(0));
                cards.add(bigJokers.get(1));
                cards.add(redRanks.get(0));
                RevealChoice suitChoice = new RevealChoice(player, redRanks.get(0).getColor(), false, true, 4, cards,
                        getPlayerName(player) + "用" + PlayValidator.describeCards(cards, state.getRuleConfig()) + "反主抄底。");
                if (bestChoice == null || scoreRevealChoice(player, suitChoice) > scoreRevealChoice(player, bestChoice)) {
                    bestChoice = suitChoice;
                }
            }
            if (hasSmallPair && !blackRanks.isEmpty()) {
                ArrayList<Card> cards = new ArrayList<>();
                cards.add(smallJokers.get(0));
                cards.add(smallJokers.get(1));
                cards.add(blackRanks.get(0));
                RevealChoice suitChoice = new RevealChoice(player, blackRanks.get(0).getColor(), false, true, 4, cards,
                        getPlayerName(player) + "用" + PlayValidator.describeCards(cards, state.getRuleConfig()) + "反主抄底。");
                if (bestChoice == null || scoreRevealChoice(player, suitChoice) > scoreRevealChoice(player, bestChoice)) {
                    bestChoice = suitChoice;
                }
            }
        }
        return bestChoice;
    }

    private boolean containsBigJoker(List<Card> hand) {
        for (Card card : hand) {
            if (card.getValue() == 15) {
                return true;
            }
        }
        return false;
    }

    private boolean containsSmallJoker(List<Card> hand) {
        for (Card card : hand) {
            if (card.getValue() == 14) {
                return true;
            }
        }
        return false;
    }

    private int countTrumpCards(List<Card> hand, RuleConfig config) {
        int count = 0;
        for (Card card : hand) {
            if (PlayValidator.isTrump(card, config)) {
                count++;
            }
        }
        return count;
    }

    private int countPairs(List<Card> hand) {
        int count = 0;
        boolean[] used = new boolean[hand.size()];
        for (int i = 0; i < hand.size(); i++) {
            if (used[i]) {
                continue;
            }
            for (int j = i + 1; j < hand.size(); j++) {
                if (used[j]) {
                    continue;
                }
                Card first = hand.get(i);
                Card second = hand.get(j);
                if (first.getValue() == second.getValue() && first.getColor() == second.getColor()) {
                    used[i] = true;
                    used[j] = true;
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private int countPointCards(List<Card> hand) {
        int count = 0;
        for (Card card : hand) {
            count += PlayValidator.getCardPoints(card);
        }
        return count;
    }

    private void collectRankPairs(List<Card> hand, int rankValue, ArrayList<ArrayList<Card>> redPairs, ArrayList<ArrayList<Card>> blackPairs) {
        for (CardColor suit : new CardColor[]{CardColor.Spade, CardColor.Heart, CardColor.Club, CardColor.Diamond}) {
            ArrayList<Card> suitCards = new ArrayList<>();
            for (Card card : hand) {
                if (card.getValue() == rankValue && card.getColor() == suit) {
                    suitCards.add(card);
                }
            }
            if (suitCards.size() >= 2) {
                ArrayList<Card> pair = new ArrayList<>();
                pair.add(suitCards.get(0));
                pair.add(suitCards.get(1));
                if (suit == CardColor.Heart || suit == CardColor.Diamond) {
                    redPairs.add(pair);
                } else {
                    blackPairs.add(pair);
                }
            }
        }
    }

    private Card findSingleRank(List<Card> hand, int rankValue, boolean red) {
        for (Card card : hand) {
            boolean isRed = card.getColor() == CardColor.Heart || card.getColor() == CardColor.Diamond;
            if (card.getValue() == rankValue && isRed == red) {
                return card;
            }
        }
        return null;
    }

    private void applyRevealChoice(RevealChoice choice, boolean keepDealer, boolean setDealerToPlayer) {
        RuleConfig config = state.getRuleConfig();
        config.setNoTrump(choice.noTrump);
        if (!choice.noTrump) {
            config.setTrumpSuit(choice.trumpSuit);
        }
        PlayValidator.applyRuleConfigToUtility(config);
        if (setDealerToPlayer) {
            state.setDealerPlayer(choice.player);
            state.setDealerTeam(getTeamIndex(choice.player));
        } else if (!keepDealer) {
            state.setDealerPlayer(choice.player);
            state.setDealerTeam(getTeamIndex(choice.player));
        }
        currentRevealLevel = Math.max(currentRevealLevel, choice.level);
        String label = config.getTrumpSuitLabel() + " " + RuleConfig.formatCardValue(state.getCurrentRankValue());
        if (choice.noTrump) {
            label = "无主 " + RuleConfig.formatCardValue(state.getCurrentRankValue());
        }
        state.setTrumpRevealLabel(label);
        resortAllHands();
        updateLastReveal(choice.player, choice.cards, choice.summary);
    }

    private MoveResult continueAfterBury(int buryPlayer) {
        RevealChoice counterChoice = findPostBuryCounterChoice();
        if (counterChoice != null) {
            if (counterChoice.player == PLAYER_SELF) {
                pendingHumanSpecialChoice = counterChoice;
                state.setStatusMessage("你可用 " + describeCards(counterChoice.cards) + (counterChoice.noTrump ? " 反成无主并抄底，点“抄底”确认。" : " 反主抄底，点“抄底”确认。"));
                return MoveResult.success(state.getStatusMessage(), null);
            }
            applyRevealChoice(counterChoice, true, false);
            kittyOwnerPlayer = counterChoice.player;
            state.getHands()[kittyOwnerPlayer].addAll(state.getKitty());
            PlayValidator.sortForDisplay(state.getHands()[kittyOwnerPlayer], state.getRuleConfig());
            state.getKitty().clear();
            state.setPhase(GamePhase.BURY_KITTY);
            state.setStatusMessage(getPlayerName(counterChoice.player) + "反主抄底成功，请重新放底牌。");
            if (kittyOwnerPlayer == PLAYER_SELF) {
                return MoveResult.success("你反主抄底成功，请重新选 8 张放底牌。", null);
            }
            return handleAiBuryKitty();
        }
        return startPlayingAfterBury(buryPlayer);
    }

    private MoveResult applyPendingHumanSpecialChoice() {
        if (pendingHumanSpecialChoice == null || pendingHumanSpecialChoice.player != PLAYER_SELF) {
            return MoveResult.fail("当前没有可确认的反主或抄底。");
        }
        RevealChoice choice = pendingHumanSpecialChoice;
        pendingHumanSpecialChoice = null;
        applyRevealChoice(choice, true, false);
        if (choice.copyKitty) {
            kittyOwnerPlayer = choice.player;
            state.getHands()[kittyOwnerPlayer].addAll(state.getKitty());
            PlayValidator.sortForDisplay(state.getHands()[kittyOwnerPlayer], state.getRuleConfig());
            state.getKitty().clear();
            state.setPhase(GamePhase.BURY_KITTY);
            state.setStatusMessage(choice.noTrump
                    ? "你反成无主并抄底成功，请重新选 8 张放底牌。"
                    : "你反主抄底成功，请重新选 8 张放底牌。");
            return MoveResult.success(state.getStatusMessage(), null);
        }
        state.setStatusMessage("你反主成功，主牌已更新。");
        return finalizeRevealStage();
    }

    private MoveResult startPlayingAfterBury(int buryPlayer) {
        state.setPhase(GamePhase.PLAYING);
        state.setTurnState(new TurnState(leadPlayerAfterBury, leadPlayerAfterBury));
        lastHumanUndoState = null;
        if (leadPlayerAfterBury == PLAYER_SELF) {
            state.setStatusMessage("底牌已放好，你先出牌。");
            return MoveResult.success(state.getStatusMessage(), null);
        }
        state.setStatusMessage(getPlayerName(buryPlayer) + "已经放好底牌，" + getPlayerName(leadPlayerAfterBury) + "先出牌。");
        return MoveResult.success(state.getStatusMessage(), null);
    }

    private HumanUndoState captureHumanUndoState() {
        return new HumanUndoState(
                new ArrayList<Card>(state.getHands()[PLAYER_SELF]),
                new ArrayList<PlayedHand>(state.getCurrentTrick()),
                copyTurnState(state.getTurnState()),
                state.getStatusMessage(),
                state.getTrickNumber()
        );
    }

    private void rememberHumanUndoState(HumanUndoState undoState) {
        if (undoState == null || state.getCurrentTrick().size() >= 4 || state.getPhase() != GamePhase.PLAYING) {
            lastHumanUndoState = null;
            return;
        }
        lastHumanUndoState = undoState;
    }

    private void restoreHumanUndoState(HumanUndoState undoState) {
        state.getHands()[PLAYER_SELF].clear();
        state.getHands()[PLAYER_SELF].addAll(undoState.handSnapshot);
        state.getCurrentTrick().clear();
        state.getCurrentTrick().addAll(undoState.trickSnapshot);
        state.setTurnState(copyTurnState(undoState.turnStateSnapshot));
        state.setStatusMessage(undoState.statusMessage);
        state.setTrickNumber(undoState.trickNumber);
    }

    private TurnState copyTurnState(TurnState source) {
        if (source == null) {
            return null;
        }
        TurnState copy = new TurnState(source.getLeadPlayer(), source.getCurrentPlayer());
        copy.setLeadPattern(source.getLeadPattern());
        copy.setPendingWinner(source.getPendingWinner());
        copy.setPendingPoints(source.getPendingPoints());
        return copy;
    }

    private String suitLabel(CardColor suit) {
        if (suit == CardColor.Spade) {
            return "黑桃";
        }
        if (suit == CardColor.Heart) {
            return "红桃";
        }
        if (suit == CardColor.Club) {
            return "梅花";
        }
        return "方块";
    }

    private RevealChoice findKittyRevealChoice() {
        RevealChoice bestChoice = null;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 3; i < state.getKitty().size(); i++) {
            Card kittyCard = state.getKitty().get(i);
            for (int player = 0; player < 4; player++) {
                RevealChoice choice = getRevealChoiceWithKitty(state.getHands()[player], state.getCurrentRankValue(), player, kittyCard);
                if (choice != null) {
                    int score = scoreRevealChoice(player, choice);
                    if (score > bestScore) {
                        bestScore = score;
                        bestChoice = choice;
                    }
                }
            }
        }
        return bestChoice;
    }

    private RevealChoice getRevealChoiceWithKitty(List<Card> hand, int rankValue, int player, Card kittyCard) {
        if (kittyCard == null) {
            return null;
        }
        if (kittyCard.getValue() == 15) {
            for (Card card : hand) {
                if (card.getValue() == rankValue && (card.getColor() == CardColor.Heart || card.getColor() == CardColor.Diamond)) {
                    ArrayList<Card> cards = new ArrayList<>();
                    cards.add(kittyCard);
                    cards.add(card);
                    return new RevealChoice(player, card.getColor(), false, false, 1, cards,
                            getPlayerName(player) + "用底牌配亮 " + PlayValidator.describeCards(cards, state.getRuleConfig()) + "。");
                }
            }
        }
        if (kittyCard.getValue() == 14) {
            for (Card card : hand) {
                if (card.getValue() == rankValue && (card.getColor() == CardColor.Spade || card.getColor() == CardColor.Club)) {
                    ArrayList<Card> cards = new ArrayList<>();
                    cards.add(kittyCard);
                    cards.add(card);
                    return new RevealChoice(player, card.getColor(), false, false, 1, cards,
                            getPlayerName(player) + "用底牌配亮 " + PlayValidator.describeCards(cards, state.getRuleConfig()) + "。");
                }
            }
        }
        if (kittyCard.getValue() == rankValue) {
            if (kittyCard.getColor() == CardColor.Heart || kittyCard.getColor() == CardColor.Diamond) {
                for (Card card : hand) {
                    if (card.getValue() == 15) {
                        ArrayList<Card> cards = new ArrayList<>();
                        cards.add(card);
                        cards.add(kittyCard);
                        return new RevealChoice(player, kittyCard.getColor(), false, false, 1, cards,
                                getPlayerName(player) + "用底牌配亮 " + PlayValidator.describeCards(cards, state.getRuleConfig()) + "。");
                    }
                }
            }
            if (kittyCard.getColor() == CardColor.Spade || kittyCard.getColor() == CardColor.Club) {
                for (Card card : hand) {
                    if (card.getValue() == 14) {
                        ArrayList<Card> cards = new ArrayList<>();
                        cards.add(card);
                        cards.add(kittyCard);
                        return new RevealChoice(player, kittyCard.getColor(), false, false, 1, cards,
                                getPlayerName(player) + "用底牌配亮 " + PlayValidator.describeCards(cards, state.getRuleConfig()) + "。");
                    }
                }
            }
        }
        return null;
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

    private static class RevealChoice {
        final int player;
        final CardColor trumpSuit;
        final boolean noTrump;
        final boolean copyKitty;
        final int level;
        final ArrayList<Card> cards;
        final String summary;

        RevealChoice(int player, CardColor trumpSuit, boolean noTrump, boolean copyKitty, int level,
                     ArrayList<Card> cards, String summary) {
            this.player = player;
            this.trumpSuit = trumpSuit;
            this.noTrump = noTrump;
            this.copyKitty = copyKitty;
            this.level = level;
            this.cards = cards;
            this.summary = summary;
        }
    }

    private static class NormalizedLeadPlay {
        final ArrayList<Card> cards;
        final PlayPattern pattern;
        final String message;

        NormalizedLeadPlay(ArrayList<Card> cards, PlayPattern pattern, String message) {
            this.cards = cards;
            this.pattern = pattern;
            this.message = message;
        }
    }

    private static class HumanUndoState {
        final ArrayList<Card> handSnapshot;
        final ArrayList<PlayedHand> trickSnapshot;
        final TurnState turnStateSnapshot;
        final String statusMessage;
        final int trickNumber;

        HumanUndoState(ArrayList<Card> handSnapshot, ArrayList<PlayedHand> trickSnapshot,
                       TurnState turnStateSnapshot, String statusMessage, int trickNumber) {
            this.handSnapshot = handSnapshot;
            this.trickSnapshot = trickSnapshot;
            this.turnStateSnapshot = turnStateSnapshot;
            this.statusMessage = statusMessage;
            this.trickNumber = trickNumber;
        }
    }
}
