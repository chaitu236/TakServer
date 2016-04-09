# TakServer

*Last updated on 04/08/2016*

Server to handle online TAK games

The input/output of server is all text.

The client to server commands and their format is as below
(format of all squares is [Capital letter][digit]. e.g., A2, B5, C4, (row numbers start from 1)

**Since the server and client are still in beta, the API is bound to change to support more features (though I would try to keep the changes to a minimum)**

|Commands to server|Description|
|-----------------|-----------|
|Client **client name**      |Informs the server of the client being connected from|
|Register **username email** |Register with the given username and email|
|Login **username password** |Login with the username and password|
|Login Guest |Login as a guest|
|Seek **no** **time** |Seeks a game of board size **no** with time per player **time** specified in seconds|
|Accept **no** |Accepts the seek with the number **no**|
|Game#**no** P **Sq** C\|W |Sends a 'Place' move to the specified game no. The optional suffix 'C' or 'W' denote if it is a capstone or a wall (standing stone)|
|Game#**no** M **Sq1** **Sq2** **no1** **no2**...|Sends a 'Move' move to the specified game no. **Sq1** is beginning square, **Sq2** is ending square, **no1**, **no2**, **no3**.. are the no. of pieces dropped in the in-between squares (including the last square)|
|Game#**no** OfferDraw |Offers the opponent draw or accepts the opponent's draw offer|
|Game#**no** RemoveDraw |Removes your draw offer|
|Game#**no** Resign |Resign the game|
|Game#**no** Show |Prints a somewhat human readable game position of the game number **no**|
|List |Send list of seeks|
|GameList |Send list of games in progress|
|Observe **no** |Observe the specified game. Server sends the game moves and clock info|
|Unobserve **no** |Unobserve the specified game|
|Game#**no** Show **Sq** |Prints the position in the specified square (this is used mainly to convert server notation to PTN notation)|
|Shout **text** |Send text to all logged in players|
|Ping |Pings to inform server that the client is alive. Recommended ping spacing is 30 seconds. Server may disconnect clients if pings are not received|
|quit |Sent by client to indicate it is going to quit. Server removes all seeks, abandons (which loses) game if any|

The *Client*, *Login* and *Register* are the only three commands which work while not logged in.

The server to client messages and their format is as below.
The list does not include error messages, you're free to poke around and figure out the error messages on your own or look at the code.

|Messages from server|Description|
|--------------------|-----------|
|Welcome! |Just a welcome message when connected to server|
|Login or Register |Login with username/password or login as guest or register after this message|
|Welcome **name**! |A welcome message indicating that you've logged in as **name**|
|GameList Add Game#**no** **player_white** vs **player_black**, **size**x**size**, **original_time**, **moves** half-moves played, **player_name** to move |Notifies client that a game has started (which the client can observe if it wants)|
|Game Start **no** **size** **player_white** **player_black** **your color** |Notifies client to start a game. The game no. being **no**, players' names being **white_player**, **black_player** and **your_color** being your color which could be either "white" or "black"|
|Game#**no** P **Sq** C\|W|The 'Place' move played by the other player in game number **no**. The format is same as the command from client to server|
|Game#**no** M **Sq1** **Sq2** **no1** **no2**...|The 'Move' move played by the other player in game number **no**. The format is same as the command from client to server|
|Game#**no** Time **whitetime** **blacktime** |Update the clock with the time specified for white and black players|
|Game#**no** over **result**|Game number **no** is over. **result** is one of *R-0*, *0-R*, *F-0*, *0-F*, *1/2-1/2*|
|Game#**no** Abandoned|Game number **no** is abandoned by the opponent as he quit. Clients can treat this as resign.|
|Seek new **no** **name** **boardsize** **time** |There is a new seek with seek no. **no** posted by **name** with board size **boardsize** with **time** seconds for each player|
|Seek remove **no** **name** **boardsize** **time** |Existing seek no. **no** is removed (either the client has joined another game or has changed his seek or has quit)|
|Observe Game#**no** **player_white** vs **player_black**, **size**x**size**, **original_time**, **moves** half-moves played, **player_name** to move| Start observing the game number **no** of board size **size** with original time setting of **origin_time** seconds where **moves** half-moves are played and it is **player_name**'s turn to move|
|Shout \<**player**\> **text** |Chat message from **player**|
|Message **text** |A message from server. Might be used to indicate announcements like name accepted/server going down, etc|
|Error **text** |An error message|
|Online **no** |**no** players are connected to server|
|NOK |Indicates the command client send is invalid or unrecognized|
|OK  |Indicates previous command is ok. Clients can ignore this. *I might remove this message altogether in future as it serves no real purpose*|

##Info for Client developers
Stand alone clients can connect directly to playtak.com at port 10000 (but this communication will not be encrypted)
<br>
The defalut Web client runs on playtak.com port 80/443.
<br>
**telnet to playtak.com on port 10000 to test the commands.**

Typical communication is like below
* Connect to server. Server gives welcome message
* Server sends "Login or Register"
* Client replies with login information or registers (If Client registers, password is sent to the mail and it can login with the password)
* Server accepts name or asks for another name if the one provided is invalid or already taken
* Server sends list of seeks with "Seek new" messages and games in progress with "GameList Add" messages
* Client posts seek or accepts existing seek
* If seek is accepted, server removes existing seeks for both the players (sends "Seek remove" for all) and starts game
* Client sends moves, server validates moves and sends the move to other client. If invalid, server sends a "NOK" message.
* Game progresses and ends either in a win/lose/draw or abandonment.
