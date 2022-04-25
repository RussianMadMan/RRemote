package ru.rmm.server.controllers;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ru.rmm.server.ca.CAException;
import ru.rmm.server.ca.CertificateAuthority;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


@Controller
public class EntryPointController {

    @GetMapping("/")
    public ModelAndView handleStartingPage(HttpServletRequest request){

        if(!CertificateAuthority.sslActive){
            return new ModelAndView("redirect:/admin/");
        }else{
            if(request.isUserInRole("ROLE_ADMIN")){
                return new ModelAndView("redirect:/admin/");
            }else if(request.isUserInRole("ROLE_USER"))
            {
                return new ModelAndView("redirect:/user/");
            }
            return new ModelAndView("redirect:/reg");
        }
    }

    @RequestMapping(value="/invalidate", method= RequestMethod.GET)
    public String invalidate(HttpSession session, Model model) {
        session.invalidate();
        return "redirect:/";
    }
}
