package dev.ldv;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/info")
public class InfoResource {

    private final InfoService infoService;

    public InfoResource(InfoService infoService) {
        this.infoService = infoService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Info info() {
        return infoService.getInfo();
    }
}
