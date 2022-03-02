package com.sugar.redis6.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@RestController
public class RedisController {

    @Autowired
    private RedisTemplate redisTemplate;

    /** 乐观锁 + lua 解决超售、库存遗留问题
     * modify 
     * @author sugar
     * @date 2022/3/2 18:22
     */
    @RequestMapping("/go")
    public String Ms() {

        String userId = new Random().nextInt(50000) + "";
        String productId = "plane";
        //set good:plane 10
        // 用户 key 商品 key
        String userKey = "user:" + userId;
        String prodKey = "good:" + productId;

        //SessionCallback sessionCallback = new SessionCallback<List>() {
        //    @Override
        //    public List execute(RedisOperations operations) throws DataAccessException {
        //        // 监视库存
        //        redisTemplate.watch(prodKey);
        //
        //        // 获取库存, 若库存为0, 表示秒杀未开始
        //        int remain = (int)redisTemplate.opsForValue().get(prodKey);
        //        if (remain == 0) {
        //            System.out.println("秒杀未开始");
        //            return null;
        //        }
        //
        //        // 判断库存
        //        if ((int)redisTemplate.opsForValue().get(prodKey) < 1) {
        //            System.out.println("库存不足啦");
        //            return null;
        //        }
        //
        //        // 判断用户重复秒杀
        //        Boolean member = redisTemplate.opsForSet().isMember(userKey, prodKey);
        //        if (member) {
        //            System.out.println("重复秒杀");
        //            return null;
        //        }
        //
        //        // 开始事务, 抢购开始
        //        redisTemplate.multi();
        //        redisTemplate.opsForValue().decrement(prodKey);
        //        redisTemplate.opsForSet().add(userKey,prodKey);
        //
        //        return redisTemplate.exec();
        //    }
        //};
        //List<Object> result = (List<Object>) redisTemplate.execute(sessionCallback);

        // 采用 lua 脚本解决库存遗留问题
        DefaultRedisScript redisScript = new DefaultRedisScript();
        redisScript.setLocation(new ClassPathResource("redis.lua"));
        redisScript.setResultType(Long.class);
        List<String> list = new ArrayList<>();
        list.add(userId);
        list.add(productId);

        Object result = redisTemplate.execute(redisScript, list);

        switch (result.toString()) {
            case "1":
                System.out.println("秒杀成功");
                System.out.println("库存剩余" + redisTemplate.opsForValue().get(prodKey));
            case "2":
                System.out.println("重复抢购");
            case "0":
                System.out.println("抢购失败");
        }
        return null;
    }

    @RequestMapping("/lock")
    public void lock() {
        // 获取锁
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "我是一把锁",3, TimeUnit.SECONDS);

        // 获得成功
        if (lock) {
            Object age = redisTemplate.opsForValue().get("age");
            // 业务逻辑
            redisTemplate.opsForValue().increment("age");
            System.out.println(age.toString() + "成功");
            //释放锁
            redisTemplate.delete("lock");
        }else {
            try {
                Thread.sleep(100);
                lock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
