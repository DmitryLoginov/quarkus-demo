package dev.ldv;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/info")
public class InfoResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Info info() {
        return new Info("book-catalog", "Quarkus");
    }
}
