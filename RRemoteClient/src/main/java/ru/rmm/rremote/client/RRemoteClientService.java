package ru.rmm.rremote.client;

import ch.qos.logback.core.net.ssl.SSL;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.util.MultiPartContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import ru.rmm.rremote.comms.Endpoints;

import javax.crypto.spec.PSource;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class RRemoteClientService {


    private static Logger logger = LoggerFactory.getLogger(RRemoteClientService.class);

    private WebSocketStompClient stompClient;
    private StompSession session;

    private final EventSource<RRemoteLifecycleEvent> lifecycle = new EventSource<>();
    private void call(RRemoteLifecycleEvent event){
        lifecycle.call(event);
    }

    private final String host;
    private final SSLContext sslContext;

    public void addLifecycleEventHandler(EventHandler<RRemoteLifecycleEvent> handler){
        lifecycle.addHandler(handler);
    }

    public void removeLifecycleEventHandler(EventHandler<RRemoteLifecycleEvent> handler){
        lifecycle.removeHandler(handler);
    }

    public RRemoteClientService(String host, SSLContext sslContext){
            this.host = host;
            this.sslContext = sslContext;
    }
    public void start(){
        logger.debug("Starting RRemoteClient with host {}", host);
        initStomp(sslContext);
        connectStomp(host);
    }

    public void setServiceMessageHandler(Class type, EventHandler<Object> handler){
        if(session == null){
            logger.error("Trying to subscribe without active session");
            throw new IllegalStateException("No connection");
        }
        var stomphandler = new RRemoteSessionHandler(this, handler, type);
        var sub = session.subscribe(Endpoints.ServiceMessageEndpoint, stomphandler);
    }

    public static KeyStore reqisterAtRRemote(String name, String host, SSLContext sslContext) throws Exception {
        final SslContextFactory ssl = new SslContextFactory.Client();
        ssl.setSslContext(sslContext);
        HttpClient httpClient = new HttpClient(ssl);
        httpClient.start();
        String url = String.format("https://%s%s", host, Endpoints.GenerateCertificateEndpoint);
        var multipart = new MultiPartContentProvider();
        multipart.addFieldPart("username", new StringContentProvider(name), null);
        multipart.addFieldPart("role", new StringContentProvider("ROLE_DEVICE"), null);
        var request = httpClient.POST(url);
        request.content(multipart);
        var response = request.send();
        if(response.getStatus() != 200){
            logger.error("Ошибка получения сертификата код {}", response.getStatus());
            return null;
        }
        url = String.format("https://%s%s", host, Endpoints.GetCertificateEndpoint);
        response = httpClient.GET(url);
        if(response.getStatus() != 200){
            logger.error("Ошибка скачивания сертификата код {}", response.getStatus());
            return null;
        }
        byte[] raw = response.getContent();
        ByteArrayInputStream is = new ByteArrayInputStream(raw);
        KeyStore newSSL = KeyStore.getInstance("PKCS12");
        newSSL.load(is, "12345".toCharArray());
        return newSSL;
    }

    private void initStomp(SSLContext sslContext){

        final SslContextFactory ssl = new SslContextFactory.Client();
        ssl.setSslContext(sslContext);
        HttpClient httpClient = new HttpClient(ssl);
        WebSocketClient client = new WebSocketClient(httpClient);
        JettyWebSocketClient jettyClient = new JettyWebSocketClient(client);
        stompClient = new WebSocketStompClient(jettyClient);
        stompClient.setTaskScheduler(new ConcurrentTaskScheduler());
        stompClient.start();
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    private void connectStomp(String host){
        String url = "wss://{host}/test"; //todo change endpoint
        var future = stompClient.connect(url, new RRemoteSessionHandler(this), host);
        future.addCallback(new ListenableFutureCallback<StompSession>() {
            @Override
            public void onFailure(Throwable ex) {
                logger.error("RRemote failed to connect to host {} with {}", host, ex);
                lifecycle.call(new RRemoteLifecycleEvent(RRemoteLifecycleEvent.Type.CONNECTION_FAILURE, ex));
            }

            @Override
            public void onSuccess(StompSession result) {
                logger.info("RRemote connected to host {}", host);
                session = result;
                lifecycle.call(new RRemoteLifecycleEvent(RRemoteLifecycleEvent.Type.CONNECTION_SUCCESS, null));
            }
        });
    }

    private class RRemoteSessionHandler extends StompSessionHandlerAdapter {

        private final RRemoteClientService service;
        private final EventSource<Object> source = new EventSource<>();
        private Class type = String.class;


        RRemoteSessionHandler(RRemoteClientService service){
            this.service = service;
        }

        public RRemoteSessionHandler(RRemoteClientService service, EventHandler<Object> handler, Class cl){
            this.service = service;
            source.addHandler(handler);
            type = cl;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return type;
        }

        @Override
        public void handleFrame(StompHeaders headers, @Nullable Object payload) {
            source.call(payload);
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {

        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            logger.warn("Transport error", exception);
            if(!session.isConnected()){
                service.call(new RRemoteLifecycleEvent(RRemoteLifecycleEvent.Type.DISCONNECTED, exception));
                service.connectStomp(service.host);
            }
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            logger.error("Exception with {}{}{}{}", session, command, headers, exception);
            service.call(new RRemoteLifecycleEvent(RRemoteLifecycleEvent.Type.MESSAGE_ERROR, exception));
        }
    }

    @Data
    @AllArgsConstructor
    public static class RRemoteLifecycleEvent{
        public enum Type{
            CONNECTION_SUCCESS, CONNECTION_FAILURE, DISCONNECTED, MESSAGE_ERROR
        }
        Type type;
        Throwable ex;
    }
    public static class RRemoteMessageEvent<T>{

    }
}

class EventSource<T>{
    Set<EventHandler<T>> handlers = new HashSet<>();
    void addHandler(EventHandler<T> handler){
        handlers.add(handler);
    }

    void removeHandler(EventHandler<T> handler){
        handlers.remove(handler);
    }

    void call(T event){
        handlers.forEach((handler)->handler.handle(event));
    }
}