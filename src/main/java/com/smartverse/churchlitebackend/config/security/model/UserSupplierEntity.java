package com.smartverse.churchlitebackend.config.security.model;


import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
@Table(name = "pessoa_usuario")
public class UserSupplierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String email;

    private String password;

    private String tenant;

}
