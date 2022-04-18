package ru.rmm.server.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import ru.rmm.server.ca.CertificateAuthority;

import java.net.http.HttpRequest;

@Controller
public class AdminController {
    @GetMapping("/admin")
    public String adminPage(){
        if(CertificateAuthority.sslActive) {
            return null;
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

}
