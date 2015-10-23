/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static tak.Game.DEFAULT_SIZE;

/**
 *
 * @author chaitu
 */
public class Seek {
    Client client;
    int boardSize;
    int no;
    
    static int seekNo=0;
    
    static Map<Integer, Seek> seeks = Collections.synchronizedMap(new HashMap<Integer, Seek>());
    static Set<Client> seekListeners = Collections.synchronizedSet(new HashSet<Client>());
    
    static Seek newSeek(Client c, int b) {
        Seek sk = new Seek(c, b);
        addSeek(sk);
        return sk;
    }
    
    Seek(Client c, int b) {
        client = c;
        no = ++seekNo;
        
        if (b < 4 || b > 8)
            b = DEFAULT_SIZE;
        boardSize = b;
    }

    static void removeSeek(int b) {
        Seek sk=Seek.seeks.get(b);
        Seek.seeks.remove(b);
        updateListeners("remove "+sk.toString());
    }
    
    static void addSeek(Seek sk) {
        Seek.seeks.put(sk.no, sk);
        updateListeners("new "+sk.toString());
    }
        
    static void sendListTo(Client c) {
        for (Integer no : Seek.seeks.keySet()) {
            c.send("Seek new "+Seek.seeks.get(no));
        }
    }
    static void registerListener(Client c) {
        seekListeners.add(c);
    }
    
    static void updateListeners(final String st) {
        new Thread() {
            @Override
            public void run() {
                for (Client cc : seekListeners) {
                    cc.send("Seek " + st);
                }
            }
        }.start();
    }
    
    static void unregisterListener(Client c) {
        seekListeners.remove(c);
    }
    @Override
    public String toString() {
        return no+" "+client.name+" "+boardSize;
    }
}
