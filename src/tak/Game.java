/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

import java.util.ArrayList;

/**
 *
 * @author chaitu
 */
public class Game {

    ClientConnection connection1;
    ClientConnection connection2;

    int no;
    int boardSize;
    int moveCount;
    ArrayList[][] board;

    static int DEFAULT_SIZE = 4;

    static int gameNo = 0;

    Game(ClientConnection c1, ClientConnection c2, int b) {
        connection1 = c1;
        connection2 = c2;

        if (b != 4 && b != 5 && b != 6 && b != 8) {
            b = DEFAULT_SIZE;
        }

        boardSize = b;
        moveCount = 0;
        board = new ArrayList[boardSize][boardSize];
        no = ++gameNo;

        for (int i = 0; i < b; i++) {
            for (int j = 0; j < b; j++) {
                board[i][j] = new ArrayList();
            }
        }
    }

    Game(ClientConnection c1, ClientConnection c2) {
        this(c1, c2, DEFAULT_SIZE);
    }

    boolean move(String cmd[], ClientConnection c) {
        if (cmd.length >= 3) {
            switch (cmd[2]) {
                case "P":
                    if (cmd.length == 4 || cmd.length == 5) {
                        int file = cmd[3].charAt(0) - 'A';
                        int rank = cmd[3].charAt(1) - '0';
                    }
                    break;
                case "M":
                    break;
            }
        }
        return false;
    }
}
