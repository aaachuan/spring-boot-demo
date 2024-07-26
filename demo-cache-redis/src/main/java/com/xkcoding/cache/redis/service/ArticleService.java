package com.xkcoding.cache.redis.service;

import com.xkcoding.cache.redis.entity.Article;
import com.xkcoding.cache.redis.entity.User;

import java.util.List;

public interface ArticleService {
    void articleVote(User user, Article article);

    Long postArticle(User user, Article article);

    List<Article> getArticles(Long pageNo, String ordersBy) throws Exception;

    void addRemoveGroups(Long articleId, List<String> addToGroup,List<String> moveFromGroup);

    List<Article> getGroupArticles(String groupName, Long page, String ordersBy);
}
