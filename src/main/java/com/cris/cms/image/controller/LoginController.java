package com.cris.cms.image.controller;

import com.cris.cms.image.model.LoginForm;
import com.cris.cms.image.services.LoginService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/babio")
public class LoginController {
    private LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @GetMapping
    public String showCrewDetails(Model model) {
        return loginService.showCrewDetails(model);
    }

    @PostMapping("/turnOn")
    public String turnOn(@ModelAttribute LoginForm loginForm, Model model) {
        return loginService.turnOn(loginForm, model);
    }

    @PostMapping("/initiateBA")
    public String initiateBA(@ModelAttribute LoginForm loginForm, Model model) {
        return loginService.initiateBA(loginForm, model);
    }

    @PostMapping("/startBreath")
    public void startBreath(@ModelAttribute LoginForm loginForm, HttpServletResponse response) throws Exception {
        loginService.startBreath(loginForm, response);
    }

    @PostMapping("initiateBio")
    public String initiateBio(@ModelAttribute LoginForm loginForm, Model model) {
        return loginService.initiateBio(loginForm, model);
    }

    @PostMapping("/bioVer")
    public void bioVer(@ModelAttribute LoginForm loginForm, HttpServletResponse response) throws Exception {
        loginService.bioVer(loginForm, response);
    }

}
