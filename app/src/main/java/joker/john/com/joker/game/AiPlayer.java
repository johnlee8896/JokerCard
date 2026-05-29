package joker.john.com.joker.game;

import java.util.ArrayList;

import joker.john.com.joker.Card;

public class AiPlayer {
    public ArrayList<Card> choosePlay(GameState state, int player) {
        ArrayList<Card> hand = state.getHands()[player];
        TurnState turnState = state.getTurnState();
        if (state.getCurrentTrick().isEmpty() || turnState.getLeadPattern() == null) {
            return PlayValidator.suggestLead(hand, state.getRuleConfig());
        }
        return PlayValidator.suggestFollow(hand, turnState.getLeadPattern(), state.getRuleConfig());
    }
}
