/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.lang.Math;
import java.util.*;
/**
 * Glicko ratings are calculated every interval of time, currently every 2 days.
 * Calling the public calculateAll() method will calculate all ratings from the
 * very beginning of the non-anonymous database, i.e. April 23rd. The calculate()
 * public method will calculate only the ratings from the most recent period. It
 * will be called from within the Game Class.
 * @author Abyss
 */
public class Glicko {

    private static final double TAO = 0.7; // Systems constant
    private static final double EPSILON = 0.000001; //Convergence tolerance
    public static Set<Player> activePlayers = new HashSet();
    public static long interval = 172800000L; // 2 days
    public static long startUnix = 1461369600000L; //April 23rd //1475002312429L;
    public static long endUnix;
    public static final String[] SET_VALUES = new String[] { "alphabot", "alphatak_bot",
       "TakticianBot", "TakticianBotDev", "ShlktBot", "cutak_bot", "takkybot",
        "AlphaTakBot_5x5", "TakkerusBot", "BeginnerBot", "TakticianDev", "FriendlyBot",
    "IntuitionBot", "Anon"};
    public static Set<String> badPlayers = new HashSet<String>(Arrays.asList(SET_VALUES));
    public static Set<List> newGames = new HashSet();
    public static boolean calculating = false;



    public static void calculate() {
        calculating = true;
        getGamesFromDB();
        calcRatings();
        Player.updateAllPlayers(); //Updates and changes SQL database
        calculating = false;
    }

    public static void calculateAll(long time) {
        calculating = true;
        while(startUnix + interval + interval < time) { // 4 days out
            getGamesFromDB();
            calcRatings();
            Player.updateAllNoSQL(); // Just updates internal ratings.
        }
        Player.updateAllPlayers(); //Updates (redundant) and changes databases.
        TakServer.Log("Finished calculation of all ratings");
        calculating = false;
    }

    public static void printActivePlayers() {
        for(Player x : activePlayers) {
            String name = x.getName();
            System.out.println(name);
        }
    }

