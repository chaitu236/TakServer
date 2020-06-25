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
    
    static void Log(Object obj) {
        System.out.println(new Date()+"        "+obj);
    }
}
