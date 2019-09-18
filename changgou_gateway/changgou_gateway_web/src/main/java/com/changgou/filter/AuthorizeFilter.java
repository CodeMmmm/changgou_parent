package com.changgou.filter;

import com.changgou.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthorizeFilter implements GlobalFilter,Ordered{
    //令牌头名字
    private static final String AUTHORIZE_TOKEN = "Authorization";
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // 获取请求uri
        String path = request.getURI().getPath();

        // 如果是请求登录微服务或者搜索微服务，则放行
        if (path.startsWith("/api/user/login") || path.startsWith("/api/brand/search") || !URLFilter.hasAuthorize(path)){
            return chain.filter(exchange);
        }

        // 获取头文件中的令牌信息
        String tokent = request.getHeaders().getFirst(AUTHORIZE_TOKEN);
        // 没有则从参数中获取
        if (StringUtils.isEmpty(tokent)){
            tokent = request.getQueryParams().getFirst(AUTHORIZE_TOKEN);
        }
        // 没有则从cookie中获取
        if (StringUtils.isEmpty(tokent)){
            HttpCookie cookie = request.getCookies().getFirst(AUTHORIZE_TOKEN);
            if (cookie != null){
                tokent = cookie.getValue();
            }
        }

        // 没有则返回401
        if (StringUtils.isEmpty(tokent)){
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }else {
            // 有则判断请求头
            //判断当前令牌是否有bearer前缀，如果没有，则添加前缀 bearer
            if(!tokent.startsWith("bearer ") && !tokent.startsWith("Bearer ")){
                tokent="bearer "+tokent;
            }
            //将令牌封装到头文件中
            request.mutate().header(AUTHORIZE_TOKEN,tokent);
        }

        return chain.filter(exchange);
    }

    /**
     * 优先级，返回数字越小，优先级越高
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
