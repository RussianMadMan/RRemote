package ru.rmm.server.controllers;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.rmm.server.RRemoteServerService;
import ru.rmm.server.models.MyUserPrincipal;
import ru.rmm.server.models.RRemoteUser;
import ru.rmm.server.models.RRemoteUserRepo;

import java.security.Principal;
import java.util.stream.Collectors;

@Controller
public class DeviceController {

    @Autowired
    RRemoteServerService service;

    @GetMapping("/device/token")
    public ResponseEntity<String> generateFriendshipToken(Principal p){
        String token = service.generateTokenFor(p);
        return ResponseEntity.status(200).body(token);
    }


    @GetMapping("/device/friendlist")
    public @ResponseBody String getFriendList(Principal p){
        var friends = MyUserPrincipal.extract(p).user.getFriends();
        StringBuilder friendString = new StringBuilder();
        friends.stream()
                .map(rRemoteUser -> rRemoteUser.username)
                .forEach(s -> {
                    friendString.append(s).append("\r\n");
                });
        return  friendString.toString();
    }


}
