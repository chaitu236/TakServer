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
        /* This condition should be whether or not all user's ratings should
        be recalculated from the very beginning of ratings time, April 23rd. 
        This can be decided at your discretion, and should probably involved
        saving the TakRatings.startUnix variable to some output. I will leave
        it true for now, so it will always reset and calculate all ratings when
        the server is run.*/
        if(true) { 
            TakServer.Log("Starting attempt");
            //Player.allToDefaultR();
            //Glicko.calculateAll(time);
            Elo.getGamesSince(1461369600000L);
            TakServer.Log("All ratings calculated.");
        }
        else {
           //Initialize TakRatings.startUnix to a predefined value.
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
