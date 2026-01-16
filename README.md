This is the MCTS-based agent Detective Götzbach for the Heimlich&Co Boardgame

Creators: Luis Kolvenbach & Martin Götz

Progress: 
For now, we created the initial setup of the agent repository and did some brainstorming on how we want our agent to act.

Brainstorming:
We want our agent to act according to different strategies, depending on the game state. At the beginning of the game, until the first player reaches a certain threshold of points, the agent acts more randomly, immitating npc behaviour in order to not get recognized player. This timeframe is also already utilized to possibly identify the other agents character. After the threshold is reached, the agent switches his behaviour, getting more selfish, but still pushing other agents that were previously identified as likely being npcs, at least as much as the own character. If at a certain point winning seems very unlikely, our agent tries to push another npc character, in order to force a draw (draw is better than loss)

Detecting other agents: Each time the competing player distributes points to the game characters, we calculate the possiblities of each character being the one of the competing agent, based on how many points were distributed to a certain character (higher point distribution means higher likelyhood). This score is averaged across all rounds, becoming more accurate with increasing game progression (assuming that the competing agent has the tendency to give, on average, more points to his own character, which is necessary in order to win). 

Keeping Track of the Cards: In order to use the cards in the most effective way possibe we have to keep track of them and then consider more likely acctions in our decission making (eg. a player is most likely to defent itself afther beeing moved to the ruin). Since there is a finite amout it is possibe to keep track of them and record played cards and consider (some) more likely possibilities. Also our agent should consider the total amount of cards currently in the game before using his own, as reacting on an opponents card is always better then playing first with the opponent having the ability to counter. The agent should also adapt his playstyle based on their own cards, to utilize them as well as possible. 
