# TakServer
Server to handle online TAK games

The input/output of server is all text.
When the client connects to server, it gets a "welcome!" message and a prompt for Name.
When the client provides the name in the format specified in the table below, the server sends another welcome
message. Until the name is given, client cannot send any other command except quit.

The client to server commands and their format is as below
(format of all squares is [Capital letter][digit]. e.g., A2, B5, C4)

|Commands to server|Description|
|-----------------|-----------|
|Name **text**      |Sets player name to **text**|
|List             |Gives list of all games|
|Seek **no** |Seeks a game of board size *no*|
|Accept **no**|Accepts the game with the game number **no**|
|Game#**no** P **Sq** C\|W|Sends a 'Place' move to the specified game no. The optional suffix 'C' or 'W' denote if it is a capstone or a wall (standing stone)|
|Game#**no** M **Sq1** **Sq2** **no1** **no2**...|Sends a 'Move' move to the specified game no. **Sq1** is beginning square, **Sq2** is ending square, **no1**, **no2**, **no3**.. are the no. of pieces dropped in the in-between squares (including the last square)|
|Game#**no** Show |Gives the full game state of game number **no**|
|quit |Sent by client to indicate it is going to quit. Server removes all seeks, abandons game if any|

The server to client messages and their format is as below

|Messages from server|Description|
|--------------------|-----------|
|Name? **text**      |Server prompts the client for its name in this way. **text** is info such as length of name, characters accepted, or if previously provided name was already taken|
|Game Start **no** **size** **player_white** **player_black** **your color**|Notifies client that a game has started. The game no. being **no**, players' names being **white_player**, **black_player** and **your_color** being your color which could be either "white" or "black"|
|Game#**no** P **Sq** C\|W|The 'Place' move played by the other player in game number **no**. The format is same as the command from client to server|
|Game#**no** M **Sq1** **Sq2** **no1** **no2**...|The 'Move' move played by the other player in game number **no**. The format is same as the command from client to server|
|Game#**no** over **result**|Game number **no** is over. **result** is one of *R-0*, *0-R*, *F-0*, *0-F*, *1/2-1/2*|
|Game#**no** Abandoned|Game number *no** is abandoned by the opponent as he quit. Clients can treat this as resign.|
|Seek new **no** **name** **boardsize**|There is a new seek with seek no. **no** posted by **name** with board size **boardsize**|
|Seek remove **no** **name** **boardsize**|Existing seek no. **no** is removed (either the client has joined another game or has changed his seek or has quit)|
|Message **text** |A generic message from server. Might be used to indicate announcements like name accepted/server going down, etc|
|Online **no** |**no** players are connected to server|
|NOK |Indicates the command client send is invalid or unrecognized|
|OK  |Indicates previous command is ok. Clients can ignore this. I might remove this message altogether in future as it serves no real purpose|

More commands or messages might be added for any upcoming features such as spectating games, chatting with opponents, etc

##Info for Client developers
Stand alone clients can connect directly to playtak.com at port 10000.
<br>
Web clients wanting to use websockets can connect to playtak.com at port 80.
<br>
https://github.com/kanaka/websockify is running on port 80 and it forwards websocket traffic to port 10000. HTTP traffic is served with default webclient.
<br>
**telnet to playtak.com on port 10000 to test the commands.**

Typical communication is like below
* Connect to server. Server gives welcome message
* Server asks for name
* Client replies with name
* Server accepts name or asks for another name if existing one is invalid or already taken
* Client asks for list of seeks
* Server responds with "Seek new" messages for all the seeks
* Client posts seek or accepts existing seek
* If seek is accepted, server removes existing seeks for both the players (sends "Seek remove" for all) and starts game
* Client sends moves, server validates moves and sends the move to other client. If invalid, server sends a "NOK" message.
* Game progresses and ends either in a win/lose/draw or abandonment.
