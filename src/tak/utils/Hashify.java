/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak.utils;

import tak.Database;
import tak.Player;

/**
 *
 * @author chaitu
 */
public class Hashify {
    public static void main(String[] args) {
        Database.initConnection();
        Player.loadFromDB();
        int count=0;
        for(Player p: Player.players.values()) {
            p.setPassword(p.getPassword());
            count++;
        }
        System.out.println("Hashified "+count+" records");
    }
}
