package org.dromara.ai.common.pagination;

public record PageQuery(int page, int size) {

    private static final int MAX_SIZE = 200;

    public PageQuery {
        page = Math.max(page, 1);
        size = Math.clamp(size, 1, MAX_SIZE);
    }
}
