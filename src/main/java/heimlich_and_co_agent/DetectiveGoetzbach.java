package heimlich_and_co_agent;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.ActionRecord;
import at.ac.tuwien.ifs.sge.util.pair.Pair;
import heimlich_and_co.HeimlichAndCo;
import heimlich_and_co.HeimlichAndCoBoard;
import heimlich_and_co.actions.HeimlichAndCoAction;
import heimlich_and_co.actions.HeimlichAndCoAgentMoveAction;
import heimlich_and_co.actions.HeimlichAndCoCardAction;
import heimlich_and_co.cards.HeimlichAndCoCard;
import heimlich_and_co.enums.Agent;
import heimlich_and_co_mcts_agent.HeimlichAndCoMCTSAgent;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class DetectiveGoetzbach extends AbstractGameAgent<HeimlichAndCo, HeimlichAndCoAction> implements GameAgent<HeimlichAndCo, HeimlichAndCoAction> {

    /**
     * determines the depth of termination for random playouts
     * can be set to -1 to always play out till the game ends
     */
    private static final int TERMINATION_DEPTH = 64;

    // Trackers for rational decision-making
    private DiceTracker diceTracker;
    private CardTracker cardTracker;


    // Tracks the board state incrementally to compare positions
    private HeimlichAndCoBoard trackedBoard;
    // Ensures we only process each ActionRecord once
    private int lastProcessedActionIndex = 0;
    // Suspicion Meter
    private IdentityTracker identityTracker;

    private HeimlichAndCoMCTSAgent MCTSAgent;

    /**
     * Determines the strategy for dealing with the randomness of a die roll.
     * <p>
     * True means that all possible outcomes of the die roll will be simulated. Meaning that for each die roll, 6 child
     * states will be added (one for each outcome). The selection can then be done by choosing a random outcome each
     * time the algorithm comes to a "die roll node".
     * <p>
     * False means that when first visiting the "die roll node", a random outcome will be chosen, and that is the only
     * outcome that is considered in that tree.
     */
    private static final boolean SIMULATE_ALL_DIE_OUTCOMES = true;

    public DetectiveGoetzbach(Logger logger) {
        super(logger);
        MCTSAgent = new HeimlichAndCoMCTSAgent(logger);
    }

    @Override
    public HeimlichAndCoAction computeNextAction(HeimlichAndCo game, long l, TimeUnit timeUnit) {

        // Initialize trackers on the first turn
        if (diceTracker == null) {
            int numPlayers = game.getNumberOfPlayers();
            diceTracker = new DiceTracker(numPlayers);
            cardTracker = new CardTracker(numPlayers);

            //Initialization of trackedBoard
            this.trackedBoard = new HeimlichAndCoBoard();

            log.inf("MctsAgent: Trackers initialized for " + numPlayers + " players.\n");
        }

        log.deb("MctsAgent: Computing next action\n");
        super.setTimers(l, timeUnit);

        Set<HeimlichAndCoAction> possibleActions = game.getPossibleActions();
        if (possibleActions.size() == 1) {
            return game.getPossibleActions().iterator().next();
        }

        // 1. Initialize memory monitoring
        Runtime runtime = Runtime.getRuntime();
        long memoryThreshold = (long) (runtime.maxMemory() * 0.90);

        try {
            log.deb("MctsAgent: Adding information to the game");
            addInformationToGame(game);

            //if less then 10 rolls in the game, we perform a random action with the MCTS Agent
            if(diceTracker.getNumbRolls() < 10){
                return MCTSAgent.computeNextAction(game,l,timeUnit);
            }

            if (SIMULATE_ALL_DIE_OUTCOMES) {
                game.setAllowCustomDieRolls(true);
            }
            MCTSNode.setPlayerId(this.playerId);
            MCTSNode tree = new MCTSNode(0, 0, game, null);
            log.deb("MctsAgent: Doing MCTS");
            // 2. The combined safety loop
            while (!this.shouldStopComputation()) {

                // Memory Safety Check: Stop if used memory exceeds 90%
                if (runtime.totalMemory() - runtime.freeMemory() > memoryThreshold) {
                    log.inf("MctsAgent: Memory threshold reached! Stopping search early.\n");
                    break;
                }

                Pair<MCTSNode, HeimlichAndCoAction> selectionPair = mctsSelection(tree, SIMULATE_ALL_DIE_OUTCOMES);
                MCTSNode newNode = mctsExpansion(selectionPair.getA(), selectionPair.getB());
                double reward = mctsSimulation(newNode);
                mctsBackpropagation(newNode, reward);
            }

            log.inf("MctsAgent: Playouts done from root node: " + tree.getPlayouts() + "\n");
            log.inf("MctsAgent: Wins/playouts from selected child node: " + tree.getBestChild().getA().getWins() + "/" + tree.getBestChild().getA().getPlayouts() + "\n");
            log.inf("MctsAgent: Q(s,a) of chosen action: " + tree.calculateQsaOfChild(tree.getBestChild().getB()) + "\n");
            return tree.getBestChild().getB();

        } catch (Exception ex) {
            log.err(ex);
            log.err("MctsAgent: An error occurred while calculating the best action. Playing a random action.\n");
        }
        //If an exception is encountered, we play a random action s.t. we do not automatically lose the game
        HeimlichAndCoAction[] actions = game.getPossibleActions().toArray(new HeimlichAndCoAction[0]);
        return actions[super.random.nextInt(actions.length)];
    }

    /**
     * Adds information that was removed by the game (i.e. hidden information).
     * Therefore, adds entries to the map which maps agents to players and entries to the map mapping the cards of players.
     * The agents are randomly assigned to players. And players are assumed to have no cards.
     *
     * @param game to add information to
     */

    /*
    private void addInformationToGame(HeimlichAndCo game) {
        //we need to determinize the tree, i.e. add information that is secret that the game hid from us
        //here we just guess
        Map<Integer, Agent> playersToAgentsMap = game.getPlayersToAgentsMap();
        List<Agent> unassignedAgents = Arrays.asList(game.getBoard().getAgents());
        unassignedAgents = new LinkedList<>(unassignedAgents);
        unassignedAgents.remove(playersToAgentsMap.get(this.playerId));
        for (int i = 0; i < game.getNumberOfPlayers(); i++) {
            if (i == this.playerId) {
                continue;
            }
            Agent chosenAgent = unassignedAgents.get(super.random.nextInt(unassignedAgents.size()));
            playersToAgentsMap.put(i, chosenAgent); //choose a random agent
            unassignedAgents.remove(chosenAgent);
            if (game.isWithCards()) {
                game.getCards().put(i, new LinkedList<>()); //other players do not get cards for now
            }
        }
    }

     */

    /**
     * Determinizes the game state by assigning identities and cards based on suspicion.
     */
    private void addInformationToGame(HeimlichAndCo game) {
        // Synchronize trackers with latest game events
        syncTrackers(game);

        // Update the identity suspicion scores based on the new sync data
        if (identityTracker == null) {
            identityTracker = new IdentityTracker(game.getNumberOfPlayers(), diceTracker);
        }
        identityTracker.calculateSuspicion();

        Map<Integer, Agent> playersToAgentsMap = game.getPlayersToAgentsMap();
        List<Agent> availableAgents = new LinkedList<>(Arrays.asList(game.getBoard().getAgents()));

        // Remove our own agent from the assignment pool
        availableAgents.remove(playersToAgentsMap.get(this.playerId));

        // Greedy Identity Assignment: match the highest suspicion pairs first
        List<Integer> opponents = new ArrayList<>();
        for (int i = 0; i < game.getNumberOfPlayers(); i++) {
            if (i != this.playerId) opponents.add(i);
        }

        while (!opponents.isEmpty() && !availableAgents.isEmpty()) {
            int bestOpp = -1;
            Agent bestAgent = null;
            double maxScore = -1.0;

            for (int oppID : opponents) {
                for (Agent agent : availableAgents) {
                    double score = identityTracker.getSuspicion(oppID, agent);
                    if (score > maxScore) {
                        maxScore = score;
                        bestOpp = oppID;
                        bestAgent = agent;
                    }
                }
            }

            if (bestOpp != -1) {
                playersToAgentsMap.put(bestOpp, bestAgent);
                availableAgents.remove(bestAgent);
                opponents.remove(Integer.valueOf(bestOpp));
            }
        }

        // Rational Card Assignment using the hidden pool
        if (game.isWithCards()) {
            List<HeimlichAndCoCard> hiddenPool = cardTracker.getHiddenPool(game.getCards().get(this.playerId));
            Collections.shuffle(hiddenPool);

            for (int i = 0; i < game.getNumberOfPlayers(); i++) {
                if (i == this.playerId) continue;

                int handSize = cardTracker.getPlayerCardCount(i);
                List<HeimlichAndCoCard> guessedHand = new LinkedList<>();
                for (int k = 0; k < handSize && !hiddenPool.isEmpty(); k++) {
                    guessedHand.add(hiddenPool.remove(0));
                }
                game.getCards().put(i, guessedHand);
            }
        }
    }



    private void mctsBackpropagation(MCTSNode node, double reward) {
        log.deb("MctsAgent: In Backpropagation\n");
        node.backpropagation(reward);
    }

    private MCTSNode mctsExpansion(MCTSNode node, HeimlichAndCoAction action) {
        log.deb("MctsAgent: In Expansion\n");
        return node.expansion(action);
    }

    private Pair<MCTSNode, HeimlichAndCoAction> mctsSelection(MCTSNode node, boolean simulateAllDieOutcomes) {
        log.deb("MctsAgent: In Selection\n");
        return node.selection(simulateAllDieOutcomes);
    }

    /**
     * Does the simulation step of MCTS. This function is implemented here and not in the MctsNode as that makes it
     * easier to handle how much time there is (left) for computation before timing out.
     *
     * @param node from where simulation should take place
     * @return 1 or 0, depending on whether the agent belonging to the player of this agent wins
     */
    private double mctsSimulation(MCTSNode node) {
        log.deb("MctsAgent: In Simulation\n");
        HeimlichAndCo game = new HeimlichAndCo(node.getGame());
        //use a termination depth were the game is evaluated and stopped
        int simulationDepth = 0;
        while (!game.isGameOver() && !this.shouldStopComputation()) {
            if (TERMINATION_DEPTH >= 0 && simulationDepth >= TERMINATION_DEPTH) {
                break;
            }
            Set<HeimlichAndCoAction> possibleActions = game.getPossibleActions();
            HeimlichAndCoAction selectedAction = possibleActions.toArray(new HeimlichAndCoAction[1])[super.random.nextInt(possibleActions.size())];
            game.applyAction(selectedAction);
            simulationDepth++;
        }

        Map<Agent, Integer> scores = game.getBoard().getScores();
        int myScore = scores.get(game.getPlayersToAgentsMap().get(this.playerId));

        // Option A: Simple Normalization (0.0 to 1.0)
        // 42 is the winning score in Heimlich & Co.
        double reward = (double) myScore / 42.0;

        // Ensure reward doesn't exceed 1.0 if someone goes slightly over 42
        return Math.min(1.0, reward);
    }


    /**
     * Synchronizes internal trackers by analyzing the ActionRecord history.
     */
    private void syncTrackers(HeimlichAndCo game) {
        List<ActionRecord<HeimlichAndCoAction>> records = game.getActionRecords();

        for (int i = lastProcessedActionIndex; i < records.size(); i++) {
            ActionRecord<HeimlichAndCoAction> record = records.get(i);
            int playerID = record.getPlayer();
            HeimlichAndCoAction action = record.getAction();

            // 1. Deducing figurine movement
            if (action instanceof HeimlichAndCoAgentMoveAction) {
                HeimlichAndCoAgentMoveAction move = (HeimlichAndCoAgentMoveAction) action;

                // Capture positions BEFORE applying the action
                Map<Agent, Integer> posBefore = new HashMap<>(trackedBoard.getAgentsPositions());

                // Rule 6: Track card gains (ruins or "No Move" on 1-3 roll)
                if (move.movesAgentsIntoRuins(trackedBoard) || move.isNoMoveAction()) {
                    cardTracker.recordCardGained(playerID);
                }

                // Apply the move to see the new positions
                action.applyAction(trackedBoard);
                Map<Agent, Integer> posAfter = trackedBoard.getAgentsPositions();

                // Compare positions to find the exact moves made
                Map<Agent, Integer> deducedMoves = new HashMap<>();
                int numFields = trackedBoard.getNumberOfFields();
                for (Agent agent : posBefore.keySet()) {
                    int dist = (posAfter.get(agent) - posBefore.get(agent) + numFields) % numFields;
                    if (dist > 0) deducedMoves.put(agent, dist);
                }

                // Update DiceTracker with the deduced movement
                diceTracker.recordTurnMovement(playerID, deducedMoves);

            }
            // 2. Identifying played cards
            else if (action instanceof HeimlichAndCoCardAction) {
                HeimlichAndCoCardAction cardAction = (HeimlichAndCoCardAction) action;

                if (!cardAction.isSkipCardAction()) {
                    // Identification via a temporary list
                    List<HeimlichAndCoCard> identificationList = new LinkedList<>(cardTracker.getTotalCardsInGame());
                    List<HeimlichAndCoCard> beforeRemoval = new LinkedList<>(identificationList);

                    // The engine's method will remove the specific card played from our list
                    cardAction.removePlayedCardFromList(identificationList);

                    if (identificationList.size() < beforeRemoval.size()) {
                        HeimlichAndCoCard playedCard = findMissingCard(beforeRemoval, identificationList);
                        cardTracker.recordCardPlayed(playerID, playedCard);
                    }
                }
                action.applyAction(trackedBoard);
            } else {
                // Keep the trackedBoard in sync for SafeMoves and DieRolls
                action.applyAction(trackedBoard);
            }
        }
        lastProcessedActionIndex = records.size();
    }


    private HeimlichAndCoCard findMissingCard(List<HeimlichAndCoCard> original, List<HeimlichAndCoCard> reduced) {
        List<HeimlichAndCoCard> temp = new LinkedList<>(original);
        for (HeimlichAndCoCard c : reduced) {
            temp.remove(c);
        }
        return temp.get(0);
    }


}
