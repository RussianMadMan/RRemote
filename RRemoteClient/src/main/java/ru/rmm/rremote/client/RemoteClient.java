package ru.rmm.rremote.client;


import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.rmm.rremote.comms.Service;

import java.util.prefs.Preferences;

import static ru.rmm.rremote.client.RRemoteClientService.RRemoteLifecycleEvent.Type.CONNECTION_SUCCESS;

public class RemoteClient{
    private static final Logger logger = LoggerFactory.getLogger(RemoteClient.class);
    public static void main(String[] args) throws Exception{
        var opts = genOpts();
        DefaultParser parser = new DefaultParser();

        var cml = parser.parse(opts, args);
        String CMDname = cml.getOptionValue("name");
        String CMDhost = cml.getOptionValue("host");
        String optionsHost =  getOptionFromSystem("host");

        String name = CMDname;
        String host = chooseOption(CMDhost, optionsHost);
        logger.info("Started with host {} and name {}", host, name);
        if(host == null){
            logger.error("No hostname set");
            return;
        }
        if(!SSLManager.isCACertPresent()){
            logger.error("Нет корневого сертфиката");
            return;
        }
        if(!SSLManager.isDeviceCertPresent()){
            logger.warn("No client certificate found, trying to register...");
            if(name == null){
                logger.error("No client certificate and device name is not set");
                return;
            }
            try {
                SSLManager.registerDevice(name, host);
                //sanity check
                if(!SSLManager.isDeviceCertPresent()){
                    logger.error("Registration complete but still no client certificate file found!");
                    return;
                }
            }catch(Exception ex){
                logger.error("Registration failed with exception", ex);
                return;
            }
        }
        var context = SSLManager.getSSLContext();
        var secondContext = SSLManager.getSSLContext();
        RRemoteClientService rremote = new RRemoteClientService(host, context);
        rremote.addLifecycleEventHandler(event -> {
            logger.info("Lifecycle Event {}", event);
            if(event.type.equals(CONNECTION_SUCCESS)){
                Service testService = new Service("Test Service", new Service.ServiceCommand[]{new Service.ServiceCommand("Test", new String[0])});
                rremote.announceServices(new Service[]{testService});
                rremote.setServiceMessageHandler(event1 -> logger.info("Service message {}", event1));
                var token = rremote.getFriendToken(secondContext);
                logger.warn("Token: {}", token);
                var friends = rremote.getFriends(secondContext);
                logger.warn("Friends: {}", friends);
            }
        });
        rremote.start();
        while(true){
            Thread.sleep(1000);
        }
    }

    private static String getOptionFromSystem(String name){
        Preferences prefs = Preferences.userNodeForPackage(RemoteClient.class);
        return prefs.get(name, null);
    }

    private static String chooseOption(String... args){
        for(String s : args){
            if(s != null)
                return s;
        }
        return null;
    }

    private static Options genOpts(){
        Options opts = new Options();
        opts.addOption("h", "host", true, "Hostname of RRemote Server" );
        opts.addOption("n", "name", true, "Name of this device for registration");
        return opts;
    }

}
