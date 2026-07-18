package com.livestream.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("agora")
public record AgoraProperties(String appId, String appCertificate, Duration tokenTtl) {
}
