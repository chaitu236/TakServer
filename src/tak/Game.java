/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

/**
 *
 * @author chaitu
 */
public class Game {

    Player white;
    Player black;
    long time;

    int no;
    int boardSize;
    int moveCount;
    int whiteCapstones;
    int blackCapstones;
    
    int whiteTilesCount;
    int blackTilesCount;
    
    boolean abandoned;
    
    Set<Client> spectators;
    
    public enum gameS {WHITE_ROAD, BLACK_ROAD, WHITE_TILE, BLACK_TILE, DRAW,
                        WHITE, BLACK, NONE};
    List<String> moveList;
    gameS gameState;
    
    static Map<Integer, Game> games=Collections.synchronizedMap(new HashMap<Integer, Game>());
    static Set<Client> gameListeners = Collections.synchronizedSet(new HashSet<Client>());
    
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
    Square[][] board;

    static int DEFAULT_SIZE = 4;

    static int gameNo = 0;
    
    static final char FLAT='f';
    static final char WALL='w';
    static final char CAPSTONE='c';

    Game(Player p1, Player p2, int b) {
        int rand = new Random().nextInt(99);
        white = (rand>=50)?p1:p2;
        black = (rand>=50)?p2:p1;
        
        time = System.currentTimeMillis();

        if (b < 4 || b > 8) {
            b = DEFAULT_SIZE;
        }

        boardSize = b;
        int capstonesCount=0;
        int tilesCount=0;
        switch(b) {
            case 4: capstonesCount = 0; tilesCount = 15; break;
            case 5: capstonesCount = 1; tilesCount = 20; break;
            case 6: capstonesCount = 1; tilesCount = 30; break;
            case 7: capstonesCount = 2; tilesCount = 40; break;
            case 8: capstonesCount = 2; tilesCount = 50; break;
        }
        whiteCapstones = blackCapstones = capstonesCount;
        whiteTilesCount = blackTilesCount = tilesCount;
        
        moveCount = 0;
        abandoned = false;
        board = new Square[boardSize][boardSize];
        no = ++gameNo;
        gameState = gameS.NONE;
        moveList = Collections.synchronizedList(new ArrayList<String>());

        for (int i = 0; i < b; i++) {
            for (int j = 0; j < b; j++) {
                Square sq = board[i][j] = new Square(j, i+1);
            }
        }
        spectators = Collections.synchronizedSet(new HashSet<Client>());
    }

    Game(Player p1, Player p2) {
        this(p1, p2, DEFAULT_SIZE);
    }
    
    static void addGame(Game g) {
        Game.games.put(g.no, g);
        Game.updateGameListListeners("Add "+g.shortDesc());
    }
    
    static void removeGame(Game g) {
        Game.updateGameListListeners("Remove "+g.shortDesc());
        Game.games.remove(g.no);
    }

    void newSpectator(Client c) {
        c.send("Observe "+shortDesc());
        sendMoveListTo(c);
        spectators.add(c);
    }
    void unSpectate(Client c) {
        spectators.remove(c);
    }
    static void sendGameListTo(Client c) {
        for (Integer no : Game.games.keySet()) {
            c.send("GameList Add "+Game.games.get(no).shortDesc());
        }
    }
    
    void sendMoveListTo(Client c) {
        for(String move:moveList)
            c.send("Game#"+no+" "+move);
    }
    
    String shortDesc(){
        StringBuilder sb=new StringBuilder("Game#"+no+" ");
        sb.append(white.getName()).append(" vs ").append(black.getName());
        sb.append(", ").append(boardSize).append("x").append(boardSize).append(", ");
        sb.append(moveCount).append(" half-moves played, ").append(isWhitesTurn()?white.getName():black.getName()).append(" to move");
        return sb.toString();
    }
    
    static void registerGameListListener(Client c) {
        gameListeners.add(c);
    }
    
    static void unregisterGameListListener(Client c) {
        gameListeners.remove(c);
    }
    
