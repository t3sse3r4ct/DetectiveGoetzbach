package heimlich_and_co_agent;

import heimlich_and_co.enums.Agent;
import java.util.Arrays;

public class IdentityTracker {
  private final int numPlayers;
  private final DiceTracker diceTracker;

  // The "Suspicion Meter" [PlayerIndex][AgentIndex]
  private final double[][] suspicionMatrix;

  public IdentityTracker(int numPlayers, DiceTracker diceTracker) {
    this.numPlayers = numPlayers;
    this.diceTracker = diceTracker;
    this.suspicionMatrix = new double[numPlayers][7];
  }

  /**
   * Updates the suspicion scores.
   * Logic: Instead of a flat average, we can weight later turns more heavily
   * to detect the "selfish" switch described in the README.
   */
  public void calculateSuspicion() {
    for (int p = 0; p < numPlayers; p++) {
      int totalPointsByPlayer = 0;
      for (Agent agent : Agent.values()) {
        totalPointsByPlayer += diceTracker.getTotalInvestedPoints(p, agent);
      }

      if (totalPointsByPlayer == 0) continue;

      for (Agent agent : Agent.values()) {
        int agentIdx = agent.ordinal();
        // Basic probability: what % of moves were for this agent?
        double basicProb = (double) diceTracker.getTotalInvestedPoints(p, agent) / totalPointsByPlayer;

        // Advanced: check the 3D history for "Burst" behavior
        int[] history = diceTracker.getMovementHistoryForAgent(p, agent);
        double burstModifier = calculateBurstFactor(history);

        suspicionMatrix[p][agentIdx] = basicProb * burstModifier;
      }

      // Normalize the row so probabilities sum to 1.0
      normalizeRow(p);
    }
  }

  /**
   * Detects if an agent was pushed "a lot at a time" (Burst Strategy)
   * as mentioned in your 3D array request.
   */
  private double calculateBurstFactor(int[] history) {
    // High values in recent turns significantly increase suspicion
    double factor = 1.0;
    int recentTurnsToCheck = 5;
    for (int i = history.length - 1; i >= 0 && i >= history.length - recentTurnsToCheck; i--) {
      if (history[i] > 3) factor += 0.2; // They spent a lot of points at once
    }
    return factor;
  }

  private void normalizeRow(int playerIndex) {
    double sum = 0;
    for (double val : suspicionMatrix[playerIndex]) sum += val;
    if (sum > 0) {
      for (int i = 0; i < 7; i++) suspicionMatrix[playerIndex][i] /= sum;
    }
  }

  public double getSuspicion(int playerID, Agent agent) {
    return suspicionMatrix[playerID][agent.ordinal()];
  }
}