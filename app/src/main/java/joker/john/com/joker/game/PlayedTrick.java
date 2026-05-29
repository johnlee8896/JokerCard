package joker.john.com.joker.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PlayedTrick implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int trickNumber;
    private final ArrayList<PlayedHand> plays;
    private final int winner;
    private final int points;

    public PlayedTrick(int trickNumber, List<PlayedHand> plays, int winner, int points) {
        this.trickNumber = trickNumber;
        this.plays = new ArrayList<>();
        this.plays.addAll(plays);
        this.winner = winner;
        this.points = points;
    }

    public int getTrickNumber() {
        return trickNumber;
    }

    public ArrayList<PlayedHand> getPlays() {
        return plays;
    }

    public int getWinner() {
        return winner;
    }

    public int getPoints() {
        return points;
    }
}
