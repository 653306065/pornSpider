package com.spider.conf;

import java.net.InetSocketAddress;
import java.net.Proxy;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ProxyConf {

    @Value("${proxy.socks.host}")
    private String socksHost;

    @Value("${proxy.socks.port}")
    private int socksPort;

    @Value("${proxy.http.host}")
    private String httpHost;

    @Value("${proxy.http.port}")
    private int httpPort;

    @Value("${proxy.enabled}")
    private boolean enabled;


    @Bean
    public Proxy getProxy() {
        return new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(socksHost, socksPort));
    }

    @PostConstruct
    public void GlobalProxy() {
        if (enabled) {
            System.setProperty("java.net.useSystemProxies", "true");
            System.setProperty("http.proxyHost", httpHost);
            System.setProperty("http.proxyPort", String.valueOf(httpPort));
        }
    }
}
