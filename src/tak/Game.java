/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author chaitu
 */
public class Game {

    Player white;
    Player black;
    long time;

    int no;
    
    //time in milli seconds
    long originalTime;
    long incrementTime;
    long whiteTime;
    long blackTime;
    long lastUpdateTime;
    boolean timerStarted;
    Timer timer;
    Timer disconnectionTimer;
    
    Player drawOfferedBy;
    Player undoRequestedBy;
    
    boolean abandoned;
    
    Set<Player> spectators;
    
    public enum gameS {WHITE_ROAD, BLACK_ROAD, WHITE_TILE, BLACK_TILE, DRAW,
                        WHITE, BLACK, ABORT, NONE};
    List<String> moveList;
    gameS gameState;
    
    static Map<Integer, Game> games=Collections.synchronizedMap(new HashMap<Integer, Game>());
    static Set<Player> gameListeners = Collections.synchronizedSet(new HashSet<Player>());
    
    public static int reconnectionTime;
    
    class Board {
        int boardSize;
        int moveCount;
        int whiteCapstones;
        int blackCapstones;

        int whiteTilesCount;
        int blackTilesCount;
        
        class Square {
            private int file, row;
            private ArrayList<Character> stack;
            int graphNo;

            Square(int f, int r) {
                stack = new ArrayList<>();
                file = f;
                row = r;
                graphNo = -1;
            }

            @Override
            public Square clone() {
                Square sq = new Square(file, row);
                sq.graphNo = graphNo;
                for(Character c: stack) {
                    sq.add(c);
                }
                return sq;
            }
            boolean isEmpty() {
                return stack.isEmpty();
            }
            void add(char c) {
                stack.add(c);
            }
            int size() {
                return stack.size();
            }
            char get(int i) {
                return stack.get(i);
            }
            char pop() {
                if(stack.size() > 0) {
                    char ret = topOfStack();
                    stack.remove(stack.size()-1);
                    return ret;
                }
                return 0;
            }
            char topOfStack() {
                if(stack.isEmpty())
                    return 0;

                return stack.get(stack.size()-1);
            }
            @Override
            public String toString() {
                return ((char)(file+'A'))+""+row+" "+graphNo;
            }

            public String stackString() {
                return stack.toString();
            }
        }
        Square[][] squares;
        
        Board(int b) {
            if (b < 3 || b > 8) {
                b = DEFAULT_SIZE;
            }
            
            boardSize = b;
            int capstonesCount=0;
            int tilesCount=0;
            switch(b) {
                case 3: capstonesCount = 0; tilesCount = 10; break;
                case 4: capstonesCount = 0; tilesCount = 15; break;
                case 5: capstonesCount = 1; tilesCount = 21; break;
                case 6: capstonesCount = 1; tilesCount = 30; break;
                case 7: capstonesCount = 2; tilesCount = 40; break;
                case 8: capstonesCount = 2; tilesCount = 50; break;
            }
            
            whiteCapstones = blackCapstones = capstonesCount;
            whiteTilesCount = blackTilesCount = tilesCount;
            
            moveCount = 0;
            
            squares = new Square[boardSize][boardSize];
            for (int i = 0; i < b; i++) {
                for (int j = 0; j < b; j++) {
                    squares[i][j] = new Square(j, i+1);
                }
            }
        }
        
        Board(int boardSize, int moveCount, int whiteCapstones,
                int blackCapstones, int whiteTilesCount, int blackTilesCount,
                Square[][] squares) {
            this.boardSize = boardSize;
            this.moveCount = moveCount;
            this.whiteCapstones = whiteCapstones;
            this.blackCapstones = blackCapstones;
            this.whiteTilesCount = whiteTilesCount;
            this.blackTilesCount = blackTilesCount;
            this.squares = squares;
        }
        
        @Override
        public Board clone() {
            Board clone = new Board(boardSize, moveCount,
                                whiteCapstones, blackCapstones,
                                whiteTilesCount, blackTilesCount,
                                getClonedSquares());
            
            return clone;
        }

