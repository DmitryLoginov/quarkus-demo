package dev.ldv;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "catalog")
public interface CatalogConfig {

    String name();
}
