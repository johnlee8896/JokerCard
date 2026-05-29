package joker.john.com.joker.game;

import java.io.Serializable;

public class GameEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        DEAL,
        HINT_READY,
        PLAY_ACCEPTED,
        TRICK_WON,
        ROUND_FINISHED,
        SETTINGS_SAVED
    }

    private final Type type;
    private final int player;
    private final String message;

    public GameEvent(Type type, int player, String message) {
        this.type = type;
        this.player = player;
        this.message = message;
    }

    public Type getType() {
        return type;
    }

    public int getPlayer() {
        return player;
    }

    public String getMessage() {
        return message;
    }
}
