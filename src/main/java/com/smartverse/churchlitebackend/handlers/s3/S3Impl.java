package com.smartverse.churchlitebackend.handlers.s3;


import com.smartverse.churchlitebackend.services.s3.S3Service;
import com.smartverse.churchlitebackend_gen.RequestUpload;
import com.smartverse.churchlitebackend_gen.RequestUploadOutput;
import com.smartverse.churchlitebackend_gen.RequestUrl;
import com.smartverse.churchlitebackend_gen.RequestUrlOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
public class S3Impl implements RequestUpload, RequestUrl {

    @Autowired
    S3Service s3Service;

    @Override
    public ResponseEntity<RequestUploadOutput> requestUpload(Integer expired, String fileName) {
        var url = s3Service.requestUpload(fileName, expired);
        var output = new RequestUploadOutput();
        output.url = url.toString();
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<RequestUrlOutput> requestUrl(Integer expired, String fileName) {
        var url = s3Service.requestDownload(fileName, expired);
        var output = new RequestUrlOutput();
        output.url = url.toString();
        return ResponseEntity.ok(output);
    }
}