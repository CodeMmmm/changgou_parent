package com.changgou.oauth.interceptor;

import com.changgou.oauth.util.AdminToken;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;

/*****
 * @Author: www.itheima.com
 * @Description: com.changgou.oauth.interceptor
 ****/
@Configuration
public class TokenRequestInterceptor implements RequestInterceptor {

    /***
     * Feign执行之前，进行拦截
     * @param template
     */
    @Override
    public void apply(RequestTemplate template) {
        /***
         * 从数据库加载查询用户信息
         * 1:没有令牌，Feign调用之前，生成令牌(admin)
         * 2:Feign调用之前，令牌需要携带过去
         * 3:Feign调用之前，令牌需要存放到Header文件中
         * 4:请求->Feign调用->拦截器RequestInterceptor->Feign调用之前执行拦截
         */

        //生成admin令牌
        String token = AdminToken.adminToken();
        template.header("Authorization","bearer "+token);
    }
}
