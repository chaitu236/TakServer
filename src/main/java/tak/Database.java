/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author chaitu
 */
public class Database {
    public static Connection playersConnection;
    public static Connection gamesConnection;
    public static void initConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            playersConnection = DriverManager.getConnection("jdbc:sqlite:players.db");
            gamesConnection = DriverManager.getConnection("jdbc:sqlite:games.db");
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
