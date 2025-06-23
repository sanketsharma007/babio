package com.cris.cms.image.controller;

import com.cris.cms.image.model.LoginForm;
import com.cris.cms.image.services.LoginService;

import org.springframework.http.ResponseEntity;
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

    @GetMapping("/")
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
    @ResponseBody
    public String startBreath(@ModelAttribute LoginForm loginForm, Model model) throws Exception {
        return loginService.startBreath(loginForm, model).getBody();
    }

    @PostMapping("/initiateBio")
    public String initiateBio(@ModelAttribute LoginForm loginForm, Model model) {
        return loginService.initiateBio(loginForm, model);
    }

    @PostMapping("/bioVer")
    @ResponseBody
    public String bioVer(@ModelAttribute LoginForm loginForm, Model model) throws Exception {
        return loginService.bioVer(loginForm, model).getBody();

    }

    @PostMapping("/bioReg")
    @ResponseBody
    public String bioReg(
            @ModelAttribute LoginForm loginForm, Model model) throws Exception {
        return loginService.bioReg(loginForm, model).getBody();
    }

    @PostMapping("/deleteFPData")
    public ResponseEntity<String> deleteFPData(@ModelAttribute LoginForm loginForm) {
        return loginService.deleteFPData(loginForm);
    }

}