        Square[][] getClonedSquares() {
            Square[][] clone = new Square[boardSize][boardSize];

            for(int i=0;i<boardSize;i++){
                for(int j=0;j<boardSize;j++) {
                    clone[i][j] = squares[i][j].clone();
                }
            }
            return clone;
        }
        
        private Square getSquare(char file, int rank) {
            if(!boundsCheck(file, rank))
                return null;
            return board.squares[rank-1][file-'A'];
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("wc=").append(whiteCapstones).append(" bc=")
                    .append(blackCapstones).append(" wt=")
                    .append(whiteTilesCount).append(" bt=")
                    .append(blackTilesCount).append(" mc=")
                    .append(moveCount);
            sb.append("\n").append(getBoardString());
            return sb.toString();
        }
        
        public String getBoardString() {
            StringBuilder sb = new StringBuilder();
            for(int i=boardSize;i>0;i--){
                for(char j='A';j<boardSize+'A';j++){
                    sb.append(getSquare(j, i).stackString()).append(" ");
                }
                sb.append("\n");
            }
            return sb.toString();
        }
    }
    
    Board board;
    Stack<Board> boardHistory;

    static int DEFAULT_SIZE = 5;

    static AtomicInteger gameNo = new AtomicInteger(0);
    
    static final char FLAT='f';
    static final char WALL='w';
    static final char CAPSTONE='c';

    /**
     * 
     * @param p1: player who accepted the Seek
     * @param p2: player who posted the seek
     * @param b: board size
     * @param t: time in seconds
     * @param clr: color choice of p2
     */
    Game(Player p1, Player p2, int b, int t, int i, Seek.COLOR clr) {
        int rand = new Random().nextInt(99);
        
        if(clr == Seek.COLOR.ANY) {
            white = (rand>=50)?p1:p2;
            black = (rand>=50)?p2:p1;
        } else {
            white = (clr == Seek.COLOR.WHITE)?p2:p1;
            black = (clr == Seek.COLOR.WHITE)?p1:p2;
        }
        
        originalTime = whiteTime = blackTime = t*1000;
        incrementTime = i*1000;
        
        timer = new Timer();
        timerStarted = false;
        
        disconnectionTimer = null;
        
        time = System.currentTimeMillis();

        abandoned = false;
        
        board = new Board(b);
        boardHistory = new Stack<>();
        
        no = gameNo.incrementAndGet();
        gameState = gameS.NONE;
        drawOfferedBy = null;
        undoRequestedBy = null;
        
        moveList = Collections.synchronizedList(new ArrayList<String>());
        
        boardHistory.push(board.clone());//store empty position
        spectators = Collections.synchronizedSet(new HashSet<Player>());
    }
    
