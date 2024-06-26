package org.lowcoder.api.framework.warmup;

import org.lowcoder.sdk.config.CommonConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class WarmupHelper {

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @Autowired
    private CommonConfig commonConfig;

    public Mono<Void> warmup() {
        return warmupIoResources();
    }

    private Mono<Void> warmupIoResources() {
        return reactiveRedisTemplate.getExpire("warmUpKey")
                .then(reactiveMongoTemplate.collectionExists("serverConfig"))
                .then();
    }
}
