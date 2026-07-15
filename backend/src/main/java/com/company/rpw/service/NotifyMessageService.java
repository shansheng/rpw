package com.company.rpw.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.rpw.entity.NotifyMessage;
import com.company.rpw.mapper.NotifyMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 站内信消息服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyMessageService {

    private final NotifyMessageMapper notifyMessageMapper;

    /**
     * 统计指定用户的未读站内信数量。
     * 仅统计 read_status=0（未读）且未逻辑删除的记录；
     * deleted=0 由 MyBatis-Plus 的 @TableLogic 自动追加。
     *
     * @param userId 当前登录用户编号
     * @return 未读数量
     */
    public long getUnreadCount(Long userId) {
        if (userId == null) {
            return 0L;
        }
        Long count = notifyMessageMapper.selectCount(
                new QueryWrapper<NotifyMessage>()
                        .eq("user_id", userId)
                        .eq("read_status", 0)
        );
        return count == null ? 0L : count;
    }
}
