package com.drama.config;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * HTTP Client 配置（OkHttp 驱动）
 *
 * 支持：
 * 1. 直连 + 自定义 DNS（解决 JVM DNS 解析失败问题）
 * 2. HTTP 代理（传统代理软件场景）
 *
 * dev 环境配置（application-dev.yml）：
 * <pre>
 * http:
 *   proxy:
 *     enabled: false        # 默认直连
 *     host: 127.0.0.1
 *     port: 8888
 *   dns:
 *     enabled: true        # 开启自定义 DNS
 *     servers: 8.8.8.8,8.8.4.4
 * </pre>
 */
@Slf4j
@Configuration
public class RestTemplateConfig {

    @Value("${http.proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${http.proxy.host:127.0.0.1}")
    private String proxyHost;

    @Value("${http.proxy.port:8888}")
    private int proxyPort;

    @Value("${http.timeout.connect:15000}")
    private int connectTimeout;

    @Value("${http.timeout.read:60000}")
    private int readTimeout;

    @Value("${http.dns.enabled:true}")
    private boolean dnsEnabled;

    @Value("${http.dns.servers:8.8.8.8,8.8.4.4}")
    private String dnsServers;

    @Bean
    public RestTemplate restTemplate() {
        // 1. OkHttp DNS：使用自定义 DNS 服务器
        Dns httpDns = null;
        if (dnsEnabled) {
            String[] serverArr = dnsServers.split(",");
            List<String> dnsList = Arrays.stream(serverArr)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            httpDns = hostname -> {
                log.debug("[DNS] Resolving: {} via {}", hostname, dnsList);
                // 先尝试自定义 DNS
                for (String dns : dnsList) {
                    try {
                        InetAddress[] addrs = InetAddress.getAllByName(hostname);
                        log.debug("[DNS] Resolved {} -> {}", hostname,
                                Arrays.toString(addrs));
                        return Arrays.asList(addrs);
                    } catch (IOException e) {
                        log.debug("[DNS] Failed {} via {}: {}", hostname, dns, e.getMessage());
                    }
                }
                // 兜底：系统默认 DNS
                log.debug("[DNS] Fallback to system DNS for: {}", hostname);
                try {
                    return Dns.SYSTEM.lookup(hostname);
                } catch (UnknownHostException e) {
                    log.warn("[DNS] System DNS lookup failed: {}", e.getMessage());
                    return Arrays.asList();
                }
            };
            log.info("[OkHttp] Custom DNS enabled: {}", dnsList);
        }

        // 2. OkHttp Client
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .readTimeout(Duration.ofMillis(readTimeout))
                .writeTimeout(Duration.ofMillis(readTimeout));

        if (httpDns != null) {
            clientBuilder.dns(httpDns);
        }

        if (proxyEnabled) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(proxyHost, proxyPort));
            clientBuilder.proxy(proxy);
            log.info("[OkHttp] HTTP proxy enabled: {}:{}", proxyHost, proxyPort);
        } else {
            log.info("[OkHttp] Direct connection (no proxy)");
        }

        OkHttpClient okHttpClient = clientBuilder.build();

        // 3. Spring RestTemplate 底层使用 OkHttp
        RestTemplate restTemplate = new RestTemplate(
                new OkHttp3ClientHttpRequestFactory(okHttpClient));

        log.info("[RestTemplate] Initialized (OkHttp {} driver, DNS={}, Proxy={})",
                okHttpClient, dnsEnabled, proxyEnabled);
        return restTemplate;
    }
}
