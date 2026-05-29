package joker.john.com.joker.game;

import java.io.Serializable;
import java.util.ArrayList;

import joker.john.com.joker.Card;

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
}
