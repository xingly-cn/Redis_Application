package com.sugar.redis6.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Random;

@RestController
public class RedisController {

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping("/go")
    public String Ms() {

        String userId = new Random().nextInt(50000) + "";
        String productId = "plane";
        //set good:plane 10
        // 用户 key 商品 key
        String userKey = "user:" + userId;
        String prodKey = "good:" + productId;

        SessionCallback sessionCallback = new SessionCallback<List>() {
            @Override
            public List execute(RedisOperations operations) throws DataAccessException {
                // 监视库存
                redisTemplate.watch(prodKey);

                // 获取库存, 若库存为0, 表示秒杀未开始
                int remain = (int)redisTemplate.opsForValue().get(prodKey);
                if (remain == 0) {
                    System.out.println("秒杀未开始");
                    return null;
                }

                // 判断库存
                if ((int)redisTemplate.opsForValue().get(prodKey) < 1) {
                    System.out.println("库存不足啦");
                    return null;
                }

                // 判断用户重复秒杀
                Boolean member = redisTemplate.opsForSet().isMember(userKey, prodKey);
                if (member) {
                    System.out.println("重复秒杀");
                    return null;
                }

                // 开始事务, 抢购开始
                redisTemplate.multi();
                redisTemplate.opsForValue().decrement(prodKey);
                redisTemplate.opsForSet().add(userKey,prodKey);

                return redisTemplate.exec();
            }
        };

        List<Object> result = (List<Object>) redisTemplate.execute(sessionCallback);

        if (result != null && result.size() != 0) {
            System.out.println("秒杀成功" + userKey);
            System.out.println("剩余库存：" + redisTemplate.opsForValue().get(prodKey));
        }else {
            System.out.println("秒杀失败");
        }

        return null;
    }
}
