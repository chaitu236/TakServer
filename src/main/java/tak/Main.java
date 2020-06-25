package tak;

/**
 *
 * @author chaitu
 */

public class Main {
    public static void main(String[] args) {
        Settings.parse();
        Database.initConnection();
        Player.loadFromDB();

        IRCBridge.init();

        if(args.length>0)
            TakServer.port = Integer.parseInt(args[0]);

        TakServer takServer = new TakServer();
        takServer.start();
        TakServer.Log("dir: "+System.getProperty("user.dir"));
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
