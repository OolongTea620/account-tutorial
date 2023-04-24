package com.zero.account.service;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTestService {

    private final RedissonClient redissonClient;

    public String getLock(){
        RLock lock = redissonClient.getLock("sampleLock");
        try {
            boolean isLock = lock.tryLock(10, 3, TimeUnit.SECONDS);
            if (!isLock) {
                log.error("===========================Lock acquisition failed=====");
                return "Lock failed";
            }
        }catch (Exception e) {
            log.error("Redis lock failed");
        }
        return "get lock success";
    }
}
