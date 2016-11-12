/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author chaitu
 */
public class TakServer extends Thread{

    public static int port = 10000;
    /**
     */
    @Override
    public void run () {
        ServerSocket ssocket;
        try {
            ssocket = new ServerSocket(port);
            while(true) {
                TakServer.Log("Waiting for connection");
                Socket socket = ssocket.accept();
                Client cc = new Client(socket);
                cc.start();
            }
        } catch (IOException ex) {
            Logger.getLogger(TakServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public static void main(String[] args) {
        //long time = System.currentTimeMillis();
        Settings.parse();
        Database.initConnection();
        Player.loadFromDB();
        Game.setGameNo();
        
        IRCBridge.init();
        
        if(args.length>0)
            port = Integer.parseInt(args[0]);
        
        TakServer takServer = new TakServer();
        takServer.start();
        TakServer.Log("dir: "+System.getProperty("user.dir"));
        /* Currently we are always calculating ratings from all games upon
        startup. This should be changed later on, when you decide how exactly
        you want to track when the last ratings calculation was. For now, though
        it makes very little difference, as the ratings calculation takes about
        1-2 seconds in total. NOTE: When displaying player's ratings, use the
        method .getDisplayRating() , not .getR4(). 
        */
        if(true) { 
            TakServer.Log("Starting attempt");
            //Player.allToDefaultR();
            //Glicko.calculateAll(time);
            Elo.getGamesSince(1461369600000L);
            TakServer.Log("All ratings calculated.");
        }
        
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                Client.sigterm();
            }
        });
    }
    static void Log(Object obj) {
        System.out.println(new Date()+"        "+obj);
    }
}
