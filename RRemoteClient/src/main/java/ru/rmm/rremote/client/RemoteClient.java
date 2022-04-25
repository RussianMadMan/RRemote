package ru.rmm.rremote.client;


import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.WebSocketClient;
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

public class RemoteClient{

    public static void main(String[] args) throws Exception{

        var sslContext = createSSLContext();
        String host = "192.168.168.54";

        final SslContextFactory ssl = new SslContextFactory.Client();
        ssl.setSslContext(sslContext);
        HttpClient httpClient = new HttpClient(ssl);
        WebSocketClient client = new WebSocketClient(httpClient);

        JettyWebSocketClient jettyClient = new JettyWebSocketClient(client);
        WebSocketStompClient stompClient = new WebSocketStompClient(jettyClient);
        stompClient.setTaskScheduler(new ConcurrentTaskScheduler());
        stompClient.start();
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        String url = "wss://{host}/test";
        var future = stompClient.connect(url, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                System.out.println("Connected");
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                if(!session.isConnected()){

                }
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                System.out.println(String.format("Shat itself %s", exception.toString()));
            }

        }, host);


        var session = future.get();


        //session.send("/app/hello", "hello");

        var sub = session.subscribe("/user/queue/device", new StompSessionHandler() {

            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                System.out.println(String.format("afterConnected: %s", connectedHeaders));
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                System.out.println(String.format("handleException: %s", exception));
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                System.out.println(String.format("handleTransportError: %s", exception));
            }

            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ServiceMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload){
                System.out.println(String.format("Got Message! %s", payload));
            }

        });

        System.out.println(session.getSessionId());
        while(true){
            Thread.sleep(1000);
        }


    }

    private static SSLContext createSSLContext() throws NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException, KeyManagementException, CertificateException {

        String keyStorePath = "device.pfx";
        String trustStorePath = "RRemote CA.cer";

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(new FileInputStream(keyStorePath), "12345".toCharArray());
        kmf.init(keystore, "12345".toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore truststore = KeyStore.getInstance("PKCS12");
        truststore.load(null, null);
        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        FileInputStream is = new FileInputStream (trustStorePath);
        X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
        truststore.setCertificateEntry("ca", cer);
        tmf.init(truststore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        var km = kmf.getKeyManagers();
        var tm = tmf.getTrustManagers();
        sslContext.init(km, tm, new SecureRandom());
        SSLContext.setDefault(sslContext);


        return sslContext;
    }

}
