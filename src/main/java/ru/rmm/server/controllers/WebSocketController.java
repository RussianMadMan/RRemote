package ru.rmm.server.controllers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import ru.rmm.rremote.comms.ServiceAnnounce;
import ru.rmm.server.RRemoteServerService;

import java.security.Principal;
import java.util.Map;

@Controller
public class WebSocketController {
        Logger logger = LoggerFactory.getLogger(WebSocketController.class);

        @Autowired
        @Qualifier("getRRemote")
        RRemoteServerService service;

        @MessageMapping("/updateservice")
        public void handleUpdateService(ServiceAnnounce announce, Principal principal){
                logger.warn("Updated services of {} {}", principal.getName(), announce);
                service.updateService(principal, announce);

        }



}
