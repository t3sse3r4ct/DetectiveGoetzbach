package heimlich_and_co_agent;

import java.util.*;
import heimlich_and_co.cards.HeimlichAndCoCard;
import heimlich_and_co.factories.HeimlichAndCoCardStackFactory;
import heimlich_and_co.util.CardStack;

public class CardTracker {
  private final List<HeimlichAndCoCard> totalCardsInGame;
  private final List<HeimlichAndCoCard> graveyard; // Played cards
  private final int[] playerCardCounts;

  // Constants based on Section 6 of the game instructions
  private static final int INITIAL_CARDS_PER_PLAYER = 2;
  private static final int MAX_CARDS_PER_PLAYER = 4;

  public CardTracker(int numPlayers) {
    // Use the factory to get the standard set of 25 implemented cards
    CardStack<HeimlichAndCoCard> initialStack = HeimlichAndCoCardStackFactory.newInstance();
    this.totalCardsInGame = new ArrayList<>();

    // Extract all cards from the stack into a list to know the "universe" of cards
    while (initialStack.count() > 0) {
      totalCardsInGame.add(initialStack.drawCard());
    }

    this.graveyard = new ArrayList<>();
    this.playerCardCounts = new int[numPlayers];

    // Corrected: Section 6 states "At the beginning of the game, each player receives two cards"
    Arrays.fill(playerCardCounts, INITIAL_CARDS_PER_PLAYER);
  }

  /**
   * Tracks when a player gains a card.
   * Per Section 6, this happens when moving to ruins or choosing 0 move points on a '1-3' roll.
   */
  public void recordCardGained(int playerID) {
    // Section 6: "A player can have 4 cards at most on hand."
    if (playerCardCounts[playerID] < MAX_CARDS_PER_PLAYER) {
      playerCardCounts[playerID]++;
    }
  }

  /**
   * Tracks when a player plays a card.
   * Per Section 5.1, cards are removed from the game once played.
   */
  public void recordCardPlayed(int playerID, HeimlichAndCoCard card) {
    graveyard.add(card);
    if (playerCardCounts[playerID] > 0) {
      playerCardCounts[playerID]--;
    }
  }

  /**
   * Calculates the pool of hidden cards for MCTS determinization.
   * Pool = Total - Graveyard - Agent's Own Hand.
   */
  public List<HeimlichAndCoCard> getHiddenPool(List<HeimlichAndCoCard> ownCards) {
    List<HeimlichAndCoCard> hiddenPool = new ArrayList<>(totalCardsInGame);
    for (HeimlichAndCoCard played : graveyard) {
      hiddenPool.remove(played);
    }
    for (HeimlichAndCoCard own : ownCards) {
      hiddenPool.remove(own);
    }
    return hiddenPool;
  }

  public int getPlayerCardCount(int playerID) {
    return playerCardCounts[playerID];
  }


  /**
   * Returns a copy of the list of all cards that were initialized at the start of the game.
   * This is used by the agent to identify which cards are played by opponents.
   */
  public List<HeimlichAndCoCard> getTotalCardsInGame() {
    // Returning a new list copy is safer to prevent accidental modification
    // of the tracker's internal "universe" list.
    return new ArrayList<>(totalCardsInGame);
  }
}