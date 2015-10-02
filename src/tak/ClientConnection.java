package tak;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author chaitu
 */
public class ClientConnection extends Thread {

    Socket socket;
    BufferedReader clientReader;
    PrintWriter clientWriter;
    String name = null;

    static Set<String> names = new HashSet<>();
    static Set<ClientConnection> clientConnections = new HashSet<>();

    Game game = null;
    Seek seek = null;

    String placeString = "Game#(\\d+) P ([A-Z])(\\d)(\\sC?)";
    Pattern placePattern;

    String moveString = "Game#(\\d+) m ([A-Z])(\\d) ([A-Z])(\\d) (\\d+)";
    Pattern movePattern;

    String seekString = "Seek (\\d)";
    Pattern seekPattern;

    String startGameString = "Start (\\d+)";
    Pattern startGamePattern;

    String listString = "List";
    Pattern listPattern;

    String nameString = "Name ([a-z]{4,10})";
    Pattern namePattern;

    ClientConnection(Socket socket) {
        this.socket = socket;
        names = new HashSet<String>();

        placePattern = Pattern.compile(placeString);
        movePattern = Pattern.compile(moveString);
        seekPattern = Pattern.compile(seekString);
        startGamePattern = Pattern.compile(startGameString);
        listPattern = Pattern.compile(listString);
        namePattern = Pattern.compile(nameString);

        try {
            clientReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            clientWriter = new PrintWriter(socket.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(ClientConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        clientConnections.add(this);
    }

    void sendOK() {
        send("OK");
    }

    void send(String st) {
        clientWriter.println(st);
        clientWriter.flush();
    }

    void updateList() {
        for (ClientConnection cc : clientConnections) {
            cc.send("List " + Seek.seeks.toString());
        }
    }

    void removeSeeks() {
        if (seek != null) {
            Seek.seeks.remove(seek.no);
            updateList();
        }
    }

    @Override
    public void run() {
        String temp;
        try {
            while ((temp = clientReader.readLine()) != null && !temp.equals("quit")) {
                Matcher m;

                if (name == null) {
                    if ((m = namePattern.matcher(temp)).find()) {
                        String tname = m.group(1).trim();
                        if (!names.contains(tname)) {
                            name = tname;
                            names.add(tname);
                            sendOK();
                        }
                    }
                } else {
                    //List all seeks
                    if ((m = listPattern.matcher(temp)).find()) {
                        send("List " + Seek.seeks.toString());
                    }
                    //Seek a game
                    else if ((m = seekPattern.matcher(temp)).find()) {
                        if (seek != null) {
                            Seek.seeks.remove(seek.no);
                        }
                        seek = new Seek(this, Integer.parseInt(m.group(1)));
                        Seek.seeks.put(seek.no, seek);
                        sendOK();
                        updateList();
                    }
                    //Start a game by accepting a seek
                    else if ((m = startGamePattern.matcher(temp)).find()) {
                        Seek sk = Seek.seeks.get(m.group(1));
                        if (sk != null) {
                            removeSeeks();
                            
                            ClientConnection otherClient = sk.client;
                            int sz = sk.boardSize;
                            otherClient.removeSeeks();
                            
                            game = new Game(this, otherClient, sz);
                            updateList();
                        }
                    }
                }
            }

            clientConnections.remove(this);
            socket.close();

            removeSeeks();
            if (name != null) {
                names.remove(name);
            }
        } catch (IOException ex) {
            Logger.getLogger(ClientConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
