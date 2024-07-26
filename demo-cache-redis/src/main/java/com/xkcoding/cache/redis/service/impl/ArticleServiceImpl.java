package com.xkcoding.cache.redis.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.xkcoding.cache.redis.entity.Article;
import com.xkcoding.cache.redis.entity.User;
import com.xkcoding.cache.redis.exception.AppException;
import com.xkcoding.cache.redis.service.ArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private RedisTemplate<String, Serializable> redisCacheTemplate;
    private static final Long ONE_WEEK_IN_SECONDS = 7 * 86400L;

    private static final Long VOTE_SCORE = 432L;

    private static final Long ARTICLES_PER_PAGE = 25L;

    @Override
    public void articleVote(User user, Article article) {
        Instant now = Instant.now();
        long timestampMillis = now.toEpochMilli();
        long cutOff = timestampMillis - ONE_WEEK_IN_SECONDS;
        Double score = redisCacheTemplate.opsForZSet().score("time:", "article:" + article.getId());
        //article超过一周后不能投票，这里检查是否还能继续投票
        if (score - cutOff < 0.0d)
            return;
        Long addUserToVoteSet = redisCacheTemplate.opsForSet().add("voted:" + article.getId(), "user:" + user.getId());
        if (Objects.nonNull(addUserToVoteSet)) {
            redisCacheTemplate.opsForZSet().incrementScore("score:","article:" + article.getId(),VOTE_SCORE);
            redisCacheTemplate.opsForHash().increment("article:" + String.valueOf(article.getId()), "votes", 1);
        }
    }

    @Override
    public Long postArticle(User user, Article article) {
        long articleId = IdUtil.getSnowflake(1,1).nextId();

        String voted = "voted:" + articleId;
        String userVoted = "user:" + user.getId();
        redisCacheTemplate.opsForSet().add(voted, userVoted);
        redisCacheTemplate.expire(voted, ONE_WEEK_IN_SECONDS, TimeUnit.SECONDS);

        redisCacheTemplate.opsForHash().put("article:" + articleId, "title", article.getTitle());
        redisCacheTemplate.opsForHash().put("article:" + articleId, "link", article.getLink());
        redisCacheTemplate.opsForHash().put("article:" + articleId, "poster", article.getPosterId());
        long nowMilli = Instant.now().toEpochMilli();
        redisCacheTemplate.opsForHash().put("article:" + articleId, "time", nowMilli);
        redisCacheTemplate.opsForHash().put("article:" + articleId, "votes", 1);

        redisCacheTemplate.opsForZSet().add("score:", "article:" + articleId, nowMilli + VOTE_SCORE);
        redisCacheTemplate.opsForZSet().add("time:", "article:" + articleId, nowMilli);

        return articleId;

    }

    @Override
    public List<Article> getArticles(Long pageNo, String ordersBy) throws AppException {
        if ("score:".equals(ordersBy) || "time:".equals(ordersBy)) {
            Long start = (pageNo - 1) * ARTICLES_PER_PAGE;
            Long end = start + ARTICLES_PER_PAGE - 1;
            Set<Serializable> rangeArticleIds = redisCacheTemplate.opsForZSet().range(ordersBy, start, end);
            List<Article> articleList = new ArrayList<>();
            for (Serializable articleId: rangeArticleIds) {
                Map<Object, Object> articleData = redisCacheTemplate.opsForHash().entries((String) articleId);
                String title = (String) articleData.get("title");
                String link = (String) articleData.get("link");
                Long posterId = (Long) articleData.get("posterId");
                Long time = (Long) articleData.get("time");
                Long votes = (Long) articleData.get("votes");

                Article article = new Article();
                article.setId((Long) articleId);
                article.setTitle(title);
                article.setLink(link);
                article.setPosterId(posterId);
                article.setTime(time);
                article.setVotes(votes);

                articleList.add(article);

            }
            return articleList;
        } else {
            throw new AppException("Unsupported ordersBy ZSet:" + ordersBy);
        }
    }

    @Override
    public void addRemoveGroups(Long articleId, List<String> addToGroup, List<String> moveFromGroup) {
        String article = "article:" + articleId;
        for (String addGroupName: addToGroup) {
            redisCacheTemplate.opsForSet().add("group:" + addGroupName, article);
        }
        for (String moveGroupName: moveFromGroup) {
            redisCacheTemplate.opsForSet().remove("group:" + moveGroupName, article);
        }
    }

    @Override
    public List<Article> getGroupArticles(String groupName, Long page, String ordersBy) {
        if (!StrUtil.equals(ordersBy,"score:")|| !StrUtil.equals(ordersBy,"time:"))
            throw new AppException("Unsupported ordersBy ZSet:" + ordersBy);
        String key = ordersBy + groupName;
        if(!redisCacheTemplate.hasKey(key)) {
            redisCacheTemplate.opsForZSet().intersectAndStore("group:" + groupName, Collections.singleton(ordersBy), key, RedisZSetCommands.Aggregate.MAX);
        }
        return getArticles(page, key);
    }


}