    private static void getGamesFromDB() {
        double[] defaultValues = {1500, 350, 0.06};
        endUnix = startUnix + interval; //2 days
        try (Statement stmt = Database.gamesConnection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM games WHERE size > 4 AND date > " + startUnix +
                        " AND date < " + endUnix + " ;")) {
            while(rs.next()) {
                String w = rs.getString("player_white");
                String b = rs.getString("player_black");
                String n = rs.getString("notation");
                String result = rs.getString("result");
                int timerTime = rs.getInt("timertime");
                int increment = rs.getInt("timerinc");

                if(badPlayers.contains(w))
                    continue;
                else if(badPlayers.contains(b))
                    continue;
                else if(w.contains("Guest") || b.contains("Guest"))
                    continue;
                else if(result.contains("0-0"))
                    continue;
                else if(n.length() <= 4) { //length==4 if only 1 flat was played
                    continue;
                }
                else if(0 < timerTime && (timerTime + 30 * increment) < 600) { //Fast Games
                    continue;
                }
                else {
                    Player white;
                    Player black;

                    if(Player.players.containsKey(w)) {
                        white = Player.players.get(w);
                    }
                    else {
                        continue; //This should not fire.
                        //Player.createPlayer(w, "fake@fake");
                        //white = Player.players.get(w);
                    }
                    if(Player.players.containsKey(b)) {
                        black = Player.players.get(b);
                    }
                    else {
                        continue; // This should not fire.
                        //Player.createPlayer(b, "fake@fake");
                        //black = Player.players.get(b);
                    }
                    if ((Objects.equals(result, "1-0")) ||
                            Objects.equals(result, "R-0") ||
                            Objects.equals(result, "F-0")){
                        // White win
                        white.addRecentWin(black);
                        black.addRecentLoss(white);
                    }
                    else if ((Objects.equals(result, "0-1")) ||
                            Objects.equals(result, "0-R") ||
                            Objects.equals(result, "0-F")){
                        white.addRecentLoss(black);
                        black.addRecentWin(white);
                    }
                    else {
                        white.addRecentDraw(black);
                        black.addRecentDraw(white);
                    }
                    activePlayers.add(white);
                    activePlayers.add(black);
                    if (white.getR4() == 0) {
                        white.ratingToDefault();
                    }
                    if (black.getR4() == 0) {
                        black.ratingToDefault();
                    }
                }

            }
         }
         catch (SQLException ex) {
         }
        //Player.updateAllPlayers(defaultValues);
        //System.out.println("Finished");
    }


    private static void calcRatings() {
        for(Player p : activePlayers) {
            calcGlicko2(p);
        }
        Collection<Player> allPlayers = Player.players.values();
        for(Player p : allPlayers) {
            if(activePlayers.contains(p)) {
                //if(p.getName().contains("Abyss")) {
                    //System.out.println(p.recentLosses);
                    //System.out.println(p.recentWins);
                //}
                //p.updateRating();
                p.clearGames();
            }
            else {
                inactiveAdjustPhi(p);
                //System.out.println("Inactive: " + p.getName());
            }
        }
        startUnix = endUnix;
        endUnix += interval;
    }

    private static void calcGlicko2(Player mainPlayer) {
        double v = 0;
        double delta = 0;
        double mu = (mainPlayer.getR4() - 1500) / 173.7178;
        double phi = (mainPlayer.getR5() / 173.7178);
        double sigma = mainPlayer.getR6();
        double oppMu;
        double oppPhi;
        double goj;
        double E;

        for(Player opp : mainPlayer.recentWins) {
            oppMu = (opp.getR4() - 1500) / 173.7178;
            oppPhi = opp.getR5() / 173.7178;
            goj = gOfPhi(oppPhi);
            E = gl2E(mu, oppMu, oppPhi);
            v += ((goj*goj) * E * (1-E));
            delta += (goj * (1-E)); // 1 - E because these are just wins.
        }
        for(Player opp : mainPlayer.recentLosses) {
            oppMu = (opp.getR4() - 1500) / 173.7178;
            oppPhi = opp.getR5() / 173.7178;
            goj = gOfPhi(oppPhi);
            E = gl2E(mu, oppMu, oppPhi);
            v += ((goj*goj) * E * (1-E));
            delta += (goj * (0-E)); // 0 - E because these are just losses.
        }
        for(Player opp : mainPlayer.recentDraws) {
            oppMu = (opp.getR4() - 1500) / 173.7178;
            oppPhi = opp.getR5() / 173.7178;
            goj = gOfPhi(oppPhi);
            E = gl2E(mu, oppMu, oppPhi);
            v += ((goj*goj) * E * (1-E));
            delta += (goj * (0.5-E)); // 0.5 - E because these are just draws.
        }
        v = 1 / v;
        double muPrime = delta; //A slight shortcut
        delta *= v;
        // Begin the Illinois algorithm
        double smallA = Math.log(sigma * sigma);
        double bigA = smallA;
        double B;
        //Initializing B
        if(delta*delta > phi*phi + v)
            B = Math.log(delta*delta - phi*phi - v);
        else {
            int k = 1;
            double test = glF(bigA - k*TAO, delta, phi, v, smallA);
            while(test < 0){
                k++;
                test = glF(bigA - k*TAO, delta, phi, v, smallA);
            }
            B = bigA - k * TAO;
        }

        double fa = glF(bigA, delta, phi, v, smallA);
        double fb = glF(B, delta, phi, v, smallA);
        double C;
        double fc;
        while(Math.abs(B-bigA) > EPSILON) {
            C = bigA + ((bigA - B)*fa) / (fb - fa);
            fc = glF(C, delta, phi, v, smallA);
            if(fc * fb < 0) {
                bigA = B;
                fa = fb;
            }
            else {
                fa /= 2;
            }
            B = C;
            fb = fc;
        }
        // End Illinois alogorithm
        double sigmaPrime = Math.exp(bigA/2);
        //System.out.println(mainPlayer.getName() + " sigmaPrime  " + sigmaPrime);
        double phiStar = Math.sqrt(phi*phi + sigmaPrime*sigmaPrime);
        double phiPrime = 1 / Math.sqrt((1 / (phiStar*phiStar)) + 1 / v);
        muPrime *= (phiPrime*phiPrime);
        muPrime += mu;
        mainPlayer.saveNewRating(173.7178 * muPrime + 1500, 173.7178 * phiPrime,
                sigmaPrime);

    }


    private static double gOfPhi(double p) {
        return 1 / (Math.sqrt(1 + 3 * p * p / (Math.PI * Math.PI)));
    }

    private static double gl2E(double m, double oppM, double oppP) {
        double goj = gOfPhi(oppP);
        return 1 / (1 + Math.exp(-1 * goj * (m - oppM)));
    }

    private static double glF(double x, double delt, double ph,
            double v, double a) {
        double ex = Math.exp(x);
        double num1 = ex * (delt*delt - ph*ph - v - ex);
        double num2 = 2 * (ph*ph + v + ex) * (ph*ph + v + ex);
        return (num1/num2) - ((x-a) / (TAO*TAO));

    }

    private static void inactiveAdjustPhi(Player pl) {
        double phi = pl.getR5();
        double sigma = pl.getR6();
        pl.saveNewInactiveRD(Math.sqrt(phi*phi + sigma*sigma));
    }
}
