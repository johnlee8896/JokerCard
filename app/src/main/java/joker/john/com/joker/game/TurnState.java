package joker.john.com.joker.game;

import java.io.Serializable;

public class TurnState implements Serializable {
    private static final long serialVersionUID = 1L;

    private int leadPlayer;
    private int currentPlayer;
    private PlayPattern leadPattern;
    private int pendingWinner = -1;
    private int pendingPoints;

    public TurnState(int leadPlayer, int currentPlayer) {
        this.leadPlayer = leadPlayer;
        this.currentPlayer = currentPlayer;
    }

    public int getLeadPlayer() {
        return leadPlayer;
    }

    public void setLeadPlayer(int leadPlayer) {
        this.leadPlayer = leadPlayer;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(int currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public PlayPattern getLeadPattern() {
        return leadPattern;
    }

    public void setLeadPattern(PlayPattern leadPattern) {
        this.leadPattern = leadPattern;
    }

    public int getPendingWinner() {
        return pendingWinner;
    }

    public void setPendingWinner(int pendingWinner) {
        this.pendingWinner = pendingWinner;
    }

    public int getPendingPoints() {
        return pendingPoints;
    }

    public void setPendingPoints(int pendingPoints) {
        this.pendingPoints = pendingPoints;
    }
}
