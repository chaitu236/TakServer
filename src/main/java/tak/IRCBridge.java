/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapted from http://archive.oreilly.com/pub/h/1966
 *
 * @author chaitu
 */
public class IRCBridge {

    public static BufferedReader reader = null;
    public static PrintWriter writer = null;

    public static boolean enabled;
    public static String server;
    public static String nick;
    public static String login;
    public static String channel;
    public static String password;
    
    public static Thread thread;

    private static boolean connected = false;

    public static void init() {
        thread = new Thread() {
            @Override
            public void run() {
                if(!enabled)
                    return;
                try {
                    // Connect directly to the IRC server.
                    Socket socket = new Socket(server, 6667);
                    writer = new PrintWriter(socket.getOutputStream());
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    // Log on to the server.
                    writer.write("NICK " + nick + "\r\n");
                    writer.write("USER " + login + " 8 * : Java IRC Hacks Bot\r\n");
                    writer.flush();

                    System.out.println("Connecting to irc "+channel);
                    // Read lines from the server until it tells us we have connected.
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("004")) {
                            // We are now logged in.
                            break;
                        } else if (line.contains("433")) {
                            System.out.println("Nickname is already in use.");
                            socket.close();
                            return;
                        }
                    }

                    // Join the channel.
                    writer.write("JOIN " + channel + "\r\n");
                    writer.flush();
                    System.out.println("Connected to irc "+channel);
                    connected = true;

                    // Keep reading lines from the server.
                    while ((line = reader.readLine()) != null) {
                        if (line.toUpperCase().startsWith("PING ")) {
                            // We must respond to PINGs to avoid being disconnected.
                            writer.write("PONG " + line.substring(5) + "\r\n");
                            writer.flush();
                        } else {
                            if(line.contains("PRIVMSG "+channel)) {
                                String user = line.split("!")[0].split(":")[1];
                                String msg = line.split("PRIVMSG "+channel+" :")[1];
                                Client.sendAllOnline("Shout <IRC> <"+user+"> "+msg);
                                TakServer.Log("IRC:"+user+":"+msg);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println(e);
                } finally {
                    writer.close();
                    try {
                        reader.close();
                    } catch (IOException ex) {
                        Logger.getLogger(IRCBridge.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    connected = false;
                    writer = null;
                }
            }
        };
        thread.start();
    }

    public static void send(String msg) {
        if (connected && writer != null) {
            writer.write("PRIVMSG " + channel + " :" + msg + "\r\n");
            writer.flush();
        }
    }
}
