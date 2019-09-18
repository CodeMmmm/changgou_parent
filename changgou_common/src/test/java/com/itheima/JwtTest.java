package com.itheima;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {
    @Test
    public void testCreateJwt(){
        JwtBuilder builder = Jwts.builder();
        builder.setId("888"); // 设置唯一编号
        builder.setSubject("小白测试"); // 设置主题
        builder.setIssuedAt(new Date()); // 设置签发日期
        builder.setExpiration(new Date(System.currentTimeMillis()+300000)); // 设置过期时间
        builder.signWith(SignatureAlgorithm.HS256,"itcast"); // 设置加密算法，并指定盐

        // 传入自定义数据
        Map<String,Object> userInfo = new HashMap<>();
        userInfo.put("username","老王");
        userInfo.put("sex","男");
        userInfo.put("address","cq");
        builder.addClaims(userInfo);

        System.out.println(builder.compact()); // 创建token，并返回字符串
    }

    @Test
    public void testParseJwt(){
        String compactJwt = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI4ODgiLCJzdWIiOiLlsI_nmb3mtYvor5UiLCJpYXQiOjE1Njc1ODkzOTAsImV4cCI6MTU2NzU4OTY5MCwiYWRkcmVzcyI6ImNxIiwic2V4Ijoi55S3IiwidXNlcm5hbWUiOiLogIHnjosifQ.CEKBanItMrVOMapIjs9zj9oTqu4493z3En_xyW81dGo";

        Claims claims = Jwts.parser().setSigningKey("itcast").parseClaimsJws(compactJwt).getBody();

        System.out.println(claims);
    }

}
