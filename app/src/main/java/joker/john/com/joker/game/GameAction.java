package joker.john.com.joker.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import joker.john.com.joker.Card;

public class GameAction implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        DEAL,
        PLAY,
        HINT,
        TRICK_END,
        ROUND_END,
        RESTART
    }

    private final Type type;
    private final int player;
    private final int trickNumber;
    private final ArrayList<Card> cards;
    private final String message;

    public GameAction(Type type, int player, int trickNumber, List<Card> cards, String message) {
        this.type = type;
        this.player = player;
        this.trickNumber = trickNumber;
        this.cards = new ArrayList<>();
        if (cards != null) {
            this.cards.addAll(cards);
        }
        this.message = message;
    }

    public Type getType() {
        return type;
    }

    public int getPlayer() {
        return player;
    }

    public int getTrickNumber() {
        return trickNumber;
    }

    public ArrayList<Card> getCards() {
        return cards;
    }

    public String getMessage() {
        return message;
    }
}
