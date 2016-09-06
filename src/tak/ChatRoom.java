/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author chaitu
 */
public class ChatRoom {
    static final ConcurrentHashMap<String, ChatRoom> chatRooms = new ConcurrentHashMap<>();
    
    private final String name;
    static Set<Client> members;
    
    ChatRoom(String name) {
        this.name = name;
        members = Collections.synchronizedSet(new HashSet<Client>());
    }
    
    public static ChatRoom addRoom(String name) {
        if(chatRooms.containsKey(name))
            return null;
        
        ChatRoom room = new ChatRoom(name);
        chatRooms.put(name, room);
        return room;
    }
    
    private static void removeRoom(String name) {
        chatRooms.remove(name);
    }
    
    public static void removeRoomIfEmpty(String name) {
        ChatRoom room = ChatRoom.get(name);
        if(room.isEmpty())
            removeRoom(name);
    }
    
    public static ChatRoom get(String roomName) {
        return chatRooms.get(roomName);
    }
    
    public String getName() {
        return name;
    }
    
    public void shout(Client client, String msg) {        
        if(!contains(client)) {
            client.sendNOK();
            client.send("Error:"+"You need to be in the room");
            return;
        }
        
        sendAll("ShoutRoom "+name+" <"+client.player.getName()+"> "+msg);
    }
    
    public void addMember(Client client) {
        members.add(client);
    }
    
    public boolean isEmpty() {
        return members.isEmpty();
    }
    
    public void removeMember(Client client) {
        members.remove(client);
        
        if(isEmpty())
            ChatRoom.removeRoom(name);
    }
    
    private boolean contains(Client client) {
        return members.contains(client);
    }
    
    private void sendAll(final String msg) {
        new Thread() {
            @Override
            public void run() {
                for (Client cc : members) {
                    cc.sendWithoutLogging(msg);
                }
            }
        }.start();
    }
}
