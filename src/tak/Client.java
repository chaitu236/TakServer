package tak;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
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
public class Client extends Thread {

    Socket socket;
    BufferedReader clientReader;
    PrintWriter clientWriter;
    Player player = null;
    int clientNo;
    
    static int totalClients=0;
    static int onlineClients=0;

    static Set<Client> clientConnections = new HashSet<>();

    Game game = null;
    Seek seek = null;
    ArrayList<Game> spectating;

    String loginString = "^Login ([a-zA-Z][a-zA-Z0-9_]{3,9}) ([a-zA-Z0-9_]{3,50})";
    Pattern loginPattern;
    
    String registerString = "^Register ([a-zA-Z][a-zA-Z0-9_]{3,9}) ([A-Za-z.0-9_-]{1,30}@[A-Za-z.0-9_-]{3,30})";
    Pattern registerPattern;
    
    String clientString = "^Client ([A-Za-z-.0-9]{4,15})";
    Pattern clientPattern;
    
    String placeString = "^Game#(\\d+) P ([A-Z])(\\d)( C)?( W)?";
    Pattern placePattern;

    String moveString = "^Game#(\\d+) M ([A-Z])(\\d) ([A-Z])(\\d)(( \\d)+)";
    Pattern movePattern;

    String seekString = "^Seek (\\d)";
    Pattern seekPattern;

    String acceptSeekString = "^Accept (\\d+)";
    Pattern acceptSeekPattern;

    String listString = "^List";
    Pattern listPattern;

    String gameListString = "^GameList";
    Pattern gameListPattern;
    
    String observeString = "^Observe (\\d+)";
    Pattern observePattern;
    
    String unobserveString = "^Unobserve (\\d+)";
    Pattern unobservePattern;

    String gameString = "^Game#(\\d+) Show$";
    Pattern gamePattern;
    
    String getSqStateString = "^Game#(\\d+) Show ([A-Z])(\\d)";
    Pattern getSqStatePattern;
    
    String shoutString = "Shout ([^\n\r]{1,256})";
    Pattern shoutPattern;
    
