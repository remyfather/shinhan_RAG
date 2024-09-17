package com.flutter.DataPreprocessingService.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.SecureRandom;

@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.username}")
    private String username;

    @Value("${spring.elasticsearch.password}")
    private String password;

    @Bean
    public ElasticsearchClient elasticsearchClient() throws Exception {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        // SSLContext 생성 (HTTPS를 사용할 경우에만 필요)
        SSLContext sslContext = createSSLContext();

        // 여기에서 프로토콜을 http로 변경합니다
        RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", 9200, "http")) // 수정된 부분
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider)
                        // HTTPS를 사용하지 않으므로 SSL 설정은 필요 없을 수 있습니다
                        .setSSLHostnameVerifier((hostname, session) -> true)); // 호스트 이름 검증 무시

        RestClient restClient = builder.build();

        // ObjectMapper에 JavaTimeModule 등록
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        RestClientTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper(objectMapper));

        return new ElasticsearchClient(transport);
    }

    private SSLContext createSSLContext() throws Exception {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        X509TrustManager trustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new X509TrustManager[]{trustManager}, new SecureRandom());
        return sslContext;
    }
}
