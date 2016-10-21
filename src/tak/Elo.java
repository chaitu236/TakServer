/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;
import java.lang.Math;
import java.util.Objects;
import java.util.HashSet;
import java.util.Arrays;
import java.util.HashSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author Abyss, NohatCoder
 */
public class Elo {
    
    public static final long startUnix = 1461369600000L; //April 23rd
    public static final String[] INVALID = new String[] {"cutak_bot", "Anon",
        "FriendlyBot"}; //These players' games should never be counted
    public static final String[] BOTACCOUNTS = new String[] {"alphabot", "alphatak_bot", 
    "TakticianBot", "TakticianBotDev", "ShlktBot", "takkybot", "AlphaTakBot_5x5", 
    "TakkerusBot", "BeginnerBot", "TakticianDev", "IntuitionBot"};
    public static HashSet<String> badPlayers = new HashSet<>(Arrays.asList(INVALID));
    public static HashSet<String> bots = new HashSet<>(Arrays.asList(BOTACCOUNTS));
    public static final int BONUSRATING = 550;
    public static final int BONUSFACTOR = 60;
    
    public static float winLossDraw(String result) {
        //This will only be called for white
        if ((Objects.equals(result, "1-0")) || 
            Objects.equals(result, "R-0") || 
            Objects.equals(result, "F-0")) {
            return 1;
        }
        else if ((Objects.equals(result, "0-1")) || 
                Objects.equals(result, "0-R") || 
                Objects.equals(result, "0-F")) {
            return 0;
        }
        else {
            return (float) 0.5;
        }
    }
    
    public static void getGamesSince(Long time) {
        if(time == startUnix) { //We are calculating from the "beginning of time"
            Player.allToDefaultR();
        }
        try (Statement stmt = Database.gamesConnection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM games WHERE size > 4 AND date > " + time + 
                        " ;")) {
            while(rs.next()) {
                String w = rs.getString("player_white");
                String b = rs.getString("player_black");
                String n = rs.getString("notation");
                String result = rs.getString("result");
                int timerTime = rs.getInt("timertime");
                int increment = rs.getInt("timerinc");
                if(badPlayers.contains(w)) {

                }
                else if(badPlayers.contains(b)) {

                }
                else if(w.contains("Guest") || b.contains("Guest")) {

                }
                else if(result.contains("0-0")) {

                }
                else if(n.length() <= 4){ //length==4 if only 1 flat was played
                         
                }
                else if(timerTime > 0 && timerTime + increment * 30 <= 600) {
                    
                }
                else { 
                    Player white;
                    Player black;
                    if(Player.players.containsKey(w)) {
                        white = Player.players.get(w);  
                    }
                    else {
                        //continue; //This should not fire.
                        Player.createPlayer(w, "fake@fake");
                        white = Player.players.get(w); 
                    }
                    if(Player.players.containsKey(b)) {
                        black = Player.players.get(b); 
                    }
                    else {
                        //continue; // This should not fire.
                        Player.createPlayer(b, "fake@fake");
                        black = Player.players.get(b);
                    }
                    float r = winLossDraw(result);
                    calcGame(white, black, r);
                }
               
            }

        }
        catch (SQLException ex) {
        }
        Player.updateAllPlayers(); //Sends to the Sql database
    }
    
    public static void calcGame(Player white, Player black, float result) {
        double whiteStrength = strengthFunc(white.getR4());
        double blackStrength = strengthFunc(black.getR4());
        double expected = whiteStrength / (whiteStrength + blackStrength);
        double fairness = expected * (1 - expected);
        String wName = white.getName();
        String bName = black.getName();
        //Bot games do not affect the ratings of humans
        if(bots.contains(wName)) { 
                adjustPlayer(white, result - expected, fairness); 
        }
        else if(bots.contains(bName)) {
                adjustPlayer(black, expected - result, fairness);
        }
        else { 
            adjustPlayer(white, result - expected, fairness);
            adjustPlayer(black, expected - result, fairness);
        }
    }
    
    private static double strengthFunc(double elo) {
        return Math.pow(10, elo/400);
    }
        
    
    private static void adjustPlayer(Player pl, double amount, double fairness) {
        double initRating = pl.getR4();
        double hidden = pl.getR5();
        double maxRating = pl.getR6();
        double bonus;
        int games = pl.getR7();
        float participation = pl.getR8();
        bonus = Math.max(0.0, amount * hidden * ((double)BONUSFACTOR / (double)BONUSRATING));
        if(pl.getId()==27) {
            //System.out.println("bonus: " + bonus);
        }
        pl.setR5(hidden - bonus);
        double k = 10 + 15.0 * Math.pow(0.5, (float)games/200) + (15.0 * 
                Math.pow(0.5, (float)(maxRating - 1000) / 300.0));
        pl.setR4(initRating + amount * k + bonus);
        pl.setR8(participation + (float)fairness);
        pl.setR7(games + 1);
        if(maxRating < pl.getR4()) {
            pl.setR6(pl.getR4());
        }
        
    } 
}
