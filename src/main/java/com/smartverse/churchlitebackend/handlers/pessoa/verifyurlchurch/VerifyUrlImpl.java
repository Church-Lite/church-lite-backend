package com.smartverse.churchlitebackend.handlers.pessoa.verifyurlchurch;

import com.smartverse.churchlitebackend_gen.VerifyURL;
import com.smartverse.churchlitebackend_gen.VerifyURLOutput;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VerifyUrlImpl implements VerifyURL {

    @Override
    public VerifyURLOutput verifyURL(String token) {
        return null;
    }
}
