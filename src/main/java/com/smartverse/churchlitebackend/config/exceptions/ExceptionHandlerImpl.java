package com.smartverse.churchlitebackend.config.exceptions;


import com.potatotech.authorization.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDate;
import java.util.LinkedHashMap;

@ControllerAdvice
public class ExceptionHandlerImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlerImpl.class);

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<?> handlerException(ServiceException exception){
        LOGGER.error("Erro de servico tratado pela API", exception);
        var ret = new LinkedHashMap<>();
        ret.put("message",exception.getMessage());
        ret.put("status", exception.getStatus().value());
        ret.put("timestamp", LocalDate.now());
        return new ResponseEntity<>(ret, exception.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handlerUnexpectedException(Exception exception) {
        LOGGER.error("Erro inesperado na API", exception);
        var ret = new LinkedHashMap<>();
        ret.put("message", "internal_server_error");
        ret.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        ret.put("timestamp", LocalDate.now());
        return new ResponseEntity<>(ret, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
