package ru.rmm.server;


import lombok.extern.java.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import ru.rmm.server.controllers.WebSocketController;

import java.security.Principal;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSockConfiguration implements WebSocketMessageBrokerConfigurer {



    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public RRemoteServerService getRRemote(){
        return RRemoteServerService.getInstance();
    }


    private TaskScheduler messageBrokerTaskScheduler;

    Logger logger = LoggerFactory.getLogger(WebSockConfiguration.class);

    @Autowired
    public void setMessageBrokerTaskScheduler(@Lazy TaskScheduler taskScheduler) {
        this.messageBrokerTaskScheduler = taskScheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/device/wss").setAllowedOrigins("*");

    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        messageConverters.add(new MappingJackson2MessageConverter());
        return true;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app");
        config.enableSimpleBroker( "/queue")
                .setTaskScheduler(this.messageBrokerTaskScheduler)
                .setHeartbeatValue(new long[] {10000, 20000});
        config.setUserDestinationPrefix("/user");

    }

    @Autowired
    private ApplicationContext appContext;


    private void remove(Principal p){
        RRemoteServerService service = (RRemoteServerService) appContext.getBean("getRRemote");
        service.disconnectUser(p);
        logger.debug("User {} diconnected", p.getName());
    }


    @EventListener
    public void handleUnsub(SessionUnsubscribeEvent event){
        remove(event.getUser());

    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event){
        remove(event.getUser());
    }

}
