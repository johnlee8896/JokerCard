package joker.john.com.joker.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import joker.john.com.joker.Card;

public class PlayedHand implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int player;
    private final ArrayList<Card> cards;
    private final PlayPattern pattern;

    public PlayedHand(int player, List<Card> cards, PlayPattern pattern) {
        this.player = player;
        this.cards = new ArrayList<>();
        this.cards.addAll(cards);
        this.pattern = pattern;
    }

    public int getPlayer() {
        return player;
    }

    public ArrayList<Card> getCards() {
        return cards;
    }

    public PlayPattern getPattern() {
        return pattern;
    }
}
