package ru.rmm.server.controllers;

import ch.qos.logback.core.pattern.util.RegularEscapeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.rmm.rremote.comms.ServiceMessage;
import ru.rmm.server.RRemoteServerService;
import ru.rmm.server.models.MyUserPrincipal;
import ru.rmm.server.models.RRemoteUser;
import ru.rmm.server.models.RRemoteUserRepo;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class UserController {

    @Autowired
    SimpMessagingTemplate template;
    @Autowired
    RRemoteUserRepo repo;
    @Autowired
    RRemoteServerService service;

    @GetMapping("/user/sendmsg")
    public ResponseEntity<String> handleMSG(@RequestParam String client, @RequestParam String service, @RequestParam String command, HttpServletRequest request){
        var rremoteClient = repo.findByUsername(client);
        //logger.debug("principal class is {}", request.getUserPrincipal().getClass());
        var myprincipal = MyUserPrincipal.extract(request.getUserPrincipal());
        var rremoteSender = myprincipal.user;
        if(rremoteClient == null){
            return ResponseEntity.status(400).body(null);
        }
        if(!rremoteClient.friends.contains(rremoteSender)){
            return ResponseEntity.status(403).body(null);
        }
        var msg = new ServiceMessage(service, command, "id", new HashMap<>());
        template.convertAndSendToUser(client, "/queue/service", msg);
        return ResponseEntity.status(200).body("OK");
    }

    @GetMapping("/user/")
    public String getUserPage(Model model, Principal p){
        var myprincipal = MyUserPrincipal.extract(p);
        var rRemoteUser = myprincipal.user;
        var clients = repo.findRRemoteUsersByFriendsContains(rRemoteUser);
        var clientNamesList = clients.stream()
                                    .map(client -> client.username)
                                    .collect(Collectors.toList());
        var services = service.getClients(clientNamesList);
        model.addAttribute("services", services);
        return "user";
    }

    @GetMapping("/user/addfriend")
    public ResponseEntity<String> addFriend(@RequestParam String token, Principal p){
        var name = service.getUserNameForToken(token);
        if(name == null){
            ResponseEntity.status(400).body(null);
        }
        var owner = repo.findByUsername(name);
        var user = repo.findByUsername(p.getName());
        assert(owner != null);
        assert(user != null);
        owner.friends.add(user);
        user.friends.add(owner);
        repo.saveAll(List.of(owner, user));
        return ResponseEntity.status(200).body("OK");
    }
}
