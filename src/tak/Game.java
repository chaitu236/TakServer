/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

/**
 *
 * @author chaitu
 */
public class Game {

    Client white;
    Client black;

    int no;
    int boardSize;
    int moveCount;
    ArrayList<Character>[][] board;

    static int DEFAULT_SIZE = 4;

    static int gameNo = 0;
    
    static final char FLAT='f';
    static final char WALL='w';
    static final char CAPSTONE='c';

    Game(Client c1, Client c2, int b) {
        white = c1;
        black = c2;

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

    Game(Client c1, Client c2) {
        this(c1, c2, DEFAULT_SIZE);
    }

    @Override
    public String toString() {
        StringBuilder sb=new StringBuilder("Game#"+no+"\n");
        sb.append(white.name).append(" vs ").append(black.name).append("\n");
        sb.append("Moves ").append(moveCount).append(" ").append(isWhitesTurn()?white.name:black.name).append("'s turn").append("\n");
        
        for(int i=boardSize;i>0;i--){
            for(char j='A';j<boardSize+'A';j++){
                sb.append(getSquare(j, i)).append(" ");
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
    
    private ArrayList getSquare(char file, int rank) {
        if(!boundsCheck(file, rank))
            return null;
        return board[rank-1][file-'A'];
    }
    
    private char pop(ArrayList<Character> stack) {
        if(stack.size() > 0) {
            char ret = topOfStack(stack);
            stack.remove(stack.size()-1);
            return ret;
        }
        return 0;
    }
    
    private boolean isWhitesTurn() {
        return moveCount%2 == 0;
    }
    
    private boolean turnOf(Client c) {
        boolean whiteTurn = isWhitesTurn();
        if(c == white && whiteTurn)
            return true;
        if(c == black && !whiteTurn)
            return true;
        return false;
    }
    
    Status placeMove(Client c, char file, int rank, boolean capstone,
            boolean wall) {
        System.out.println("file = "+file+" rank="+rank+" capstone="
                +capstone+" wall="+wall);
        ArrayList sq = getSquare(file, rank);
        if(!turnOf(c))
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
            
            sq.add(ch);
            moveCount++;
            return new Status(true);
        } else {
            return new Status("Square not empty", false);
        }
    }
    
    char topOfStack(ArrayList<Character> stack) {
        return stack.get(stack.size()-1);
    }
    
    Client stackController(ArrayList<Character> stack) {
        return Character.isUpperCase(topOfStack(stack))?white:black;
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
    
    Status moveMove(Client c, char f1, int r1, char f2, int r2, int[] vals) {
        System.out.println("Move move "+f1+" "+r1+" "+f2+" "+r2+" "+Arrays.toString(vals));
        for(int i:vals)
            System.out.println(" "+i);
        //alternate turns
        if(!turnOf(c))
            return new Status("Not your turn", false);
        
        //first moves should be place moves
        if(moveCount/2==0)
            return new Status("First move should be place", false);
        
        //moves should be horizontal or vertical
        if(f1!=f2 && r1!=r2)
            return new Status("Move should be in straight line", false);
                
        ArrayList startSq = getSquare(f1, r1);
        ArrayList endSq = getSquare(f2, r2);
        //bounds checking of squares
        if(startSq == null || endSq == null)
            return new Status("Out of bounds", false);
        
        //stack should be controlled by current player
        if(c != stackController(startSq))
            return new Status("You don't control stack", false);
        
        //sum of all vals should be less than or equal to boardSize and stack
        //ssize
        int sum=0;
        for(int v:vals)
            sum+=v;
        if(sum>boardSize || sum>startSq.size())
            return new Status("Invalid move", false);
        
        //should move atleast 2 squares
        if(vals.length < 2)
            return new Status("Move atleast 2 squares", false);
        
        //first square should not be negative
        if(vals[0]<0)
            return new Status("Invalid input", false);
        //all other squares should be greater than 0
        for(int i=1;i<vals.length;i++)
            if(vals[i]<1)
                return new Status("Should place atleast one tile in rest of"
                        + " the squares", false);
        
        char top = topOfStack(startSq);
        boolean capstone = isCapstone(top);
        
        SquareIterator sqIt = new SquareIterator(this, f1, r1, f2, r2);
        //check if none of the intermediate places are walls or capstones
        for(ArrayList<Character> sqr=sqIt.next();sqr!=null;sqr=sqIt.next()){
            if(sqr==startSq)
                continue;
            char ttop = topOfStack(sqr);
            
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
        int count=0;
        for(ArrayList<Character> sqr=sqIt.next();sqr!=endSq;
                sqr=sqIt.next(),count++){
            assert(count<vals.length);
            
            Stack<Character> moveStack = new Stack<>();
            //move to temporary stack
            if(sqr==startSq){
                for(int i=0;i<sum;i++)
                    moveStack.push(pop(sqr));
            }
            //flatten
            else if(sqr == endSq && moveStack.size()==1 &&
                    isCapstone(moveStack.peek()) && isWall(topOfStack(sqr))){
                pop(sqr);
                sqr.add(FLAT);
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
        return new Status(true);
    }
    
    void clientQuit(Client c) {
        Client otherClient = (c==white)?black:white;
        otherClient.game = null;
        otherClient.send("Game#"+no+" stopped. "+c.name+" quit");
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
        
        ArrayList<Character> next() {
            if(count >= total)
                return null;
            
            ArrayList<Character> ret = null;
            
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
