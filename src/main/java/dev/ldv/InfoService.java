package dev.ldv;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InfoService {

    private final CatalogConfig catalogConfig;

    public InfoService(CatalogConfig catalogConfig) {
        this.catalogConfig = catalogConfig;
    }

    public Info getInfo() {
        return new Info(catalogConfig.name(), "Quarkus");
    }
}
