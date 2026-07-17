package com.smartverse.churchlitebackend.handlers.storage;


import com.smartverse.churchlitebackend.services.storage.MiniIoService;

import com.smartverse.churchlitebackend_gen.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
public class StorageImpl implements RequestUpload, RequestUrl, DeleteObject {

    MiniIoService storage3Service;

    public StorageImpl(MiniIoService storage3Service) {
        this.storage3Service = storage3Service;
    }

    @Override
    public ResponseEntity<RequestUploadOutput> requestUpload(Integer expired, String fileName) {
        var url = storage3Service.requestUpload(fileName, expired);
        var output = new RequestUploadOutput();
        output.url = url.toString();
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<RequestUrlOutput> requestUrl(Integer expired, String fileName) {
        var url = storage3Service.requestDownload(fileName, expired);
        var output = new RequestUrlOutput();
        output.url = url.toString();
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<DeleteObjectOutput> deleteObject(String fileName) {
        var output = new DeleteObjectOutput();
        output.output = storage3Service.requestDelete(fileName);
        return ResponseEntity.ok(output);
    }
}
