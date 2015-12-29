/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 *
 * @author chaitu
 */
public class Player {
    String name;
    String password;
    String email;
    
    int rating_4x4;
    int rating_5x5;
    int rating_6x6;
    int rating_7x7;
    int rating_8x8;
    
    Player(String name, String password) {
        this.name = name;
        this.password = password;
    }
    
    static SecureRandom random = new SecureRandom();
    public static Player createPlayer(String name) {
        Player np = new Player(name, new BigInteger(130, random).toString(32));
        return np;
    }
}
