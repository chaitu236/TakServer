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

    String loginString = "^Login ([a-zA-Z][a-zA-Z0-9_]{3,15}) ([a-zA-Z0-9_]{3,50})";
    Pattern loginPattern;
    
    String loginGuestString = "^Login Guest";
    Pattern loginGuestPattern;
    
    String registerString = "^Register ([a-zA-Z][a-zA-Z0-9_]{3,15}) ([A-Za-z.0-9_+!#$%&'*^?=-]{1,30}@[A-Za-z.0-9-]{3,30})";
    Pattern registerPattern;
    
    String wrongRegisterString = "^Register [^\n\r]{1,256}";
    Pattern wrongRegisterPattern;
    
    String clientString = "^Client ([A-Za-z-.0-9]{4,15})";
    Pattern clientPattern;
    
    String placeString = "^Game#(\\d+) P ([A-Z])(\\d)( C)?( W)?";
    Pattern placePattern;

    String moveString = "^Game#(\\d+) M ([A-Z])(\\d) ([A-Z])(\\d)(( \\d)+)";
    Pattern movePattern;
    
    String drawString = "^Game#(\\d+) OfferDraw";
    Pattern drawPattern;
    
    String removeDrawString = "^Game#(\\d+) RemoveDraw";
    Pattern removeDrawPattern;
    
    String resignString = "^Game#(\\d+) Resign";
    Pattern resignPattern;

    String seekString = "^Seek (\\d) (\\d+)";
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
    
    String shoutString = "^Shout ([^\n\r]{1,256})";
    Pattern shoutPattern;
    
    String pingString = "^PING$";
    Pattern pingPattern;
    
    Client(Socket socket) {
        this.socket = socket;
        try {
            this.socket.setSoTimeout(90*1000);
            this.socket.setTcpNoDelay(true);
        } catch (SocketException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.clientNo = totalClients++;

        loginPattern = Pattern.compile(loginString);
        registerPattern = Pattern.compile(registerString);
        clientPattern = Pattern.compile(clientString);
        placePattern = Pattern.compile(placeString);
        movePattern = Pattern.compile(moveString);
        drawPattern = Pattern.compile(drawString);
        removeDrawPattern = Pattern.compile(removeDrawString);
        resignPattern = Pattern.compile(resignString);
        wrongRegisterPattern = Pattern.compile(wrongRegisterString);
        seekPattern = Pattern.compile(seekString);
        acceptSeekPattern = Pattern.compile(acceptSeekString);
        listPattern = Pattern.compile(listString);
        gameListPattern = Pattern.compile(gameListString);
        gamePattern = Pattern.compile(gameString);
        observePattern = Pattern.compile(observeString);
        unobservePattern = Pattern.compile(unobserveString);
        getSqStatePattern = Pattern.compile(getSqStateString);
        shoutPattern = Pattern.compile(shoutString);
        pingPattern = Pattern.compile(pingString);
        loginGuestPattern = Pattern.compile(loginGuestString);

        try {
            clientReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            clientWriter = new PrintWriter(socket.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        clientConnections.add(this);
        spectating = new ArrayList<>();
        Log("Connected "+socket.getInetAddress());
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
    
    void sendWithoutLogging(String st) {
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
            g.unSpectate(this);
        spectating.clear();
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
    
    static void sendAllOnline(final String msg) {
        new Thread() {
            @Override
            public void run() {
                for(Client c: clientConnections)
                    if(c.player!=null)
                        c.send(msg);
            }
        }.start();
    }

    void clientQuit() throws IOException {
        clientConnections.remove(this);
        if(game!=null){
            game.playerQuit(player);
        }

        Seek.unregisterListener(this);
        Game.unregisterGameListListener(this);
        removeSeeks();
        unspectateAll();
        if(game!=null)
            Game.removeGame(game);

        if (player != null) {
            player.logout();
            sendAllOnline("Online "+(--onlineClients));
        }

        socket.close();
        Log("disconnected");
    }
    
    void Log(Object obj) {
        TakServer.Log(clientNo+":"+((player!=null)?player.getName():"")+":"+obj);
    }
    
    void removeGame(Game g) {
        game = null;
    }

    @Override
    public void run() {
        String temp;
        try {
            send("Welcome!");
            send("Login or Register");
            while ((temp = clientReader.readLine()) != null && !temp.equals("quit")) {
                temp = temp.trim();
                //don't log ping patterns
                if((pingPattern.matcher(temp)).find()) {
                    sendWithoutLogging("OK");
                    continue;
                }
                Log("Read:"+temp);
                
                Matcher m;

                if (player == null) {
                    //Client name set
                    if((m = clientPattern.matcher(temp)).find()){
                        Log("Client "+m.group(1));
                    }
                    //Login Guest
                    else if ((loginGuestPattern.matcher(temp)).find()) {
                        player = new Player();
                        player.login(this);

                        send("Welcome "+player.getName()+"!");
                        Log("Player logged in");
                        
                        Seek.registerListener(this);
                        Game.registerGameListListener(this);
                        
                        sendAllOnline("Online "+(++onlineClients));
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
                                    
                                    Seek.registerListener(this);
                                    Game.registerGameListListener(this);
                                    
                                    sendAllOnline("Online "+(++onlineClients));
                                }
                            } else
                                send("Authentication failure");
                        }
                    }
                    //Registration
                    else if ((m = registerPattern.matcher(temp)).find()) {
                        String tname = m.group(1).trim();
                        if(tname.toLowerCase().contains("guest")) {
                            send("Can't register with guest in the name");
                        } else {
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
                        }
                    }
                    //Wrong registration chars
                    else if ((wrongRegisterPattern.matcher(temp)).find()) {
                        send("Unknown format for username/email. Only [a-z][A-Z][0-9][_] allowed for username, it should be 4-16 characters and should start with letter");
                    }
                    else
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
                            seek = null;
                        } else {
                            seek = Seek.newSeek(this, Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
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
                            int time = sk.time;
                            otherClient.removeSeeks();

                            unspectateAll();
                            otherClient.unspectateAll();
                            
                            game = new Game(player, otherClient.player, sz, time);
                            Game.addGame(game);
                            otherClient.game = game;
                            
                            sendOK();
                            String msg = "Game Start " + game.no +" "+sz+" "+game.white.getName()+" vs "+game.black.getName();
                            send(msg+" "+((game.white==player)?"white":"black")+" "+time);
                            otherClient.send(msg+" "+((game.white==otherClient.player)?"white":"black")+" "+time);
                        } else {
                            sendNOK();
                        }
                    }
                    //Handle place move
                    else if (game != null && (m = placePattern.matcher(temp)).find() && game.no == Integer.parseInt(m.group(1))) {
                        Status st = game.placeMove(player, m.group(2).charAt(0), Integer.parseInt(m.group(3)), m.group(4) != null, m.group(5)!=null);
                        if(st.isOk()){
                            sendOK();
                            Client other = (game.white==player)?game.black.getClient():game.white.getClient();
                            
                            if(game.gameState!=Game.gameS.NONE){
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
                        Status st = game.moveMove(player, m.group(2).charAt(0), Integer.parseInt(m.group(3)), m.group(4).charAt(0), Integer.parseInt(m.group(5)), argsint);
                        if(st.isOk()){
                            sendOK();
                            Client other = (game.white==player)?game.black.getClient():game.white.getClient();
                            if(game.gameState!=Game.gameS.NONE){
                                Game.removeGame(game);
                                game = null;
                                other.game = null;
                            }
                        } else {
                            sendNOK();
                            send("Error:"+st.msg());
                        }
                    }
                    //Handle draw offer
                    else if (game!=null && (m = drawPattern.matcher(temp)).find() && game.no == Integer.parseInt(m.group(1))) {
                        game.draw(player);
                        Client other = (game.white==player)?game.black.getClient():game.white.getClient();
                        if(game.gameState!=Game.gameS.NONE){
                            Game.removeGame(game);
                            game = null;
                            other.game = null;
                        }
                    }
                    //Handle removing draw offer
                    else if (game!=null && (m = removeDrawPattern.matcher(temp)).find() && game.no == Integer.parseInt(m.group(1))) {
                        game.removeDraw(player);
                    }
                    //Handle resignation
                    else if (game!=null && (m = resignPattern.matcher(temp)).find() && game.no == Integer.parseInt(m.group(1))) {
                        game.resign(player);
                        Client other = (game.white==player)?game.black.getClient():game.white.getClient();
                        if(game.gameState!=Game.gameS.NONE){
                            Game.removeGame(game);
                            game = null;
                            other.game = null;
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
                        String msg = "<"+player.getName()+"> "+m.group(1);
                        sendAllOnline("Shout "+msg);
                        IRCBridge.send(msg);
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
            sendAll("Message "+msg);
            
            int sleep=Integer.parseInt(br.readLine());
            TakServer.Log("sleeping "+sleep+" seconds");
            Thread.sleep(sleep);
            sendAll("Message "+br.readLine());
            
            TakServer.Log("Exiting");
        } catch (IOException | NumberFormatException | InterruptedException ex) {
            TakServer.Log(ex);
        }
        
    }
}
