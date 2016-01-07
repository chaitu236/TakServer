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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author chaitu
 */
public class Player {
    public static HashMap<String, Player> players = new HashMap<>();
    static int idCount=0;
    
    private String name;
    private String password;
    private String email;
    private int id;//Primary key

    //Ratings for 4x4.. 8x8 games
    private int r4;
    private int r5;
    private int r6;
    private int r7;
    private int r8;
    
    //variables not in database
    private Client client;
    
    Player(String name, String email, String password, int id, int r4, int r5,
                        int r6, int r7, int r8) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.id = id;
        this.r4 = r4;
        this.r5 = r5;
        this.r6 = r6;
        this.r7 = r7;
        this.r8 = r8;
        
        client = null;
    }
    
    public boolean isLoggedIn() {
        return client!=null;
    }
    
    public void login(Client client) {
        this.client = client;
    }
    
    public void logout() {
        this.client = null;
    }
    
    Player(String name, String email, String password) {
        this(name, email, password, ++idCount, 0, 0, 0, 0, 0);
    }
    
    static SecureRandom random = new SecureRandom();
    public static Player createPlayer(String name, String email) {
        Player np = new Player(name, email, new BigInteger(130, random).toString(32));
        try {
            Statement stmt = Database.connection.createStatement();
            String sql = "INSERT INTO PLAYERS (ID,NAME,PASSWORD,EMAIL,R4,R5,R6,R7,R8) "+
                    "VALUES ("+np.id+",'"+np.name+"','"+np.password+"','"+np.email+"',"+
                    np.r4+","+np.r5+","+np.r6+","+np.r7+","+np.r8+");";
            System.out.println("SQL:: "+sql);
            stmt.executeUpdate(sql);
            stmt.close();
            
            EMail.send(np.email, "playtak.com password", "Your password is "+np.password);
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
    
    public void setR4(int r4) {
        this.r4 = r4;
        Statement stmt;
        try {
            stmt = Database.connection.createStatement();
            String sql = "UPDATE PLAYERS set R4 = "+r4+" where ID="+id+";";
            stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setR5(int r5) {
        this.r5 = r5;
        Statement stmt;
        try {
            stmt = Database.connection.createStatement();
            String sql = "UPDATE PLAYERS set R5 = "+r5+" where ID="+id+";";
            stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setR6(int r6) {
        this.r6 = r6;
        Statement stmt;
        try {
            stmt = Database.connection.createStatement();
            String sql = "UPDATE PLAYERS set R6 = "+r6+" where ID="+id+";";
            stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setR7(int r7) {
        this.r7 = r7;
        Statement stmt;
        try {
            stmt = Database.connection.createStatement();
            String sql = "UPDATE PLAYERS set R7 = "+r7+" where ID="+id+";";
            stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setR8(int r8) {
        this.r8 = r8;
        Statement stmt;
        try {
            stmt = Database.connection.createStatement();
            String sql = "UPDATE PLAYERS set R8 = "+r8+" where ID="+id+";";
            stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public int getR4() {
        return r4;
    }
    
    public int getR5() {
        return r5;
    }
    
    public int getR6() {
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
    
    public static void loadFromDB() {
        idCount=0;
        try (Statement stmt = Database.connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM PLAYERS;")) {
            while(rs.next()) {
                Player np = new Player(rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getInt("id"),
                        rs.getInt("r4"),
                        rs.getInt("r5"),
                        rs.getInt("r6"),
                        rs.getInt("r7"),
                        rs.getInt("r8"));

                System.out.println("Read player "+np);
                players.put(np.name, np);
                if(idCount<np.id)
                    idCount++;
                
            }
        } catch (SQLException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) {
        Database.initConnection();
//        try {
//            Statement stmt = Database.connection.createStatement();
//            stmt.executeUpdate("CREATE TABLE PLAYERS " +
//                    "(ID INT PRIMARY KEY," +
//                    " NAME VARCHAR(20)," +
//                    " PASSWORD VARCHAR(50),"+
//                    " EMAIL VARCHAR(50),"+
//                    " R4 INT," +
//                    " R5 INT," +
//                    " R6 INT," +
//                    " R7 INT," +
//                    " R8 INT)");
//            
//            stmt.close();
//        } catch (SQLException ex) {
//            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
//        }
        
//        createPlayer("player1", "player1@playtak.com");
//        createPlayer("player2", "player2@playtak.com");
//        createPlayer("player3", "player3@playtak.com");
 
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
