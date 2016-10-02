/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

/**
 *
 * @author chaitu
 */
public class Player {
    public static HashMap<String, Player> players = new HashMap<>();
    public static HashSet<Player> modList = new HashSet<>();
    public static HashSet<Player> gagList = new HashSet<>();
    
    static int idCount=0;
    static AtomicInteger guestCount = new AtomicInteger(0);
    
    private String name;
    private String password;
    private String email;
    private int id;//Primary key

    //Ratings for 4x4.. 8x8 games
    private double r4;
    private double r5;
    private double r6;
    private int r7;
    private int r8;
    private double glicko;
    private double rd;
    private double vol;
    
    private boolean guest;
    private boolean mod = false;
    private boolean gag = false;//don't broadcast his shouts
    //variables not in database
    private Client client;
    private Game game;
    
    private String resetToken;
    // Abyss
    public final ArrayList<Player> recentWins = new ArrayList<Player>(); 
    public final ArrayList<Player> recentLosses = new ArrayList<Player>();
    public final ArrayList<Player> recentDraws = new ArrayList<Player>();
    
    Player(String name, String email, String password, int id, double r4, 
            double r5, double r6, int r7, int r8, boolean guest) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.id = id;
        this.r4 = r4;
        this.r5 = r5;
        this.r6 = r6;
        this.r7 = r7;
        this.r8 = r8;
        this.guest = guest;
        this.resetToken = "";
        
