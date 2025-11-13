package com.jankinwu.fntvserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Enumeration;

/**
 * @author Jankin Wu
 * @description 代理
 * @date 2025-11-13 16:30
 **/
@RestController
@RequestMapping("/v/api")
public class ProxyController {
    private final WebClient webClient;

    public ProxyController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @GetMapping("/**")
    public Mono<ResponseEntity<byte[]>> proxyRequest(HttpServletRequest request) {
        String targetUrl = buildTargetUrl(request);

        return webClient.get()
                .uri(targetUrl)
                .headers(httpHeaders -> copyHeaders(request, httpHeaders))
                .retrieve()
                .toEntity(byte[].class)  // 获取完整的 ResponseEntity
                .map(responseEntity -> {
                    HttpHeaders headers = new HttpHeaders();
                    // 复制原始响应头
                    responseEntity.getHeaders().forEach(headers::addAll);

                    return ResponseEntity
                            .status(responseEntity.getStatusCode())
                            .headers(headers)
                            .body(responseEntity.getBody());
                });
    }
    private void copyHeaders(HttpServletRequest request, HttpHeaders httpHeaders) {
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                httpHeaders.add(headerName, headerValue);
            }
        }
        httpHeaders.add("X-Custom-Header", "CustomHeaderValue");
    }

    private String buildTargetUrl(HttpServletRequest request) {
        String path = request.getRequestURI();
        String queryParams = request.getQueryString() != null ? "?" + request.getQueryString() : "";
        return "http://localhost:5666" + path + queryParams;
    }
}
