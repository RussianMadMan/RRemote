package ru.rmm.server.controllers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import ru.rmm.rremote.comms.ServiceMessage;
import ru.rmm.server.RRemoteServerService;
import ru.rmm.server.ca.CertificateAuthority;
import ru.rmm.server.models.MyUserPrincipal;
import ru.rmm.server.models.RRemoteUserRepo;

import javax.servlet.http.HttpServletRequest;
import java.net.http.HttpRequest;
import java.security.Principal;
import java.util.HashMap;


@Controller
public class AdminController {

    private Logger logger = LoggerFactory.getLogger(AdminController.class);

    @GetMapping("/admin/")
    public String adminPage(){
        if(CertificateAuthority.sslActive) {
            return "sslsetup";
        }else{
            return "sslsetup";
        }
    }
    @Autowired
    CertificateAuthority ca;

    @PostMapping("/admin/ssl")
    public ResponseEntity<String> genSSL(@RequestParam String domain){
        try{
            ca.generateSSLKeyStore(domain);
            return ResponseEntity.status(HttpStatus.OK).body("OK");
        }catch(Exception ex){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @GetMapping("/admin/getca")
    @ResponseBody
    public ResponseEntity<String> getCACert(){
        String pem = ca.getPemCACert();
        if(pem != null){
            return ResponseEntity.status(HttpStatus.OK).
                    header("content-disposition", "attachment; filename=\"" + ca.defaultCN +".cer" + "\"").
                    contentType(MediaType.valueOf("application/x-pem-file")).
                    body(pem);
        }else{
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

    }

    @Autowired
    @Qualifier("getRRemote")
    RRemoteServerService service;

    @GetMapping("/admin/panel")
    public String getPanelView(Model model){
        var clients = service.getClients();
        logger.debug("clients: {}", clients);
        model.addAttribute("services", service.getClients());
        return "panel";
    }

    @Autowired
    SimpMessagingTemplate template;
    @Autowired
    RRemoteUserRepo repo;


    @GetMapping("/admin/sendmsg")
    public ResponseEntity<String> handleMSG(@RequestParam String client, @RequestParam String service, @RequestParam String command, HttpServletRequest request){
        var rremoteClient = repo.findByUsername(client);
        //logger.debug("principal class is {}", request.getUserPrincipal().getClass());
        var rremoteSender = request.getUserPrincipal().getName();
        if(rremoteClient == null){
            return ResponseEntity.status(400).body(null);
        }
        var msg = new ServiceMessage(service, command, "id", new HashMap<>());
        template.convertAndSendToUser(client, "/queue/service", msg);
        return ResponseEntity.status(200).body("OK");
    }

}
