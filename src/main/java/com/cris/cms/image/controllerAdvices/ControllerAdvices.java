package com.cris.cms.image.controllerAdvices;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ControllerAdvices {

    @ExceptionHandler({InterruptedException.class, java.io.IOException.class})
    public void handleExceptions(Exception ex) {
        // Log the exception or handle it as needed
        System.err.println("An error occurred: " + ex.getMessage());
        // You can also return a specific view or response if needed
        // For example, you could return a custom error page or a JSON response
        // return "errorPage"; // Uncomment if you have an error page to return
    }
}