    static void updateGameListListeners(final String st) {
        new Thread() {
            @Override
            public void run() {
                for (Client cc : gameListeners) {
                    cc.send("GameList " + st);
                }
            }
        }.start();
    }
    @Override
    public String toString() {
        StringBuilder sb=new StringBuilder(shortDesc());
        sb.append(getBoardString());
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
    private boolean boundsCheck(char file, int rank) {
        int fl = file - 'A';
        int rk = rank - 1;
        return fl<boardSize && fl>=0 && rk<boardSize && rk>=0;
    }
    
    private Square getSquare(char file, int rank) {
        if(!boundsCheck(file, rank))
            return null;
        return board[rank-1][file-'A'];
    }
    
    private boolean isWhitesTurn() {
        return moveCount%2 == 0;
    }
    
    private boolean turnOf(Player p) {
        boolean whiteTurn = isWhitesTurn();
        return ((p==white)==whiteTurn)||((p==black)==!whiteTurn);
    }
    
    String sqState(char file, int rank) {
        Square sq = getSquare(file, rank);
        if(sq==null)
            return "[]";
        return sq.stackString();
    }
    
    void checkOutOfPieces() {
        if(gameState!=gameS.NONE)
            return;
        if((whiteTilesCount==0 && whiteCapstones==0) ||
                (blackTilesCount==0 && blackCapstones==0)){
            System.out.println("out of pieces.");
            findWhoWon();
        }
    }
    
    Status placeMove(Player p, char file, int rank, boolean capstone,
            boolean wall) {
        //System.out.println("file = "+file+" rank="+rank+" capstone="
          //      +capstone+" wall="+wall);
        Square sq = getSquare(file, rank);
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
            if(moveCount/2 == 0 && !isFlat(ch))
                return new Status("First move should be flat", false);
            
            //White places with capitals
            if(isWhitesTurn())
                ch = (char)(ch - 'a'+'A');
            
            //first moves should be played with opponent pieces
            if(moveCount/2 == 0)
                ch = Character.isUpperCase(ch)?Character.toLowerCase(ch):
                        Character.toUpperCase(ch);
            
            //check if enough capstones, and decrement if there are
            if(isCapstone(ch)) {
                int caps = isWhitesTurn()?whiteCapstones:blackCapstones;
                if(caps==0)
                    return new Status("You're out of capstones", false);
                if(isWhitesTurn())
                    whiteCapstones--;
                else
                    blackCapstones--;
            } else {
                if(isWhitesTurn())
                    whiteTilesCount--;
                else
                    blackTilesCount--;
            }
            
            sq.add(ch);
            moveCount++;
            String move="P "+file+rank+" "+(capstone?"C":"")+(wall?"W":"");
            moveList.add(move);
            sendMove(p, move);
            
            checkRoadWin();
            checkOutOfPieces();
            checkOutOfSquares();
            whenGameEnd();
            
            return new Status(true);
        } else {
            return new Status("Square not empty", false);
        }
    }
    
    Player stackController(Square sq) {
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
        for(int i=0;i<this.boardSize;i++){
            int xcnt=0;
            for(int j=0;j<this.boardSize;j++){
                Square sq = board[i][j];
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
                    if(j!=this.boardSize-1)
                        cell+=",";
                }
            }
            ret+="/";
        }
        ret+=" "+this.moveCount+" "+(isWhitesTurn()?"1":"2");
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
        if(moveCount/2==0)
            return new Status("First move should be place", false);
        
        //moves should be horizontal or vertical
        if(f1!=f2 && r1!=r2)
            return new Status("Move should be in straight line", false);
        
