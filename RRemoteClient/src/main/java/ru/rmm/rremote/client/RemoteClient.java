package ru.rmm.rremote.client;


import ch.qos.logback.core.net.ssl.SSL;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import ru.rmm.rremote.comms.ServiceMessage;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerContainer;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.prefs.Preferences;

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
        RRemoteClientService rremote = new RRemoteClientService(host, context);
        rremote.addLifecycleEventHandler(event -> logger.info("Lifecycle Event {}", event));
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
