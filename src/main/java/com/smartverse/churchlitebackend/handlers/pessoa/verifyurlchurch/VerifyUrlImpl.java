package com.smartverse.churchlitebackend.handlers.pessoa.verifyurlchurch;

import com.smartverse.churchlitebackend_gen.VerifyURL;
import com.smartverse.churchlitebackend_gen.VerifyURLOutput;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VerifyUrlImpl implements VerifyURL {

    @Override
    public ResponseEntity<VerifyURLOutput> verifyURL(String token) {

        var headers = new HttpHeaders();
        var jwtToken = "";
        headers.add("Set-Cookie", String.format("jwtToken=%s; HttpOnly; Secure; Max-Age=86400; Path=/",jwtToken));
        var output = new VerifyURLOutput();
        output.authorize = true;

        return new ResponseEntity<>(output, headers, HttpStatus.OK);
    }
}
