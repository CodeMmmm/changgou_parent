package com.changgou.token;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map;

public class MyCreateJwtTest {

    @Test
    public void testCreateToken(){

        // 证书文件路径
        String key_location = "changgou.jks";
        // 秘钥库密码
        String key_password = "changgou";
        //  秘钥密码
        String keypwd = "changgou";
        // 秘钥别名
        String alias = "changgou";

        // 访问证书路径
        ClassPathResource resource = new ClassPathResource(key_location);

        // 创建秘钥工厂
        KeyStoreKeyFactory keyFactory = new KeyStoreKeyFactory(resource,key_password.toCharArray());

        // 读取秘钥对（公钥，私钥）
        KeyPair keyPair = keyFactory.getKeyPair(alias, keypwd.toCharArray());

        // 获取私钥
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        //定义Payload
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("id", "1");
        tokenMap.put("name", "itheima");
        tokenMap.put("roles", "ROLE_VIP,ROLE_USER");

        // 生成令牌
        Jwt jwt = JwtHelper.encode(JSON.toJSONString(tokenMap), new RsaSigner(privateKey));

        // 取出令牌
        System.out.println(jwt.getEncoded());

    }
}