    public static void setGameNo() {
        try (Statement stmt = Database.gamesConnection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MAX(id) FROM games;")) {
            gameNo.set(rs.getInt(1));
        } catch (SQLException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    static void addGame(Game g) {
        Game.games.put(g.no, g);
        Game.updateGameListListeners("Add "+g.shortDesc());
    }
    
    static void removeGame(Game g) {
        Game.updateGameListListeners("Remove "+g.shortDesc());
        Game.games.remove(g.no);
    }

    void newSpectator(Player p) {
        p.send("Observe "+shortDesc());
        sendMoveListTo(p);
        spectators.add(p);
        updateTime(p);
    }
    
    void resign(Player p) {
        if(p == white)
            gameState = gameS.BLACK;
        else
            gameState = gameS.WHITE;
        whenGameEnd();
    }
    
    void saveBoardPosition() {
        boardHistory.push(board.clone());
    }
    
    void undoPosition() {
        boardHistory.pop();//discard cur pos        
        board = boardHistory.peek().clone();//replace cur pos with top of stack
        
        moveList.remove(moveList.size()-1);
    }
    
    void undo(Player p) {
        if(board.moveCount <= 0) {
            p.sendNOK();
            return;
        }
        
        if(undoRequestedBy == null) {
            undoRequestedBy = p;
            Player otherPlayer = (p==white)?black:white;
            otherPlayer.sendWithoutLogging("Game#"+no+" RequestUndo");
        } else if(undoRequestedBy != p) {
            undoRequestedBy = null;
            undoPosition();
            white.send("Game#"+no+" Undo");
            black.send("Game#"+no+" Undo");
            sendToSpectators("Game#"+no+" Undo");
            updateTimeTurnChange();
        }
    }
    
    void removeUndo(Player p) {
        if(undoRequestedBy == p) {
            undoRequestedBy = null;
            Player otherPlayer = (p==white)?black:white;
            otherPlayer.sendWithoutLogging("Game#"+no+" RemoveUndo");
        }
    }
    
    void draw(Player p) {
        if(drawOfferedBy == null) {
            drawOfferedBy = p;
            Player otherPlayer = (p==white)?black:white;
            otherPlayer.sendWithoutLogging("Game#"+no+" OfferDraw");
        } else if(drawOfferedBy!=p) {
            gameState = gameS.DRAW;
            whenGameEnd();
        }
    }
    
    void removeDraw(Player p) {
        if(drawOfferedBy == p) {
            drawOfferedBy = null;
            Player otherPlayer = (p==white)?black:white;
            otherPlayer.sendWithoutLogging("Game#"+no+" RemoveDraw");
        }
    }
    
    void unSpectate(Player p) {
        spectators.remove(p);
    }
    
    static void sendGameListTo(Player p) {
        for (Integer no : Game.games.keySet()) {
            p.sendWithoutLogging("GameList Add "+Game.games.get(no).shortDesc());
        }
    }
    
    void sendMoveListTo(Player p) {
        for(String move:moveList)
            p.sendWithoutLogging("Game#"+no+" "+move);
    }
    
    String shortDesc(){
        StringBuilder sb=new StringBuilder("Game#"+no+" ");
        sb.append(white.getName()).append(" vs ").append(black.getName());
        sb.append(", ").append(board.boardSize).append("x").append(board.boardSize).append(", ");
        sb.append(originalTime/1000).append(", ");
        sb.append(incrementTime/1000).append(", ");
        sb.append(board.moveCount).append(" half-moves played, ").append(isWhitesTurn()?white.getName():black.getName()).append(" to move");
        return sb.toString();
    }
    
    static void registerGameListListener(Player p) {
        gameListeners.add(p);
        sendGameListTo(p);
    }
    
    static void unregisterGameListListener(Player p) {
        gameListeners.remove(p);
    }
    
    static void updateGameListListeners(final String st) {
        new Thread() {
            @Override
            public void run() {
                for (Player p : gameListeners) {
                    p.sendWithoutLogging("GameList " + st);
                }
            }
        }.start();
    }
    
    @Override
    public String toString() {
        StringBuilder sb=new StringBuilder(shortDesc());
        sb.append("\n").append(board.getBoardString());
        return sb.toString();
    }
    
    private boolean boundsCheck(char file, int rank) {
        int fl = file - 'A';
        int rk = rank - 1;
        return fl<board.boardSize && fl>=0 && rk<board.boardSize && rk>=0;
    }
    
    private boolean isWhitesTurn() {
        return board.moveCount%2 == 0;
    }
    
    private boolean turnOf(Player p) {
        boolean whiteTurn = isWhitesTurn();
        return ((p==white)==whiteTurn)||((p==black)==!whiteTurn);
    }
    
    String sqState(char file, int rank) {
        Board.Square sq = board.getSquare(file, rank);
        if(sq==null)
            return "[]";
        return sq.stackString();
    }
    
    void checkOutOfPieces() {
        if(gameState!=gameS.NONE)
            return;
        if((board.whiteTilesCount==0 && board.whiteCapstones==0) ||
                (board.blackTilesCount==0 && board.blackCapstones==0)){
            System.out.println("out of pieces.");
            findWhoWon();
        }
    }
    
    void timeCleanup() {
        white.removeGame();
        black.removeGame();
        Game.removeGame(this);
    }
    
    void updateTime(Player p) {
        if(whiteTime == -1 || board.moveCount == 0)
            return;
        
        long curTime = System.nanoTime();
        long elapsedMS = (curTime - lastUpdateTime)/1000000;
        long timeToCount = 0;
        
        if(!isWhitesTurn()) {
            blackTime -= elapsedMS;
        } else {
            whiteTime -= elapsedMS;
        }
        
        lastUpdateTime = curTime;

        String msg="Game#"+no+" Time "+whiteTime/1000+" "+blackTime/1000;
        p.sendWithoutLogging(msg);
    }
    
    void updateTimeTurnChange() {
        if(whiteTime == -1 || gameState!=gameS.NONE)
            return;
        //start time after 1st move is played
        if(!timerStarted) {
            timerStarted = true;
            this.lastUpdateTime = System.nanoTime();
        }
        
        long curTime = System.nanoTime();
        long elapsedMS = (curTime - lastUpdateTime)/1000000;
        long timeToCount=0;
        
        if(isWhitesTurn()) {
            blackTime -= elapsedMS;
            blackTime += incrementTime;
            timeToCount = whiteTime;
        } else {
            whiteTime -= elapsedMS;
            whiteTime += incrementTime;
            timeToCount = blackTime;
        }
        
        lastUpdateTime = curTime;
        if(timeToCount<0)
            timeToCount = 0;
        
        timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(isWhitesTurn()) {
                    gameState = gameS.BLACK;
                } else {
                    gameState = gameS.WHITE;
                }
                whenGameEnd();
                timeCleanup();
            }
        }, timeToCount);
        String msg="Game#"+no+" Time "+whiteTime/1000+" "+blackTime/1000;
        white.sendWithoutLogging(msg);
        black.sendWithoutLogging(msg);
        sendToSpectators(msg);
    }
    
    Status placeMove(Player p, char file, int rank, boolean capstone,
            boolean wall) {
        //System.out.println("file = "+file+" rank="+rank+" capstone="
          //      +capstone+" wall="+wall);
        Board.Square sq = board.getSquare(file, rank);
        if(!turnOf(p))
            return new Status("Not your turn", false);
        
        if(sq!=null && sq.isEmpty()) {
            char ch;
            if(capstone)
                ch = CAPSTONE;
            else if(wall)
                ch = WALL;
            else
                ch = FLAT;
            
            //First move should always be a flat
            if(board.moveCount/2 == 0 && !isFlat(ch))
                return new Status("First move should be flat", false);
            
            //White places with capitals
            if(isWhitesTurn())
                ch = (char)(ch - 'a'+'A');
            
            //first moves should be played with opponent pieces
            if(board.moveCount/2 == 0)
                ch = Character.isUpperCase(ch)?Character.toLowerCase(ch):
                        Character.toUpperCase(ch);
            
            //check if enough capstones, and decrement if there are
            if(isCapstone(ch)) {
                int caps = isWhitesTurn()?board.whiteCapstones:board.blackCapstones;
                if(caps==0)
                    return new Status("You're out of capstones", false);
                if(isWhitesTurn())
                    board.whiteCapstones--;
                else
                    board.blackCapstones--;
            } else {
                if(isWhitesTurn())
                    board.whiteTilesCount--;
                else
                    board.blackTilesCount--;
            }
            
            sq.add(ch);
            board.moveCount++;
            String move="P "+file+rank+" "+(capstone?"C":"")+(wall?"W":"");
            moveList.add(move.trim());
            saveBoardPosition();
            undoRequestedBy = null;//remove any requests on move
            sendMove(p, move.trim());
            
            checkRoadWin();
            checkOutOfPieces();
            checkOutOfSquares();
            whenGameEnd();
            
            updateTimeTurnChange();
            return new Status(true);
        } else {
            return new Status("Square not empty", false);
        }
    }
    
    Player stackController(Board.Square sq) {
        return Character.isUpperCase(sq.topOfStack())?white:black;
    }
    
    boolean isCapstone(char c) {
        return Character.toLowerCase(c) == CAPSTONE;
    }
    
    boolean isWall(char c) {
        return Character.toLowerCase(c) == WALL;
    }
    
    boolean isFlat(char c) {
        return Character.toLowerCase(c) == FLAT;
    }
    
    boolean isBlack(char c) {
        return (c==FLAT)||(c==WALL)||(c==CAPSTONE);
    }
    
    boolean isWhite(char c) {
        return c!=0 && !isBlack(c);
    }
    
    static int abs(int x) {
        return x>0?x:-x;
    }
    
    String getPTN() {
        String ret="";
        for(int i=0;i<board.boardSize;i++){
            int xcnt=0;
            for(int j=0;j<board.boardSize;j++){
                Board.Square sq = board.squares[i][j];
                if(sq.isEmpty())
                    xcnt=0;
                else {
                    ret+="x"+xcnt;
                    String cell="";
                    for(int k=0;k<sq.size();k++){
                        char ch = sq.get(k);
                        cell+=isWhite(ch)?"1":"2";
                        if(isWall(ch))
                            cell+="S";
                        else if(isCapstone(ch))
                            cell+="C";
                    }
                    if(j!=board.boardSize-1)
                        cell+=",";
                }
            }
            ret+="/";
        }
        ret+=" "+board.moveCount+" "+(isWhitesTurn()?"1":"2");
        return ret;
    }
    
    Status moveMove(Player p, char f1, int r1, char f2, int r2, int[] vals) {
//        System.out.print("moveMove "+f1+""+r1+"->"+f2+""+r2+" ");
//        for(int i:vals)
//            System.out.print(i);
//        System.out.println("");
        //alternate turns
        if(!turnOf(p))
            return new Status("Not your turn", false);
        
        //first moves should be place moves
        if(board.moveCount/2==0)
            return new Status("First move should be place", false);
        
        //moves should be horizontal or vertical
        if(f1!=f2 && r1!=r2)
            return new Status("Move should be in straight line", false);
        
        Board.Square startSq = board.getSquare(f1, r1);
        Board.Square endSq = board.getSquare(f2, r2);
        //bounds checking of squares
        if(startSq == null || endSq == null)
            return new Status("Out of bounds", false);
        
        //stack should be controlled by current player
        if(p != stackController(startSq))
            return new Status("You don't control stack", false);
        
        //carry size should be less than or equal to boardSize and stack size
        int carrySize=0;
        for(int v:vals)
            carrySize+=v;
        if(carrySize>board.boardSize || carrySize>startSq.size())
            return new Status("Invalid move", false);
        
        //length of vals should be one less than no. of squares involved
        int num = (f1==f2)?abs(r1-r2):abs(f1-f2);
        if(vals.length != num)
            return new Status("Invalid move.", false);
        
        //first square should not be negative
        /*if(vals[0]<0)
            return new Status("Invalid input", false);*/
        //all other squares should be greater than 0
        for(int i=0;i<vals.length;i++)
            if(vals[i]<1)
                return new Status("Should place atleast one tile in rest of"
                        + " the squares", false);
        
        char top = startSq.topOfStack();
        boolean capstone = isCapstone(top);
        
        SquareIterator sqIt = new SquareIterator(this, f1, r1, f2, r2);
        //check if none of the intermediate places are walls or capstones
        for(Board.Square sqr=sqIt.next();sqr!=null;sqr=sqIt.next()){
            if(sqr==startSq)
                continue;
            char ttop = sqr.topOfStack();
            
            //can't stack over capstones
            if(isCapstone(ttop))
                return new Status("Can't stack over capstones", false);
            
            //can't stack over walls.. but wall in last square can be flattened
            if(isWall(ttop)) {
                if(sqr!=endSq)
                    return new Status("Can't stack over walls", false);
                else {
                    if(vals[vals.length-1]!=1 || !capstone)
                        return new Status("Capstone should be on top to flatten"
                                + " walls", false);
                }
            }
        }
        
        //do the actual moving of stack
        sqIt.reset();
        int count=-1;
        Stack<Character> moveStack = new Stack<>();
        
        for(Board.Square sqr=sqIt.next();sqr!=null;
                sqr=sqIt.next(),count++){
            assert(count<vals.length);
            
            //move to temporary stack
            if(sqr==startSq){
                for(int i=0;i<carrySize;i++)
                    moveStack.push(sqr.pop());
            }
            //flatten
            else if(sqr == endSq && moveStack.size()==1 &&
                    isCapstone(moveStack.peek()) && isWall(sqr.topOfStack())){
                char ch = sqr.pop();
                sqr.add(isWhite(ch)?Character.toUpperCase(FLAT):FLAT);
                sqr.add(moveStack.pop());
            }
            //move elements
            else {
                for(int i=0;i<vals[count];i++){
                    sqr.add(moveStack.pop());
                }
            }
        }
        board.moveCount++;
        
        String move = "M "+f1+r1+" "+f2+r2+" ";
        for(int val: vals)
            move+=val+" ";
        moveList.add(move.trim());
        saveBoardPosition();
        undoRequestedBy = null;//remove any requests on move
        sendMove(p, move.trim());
        
        checkRoadWin();
        checkOutOfSquares();
        whenGameEnd();
        
        updateTimeTurnChange();
        
        return new Status(true);
    }
    
    void whenGameEnd() {
        if(gameState==gameS.NONE)
            return;
        String msg="";
        msg += gameStateString();
        timer.cancel();
        
        if(disconnectionTimer != null)
            disconnectionTimer.cancel();
        
        if(!abandoned)
            msg = "Game#"+no+" Over "+msg;
        else {
            Player abandoningPlayer;
            if(gameState==gameS.WHITE)
                abandoningPlayer = black;
            else
                abandoningPlayer = white;
            
            msg = "Game#"+no+" Abandoned. "+abandoningPlayer.getName()+" quit";
        }
        
        if(!(abandoned && gameState==gameS.BLACK))
            white.send(msg);
        if(!(abandoned && gameState==gameS.WHITE))
            black.send(msg);
        sendToSpectators(msg);
        
        saveToDB();
    }
    
    private String moveListString() {
        StringBuilder sb = new StringBuilder();
        String prefix = "";
        for(String move: moveList) {
            sb.append(prefix);
            prefix = ",";
            sb.append(move);
        }
        return sb.toString();
    }
    
    private String gameStateString() {
        switch(gameState) {
            case DRAW: return "1/2-1/2";
            case WHITE_ROAD: return "R-0";
            case BLACK_ROAD: return "0-R";
            case WHITE_TILE: return "F-0";
            case BLACK_TILE: return "0-F";
            case WHITE: return "1-0";
            case BLACK: return "0-1";
            case ABORT: return "0-0";
            default: return "---";
        }
    }
    
    void saveToDB() {
        try {
            Statement stmt = Database.gamesConnection.createStatement();
            String sql = "INSERT INTO games "+
                    "VALUES (NULL,"+time+","+board.boardSize+",'"+white.getName()+"','"+black.getName()+"','"+moveListString()+"','"+
                    gameStateString()+"');";
            //System.out.println("SQL:: "+sql);
            stmt.executeUpdate(sql);
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void sendMove(Player p, String move) {
        String msg="Game#"+no+" "+move;
        sendToOtherPlayer(p, msg);
        sendToSpectators(msg);
    }
    
    void sendToOtherPlayer(Player p, String move) {
        if(white==p)
            black.sendWithoutLogging(move);
        else
            white.sendWithoutLogging(move);
    }
    
    void sendToSpectators(final String msg) {
        new Thread() {
            @Override
            public void run() {
                for(Player p:spectators)
                    p.sendWithoutLogging(msg);
            }
        }.start();
    }
    void findWhoWon() {
        int blackCount=0, whiteCount=0;
        for(int i=0;i<board.boardSize;i++){
            for(int j=0;j<board.boardSize;j++){
                char ch = board.squares[i][j].topOfStack();
                if(ch!=0 && !isWall(ch) && !isCapstone(ch)){
                    if(isWhite(ch))
                        whiteCount++;
                    else
                        blackCount++;
                }
            }
        }
        if(whiteCount==blackCount)
            gameState = gameS.DRAW;
        else if(whiteCount>blackCount)
            gameState = gameS.WHITE_TILE;
        else
            gameState = gameS.BLACK_TILE;
    }
    void checkOutOfSquares() {
        if(gameState!=gameS.NONE)
            return;
        for(int i=0;i<board.boardSize;i++){
            for(int j=0;j<board.boardSize;j++){
                if(board.squares[i][j].isEmpty())
                    return;
            }
        }
        System.out.println("Out of squares");
        findWhoWon();
    }
    
    void checkRoadWin() {
        boolean whiteWin=false, blackWin=false;
        class Graph {
            private int lf, rf, tr, br;
            private ArrayList<Board.Square> squares;
            int no;
            
            Graph(int i, int j) {
                no = i+j*board.boardSize;
                lf=rf=tr=br=1000;
                squares = new ArrayList<>();
            }
            boolean add(Board.Square sq) {
               if(lf==1000 || sq.file < lf) lf = sq.file;
               if(rf==1000 || sq.file > rf) rf = sq.file;
               if(br==1000 || sq.row-1 < br) br = sq.row-1;
               if(tr==1000 || sq.row-1 > tr) tr = sq.row-1;
               
               squares.add(sq);
               sq.graphNo = no;
               //System.out.println("add "+no+" "+lf+" "+rf+" "+br+" "+tr);
               
               return (lf==0 && rf == board.boardSize-1) || (br==0 && tr==board.boardSize-1);
            }
            boolean merge(Graph g) {
                if(g==this)
                    return false;
                boolean ret=false;
                //System.out.println("merge "+g.no+" with "+no);
                for(Board.Square sq: g.squares) {
                    ret |= add(sq);
                    sq.graphNo = no;
                }
                g.no = no;
                //System.out.println("      "+no+" "+lf+" "+rf+" "+br+" "+tr);
                //System.out.println(this);
                return ret;
            }
            @Override
            public String toString() {
                StringBuilder sb=new StringBuilder();
                sb.append("(").append(no).append("::").append(lf+" "+rf+" "+br+" "+tr).append(")");
                sb.append("[");
                for(Board.Square sq: squares)
                    sb.append(sq).append(" ");
                sb.append("]");
                return sb.toString();
            }
        }
        Graph graph[] = new Graph[board.boardSize*board.boardSize];
        for(int i=0;i<board.boardSize;i++)
            for(int j=0;j<board.boardSize;j++)
                graph[i+j*board.boardSize] = new Graph(i, j);
                
        for(int i=0;i<board.boardSize;i++){
            for(int j=0;j<board.boardSize;j++){
                Board.Square sq = board.getSquare((char)('A'+i), j+1);
                sq.graphNo = -1;
                
                char ch = sq.topOfStack();
                if(ch==0 || isWall(ch))
                    continue;
                graph[i+j*board.boardSize].add(sq);
                
                boolean left=false;
                boolean over=false;
                
                Board.Square lsq = board.getSquare((char)('A'+i-1), j+1);
                //System.out.println("lsq "+lsq);
                if(lsq!=null){
                    char lch = lsq.topOfStack();
                    if(lch!=0 && isWhite(ch)==isWhite(lch) && !isWall(lch)){
                        over = graph[lsq.graphNo].merge(graph[i+j*board.boardSize]);
                        graph[i+j*board.boardSize] = graph[lsq.graphNo];
                    }
                }
                if(over) {
                    if(isWhite(ch))
                        whiteWin=true;
                    else
                        blackWin=true;
                }
                
                Board.Square tsq = board.getSquare((char)('A'+i), j-1+1);
                if(tsq!=null){
                    char tch = tsq.topOfStack();
                    if(tch!=0 && isWhite(ch)==isWhite(tch) && !isWall(tch)){
                        over = graph[tsq.graphNo].merge(graph[i+j*board.boardSize]);
                        graph[i+j*board.boardSize] = graph[tsq.graphNo];
                    }
                }
                if(over) {
                    if(isWhite(ch))
                        whiteWin=true;
                    else
                        blackWin=true;
                }
            }
        }
        if(whiteWin && blackWin){
            gameState = isWhitesTurn()?gameS.BLACK_ROAD:gameS.WHITE_ROAD;
        } else if (whiteWin) {
            gameState = gameS.WHITE_ROAD;
        } else if (blackWin) {
            gameState = gameS.BLACK_ROAD;
        }
    }
    
    Player otherPlayer(Player p) {
        return (p==white)?black:white;
    }
    void playerQuit(Player p) {
        Player otherPlayer = otherPlayer(p);
        abandoned = true;
        
        p.removeGame();
        otherPlayer.removeGame();
        
        gameState = (p==white)?gameS.BLACK:gameS.WHITE;
        whenGameEnd();
        Game.removeGame(this);
    }
    
    void playerDisconnected(Player p) {
        Player otherPlayer = otherPlayer(p);
        if(!otherPlayer.isLoggedIn()) {
            //Both players have disconnected - so we abort the game
            gameState = gameS.ABORT;
            whenGameEnd();
            Game.removeGame(this);
            p.removeGame();
            otherPlayer.removeGame();
            return;
        }
        otherPlayer.send("Message "+p.getName()+" has disconnected. They have "+
                                    reconnectionTime +" seconds to reconnect");
        sendToSpectators("Message "+p.getName()+" has disconnected. They have "+
                                    reconnectionTime +" seconds to reconnect");
        disconnectionTimer = new Timer();
        disconnectionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                playerQuit(white.isLoggedIn()?black:white);
            }}, reconnectionTime*1000);
    }
    
    void playerRejoin(Player p) {
        disconnectionTimer.cancel();
        disconnectionTimer = null;//Could cause race. TODO: Fix
        
        Player otherPlayer = otherPlayer(p);
        
        String msg = "Game Start " + no +" "+board.boardSize+" "+white.getName()+" vs "+black.getName();
        p.send(msg+" "+((white==p)?"white":"black")+" "+originalTime);
        
        sendMoveListTo(p);
        updateTime(p);
        p.send("Message Your game is resumed");
        
        otherPlayer.send("Message "+p.getName()+" has reconnected.");
        sendToSpectators("Message "+p.getName()+" has reconnected.");
    }
    
    class SquareIterator {
        Game game;
        char f1, f2;
        int r1, r2;
        
        static final int EAST = 0;
        static final int WEST = 1;
        static final int NORTH = 2;
        static final int SOUTH = 3;
        
        int direction;
        int count=0;
        int total;
        
        SquareIterator(Game game, char f1, int r1, char f2, int r2) {
            this.game = game;
            this.f1 = f1;
            this.r1 = r1;
            this.f2 = f2;
            this.r2 = r2;
            
            assert(f1==f2 || r1 == r2);
            assert(boundsCheck(f1, r1) && boundsCheck(f2, r2));
            
            if(f1==f2)
                direction = (r2-r1)>0?NORTH:SOUTH;
            else
                direction = (f2-f1)>0?EAST:WEST;
            
            switch(direction) {
                case EAST: total = f2-f1+1; break;
                case WEST: total = f1-f2+1; break;
                case NORTH: total = r2-r1+1; break;
                case SOUTH: total = r1-r2+1; break;
            }
        }
        
        Board.Square next() {
            if(count >= total)
                return null;
            
            Board.Square ret = null;
            
            switch(direction) {
                case EAST: ret = game.board.getSquare((char)(f1+count), r1); break;
                case WEST: ret = game.board.getSquare((char)(f1-count), r1); break;
                case NORTH: ret = game.board.getSquare(f1, r1+count); break;
                case SOUTH: ret = game.board.getSquare(f1, r1-count); break;
            }
            count++;
            return ret;
        }
        void reset() {
            count = 0;
        }
    }
    public static void main(String[] args) {
        Database.initConnection();
        try {
            Statement stmt = Database.gamesConnection.createStatement();
            stmt.executeUpdate("CREATE TABLE games " +
                    "(id INTEGER PRIMARY KEY," +
                    " date INT," +
                    " size INT," +
                    " player_white VARCHAR(20)," +
                    " player_black VARCHAR(20)," +
                    " notation TEXT," +
                    " result VARCAR(10));"
                    );
            
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
