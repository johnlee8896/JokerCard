package joker.john.com.joker.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 完整牌墩结果，包含四家出牌与收分。
 * 创建时间：2026-06-02
 * 最近修改：2026-06-02
 * by john
 */
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

    public PlayedHand getWinningHand() {
        for (PlayedHand playedHand : plays) {
            if (playedHand.getPlayer() == winner) {
                return playedHand;
            }
        }
        return plays.isEmpty() ? null : plays.get(0);
    }
}
