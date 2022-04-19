package ru.rmm.server.controllers;

import jdk.jfr.Frequency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.rmm.server.ClientRoles;
import ru.rmm.server.ca.CertificateAuthority;
import ru.rmm.server.ca.CertificateWithKey;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static java.util.stream.Collectors.toList;

@Controller
public class RegistrationController {

    Logger logger = LoggerFactory.getLogger(RegistrationController.class);

    private boolean checkIfPrivileged(HttpServletRequest request){
        boolean privileged;
        if(request.isUserInRole("ROLE_ADMIN") || request.getRemoteAddr().equals("127.0.0.1") || request.getRemoteAddr().equals(request.getLocalAddr())){
            privileged = true;
        }else{
            privileged = false;
        }
        return privileged;
    }

    @GetMapping("/reg")
    public String getRegPage(HttpServletRequest request, Model model){
        boolean pr =  checkIfPrivileged(request);
        var roles = Arrays.stream(ClientRoles.values()).filter(role -> !role.privileged || pr).collect(toList());
        model.addAttribute("roles", roles);
        return "reg";
    }

    @Autowired
    CertificateAuthority ca;

    @PostMapping("/gencert")
    public ResponseEntity<byte[]> generateUserCert(HttpServletRequest request, @RequestParam String username, @RequestParam String role) {
        boolean pr = checkIfPrivileged(request);
        try{
            var actualRole = ClientRoles.valueOf(role);
            if(!(!actualRole.privileged || pr)){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
            CertificateWithKey cert = ca.generateCertificateAndKey(username, actualRole);
            byte[] store = ca.storeAsPKCS12(cert);
            request.getSession().setAttribute("New-Cert", store);
            return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_OCTET_STREAM).body(null);
        }catch(Exception ex){
            logger.error("Ошибка генерации сертификата пользователя", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

        }

    }

    @GetMapping("/getcert")
    public ResponseEntity<byte[]> downloadUserCert(HttpServletRequest request){
        var store = (byte[])request.getSession().getAttribute("New-Cert");
        if(store == null){
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        }else{
            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header("content-disposition", "attachment; filename=\"mycert.pfx\"")
                    .body(store);
        }
    }
}
