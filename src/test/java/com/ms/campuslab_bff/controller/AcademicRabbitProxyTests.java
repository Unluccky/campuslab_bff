package com.ms.campuslab_bff.controller;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AcademicRabbitProxyTests {
    private HttpServer upstream;
    private MockMvc mvc;
    private final AtomicReference<String> received = new AtomicReference<>();
    private static final String TENANT = "c98282a6-2da3-4791-98c0-dcf952c6454b";
    private static final String ROOT = "/api/bff/academic/rabbit";

    @BeforeEach void start() throws Exception {
        upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        upstream.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            received.set(exchange.getRequestMethod()+" "+exchange.getRequestURI()+" "+body);
            assertEquals("Bearer test-token", exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        upstream.start();
        mvc = MockMvcBuilders.standaloneSetup(new AcademicRabbitController(
            "http://127.0.0.1:"+upstream.getAddress().getPort(), TENANT)).build();
    }
    @AfterEach void stop() { upstream.stop(0); }
    private JwtAuthenticationToken identity(String role, String issuer) {
        var jwt=Jwt.withTokenValue("test-token").header("alg","RS256").subject("test")
            .issuer(issuer).build();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(role)));
    }
    private JwtAuthenticationToken admin() {
        return identity("ROLE_ADMIN", "https://login.microsoftonline.com/"+TENANT+"/v2.0");
    }
    @Test void forwardsDeleteBindingBodyAndBearer() throws Exception {
        mvc.perform(delete(ROOT+"/bindings").principal(admin()).contentType("application/json")
            .content("{\"exchange\":\"lab.test\",\"queue\":\"lab.test\",\"routingKey\":\"test.created\"}"))
            .andExpect(status().isNoContent());
        assertTrue(received.get().startsWith("DELETE /api/messaging/admin/bindings "));
        assertTrue(received.get().contains("test.created"));
    }
    @Test void forwardsQueueDeletionToFixedUpstream() throws Exception {
        mvc.perform(delete(ROOT+"/queues/lab.test").principal(admin())).andExpect(status().isNoContent());
        assertEquals("DELETE /api/messaging/admin/queues/lab.test ", received.get());
    }
    @Test void studentAndForeignIssuerNeverReachBroker() throws Exception {
        for(var identity:List.of(identity("ROLE_ESTUDIANTE","https://login.microsoftonline.com/"+TENANT+"/v2.0"),
            identity("ROLE_ADMIN","https://untrusted.example"))) {
            mvc.perform(get(ROOT+"/queues/lab.test").principal(identity)).andExpect(status().isForbidden());
        }
        assertNull(received.get());
    }
    @Test void coreTopologyAndUnknownResourcesAreBlocked() throws Exception {
        for(String path:List.of("/queues/q.cmd.email","/other/lab.test"))
            mvc.perform(delete(ROOT+path).principal(admin())).andExpect(status().isBadRequest());
        assertNull(received.get());
    }
}
