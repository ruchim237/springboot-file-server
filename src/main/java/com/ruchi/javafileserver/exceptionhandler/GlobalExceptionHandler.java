package com.ruchi.javafileserver.exceptionhandler;

import com.ruchi.javafileserver.view.ErrorModel;
import com.ruchi.javafileserver.view.ViewConstants;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleRuntimeException(final Exception exception, Model model) {
        model.addAttribute(ErrorModel.STATUS, HttpStatus.INTERNAL_SERVER_ERROR);
        model.addAttribute(ErrorModel.ERROR, exception.getMessage());
        return ViewConstants.ERROR;
//        return ResponseEntity.internalServerError().body(exception.getMessage());
    }
}