        client = null;
        game = null;
    }
    
    public static String hash(String st) {
        return BCrypt.hashpw(st, BCrypt.gensalt());
    }
    
    public boolean authenticate(String candidate) {
        return BCrypt.checkpw(candidate, password);
    }
    
    public void sendResetToken() {
        resetToken = new BigInteger(130, random).toString(32);
        EMail.send(this.email, "playtak.com reset password", "Your reset token is " + resetToken);
    }
    
    public boolean resetPassword(String token, String newPass) {
        if((!"".equals(resetToken)) && token.equals(resetToken)) {
            setPassword(newPass);
            resetToken = "";
            return true;
        }
        return false;
    }
    
    public boolean isLoggedIn() {
        return client!=null;
    }
    
    public Game getGame() {
        return game;
    }
    
    public void setGame(Game g) {
        game = g;
    }
    
    public void removeGame() {
        game = null;
    }
    
    public void send(String msg) {
        if(client != null)
            client.send(msg);
    }
    
    public void sendNOK() {
        if(client != null)
            client.sendNOK();
    }
    
    public void sendWithoutLogging(String msg) {
        if(client != null)
            client.sendWithoutLogging(msg);
    }
    
    public void login(Client c) {
        this.client = c;
        resetToken = "";//If a user is able to login, he has the password
        
        if(game != null)
            game.playerRejoin(this);
    }
    
    public void logout() {
        if(client != null) {
            client.disconnect();
            this.client = null;
        }
    }
    
    public boolean isMod() {
        return mod;
    }
    
    public void setMod() {
        mod = true;
        modList.add(this);
    }
    
    public void unMod() {
        mod = false;
        modList.remove(this);
    }
    
    public void gag() {
        gag = true;
        Player.gagList.add(this);
    }
    
    public void unGag() {
        gag = false;
        Player.gagList.remove(this);
    }
    
    public boolean isGagged() {
        return gag;
    }
    
    public void loggedOut() {
        this.client = null;
        if(guest) {
            Player.modList.remove(this);
            Player.gagList.remove(this);
        }
    }
    
    Player(String name, String email, String password, boolean guest) {
        this(name, email, password, guest?0:++idCount, 0, 0, 0, 0, 0, guest);
    }
    
    Player() {
        this("Guest"+guestCount.incrementAndGet(), "", "", true);
        Player.players.put(this.name, this);
    }
    
    Client getClient() {
        return client;
    }
    
    static SecureRandom random = new SecureRandom();
    public static Player createPlayer(String name, String email) {
        String tmpPass = new BigInteger(130, random).toString(32);
        
        Player np = new Player(name, email, Player.hash(tmpPass), false);
        try {
            Statement stmt = Database.playersConnection.createStatement();
            String sql = "INSERT INTO players (id,name,password,email,r4,r5,r6,r7,r8) "+
                    "VALUES ("+np.id+",'"+np.name+"','"+np.password+"','"+np.email+"',"+
                    np.r4+","+np.r5+","+np.r6+","+np.r7+","+np.r8+");";
            //System.out.println("SQL:: "+sql);
            stmt.executeUpdate(sql);
            stmt.close();
            
            //EMail.send(np.email, "playtak.com password", "Your password is "+tmpPass+". You can change it on playtak.com.");
            players.put(np.name, np);
        } catch (SQLException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
        return np;
    }
    
    @Override
    public String toString() {
        return name+" "+password+" "+email+" "+r4+" "+r5+" "+r6+" "+r7+" "+r8;
    }
    
    public void setR4(double r4) {
        this.r4 = r4;
        Statement stmt;
        try {
            stmt = Database.playersConnection.createStatement();
            String sql = "UPDATE players set r4 = "+r4+" where id="+id+";";
            stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setR5(double r5) {
        this.r5 = r5;
        Statement stmt;
        try {
            stmt = Database.playersConnection.createStatement();
            String sql = "UPDATE players set r5 = "+r5+" where id="+id+";";
            stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setR6(double r6) {
        this.r6 = r6;
        Statement stmt;
        try {
            stmt = Database.playersConnection.createStatement();
            String sql = "UPDATE players set r6 = "+r6+" where id="+id+";";
            stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setR7(int r7) {
        this.r7 = r7;
        Statement stmt;
        try {
            stmt = Database.playersConnection.createStatement();
            String sql = "UPDATE players set r7 = "+r7+" where id="+id+";";
            stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setR8(int r8) {
        this.r8 = r8;
        Statement stmt;
        try {
            stmt = Database.playersConnection.createStatement();
            String sql = "UPDATE players set r8 = "+r8+" where id="+id+";";
            stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setPassword(String pass) {
        this.password = Player.hash(pass);
        
        Statement stmt;
        try {
            stmt = Database.playersConnection.createStatement();
            String sql = "UPDATE players set password = \""+this.password+"\" where id="+id+";";
            stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public double getR4() {
        return r4;
    }
    
    public double getR5() {
        return r5;
    }
    
    public double getR6() {
        return r6;
    }
    
    public int getR7() {
        return r7;
    }
    
    public int getR8() {
        return r8;
    }
    
    public String getName() {
        return name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public int getId() {
        return id;
    }
    
    public void addRecentWin(Player opp) {
        recentWins.add(opp);
    }
    
    public void addRecentLoss(Player opp) {
        recentLosses.add(opp);
    }
    
    public void addRecentDraw(Player opp) {
        recentDraws.add(opp);
    }
    
    public void saveNewRating(double g, double r, double s) {
        this.glicko = g;
        this.rd = r;
        this.vol = s;
    }
    
    public void saveNewRating(double r) {
        this.rd = r;
    }
    
    public void updateRating() {
        setR4(this.glicko);
        setR5(this.rd);
        setR6(this.vol);
    }
    
    public void ratingToDefault() {
        setR4(1500.0);
        setR5(350.0);
        setR6(0.06);
    }
    
    public void clearGames() {
        recentWins.clear();
        recentLosses.clear();
        recentDraws.clear();
    }
    
    public static void updateAllPlayers() {
        Connection playersConnection = null;
        String sql = "UPDATE players SET r4 = ? , "
                + "r5 = ? , r6 = ? "
                + "WHERE id = ?";
        try {
            Class.forName("org.sqlite.JDBC");
            playersConnection = DriverManager.getConnection("jdbc:sqlite:players.db");
            PreparedStatement pstmt = playersConnection.prepareStatement(sql);
            Collection<Player> allPlayers = players.values();
            playersConnection.setAutoCommit(false);
            for(Player p : allPlayers){
                pstmt.setDouble(1, p.glicko);
                pstmt.setDouble(2, p.rd);
                pstmt.setDouble(3, p.vol);
                pstmt.setInt(4, p.id);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            playersConnection.commit();
            playersConnection.setAutoCommit(true);
            playersConnection.close();
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void updateAllPlayers(double[] nums) {
        Connection playersConnection = null;
        String sql = "UPDATE players SET r4 = ? , "
                + "r5 = ? , r6 = ? "
                + "WHERE id = ?";
        try {
            Class.forName("org.sqlite.JDBC");
            playersConnection = DriverManager.getConnection("jdbc:sqlite:players.db");
            PreparedStatement pstmt = playersConnection.prepareStatement(sql);
            Collection<Player> allPlayers = players.values();
            playersConnection.setAutoCommit(false);
            for(Player p : allPlayers){
                pstmt.setDouble(1, 1500);
                pstmt.setDouble(2, 350);
                pstmt.setDouble(3, 0.06);
                pstmt.setInt(4, p.id);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            playersConnection.commit();
            playersConnection.setAutoCommit(true);
            playersConnection.close();
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void loadFromDB() {
        idCount=0;
        try (Statement stmt = Database.playersConnection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM players;")) {
            while(rs.next()) {
                Player np = new Player(rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getInt("id"),
                        rs.getDouble("r4"),
                        rs.getDouble("r5"),
                        rs.getDouble("r6"),
                        rs.getInt("r7"),
                        rs.getInt("r8"),
                        false);

                //System.out.println("Read player "+np);
                players.put(np.name, np);
                if(idCount<np.id)
                    idCount=np.id;
                
            }
        } catch (SQLException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) {
        Database.initConnection();
//        try {
//            Statement stmt = Database.connection.createStatement();
//            stmt.executeUpdate("CREATE TABLE players " +
//                    "(id INT PRIMARY KEY," +
//                    " name VARCHAR(20)," +
//                    " password VARCHAR(50),"+
//                    " email VARCHAR(50),"+
//                    " r4 INT," +
//                    " r5 INT," +
//                    " r6 INT," +
//                    " r7 INT," +
//                    " r8 INT)");
//            
//            stmt.close();
//        } catch (SQLException ex) {
//            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//        createPlayer("kaka", "kaka@playtak.com");
//        createPlayer("kaki", "kaki@playtak.com");
//        createPlayer("baba", "baba@playtak.com");

        loadFromDB();
        
        //Test update
//        Player p3 = players.get("player3");
//        System.out.println("player3 "+p3);
//        p3.setR5(57);
//        System.out.println("player3 "+p3);
        
        //Test create after load
        createPlayer("player4", "player4@playtak.com");
    }
}
