package joker.john.com.joker.game;

import java.io.Serializable;

public enum GamePhase implements Serializable {
    REVEAL_TRUMP,
    BURY_KITTY,
    PLAYING,
    ROUND_END
}
