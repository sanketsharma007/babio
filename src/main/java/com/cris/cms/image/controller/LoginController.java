package com.cris.cms.image.controller;

import com.cris.cms.image.model.LoginForm;
import com.cris.cms.image.services.LoginService;

import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;

import org.springframework.http.ResponseEntity;
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

    @GetMapping("/")
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
    public String initiateBio(@ModelAttribute LoginForm loginForm, Model model) {
        model.addAttribute("loginForm", loginForm);
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
