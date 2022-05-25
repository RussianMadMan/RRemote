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
import ru.rmm.rremote.comms.Service;
import ru.rmm.rremote.comms.ServiceAnnounce;
import ru.rmm.rremote.comms.ServiceMessage;

import javax.crypto.spec.PSource;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static ru.rmm.rremote.comms.Endpoints.DeviceGetFriends;

public class RRemoteClientService {


    private static Logger logger = LoggerFactory.getLogger(RRemoteClientService.class);

    private WebSocketStompClient stompClient;
    private StompSession session;

    private HttpClient client;


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

    public void setServiceMessageHandler( EventHandler<ServiceMessage> handler){
        checkSession("Trying to subscribe without active session");
        var stomphandler = new RRemoteSessionHandler<ServiceMessage>(this, handler, ServiceMessage.class);
        var sub = session.subscribe(Endpoints.ServiceMessageEndpoint, stomphandler);
    }
    public void announceServices(Service[] services){
        checkSession("Trying to subscribe without active session");
        ServiceAnnounce msg = new ServiceAnnounce(services);
        logger.debug("Announcing {} to {}", services, Endpoints.ServiceAnnounceEndpoint);
        session.send(Endpoints.ServiceAnnounceEndpoint, msg);
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

    public String getFriendToken(SSLContext clientCert){
        checkSession("Trying to generate tokens with no session");

        try {
            var token = doGet(clientCert, Endpoints.DeviceGenerateToken);
            return token;
        }catch (Exception ex){
            logger.error("Error generating token", ex);
            return null;
        }
    }

    public String[] getFriends(SSLContext clientCert){
        checkSession("Trying to generate tokens with no session");
        try{
            var friends = doGet(clientCert, Endpoints.DeviceGetFriends);
            if(friends == null)
                return null;
            return friends.split("[\r\n]");
        }catch (Exception ex){
            logger.error("Error getting friend list", ex);
            return null;
        }
    }
    private String doGet(SSLContext context, String endpoint) throws Exception {
        String url = String.format("https://%s%s", host, endpoint);
        var response = client.GET(url);
        if(response.getStatus() != 200){
            logger.error("Error performing HTTP GET to {} {}", url, response.getStatus());
            return null;
        }
        return response.getContentAsString();
    }
    private void checkSession(String message){
        if(session == null){
            logger.error(message);
            throw new IllegalStateException("No connection");
        }
    }

    private void initStomp(SSLContext sslContext){

        final SslContextFactory ssl = new SslContextFactory.Client();
        ssl.setSslContext(sslContext);

        HttpClient httpClient = new HttpClient(ssl);
        client = httpClient;
        WebSocketClient client = new WebSocketClient(httpClient);
        JettyWebSocketClient jettyClient = new JettyWebSocketClient(client);
        stompClient = new WebSocketStompClient(jettyClient);
        stompClient.setTaskScheduler(new ConcurrentTaskScheduler());
        stompClient.start();
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    private void connectStomp(String host){
        String url = "wss://{host}/device/wss";
        var future = stompClient.connect(url, new RRemoteSessionHandler<String>(this), host);
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

    private class RRemoteSessionHandler<T> extends StompSessionHandlerAdapter {

        private final RRemoteClientService service;
        private final EventSource<T> source = new EventSource<>();
        private Class type = String.class;


        RRemoteSessionHandler(RRemoteClientService service){
            this.service = service;
        }

        public RRemoteSessionHandler(RRemoteClientService service, EventHandler<T> handler, Class cl){
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
            if(payload != null)
                source.call((T)payload);
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