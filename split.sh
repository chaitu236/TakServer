#!/bin/bash

#remove existing
rm players.db
rm games.db

#create db, tables
echo "CREATE TABLE players (id INT PRIMARY_KEY, name VARCHAR(20), password VARCHAR(50), email VARCHAR(50), r4 INT, r5 INT,r6 INT, r7 INT,r8 INT);" | sqlite3 players.db
echo "CREATE TABLE games (id INTEGER PRIMARY KEY, date INT, size INT, player_white VARCHAR(20), player_black VARCHAR(20), notation TEXT, result VARCAR(10));" | sqlite3 games.db

#now copy
echo "ATTACH 'database.db' AS SRC; INSERT INTO players SELECT * from SRC.players;" | sqlite3 players.db
echo "ATTACH 'database.db' AS SRC; INSERT INTO games SELECT * from SRC.games;" | sqlite3 games.db
