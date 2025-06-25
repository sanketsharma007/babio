package com.cris.cms.image.controller;

import com.cris.cms.image.model.LoginForm;
import com.cris.cms.image.services.LoginService;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/babio")
public class LoginController {
    private LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @GetMapping(value = {"", "/"})
    public String showCrewDetails(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return loginService.showCrewDetails(model);
    }

    @PostMapping("/initiateBA")
    public String initiateBA(@ModelAttribute LoginForm loginForm, Model model) {
        return loginService.initiateBA(loginForm, model);
    }

    @PostMapping("/startBreath")
    public void startBreath(@ModelAttribute LoginForm loginForm, HttpServletResponse response) throws Exception {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();

        // Call a new streaming method in your service
        loginService.startBreath(loginForm, writer);

        writer.flush();
    }

    @PostMapping("/initiateBio")
    public String initiateBio(@ModelAttribute LoginForm loginForm, Model model) throws Exception{
        model.addAttribute("loginForm", loginForm);
        return loginService.initiateBio(loginForm, model);
    }

    @PostMapping("/bioVer")
    public void bioVer(@ModelAttribute LoginForm loginForm, HttpServletResponse response) throws Exception {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();

        loginService.bioVer(loginForm, writer);
        writer.flush();

    }

    @PostMapping("/bioReg")
    public void bioReg(
            @ModelAttribute LoginForm loginForm, HttpServletResponse response) throws Exception {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        loginService.bioReg(loginForm, writer);
        writer.flush();
    }

    @PostMapping("/deleteFPData")
    public void deleteFPData(@ModelAttribute LoginForm loginForm, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        loginService.deleteFPData(loginForm, writer);
        writer.flush();
    }

}
