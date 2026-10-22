package com.ms.campuslab_bff.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.util.Map;

@RestController @RequestMapping("/api/bff/academic")
public class AcademicBookingsController {
 private final RestClient client;
 private final RestClient catalog;
 private final String issuer;
 public AcademicBookingsController(@Value("${campuslab.bookings.url:http://localhost:8083}") String url,
   @Value("${campuslab.catalog.base-url:http://localhost:8082}") String catalogUrl,
   @Value("${campuslab.azure.tenant-id:c98282a6-2da3-4791-98c0-dcf952c6454b}") String tenant) {
  var factory=new org.springframework.http.client.JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(3)).build());
  factory.setReadTimeout(java.time.Duration.ofSeconds(10));
  client=RestClient.builder().baseUrl(url).requestFactory(factory).build();catalog=RestClient.builder().baseUrl(catalogUrl).requestFactory(factory).build();issuer="https://login.microsoftonline.com/"+tenant+"/v2.0";
 }
 private ResponseEntity<String> forward(HttpMethod method,String path,Object body,JwtAuthenticationToken auth) {
  if(!issuer.equals(auth.getToken().getIssuer().toString())) return ResponseEntity.status(403).build();
  try {
   var request=(path.startsWith("/api/catalogo")?catalog:client).method(method).uri(path).headers(h->h.setBearerAuth(auth.getToken().getTokenValue()));
   if(body!=null) request.contentType(MediaType.APPLICATION_JSON).body(body);
   var result=request.retrieve().toEntity(String.class);
   return ResponseEntity.status(result.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(result.getBody());
  } catch(RestClientResponseException e) {
   return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
  } catch(RestClientException e) { return ResponseEntity.status(503).contentType(MediaType.APPLICATION_JSON).body("{\"mensaje\":\"Servicio de reservas no disponible\"}"); }
 }
 @GetMapping("/reservas") public ResponseEntity<String> list(JwtAuthenticationToken auth){return forward(HttpMethod.GET,"/api/reservas",null,auth);}
 @GetMapping("/recursos") public ResponseEntity<String> resources(JwtAuthenticationToken auth){return forward(HttpMethod.GET,"/api/catalogo",null,auth);}
 @GetMapping("/reservas/{id}/comprobantes/{kind}") public ResponseEntity<byte[]> voucher(@PathVariable Long id,@PathVariable String kind,JwtAuthenticationToken auth){
  if(!issuer.equals(auth.getToken().getIssuer().toString()))return ResponseEntity.status(403).build();
  if(!java.util.List.of("RETIRO","DEVOLUCION").contains(kind))return ResponseEntity.badRequest().build();
  try{var response=client.get().uri("/api/reservas/{id}/comprobantes/{kind}",id,kind).headers(h->h.setBearerAuth(auth.getToken().getTokenValue())).retrieve().toEntity(byte[].class);
   return ResponseEntity.status(response.getStatusCode()).contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=campuslab-"+id+".pdf").body(response.getBody());
  }catch(RestClientResponseException e){return ResponseEntity.status(e.getStatusCode()).build();}catch(RestClientException e){return ResponseEntity.status(503).build();}
 }
 @PostMapping("/reservas") public ResponseEntity<String> create(@RequestBody Map<String,Object> body,JwtAuthenticationToken auth){return forward(HttpMethod.POST,"/api/reservas",body,auth);}
 @PatchMapping("/reservas/{id}/estado") public ResponseEntity<String> state(@PathVariable Long id,@RequestBody Map<String,Object> body,JwtAuthenticationToken auth){return forward(HttpMethod.PATCH,"/api/reservas/"+id+"/estado",body,auth);}
 @GetMapping("/tickets") public ResponseEntity<String> tickets(JwtAuthenticationToken auth){return forward(HttpMethod.GET,"/api/tickets",null,auth);}
 @PostMapping("/tickets/{id}/respuestas") public ResponseEntity<String> answer(@PathVariable Long id,@RequestBody Map<String,Object> body,JwtAuthenticationToken auth){return forward(HttpMethod.POST,"/api/tickets/"+id+"/respuestas",body,auth);}
}
