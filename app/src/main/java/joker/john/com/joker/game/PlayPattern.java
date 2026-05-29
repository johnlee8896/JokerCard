package joker.john.com.joker.game;

import java.io.Serializable;

public class PlayPattern implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        INVALID,
        SINGLE,
        PAIR,
        TRACTOR,
        MIXED
    }

    private final Type type;
    private final int group;
    private final int cardCount;
    private final int pairCount;
    private final int topSequence;
    private final String message;

    public PlayPattern(Type type, int group, int cardCount, int pairCount, int topSequence, String message) {
        this.type = type;
        this.group = group;
        this.cardCount = cardCount;
        this.pairCount = pairCount;
        this.topSequence = topSequence;
        this.message = message;
    }

    public static PlayPattern invalid(String message) {
        return new PlayPattern(Type.INVALID, PlayValidator.GROUP_MIXED, 0, 0, -1, message);
    }

    public Type getType() {
        return type;
    }

    public int getGroup() {
        return group;
    }

    public int getCardCount() {
        return cardCount;
    }

    public int getPairCount() {
        return pairCount;
    }

    public int getTopSequence() {
        return topSequence;
    }

    public String getMessage() {
        return message;
    }

    public boolean canBeatLead() {
        return type == Type.SINGLE || type == Type.PAIR || type == Type.TRACTOR;
    }
}
