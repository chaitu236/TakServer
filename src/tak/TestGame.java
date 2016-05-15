/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads moves in PTN notation, converts to game notation and calls move methods
 * in Game
 * @author chaitu
 */
public class TestGame {
    String placeString = "([CS]?)([a-z])(\\d)$";
    Pattern placePattern;
    
    String moveString = "(\\d?)([a-z])(\\d)([<>+-])(\\d+)?$";
    Pattern movePattern;
    
    Game game;
    TestGame(int boardSize) {
        game = new Game(null, null, boardSize, 180, Seek.COLOR.ANY);
        placePattern = Pattern.compile(placeString);
        movePattern = Pattern.compile(moveString);
    }
    void move(String ptnMove) {
        Matcher m;
        
        System.out.println("Move "+ptnMove);
        if((m = movePattern.matcher(ptnMove)).find()){
            int stksize=1;
            String temp = m.group(1);
            if(temp!=null && temp.length()>0)
                stksize = Integer.parseInt(temp);
                
            temp=m.group(5);
            int movelen;
            int stk[]=null;
            
            if(temp!=null && temp.length()>0){
                movelen=temp.length();
                stk = new int[movelen];
                int stkno = Integer.parseInt(temp);
                for(int i=0;i<movelen;i++, stkno/=10)
                    stk[i] = stkno%10;
                int revstk[] = new int[movelen];
                for(int i=0;i<movelen;i++)
                    revstk[i] = stk[movelen-1-i];
                stk = revstk;
            } else {
                movelen=1;
                stk = new int[movelen];
                stk[0] = stksize;
            }
            temp=m.group(4);
            int fadd=0;
            int radd=0;
            
            switch (temp) {
                case ">":
                    fadd=1*movelen;
                    break;
                case "<":
                    fadd=-1*movelen;
                    break;
                case "+":
                    radd=1*movelen;
                    break;
                case "-":
                    radd=-1*movelen;
                    break;
            }
            
            char file=m.group(2).toUpperCase().charAt(0);
            int rank=Integer.parseInt(m.group(3));
            Status st = game.moveMove(null, file, rank, (char)(file+fadd), rank+radd, stk);
            if(!st.isOk())
                System.out.println("Error:: "+st);
            
        } else if((m = placePattern.matcher(ptnMove)).find()){
            boolean capstone, wall;
            capstone=wall=false;
            
            if(m.group(1)!=null && m.group(1).length()>0){
                //System.out.println("asdf "+m.group(1)+ m.group(1).length());
                char ch = m.group(1).charAt(0);
                if(ch=='S')
                    wall = true;
                else if(ch=='C')
                    capstone = true;
            }
            
            Status st = game.placeMove(null, m.group(2).toUpperCase().charAt(0), Integer.parseInt(m.group(3)), capstone, wall);
            if(!st.isOk())
                System.out.println("Error:: "+st);
        } else {
            return;
        }
        
        System.out.println(game.board.getBoardString());
        String msg="";
        switch(game.gameState) {
            case DRAW: msg+= "1/2-1/2"; break;
            case WHITE_ROAD: msg+="R-0"; break;
            case BLACK_ROAD: msg+="0-R"; break;
            case WHITE_TILE: msg+="F-0"; break;
            case BLACK_TILE: msg+="0-F"; break;
            case NONE: return;
        }
        System.out.println(msg);
    }
    public static void main(String[] args) {
        TestGame tg = new TestGame(5);
        String st;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            while((st=reader.readLine())!=null) {
                String[] moves = st.trim().split(" ");
                for(String str:moves)
                    tg.move(str.trim());
            }
        } catch (IOException ex) {
            Logger.getLogger(TestGame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
