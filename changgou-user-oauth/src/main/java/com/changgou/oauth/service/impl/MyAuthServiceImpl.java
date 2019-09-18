package com.changgou.oauth.service.impl;

import com.changgou.oauth.service.AuthService;
import com.changgou.oauth.util.AuthToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

@Service
public class MyAuthServiceImpl implements AuthService {
    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public AuthToken login(String username, String password, String clientId, String clientSecret) {
        AuthToken authToken = appleToken(username,password,clientId,clientSecret);
        if (authToken == null){
            throw  new RuntimeException("申请令牌失败");
        }
        return authToken;
    }

    /****
     * 认证方法
     * @param username :用户登录名字
     * @param password ：用户密码
     * @param clientId ：配置文件中的客户端ID
     * @param clientSecret ：配置文件中的秘钥
     * @return
     */
    private AuthToken appleToken(String username, String password, String clientId, String clientSecret) {
        // 获取认证的服务地址加端口
        ServiceInstance serviceInstance = loadBalancerClient.choose("user-auth");
        if (serviceInstance == null){
            throw new RuntimeException("认证服务不存在");
        }
        String uri = serviceInstance.getUri() + "/oauth/token";

        // 这里采用密码模式生成令牌
        // 请求体封装
        MultiValueMap<String,String> bodyMap = new LinkedMultiValueMap<>();
        bodyMap.add("grant_type","password");
        bodyMap.add("username",username);
        bodyMap.add("password",password);

        // 请求头封装
        MultiValueMap<String,String> headerMap = new LinkedMultiValueMap<>();
        headerMap.add("Authorization",httpBasic(clientId,clientSecret));

        //指定 restTemplate当遇到400或401响应时候也不要抛出异常，也要正常返回值
        // 400 bad request，请求报文存在语法错误
        //· 401 unauthorized，表示发送的请求需要有通过 HTTP 认证的认证信息
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                //当响应的值为400或401时候也要正常响应，不要抛出异常
                if (response.getRawStatusCode() != 400 && response.getRawStatusCode() != 401) {
                    super.handleError(response);
                }
            }
        });

        Map map = null;
        try {
            // 请求生成令牌
            ResponseEntity<Map> responseEntity = restTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<MultiValueMap<String,String>>(bodyMap, headerMap), Map.class);
            map = responseEntity.getBody();
        } catch (RestClientException e) {
            e.printStackTrace();
        }
        if (map == null || map.get("access_token") == null || map.get("refresh_token") == null || map.get("jti") == null){
            throw  new RuntimeException("生成令牌失败");
        }

        AuthToken authToken = new AuthToken();
        authToken.setAccessToken(String.valueOf(map.get("access_token")));
        authToken.setRefreshToken(String.valueOf(map.get("refresh_token")));
        authToken.setJti(String.valueOf(map.get("jti")));


        return authToken;
    }

    private String httpBasic(String clientId, String clientSecret) {
        // base64编码后的id:秘钥
        String str = clientId + ":" + clientSecret;
        byte[] encode = Base64Utils.encode(str.getBytes());
        return "Basic " + new String(encode);
    }

}
