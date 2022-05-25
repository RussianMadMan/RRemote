package ru.rmm.server;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.stereotype.Controller;
import ru.rmm.rremote.comms.Service;
import ru.rmm.rremote.comms.ServiceAnnounce;
import ru.rmm.server.models.ClientService;
import ru.rmm.server.models.FriendshipToken;

import javax.persistence.criteria.From;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class RRemoteServerService {
    private HashMap<String, ClientService> clients;
    private HashMap<String, FriendshipToken> tokens;


    private static Logger logger = LoggerFactory.getLogger(RRemoteServerService.class);

    private static final RRemoteServerService currentInstance = new RRemoteServerService();
    private RRemoteServerService(){
        logger.debug("CONSTRUCTOR");
        clients = new HashMap<>();
        tokens = new HashMap<>();
    }


    public static RRemoteServerService getInstance(){
        logger.debug("GET INSTANCE");
        return currentInstance;
    }

    public synchronized void updateService(Principal name, ServiceAnnounce announce){
        clients.put(name.getName(), new ClientService(name, announce.services));
        logger.debug("client list updated {}", clients);
    }

    public synchronized List<ClientService> getClients(Iterable<String> names){
        logger.debug("client list requested {}", clients);
        ArrayList<ClientService> arrayList = new ArrayList<>(10);
        for(String name : names){
            var services = clients.get(name);
            if(services != null){
                arrayList.add(services);
            }
        }
        return arrayList;
    }

    public synchronized List<ClientService> getClients(){
        logger.debug("client list requested {}", clients);
        ArrayList<ClientService> arrayList = new ArrayList<>(clients.size());
        arrayList.addAll(clients.values());
        return arrayList;
    }

    public synchronized  void disconnectUser(Principal p){
        clients.remove(p.getName());
        logger.debug("client list updated {}", clients);
    }

    @Scheduled(fixedRate=60000)
    public synchronized void cleanUpTokens(){
        Date now = new Date();
        tokens.values().removeIf(token -> token.validThrough.before(now));
    }
    public synchronized String generateTokenFor(Principal p){
            var ran = new Random();
            String token;
            do {
                var code = ran.nextInt(10000);
                token = String.format("%04d", code);
                if(!tokens.containsKey(token)){
                    Date expireDate = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));

                    tokens.put(token, new FriendshipToken(p.getName(), expireDate));
                    break;
                }
            }while(true);
            return token;
    }

    public synchronized String getUserNameForToken(String token){
        var owner = tokens.get(token);
        var now = new Date();
        if(owner == null || owner.validThrough.before(now)){
            return null;
        }
        return owner.name;

    }
}