        Square startSq = getSquare(f1, r1);
        Square endSq = getSquare(f2, r2);
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
        if(carrySize>boardSize || carrySize>startSq.size())
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
        for(Square sqr=sqIt.next();sqr!=null;sqr=sqIt.next()){
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
        
        for(Square sqr=sqIt.next();sqr!=null;
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
        moveCount++;
        
        String move = "M "+f1+r1+" "+f2+r2+" ";
        for(int val: vals)
            move+=val;
        moveList.add(move);
        sendMove(p, move);
        
        checkRoadWin();
        checkOutOfSquares();
        whenGameEnd();
        
        return new Status(true);
    }
    
    void whenGameEnd() {
        if(gameState==gameS.NONE)
            return;
        String msg="";
        switch(gameState) {
            case DRAW: msg+= "1/2-1/2"; break;
            case WHITE_ROAD: msg+="R-0"; break;
            case BLACK_ROAD: msg+="0-R"; break;
            case WHITE_TILE: msg+="F-0"; break;
            case BLACK_TILE: msg+="0-F"; break;
            case WHITE: msg+="1-0"; break;
            case BLACK: msg+="0-1"; break;
        }
        
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
            white.getClient().send(msg);
        if(!(abandoned && gameState==gameS.WHITE))
            black.getClient().send(msg);
        sendToSpectators(msg);
        
        saveToDB();
    }
    
    void saveToDB() {
        
    }
    
    void sendMove(Player p, String move) {
        String msg="Game#"+no+" "+move;
        sendToOtherPlayer(p, msg);
        sendToSpectators(msg);
    }
    
    void sendToOtherPlayer(Player p, String move) {
        if(white==p)
            black.getClient().send(move);
        else
            white.getClient().send(move);
    }
    
    void sendToSpectators(final String msg) {
        new Thread() {
            @Override
            public void run() {
                for(Client c:spectators)
                    c.send(msg);
            }
        }.start();
    }
    void findWhoWon() {
        int blackCount=0, whiteCount=0;
        for(int i=0;i<this.boardSize;i++){
            for(int j=0;j<this.boardSize;j++){
                char ch = board[i][j].topOfStack();
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
        for(int i=0;i<this.boardSize;i++){
            for(int j=0;j<this.boardSize;j++){
                if(board[i][j].isEmpty())
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
            private ArrayList<Square> squares;
            int no;
            
            Graph(int i, int j) {
                no = i+j*boardSize;
                lf=rf=tr=br=1000;
                squares = new ArrayList<>();
            }
            boolean add(Square sq) {
               if(lf==1000 || sq.file < lf) lf = sq.file;
               if(rf==1000 || sq.file > rf) rf = sq.file;
               if(br==1000 || sq.row < br) br = sq.row-1;
               if(tr==1000 || sq.row > tr) tr = sq.row-1;
               
               squares.add(sq);
               sq.graphNo = no;
               //System.out.println("add "+no+" "+lf+" "+rf+" "+br+" "+tr);
               
               return (lf==0 && rf == boardSize-1) || (br==0 && tr==boardSize-1);
            }
            boolean merge(Graph g) {
                if(g==this)
                    return false;
                boolean ret=false;
                //System.out.println("merge "+g.no+" with "+no);
                for(Square sq: g.squares) {
                    ret |= add(sq);
                    sq.graphNo = no;
                }
                g.no = no;
                //System.out.println("      "+no+" "+lf+" "+rf+" "+br+" "+tr);
                return ret;
            }
            @Override
            public String toString() {
                StringBuilder sb=new StringBuilder();
                sb.append("(").append(no).append(")");
                sb.append("[");
                for(Square sq: squares)
                    sb.append(sq).append(" ");
                sb.append("]");
                return sb.toString();
            }
        }
        Graph graph[] = new Graph[boardSize*boardSize];
        for(int i=0;i<boardSize;i++)
            for(int j=0;j<boardSize;j++)
                graph[i+j*boardSize] = new Graph(i, j);
                
        for(int i=0;i<boardSize;i++){
            for(int j=0;j<boardSize;j++){
                Square sq = getSquare((char)('A'+i), j+1);
                sq.graphNo = -1;
                
                char ch = sq.topOfStack();
                if(ch==0 || isWall(ch))
                    continue;
                graph[i+j*boardSize].add(sq);
                
                boolean left=false;
                boolean over=false;
                
                Square lsq = getSquare((char)('A'+i-1), j+1);
                //System.out.println("lsq "+lsq);
                if(lsq!=null){
                    char lch = lsq.topOfStack();
                    if(lch!=0 && isWhite(ch)==isWhite(lch) && !isWall(lch)){
                        over = graph[lsq.graphNo].merge(graph[i+j*boardSize]);
                        graph[i+j*boardSize] = graph[lsq.graphNo];
                    }
                }
                if(over) {
                    if(isWhite(ch))
                        whiteWin=true;
                    else
                        blackWin=true;
                }
                
                Square tsq = getSquare((char)('A'+i), j-1+1);
                if(tsq!=null){
                    char tch = tsq.topOfStack();
                    if(tch!=0 && isWhite(ch)==isWhite(tch) && !isWall(tch)){
                        over = graph[tsq.graphNo].merge(graph[i+j*boardSize]);
                        graph[i+j*boardSize] = graph[tsq.graphNo];
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
            gameState = gameS.DRAW;
        } else if (whiteWin) {
            gameState = gameS.WHITE_ROAD;
        } else if (blackWin) {
            gameState = gameS.BLACK_ROAD;
        }
    }
    
    void playerQuit(Player p) {
        Player otherPlayer = (p==white)?black:white;
        abandoned = true;
        otherPlayer.getClient().game = null;
        gameState = (p==white)?gameS.BLACK:gameS.WHITE;
        whenGameEnd();
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
        
        Square next() {
            if(count >= total)
                return null;
            
            Square ret = null;
            
            switch(direction) {
                case EAST: ret = game.getSquare((char)(f1+count), r1); break;
                case WEST: ret = game.getSquare((char)(f1-count), r1); break;
                case NORTH: ret = game.getSquare(f1, r1+count); break;
                case SOUTH: ret = game.getSquare(f1, r1-count); break;
            }
            count++;
            return ret;
        }
        void reset() {
            count = 0;
        }
    }
}
