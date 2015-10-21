/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author chaitu
 */
public class TakServer extends Thread{

    /**
     */
    @Override
    public void run () {
        ServerSocket ssocket;
        try {
            ssocket = new ServerSocket(10000);
            while(true) {
                System.out.println("waiting for connection\n");
                Socket socket = ssocket.accept();
                Client cc = new Client(socket);
                cc.start();
            }
        } catch (IOException ex) {
            Logger.getLogger(TakServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public static void main(String[] args) {
        // TODO code application logic here
        TakServer takServer = new TakServer();
        takServer.start();
        System.out.println("dir: "+System.getProperty("user.dir"));
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                Client.sigterm();
            }
        });
    }
    
}
