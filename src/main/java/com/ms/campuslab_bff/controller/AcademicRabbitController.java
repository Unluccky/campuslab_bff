package com.ms.campuslab_bff.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.util.Map;
@RestController @RequestMapping("/api/bff/academic/rabbit")
public class AcademicRabbitController {
 private final RestClient client;private final String issuer;
 public AcademicRabbitController(@Value("${campuslab.rabbit-admin.url:http://localhost:8086}")String url,@Value("${campuslab.azure.tenant-id:c98282a6-2da3-4791-98c0-dcf952c6454b}")String tenant){
  var f=new org.springframework.http.client.JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(3)).build());f.setReadTimeout(java.time.Duration.ofSeconds(5));client=RestClient.builder().baseUrl(url).requestFactory(f).build();issuer="https://login.microsoftonline.com/"+tenant+"/v2.0";
 }
 @RequestMapping(value={"/{resource}","/{resource}/{name}"},method={RequestMethod.GET,RequestMethod.POST,RequestMethod.DELETE})
 public ResponseEntity<String> route(@PathVariable String resource,@PathVariable(required=false)String name,@RequestBody(required=false)Map<String,Object> body,org.springframework.http.HttpMethod method,JwtAuthenticationToken auth){
  if(!issuer.equals(auth.getToken().getIssuer().toString())||auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_ADMIN")))return ResponseEntity.status(403).build();
  if(!java.util.List.of("queues","exchanges","bindings").contains(resource)||name!=null&&!name.matches("lab\\.[a-zA-Z0-9._-]{1,116}"))return ResponseEntity.badRequest().build();
  try{var request=client.method(method).uri("/api/messaging/admin/"+resource+(name==null?"":"/"+name)).headers(h->h.setBearerAuth(auth.getToken().getTokenValue()));if(body!=null)request.contentType(MediaType.APPLICATION_JSON).body(body);var result=request.retrieve().toEntity(String.class);return ResponseEntity.status(result.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(result.getBody());}
  catch(RestClientResponseException e){return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());}catch(RestClientException e){return ResponseEntity.status(503).build();}
 }
}
