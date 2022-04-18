package ru.rmm.server.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import ru.rmm.server.ca.CertificateAuthority;

@Controller
public class AdminController {
    @GetMapping("/admin/")
    public String adminPage(){
        if(CertificateAuthority.sslActive) {
            return null;
        }else{
            return "sslsetup";
        }
    }
}
