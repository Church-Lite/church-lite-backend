package com.smartverse.churchlitebackend.controller.storage;


import com.smartverse.churchlitebackend.service.storage.MiniIoService;
import com.smartverse.churchlitebackend.config.context.RequestUserContext;
import com.smartverse.churchlitebackend.config.context.ConfigContextImpl;
import com.potatotech.authorization.tenant.TenantContext;
import com.smartverse.churchlitebackend_gen.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend_gen.endpoints.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.UUID;

@RestController
public class StorageImpl implements RequestUpload, RequestUrl, DeleteObject {
    private static final Logger log = LoggerFactory.getLogger(StorageImpl.class);

    MiniIoService storage3Service;
    private final HttpServletRequest request;
    private final JdbcTemplate jdbcTemplate;
    private final String adminSchema;

    public StorageImpl(MiniIoService storage3Service, HttpServletRequest request,
                       DataSource dataSource, ConfigContextImpl configContext) {
        this.storage3Service = storage3Service;
        this.request = request;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.adminSchema = configContext.getDatabase().toUpperCase() + "_ADMIN";
    }

    @Override
    public ResponseEntity<RequestUploadOutput> requestUpload(Integer expired, String fileName) {
        validateMemberWrite(fileName, "upload");
        var url = storage3Service.requestUpload(fileName, expired);
        var output = new RequestUploadOutput();
        output.url = url.toString();
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<RequestUrlOutput> requestUrl(Integer expired, String fileName) {
        validateMemberRead(fileName);
        var url = storage3Service.requestDownload(fileName, expired);
        var output = new RequestUrlOutput();
        output.url = url.toString();
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<DeleteObjectOutput> deleteObject(String fileName) {
        validateMemberWrite(fileName, "delete");
        var output = new DeleteObjectOutput();
        output.output = storage3Service.requestDelete(fileName);
        return ResponseEntity.ok(output);
    }

    private void validateMemberWrite(String fileName, String operation) {
        if (!"MEMBER".equalsIgnoreCase(request.getHeader("XAccessProfile"))) return;
        var ownerPrefix = RequestUserContext.getRequired().toString().toUpperCase() + "/";
        if (fileName == null || !fileName.toUpperCase().startsWith(ownerPrefix)) {
            log.warn("member_storage_access_denied tenant={} userId={} operation={} fileName={} reason=not_owner",
                    TenantContext.getCurrentTenant(), RequestUserContext.getRequired(), operation, fileName);
            throw new ServiceException(HttpStatus.FORBIDDEN, "member_storage_object_access_denied");
        }
        log.info("member_storage_access_allowed tenant={} userId={} operation={} ownerId={}",
                TenantContext.getCurrentTenant(), RequestUserContext.getRequired(), operation, RequestUserContext.getRequired());
    }

    private void validateMemberRead(String fileName) {
        if (!"MEMBER".equalsIgnoreCase(request.getHeader("XAccessProfile"))) return;
        var ownerId = ownerId(fileName);
        var currentTenant = TenantContext.getCurrentTenant();
        var ownerTenant = jdbcTemplate.query(
                "SELECT tenant FROM \"" + adminSchema + "\".user_access WHERE id = ?",
                resultSet -> resultSet.next() ? resultSet.getString(1) : null,
                ownerId);
        if (ownerTenant == null || currentTenant == null || !currentTenant.equalsIgnoreCase(ownerTenant)) {
            log.warn("member_storage_access_denied tenant={} userId={} operation=read ownerId={} reason=tenant_mismatch ownerTenant={}",
                    currentTenant, RequestUserContext.getRequired(), ownerId, ownerTenant);
            throw new ServiceException(HttpStatus.FORBIDDEN, "member_storage_tenant_access_denied");
        }
        log.info("member_storage_access_allowed tenant={} userId={} operation=read ownerId={}",
                currentTenant, RequestUserContext.getRequired(), ownerId);
    }

    private UUID ownerId(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.contains("/")) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "member_storage_object_access_denied");
        }

        try {
            return UUID.fromString(fileName.substring(0, fileName.indexOf('/')));
        } catch (IllegalArgumentException exception) {
            log.warn("member_storage_access_denied tenant={} userId={} operation=read fileName={} reason=invalid_owner",
                    TenantContext.getCurrentTenant(), RequestUserContext.getRequired(), fileName);
            throw new ServiceException(HttpStatus.FORBIDDEN, "member_storage_object_access_denied");
        }
    }
}
