package ru.rmm.server.controllers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class WebSocketController {
        Logger logger = LoggerFactory.getLogger(WebSocketController.class);
        @MessageMapping("/hello")
        public void handle(@Headers Map headers, Principal principal)
        {
            logger.warn("{}, {}", headers, principal);
            //return "LUL: " + principal.getName();
        }

        @SubscribeMapping("/**/*")
        public void handleSub(@Headers Map headers, Principal principal){
            logger.warn("{}, {}", headers, principal);
        }
}
