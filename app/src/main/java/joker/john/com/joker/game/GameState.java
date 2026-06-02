package joker.john.com.joker.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import joker.john.com.joker.Card;

/**
 * 单局双升运行时状态。
 * 创建时间：2026-06-02
 * 最近修改：2026-06-02
 * by john
 */
public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final RuleConfig ruleConfig;
    private final ArrayList<Card>[] hands;
    private final ArrayList<Card> kitty = new ArrayList<>();
    private final ArrayList<PlayedHand> currentTrick = new ArrayList<>();
    private final ArrayList<PlayedTrick> completedTricks = new ArrayList<>();
    private final ArrayList<GameAction> actionLog = new ArrayList<>();
    private final ArrayList<GameEvent> eventLog = new ArrayList<>();
    private final int[] teamScores = new int[2];
    private TurnState turnState;
    private int trickNumber = 1;
    private int roundNumber = 1;
    private int dealerPlayer = -1;
    private int dealerTeam = -1;
    private int currentRankValue = 5;
    private String trumpRevealLabel = "";
    private GamePhase phase = GamePhase.REVEAL_TRUMP;
    private boolean roundFinished;
    private String statusMessage = "";
    private int kittyBasePoints;
    private int kittyMultiplier = 1;
    private int kittyBonusPoints;
    private int lastTrickWinner = -1;
    private String kittyWinTypeLabel = "";
    private String roundSettlementSummary = "";
    private int lastRevealPlayer = -1;
    private String lastRevealSummary = "";
    private final ArrayList<Card> lastRevealCards = new ArrayList<>();
    private final ArrayList<Card> publicRevealCards = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public GameState(RuleConfig ruleConfig) {
        this.ruleConfig = ruleConfig;
        hands = new ArrayList[4];
        for (int i = 0; i < hands.length; i++) {
            hands[i] = new ArrayList<>();
        }
    }

    public RuleConfig getRuleConfig() {
        return ruleConfig;
    }

    public ArrayList<Card>[] getHands() {
        return hands;
    }

    public ArrayList<Card> getKitty() {
        return kitty;
    }

    public ArrayList<PlayedHand> getCurrentTrick() {
        return currentTrick;
    }

    public ArrayList<PlayedTrick> getCompletedTricks() {
        return completedTricks;
    }

    public ArrayList<GameAction> getActionLog() {
        return actionLog;
    }

    public ArrayList<GameEvent> getEventLog() {
        return eventLog;
    }

    public int[] getTeamScores() {
        return teamScores;
    }

    public TurnState getTurnState() {
        return turnState;
    }

    public void setTurnState(TurnState turnState) {
        this.turnState = turnState;
    }

    public int getTrickNumber() {
        return trickNumber;
    }

    public void setTrickNumber(int trickNumber) {
        this.trickNumber = trickNumber;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public int getDealerPlayer() {
        return dealerPlayer;
    }

    public void setDealerPlayer(int dealerPlayer) {
        this.dealerPlayer = dealerPlayer;
    }

    public int getDealerTeam() {
        return dealerTeam;
    }

    public void setDealerTeam(int dealerTeam) {
        this.dealerTeam = dealerTeam;
    }

    public int getCurrentRankValue() {
        return currentRankValue;
    }

    public void setCurrentRankValue(int currentRankValue) {
        this.currentRankValue = currentRankValue;
    }

    public String getTrumpRevealLabel() {
        return trumpRevealLabel;
    }

    public void setTrumpRevealLabel(String trumpRevealLabel) {
        this.trumpRevealLabel = trumpRevealLabel;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public boolean isRoundFinished() {
        return roundFinished;
    }

    public void setRoundFinished(boolean roundFinished) {
        this.roundFinished = roundFinished;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public int getKittyBasePoints() {
        return kittyBasePoints;
    }

    public void setKittyBasePoints(int kittyBasePoints) {
        this.kittyBasePoints = kittyBasePoints;
    }

    public int getKittyMultiplier() {
        return kittyMultiplier;
    }

    public void setKittyMultiplier(int kittyMultiplier) {
        this.kittyMultiplier = kittyMultiplier;
    }

    public int getKittyBonusPoints() {
        return kittyBonusPoints;
    }

    public void setKittyBonusPoints(int kittyBonusPoints) {
        this.kittyBonusPoints = kittyBonusPoints;
    }

    public int getLastTrickWinner() {
        return lastTrickWinner;
    }

    public void setLastTrickWinner(int lastTrickWinner) {
        this.lastTrickWinner = lastTrickWinner;
    }

    public String getKittyWinTypeLabel() {
        return kittyWinTypeLabel;
    }

    public void setKittyWinTypeLabel(String kittyWinTypeLabel) {
        this.kittyWinTypeLabel = kittyWinTypeLabel;
    }

    public String getRoundSettlementSummary() {
        return roundSettlementSummary;
    }

    public void setRoundSettlementSummary(String roundSettlementSummary) {
        this.roundSettlementSummary = roundSettlementSummary;
    }

    public int getLastRevealPlayer() {
        return lastRevealPlayer;
    }

    public void setLastRevealPlayer(int lastRevealPlayer) {
        this.lastRevealPlayer = lastRevealPlayer;
    }

    public String getLastRevealSummary() {
        return lastRevealSummary;
    }

    public void setLastRevealSummary(String lastRevealSummary) {
        this.lastRevealSummary = lastRevealSummary;
    }

    public ArrayList<Card> getLastRevealCards() {
        return lastRevealCards;
    }

    public ArrayList<Card> getPublicRevealCards() {
        return publicRevealCards;
    }

    /**
     * 汇总当前局里已经公开的所有牌，供半甩和 AI 做公开信息判断。
     */
    public ArrayList<Card> buildPublicCards() {
        ArrayList<Card> publicCards = new ArrayList<>();
        for (PlayedTrick trick : completedTricks) {
            for (PlayedHand playedHand : trick.getPlays()) {
                publicCards.addAll(playedHand.getCards());
            }
        }
        for (PlayedHand playedHand : currentTrick) {
            publicCards.addAll(playedHand.getCards());
        }
        publicCards.addAll(publicRevealCards);
        return publicCards;
    }

    /**
     * 按牌值和花色统计公开牌张数。
     */
    public HashMap<String, Integer> buildPublicCardCountMap() {
        HashMap<String, Integer> counts = new HashMap<>();
        for (Card card : buildPublicCards()) {
            String key = card.getValue() + "_" + card.getColor().name();
            Integer count = counts.get(key);
            int next = count == null ? 1 : count + 1;
            counts.put(key, Math.min(2, next));
        }
        return counts;
    }
}
