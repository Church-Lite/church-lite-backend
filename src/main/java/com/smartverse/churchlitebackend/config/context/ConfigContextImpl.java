package com.smartverse.churchlitebackend.config.context;

import org.springframework.stereotype.Component;

@Component
public class ConfigContextImpl implements ConfigContext {

    @Override
    public String getUrl() {
        return switch (System.getenv(EnumConfigContext.DB_NAME.name())){
            case "POSTGRES" -> "jdbc:postgresql://"+ this.getHost()+":5434/"+this.getDatabase();
            case "MYSQL" -> "jdbc:postgresql://localhost:5432/"+this.getDatabase();
            case "SQLSERVER" -> "jdbc:postgresql://localhost:5432/"+this.getDatabase();
            default -> "";

        };
    }

    @Override
    public String getUsername() {
        return System.getenv(EnumConfigContext.DB_USERNAME.name());
    }

    @Override
    public String getPasswod() {
        return System.getenv(EnumConfigContext.DB_PASSWORD.name());
    }

    @Override
    public String getHost() {
        return  System.getenv(EnumConfigContext.SERVER_HOST.name()) == null ? "localhost" : System.getenv(EnumConfigContext.SERVER_HOST.name());
    }

    @Override
    public String getDatabase() {
        return System.getenv(EnumConfigContext.DATABASE_SCHEMA_NAME.name());
    }
}
