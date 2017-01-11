Creating a human-like computer controlled player for Unreal Tournament 2004 using a Finite State Engine method



Abstract – This paper discusses the tools, methods and usage of an Unreal Tournament bot designed to emulate human behaviour. An overview of the development tools is discussed, as well as testing results and the conclusion that this current method was unsuccessful. It also discusses methods which could be used to build on and improve the humanness of this computer controlled player.



Introduction 

I plan to use the Pogamut [1] platform, which is a middleware platform designed to create virtual agents in game engines, to create a computer controlled player for the game Unreal Tournament 2004. I intend to create a bot that will navigate the level it is in, and intelligently decide a course of action based on it’s surroundings and current in-game events. I intend to make this bot behave in a human-like way, seeking health when low, turning to face attackers, jumping while shooting to avoid getting shot.


Background 

This problem has been explored through teams competing for the botprize  [2]. The botprize tasked developers to create computer controlled bots for Unreal Tournament 2004 which would be able to successfully fool a panel of judges into thinking that they were human players, rather than computer controlled. [3]
The first team to claim the major prize in the botprize competition, entered and won. This was done by creating a bot that was considered, by the judges, to be even more human-like than the human players being judged. [4]
One of the winning bots was designed by Mihai Polceanu to record the movement of other players at runtime, and mirror the moevements of other human players in certain circumstances, in essence borrowing their humanness. This was inspired by techniques used by salesmen where mimicing is used to make another person comfortable in conversation, and this idea was adapted into a gameplay setting. The mimicing is done with some delay, and without full fidelity so that the movements don’t seem obviously mirrored. It also incorporated some realistic dodging behaviour while under fire. [5]
The second bot to be victorious on this year was from team UT^2. This bot was designed to “tenaciously pursue specific opponents without regard for optimality” [6] They used a combination of modelling action on previously observed human behaviour, and developing battle behaviours through a process of neuroevolution, an artificial neural network based on the evolutionary process which decided combat tactics. They specifically placed constraints on this neural network to make sure that battle tactics would not become too perfect, and easily identified as non-human . [6]

I had intended to use some techniques from each of these examples in my project, but found myself unable to find out just how to implement them. My bot does pursue other players after they have left it’s field of view, however it’s attention can be gained by another player if the bot is shot or another threat comes into view.


AI Method and Tools

I decided to implement a Finite State Machine to control the behaviour of my bot. This was influenced by the Hunter Bot [7] archetype within the Pogamut platform, but re-written and tweaked to behave differently in it’s states. The Pogamut platform within Netbeans provided the environment in which to create and import code for controlling bots and to access their senses, and the GameBots tool allows for these instructions from within Pogamut Netbeans to be relayed to a bot within the game, and the bot’s senses and decisions to be relayed back to Pogamut. Pogamut provides libraries to control bot behaviour as well. 


Evaluation Method
 
In order to examine the perceived human-ness of my computer controlled player, I will ask friends to join a game of Unreal Tournament 2004 in DeathMatch mode, in which my bot will also be playing. I will ensure each player’s in game name is not known to other players, using names that bear no significance to the players themselves. This way, there will be no easy way for the bot to be identified among other players by name alone. 
I will then ask the participants to play through multiple matches of the game, and at the end casually ask which player they thought was not human, and note this down. If possible, I will then do this a second time, assigning new names to each player and using this second round as another set of data around the bot’s human-ness.
If the bot is not identified for 70% of the time, I will consider my bot’s ability to act in a human-like way to be successful. 





Results

Through informal play sessions with other human players, with my bot entering the match, the bot was identified as human each time. 
Over the course of three different sessions, each with different sets of players, I asked the players which of the in game players seemed the least human. Every time, they answered and correctly identified my bot as the non-human player.
In all cases, the bot was identified by other people through it’s behaviour while in combat. It would generally stay in place, and jump when firing. This was the single behviour that seemed to indicate that this player was not actually a human.



Discussion

The goal of creating a computer controlled player that could pass for a human player in a game of Unreal Tournament 2004 was not reached, and this happened for a few reasons.
Chief of these reasons was the way the bot had a very distinct and non-variable behaviour while engaged in combat. 
A human player moves around, while potentially jumping during combat, while my bot just jumped without any other form of movement which made it an obviously non-human player. The jumping behaviour while shooting was specifically added to emulate human behaviour, but it seems to have had the opposite effect when the character is always jumping in combat.
I might have been able to improve this by implementing a random element to make the character jump sometimes (and fine tune the jumping regularity through testing) which would make the behaviour seem less robotic. Some players don’t jump at all while in combat, or might only do so in very specific situations. For future projects, I will need to further analyse whichh situations players do jump in, potentially finding a way to survey the current set of players and adapt the bot’s jumping behaviour accordingly. It stands out if it is jumping while most other players aren’t.
As discussed earlier, after some observation during the play sessions, it was found that the bot was also identified by it’s tendency to stop moving around the map while in combat. Once locked on to a target, as long as it remained in view and within distance, it would jump in place and shoot at the player. It would follow if the other player moved too far away but otherwise would not. This made the bot’s non-humanness very obvious and was a large part of the failure of this bot to reach the goal set.

Testing in a different environment may have changed the outcome to a degree as well. I was only able to include my bot in play sessions with one or two other people in addition to myself, and I feel that the bot may have bbeen better able to blend in if more players were present. This still wouldn’t prevent the above issues from making the bot obvious to players, however.

My bot did not reach it’s goal of emulating human-like behaviour to a degree well enough to fool any percentage of players, let alone the 70% goal initially stated.
