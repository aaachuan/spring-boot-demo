package com.xkcoding.cache.redis.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Article implements Serializable {

    private static final long serialVersionUID = -5671905373220239555L;
    /**
     * 主键id
     */
    private Long id;

    private String title;

    private String link;

    private Long posterId;

    private Long time;

    private Long votes;
}
