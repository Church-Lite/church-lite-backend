package com.smartverse.churchlitebackend.config.security.model;


import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Table(name = "user_access")
public class UserSupplierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String email;

    private String password;

    private String tenant;

    private String phone;

    private String cpf;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_access_profile", joinColumns = @JoinColumn(name = "user_access_id"))
    @Column(name = "profile")
    @Enumerated(EnumType.STRING)
    private Set<AccessProfile> accessProfiles = new HashSet<>();

    private boolean active;

    @Column(name = "user_confirm")
    private boolean userConfirm;

}
