/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

import java.util.HashMap;
import java.util.Map;
import static tak.Game.DEFAULT_SIZE;

/**
 *
 * @author chaitu
 */
public class Seek {
    ClientConnection client;
    int boardSize;
    int no;
    
    static int seekNo=0;
    
    static Map<Integer, Seek> seeks = new HashMap<>();
    
    Seek(ClientConnection c, int b) {
        client = c;
        no = ++seekNo;
        
        if(b!=4 && b!=5 && b!=6 && b!=8)
            b = DEFAULT_SIZE;
        boardSize = b;
    }
    
    @Override
    public String toString() {
        return no+" "+client.name+" "+boardSize;
    }
}
