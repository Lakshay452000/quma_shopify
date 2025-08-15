package com.quma.quma_shopify_backend.config;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfig {

    @Value("${elasticsearch.url}")
    private String elasticUrl;
    
    @Bean(destroyMethod = "close")
    @SuppressWarnings("deprecation")
    public RestHighLevelClient restHighLevelClient() throws Exception {
        java.net.URI uri = new java.net.URI(elasticUrl);
        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 443 : uri.getPort();
        String[] userInfo = uri.getUserInfo().split(":");
        String username = userInfo[0];
        String password = userInfo[1];

        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        RestClientBuilder builder = RestClient.builder(new org.apache.http.HttpHost(host, port, "https"))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                );

        return new RestHighLevelClient(builder);
    }
}
