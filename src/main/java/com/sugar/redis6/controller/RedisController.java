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

        // 用户 key
        String userKey = "good:" + userId;

        // 获取库存
        int remain = (int) redisTemplate.opsForValue().get(productId);
        if (remain == 0) {
            System.out.println("秒杀未开始");
            return "秒杀未开始";
        }

        // 判断用户是否重复秒杀
        Boolean member = redisTemplate.opsForSet().isMember(userKey, productId);
        if (member) {
            System.out.println("已秒杀成功,不可重复秒杀");
            return "已秒杀成功,不可重复秒杀";
        }

        // 库存小于1，秒杀结束
        if (remain <= 0) {
            System.out.println("库存不足");
            return "库存不足";
        }

        // 库存-1,用户加入名单
        // 增加事务
           redisTemplate.execute(new SessionCallback() {
               @Override
               public Object execute(RedisOperations operations) throws DataAccessException {
                   List<Object> exec = null;
                   operations.watch(productId);
                   operations.multi();
                   operations.opsForValue().decrement(productId);
                   operations.opsForSet().add(userKey, productId);
                   exec = operations.exec();
                   if (exec.size() == 0) {
                       return null;
                   }
                   int number = (int) redisTemplate.opsForValue().get(productId);
                   System.out.println(userId + " 抢购成功！ 当前剩余：" + number);
                   return null;
               }
           });
        return "秒杀成功：" + userKey;
    }
}
