package heimlich_and_co_agent;

import heimlich_and_co.enums.Agent;
import java.util.Map;

/**
 * DiceTracker monitors how players distribute their movement points across agents.
 * It is initialized with the actual number of players in the current game session.
 */
public class DiceTracker {
  // There are always 7 agents (playing pieces) regardless of player count
  private static final int NUM_AGENTS = 7;
  // Maximum expected turns for a single game
  private static final int MAX_TURNS = 100;

  /**
   * 3D Array: [PlayerID][AgentID][TurnIndex]
   * Stores the exact movement points assigned.
   */
  private final int[][][] movementHistory;

  /**
   * Tracks the current turn index for each player.
   */
  private final int[] turnCounters;
  private final int numPlayers;


  //for correct initialization use the provided command block

  /*
  // Inside your agent class
private DiceTracker diceTracker;

// When the game starts or in the first call to computeNextAction
if (diceTracker == null) {
    int numPlayers = game.getNumberOfPlayers(); // Dynamic player count
    diceTracker = new DiceTracker(numPlayers);
}
   */


  /**
   * Constructor initialized with the specific number of players from the game.
   * @param numPlayers Total players (real + dummy) provided by game.getNumberOfPlayers().
   */
  public DiceTracker(int numPlayers) {
    this.numPlayers = numPlayers;
    // Memory is allocated based on the actual player count
    this.movementHistory = new int[numPlayers][NUM_AGENTS][MAX_TURNS];
    this.turnCounters = new int[numPlayers];
  }

  /**
   * Records how a player distributed points from their die roll.
   * @param playerID The ID of the player (0 to numPlayers - 1).
   * @param agentMoves Map of Agents to the points spent on them.
   */
  public void recordTurnMovement(int playerID, Map<Agent, Integer> agentMoves) {
    if (playerID < 0 || playerID >= numPlayers) {
      return;
    }

    int currentTurn = turnCounters[playerID];
    if (currentTurn >= MAX_TURNS) {
      return;
    }

    for (Agent agent : Agent.values()) {
      int agentIndex = agent.ordinal();
      int pointsAssigned = agentMoves.getOrDefault(agent, 0);
      movementHistory[playerID][agentIndex][currentTurn] = pointsAssigned;
    }

    turnCounters[playerID]++;
  }

  /**
   * Calculates the average points a player has spent on a specific agent.
   */
  public double getAverageMovement(int playerID, Agent agent) {
    if (playerID < 0 || playerID >= numPlayers) return 0.0;

    int agentIndex = agent.ordinal();
    int totalTurns = turnCounters[playerID];
    if (totalTurns == 0) return 0.0;

    int totalPoints = 0;
    for (int i = 0; i < totalTurns; i++) {
      totalPoints += movementHistory[playerID][agentIndex][i];
    }

    return (double) totalPoints / totalTurns;
  }

  public int getTurnCount(int playerID) {
    if (playerID < 0 || playerID >= numPlayers) return 0;
    return turnCounters[playerID];
  }
}