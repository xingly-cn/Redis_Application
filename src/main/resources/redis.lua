local userId=KEYS[1];
local productId=KEYS[2];
local userKey='user:' .. userId;
local prodKey='good:' .. productId;
local userExists=redis.call("sismember",userKey,prodKey);
if tonumber(userExists)==1 then
    return 2;
end
local num=redis.call("get",prodKey);
if tonumber(num)<=0 then
    return 0;
else
    redis.call("decr",prodKey);
    redis.call("sadd",userKey,prodKey);
end
return 1;