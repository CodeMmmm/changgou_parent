package com.changgou.token;

import org.junit.Test;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;

public class MyParseJwtTest {
    @Test
    public void parseToken(){
        // 令牌
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlcyI6IlJPTEVfVklQLFJPTEVfVVNFUiIsIm5hbWUiOiJpdGhlaW1hIiwiaWQiOiIxIn0.mFId81LsI60YzsuFOAd8b1gxaY-D3urCsjseow4o3gRD1xP_Dsl2I08eymrH8RxWWpEud-oGjUnKZ5OkhlH4CQuhz7GXf35ZABb_UiFo7S-wJNBFGJr_ff-lcRhNTD5zjiE6v38_ksrQ_v2GwGfRQfyNuZma-DRSEfHZ0UbObHusmVX2mxQP_zzggb6nlJmT_S3IrllRwCNYa9_YYj9YqIMQCvINxKFFfxsW1UygveIhNhYbw81XblSDgr-lRsTnFmDbwz-3mgjUwq8MJ0KPbHNSd-ZhIebvFQxUlJP1YXJKLIAYyk8MEmkoSDI2IPqrsINAy_BIFgws-5HsPdSmag";

        // 公钥
        String public_key = "-----BEGIN PUBLIC KEY-----" +
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAm2ItUD5a9JnvGOPyY0l0" +
                "UQjIEXKY0qx92ofgIOwFxhQfZRk9/iHD4k9jj9tBAnw5Ul2w4TJ534X27dGkjOk4" +
                "IpooExb9Ahkj29KWeBPRRoF+xmCe4If0p8VLA0h+MR8317ilpfc8dAarqqJNNFas" +
                "kqstvJc/U3Eg/8v6u8pGm/JNG0kzF6goibUd3zfnZrRD7hpgaxpVIqhli4C2thrM" +
                "Gj6koKv10lX+s9QxvCVNjlk5vR5SKBYmijroiFTbBLfp1rqoyYLaS8NozNV1W9B2" +
                "16be/VuijoDx9ZnjPg4WWLlOf50Ei87HwomLhgwlWiZ/Bzo/v3G5Xn1Fi+sh2Jyp" +
                "4QIDAQAB" +
                "-----END PUBLIC KEY-----";

        // 解析令牌
        Jwt jwt = JwtHelper.decodeAndVerify(token, new RsaVerifier(public_key));

        // 获取原始内容
        String claims = jwt.getClaims();
        System.out.println(claims);

        System.out.println("--------------------------------------");

        // 令牌字符串
        System.out.println(jwt.getEncoded());
    }
}
