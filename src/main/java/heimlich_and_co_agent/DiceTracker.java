package heimlich_and_co_agent;

import heimlich_and_co.enums.Agent;
import java.util.Map;

/**
 * DiceTracker monitors how players distribute movement points across agents.
 * It stores pre-computed totals for instant retrieval and full history for strategy analysis.
 */
public class DiceTracker {
  private static final int NUM_AGENTS = 7; // Always 7 figurines in the game
  private static final int MAX_TURNS = 100;

  /**
   * 3D Array: [PlayerID][AgentID][TurnIndex]
   * Preserves the full chronological history for analyzing strategy patterns.
   */
  private final int[][][] movementHistory;

  /**
   * 2D Array: [PlayerID][AgentID]
   * Stores pre-computed total points invested for instant O(1) retrieval.
   */
  private final int[][] totalPointsPerAgent;

  private final int[] turnCounters;
  private final int numPlayers;

  /**
   * @param numPlayers Total players from game.getNumberOfPlayers().
   */
  public DiceTracker(int numPlayers) {
    this.numPlayers = numPlayers;
    this.movementHistory = new int[numPlayers][NUM_AGENTS][MAX_TURNS];
    this.totalPointsPerAgent = new int[numPlayers][NUM_AGENTS];
    this.turnCounters = new int[numPlayers];
  }

  /**
   * Records movement and updates pre-computed totals.
   * @param playerID The ID of the player making the move.
   * @param agentMoves Map of Agents to points spent.
   */
  public void recordTurnMovement(int playerID, Map<Agent, Integer> agentMoves) {
    if (playerID < 0 || playerID >= numPlayers) return;

    int currentTurn = turnCounters[playerID];
    if (currentTurn >= MAX_TURNS) return;

    for (Agent agent : Agent.values()) {
      int agentIndex = agent.ordinal();
      int pointsAssigned = agentMoves.getOrDefault(agent, 0);

      // 1. Store in history for strategy analysis
      movementHistory[playerID][agentIndex][currentTurn] = pointsAssigned;

      // 2. Update pre-computed totals for instant retrieval
      totalPointsPerAgent[playerID][agentIndex] += pointsAssigned;
    }

    turnCounters[playerID]++;
  }

  /**
   * Returns the total points invested by a player into a figurine.
   * This is called directly without any calculation at runtime.
   */
  public int getTotalInvestedPoints(int playerID, Agent agent) {
    if (playerID < 0 || playerID >= numPlayers) return 0;
    return totalPointsPerAgent[playerID][agent.ordinal()];
  }

  /**
   * Now optimized: Uses pre-computed totals for O(1) performance.
   */
  public double getAverageMovement(int playerID, Agent agent) {
    if (playerID < 0 || playerID >= numPlayers) return 0.0;
    int turns = turnCounters[playerID];
    if (turns == 0) return 0.0;

    return (double) totalPointsPerAgent[playerID][agent.ordinal()] / turns;
  }

  /**
   * Allows accessing the raw history for strategic analysis
   * (e.g., checking for burst moves vs. steady pushing).
   */
  public int[] getMovementHistoryForAgent(int playerID, Agent agent) {
    if (playerID < 0 || playerID >= numPlayers) return new int[0];
    return movementHistory[playerID][agent.ordinal()];
  }

  public int getTurnCount(int playerID) {
    if (playerID < 0 || playerID >= numPlayers) return 0;
    return turnCounters[playerID];
  }
}