    Client(Socket socket) {
        this.socket = socket;
        try {
            socket.setTcpNoDelay(true);
        } catch (SocketException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.clientNo = totalClients++;

        loginPattern = Pattern.compile(loginString);
        registerPattern = Pattern.compile(registerString);
        clientPattern = Pattern.compile(clientString);
        placePattern = Pattern.compile(placeString);
        movePattern = Pattern.compile(moveString);
        seekPattern = Pattern.compile(seekString);
        acceptSeekPattern = Pattern.compile(acceptSeekString);
        listPattern = Pattern.compile(listString);
        gameListPattern = Pattern.compile(gameListString);
        gamePattern = Pattern.compile(gameString);
        observePattern = Pattern.compile(observeString);
        unobservePattern = Pattern.compile(unobserveString);
        getSqStatePattern = Pattern.compile(getSqStateString);
        shoutPattern = Pattern.compile(shoutString);

        try {
            clientReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            clientWriter = new PrintWriter(socket.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        clientConnections.add(this);
        Seek.registerListener(this);
        Game.registerGameListListener(this);
        spectating = new ArrayList<>();
        Log("Connected");
    }

    void sendOK() {
        send("OK");
    }

    void sendNOK() {
        send("NOK");
    }

    void send(String st) {
        Log("Send:"+st);
        clientWriter.println(st);
        clientWriter.flush();
    }

    void removeSeeks() {
        if (seek != null) {
            Seek.removeSeek(seek.no);
            seek = null;
        }
    }
    
    void unspectateAll() {
        for(Game g: spectating)
            g.spectators.remove(this);
    }
    
    static void sendAll(final String msg) {
        new Thread() {
            @Override
            public void run() {
                for(Client c: clientConnections)
                    c.send(msg);
            }
        }.start();
    }

    void clientQuit() throws IOException {
        clientConnections.remove(this);
        if(game!=null){
            game.clientQuit(this);
        }

        Seek.unregisterListener(this);
        Game.unregisterGameListListener(this);
        removeSeeks();
        unspectateAll();
        if(game!=null)
            Game.removeGame(game);

        if (player != null) {
            player.logout();
            sendAll("Online "+(--onlineClients));
        }

        socket.close();
        Log("disconnected");
    }
    
    void Log(Object obj) {
        TakServer.Log(clientNo+":"+((player!=null)?player.getName():"")+":"+obj);
    }

    @Override
    public void run() {
        String temp;
        try {
            send("Welcome!");
            send("Login or Register");
            while ((temp = clientReader.readLine()) != null && !temp.equals("quit")) {
                temp = temp.trim();
                Log("Read:"+temp);
                
                Matcher m;

                if (player == null) {
                    //Client name set
                    if((m = clientPattern.matcher(temp)).find()){
                        Log("Client "+m.group(1));
                    }
                    //Login
                    else if ((m = loginPattern.matcher(temp)).find()) {
                        String tname = m.group(1).trim();
                        synchronized(Player.players) {
                            if (Player.players.containsKey(tname)) {
                                Player tplayer = Player.players.get(tname);
                                String pass = m.group(2).trim();
                                
                                if(!pass.equals(tplayer.getPassword())) {
                                    send("Authentication failure");
                                } else if(tplayer.isLoggedIn()) {
                                    send("You're already logged in");
                                } else {
                                    player = tplayer;
                                    player.login(this);
                                    
                                    send("Welcome "+player.getName()+"!");
                                    Log("Player logged in");
                                    Seek.sendListTo(this);
                                    Game.sendGameListTo(this);
                                    sendAll("Online "+(++onlineClients));
                                }
                            } else
                                send("Authentication failure");
                        }
                    }
                    //Registration
                    else if ((m = registerPattern.matcher(temp)).find()) {
                        String tname = m.group(1).trim();
                        synchronized(Player.players) {
                            if (Player.players.containsKey(tname)) {
                                send("Name already taken");
                            }
                            else {
                                String email = m.group(2).trim();
                                Player tplayer = Player.createPlayer(tname, email);
                                send("Registered "+tplayer.getName()+". Check your email for password");
                            }
                        }
                    } else
                        send("Login or Register");
                } else {
                    //List all seeks
                    if ((m = listPattern.matcher(temp)).find()) {
                        sendOK();
                        Seek.sendListTo(this);
                    } //Seek a game
                    else if (game==null && (m = seekPattern.matcher(temp)).find()) {
                        if (seek != null) {
                            Seek.removeSeek(seek.no);
                        }
                        int no = Integer.parseInt(m.group(1));
                        if(no == 0) {
                            Log("Seek remove");
                        } else {
                            seek = Seek.newSeek(this, Integer.parseInt(m.group(1)));
                            Log("Seek "+seek.boardSize);
                        }
                        sendOK();
                    } //Accept a seek
                    else if (game==null && (m = acceptSeekPattern.matcher(temp)).find()) {
                        Seek sk = Seek.seeks.get(Integer.parseInt(m.group(1)));

                        if (sk != null && game == null && sk.client.game == null && sk!=seek) {
                            removeSeeks();

                            Client otherClient = sk.client;
                            int sz = sk.boardSize;
                            otherClient.removeSeeks();

                            spectating.clear();
                            unspectateAll();
                            otherClient.unspectateAll();
                            otherClient.spectating.clear();
                            
                            game = new Game(this, otherClient, sz);
                            Game.addGame(game);
                            otherClient.game = game;
                            
                            sendOK();
                            String msg = "Game Start " + game.no +" "+sz+" "+game.white.player.getName()+" vs "+game.black.player.getName();
                            send(msg+" "+((game.white==this)?"white":"black"));
                            otherClient.send(msg+" "+((game.white==otherClient)?"white":"black"));
                        } else {
                            sendNOK();
                        }
                    }
                    //Handle place move
                    else if (game != null && (m = placePattern.matcher(temp)).find() && game.no == Integer.parseInt(m.group(1))) {
                        Status st = game.placeMove(this, m.group(2).charAt(0), Integer.parseInt(m.group(3)), m.group(4) != null, m.group(5)!=null);
                        if(st.isOk()){
                            sendOK();
                            //game.white.send(game.toString());
                            //game.black.send(game.toString());
                            Client other = (game.white==this)?game.black:game.white;
                            other.send(temp);
                            game.moveList.add(temp);
                            game.sendToSpectators(temp);
                            
                            if(game.gameState!=game.gameState.NONE){
                                String msg = "Game#"+game.no+" Over ";
                                switch(game.gameState) {
                                    case DRAW: msg+= "1/2-1/2"; break;
                                    case WHITE_ROAD: msg+="R-0"; break;
                                    case BLACK_ROAD: msg+="0-R"; break;
                                    case WHITE_TILE: msg+="F-0"; break;
                                    case BLACK_TILE: msg+="0-F"; break;
                                }
                                send(msg);
                                other.send(msg);
                                game.moveList.add(msg);
                                game.sendToSpectators(msg);
                                Game.removeGame(game);
                                game = null;
                                other.game = null;
                            }
                        } else {
                            sendNOK();
                            send("Error:"+st.msg());
                        }
                    }
                    //Handle move move
                    else if (game!=null && (m = movePattern.matcher(temp)).find() && game.no == Integer.parseInt(m.group(1))) {
                        String args[] = m.group(6).split(" ");
                        int argsint[] = new int[args.length-1];
                        for(int i=1;i<args.length;i++)
                            argsint[i-1] = Integer.parseInt(args[i]);
                        Status st = game.moveMove(this, m.group(2).charAt(0), Integer.parseInt(m.group(3)), m.group(4).charAt(0), Integer.parseInt(m.group(5)), argsint);
                        if(st.isOk()){
                            sendOK();
                            Client other = (game.white==this)?game.black:game.white;
                            other.send(temp);
                            game.moveList.add(temp);
                            game.sendToSpectators(temp);
                            if(game.gameState!=Game.gameS.NONE){
                                String msg = "Game#"+game.no+" Over ";
                                switch(game.gameState) {
                                    case DRAW: msg+= "1/2-1/2"; break;
                                    case WHITE_ROAD: msg+="R-0"; break;
                                    case BLACK_ROAD: msg+="0-R"; break;
                                    case WHITE_TILE: msg+="F-0"; break;
                                    case BLACK_TILE: msg+="0-F"; break;
                                }
                                send(msg);
                                other.send(msg);
                                game.moveList.add(msg);
                                game.sendToSpectators(msg);
                                Game.removeGame(game);
                                game = null;
                                other.game = null;
                            }
                        } else {
                            sendNOK();
                            send("Error:"+st.msg());
                        }
                    }
                    //Show game state
                    else if (game != null && (m=gamePattern.matcher(temp)).find() && game.no == Integer.parseInt(m.group(1))) {
                        sendOK();
                        send(game.toString());
                    }
                    //Show sq state for a game
                    else if (game != null && (m=getSqStatePattern.matcher(temp)).find() && game.no == Integer.parseInt(m.group(1))) {
                        send("Game#"+game.no+" Show Sq "+game.sqState(m.group(2).charAt(0), Integer.parseInt(m.group(3))));
                    }
                    //GameList
                    else if ((m=gameListPattern.matcher(temp)).find()){
                        Game.sendGameListTo(this);
                    }
                    //ObserveGame
                    else if ((m=observePattern.matcher(temp)).find()){
                        Game game = Game.games.get(Integer.parseInt(m.group(1)));
                        if(game!=null){
                            if(spectating.contains(game)) {
                                send("Message you're already observing this game");
                            } else {
                                spectating.add(game);
                                game.newSpectator(this);
                            }
                        } else
                            sendNOK();
                    }
                    //UnobserveGame
                    else if ((m=unobservePattern.matcher(temp)).find()){
                        Game game = Game.games.get(Integer.parseInt(m.group(1)));
                        if(game!=null){
                            spectating.remove(game);
                            game.unSpectate(this);
                        } else
                            sendNOK();
                    }
                    //Shout
                    else if ((m=shoutPattern.matcher(temp)).find()){
                        sendAll("Shout "+player.getName()+": "+m.group(1));
                    }
                    //Undefined
                    else {
                        sendNOK();
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                clientQuit();
            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    static void sigterm() {
        TakServer.Log("Sigterm!");
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File("message")));
            String msg = br.readLine();
            for(Client c: clientConnections)
                c.send("Message "+msg);
            int sleep=Integer.parseInt(br.readLine());
            TakServer.Log("sleeping "+sleep+" seconds");
            Thread.sleep(sleep);
            for(Client c: clientConnections)
                c.send("Message "+br.readLine());
            TakServer.Log("Exiting");
        } catch (IOException | NumberFormatException | InterruptedException ex) {
            TakServer.Log(ex);
        }
        
    }
}
