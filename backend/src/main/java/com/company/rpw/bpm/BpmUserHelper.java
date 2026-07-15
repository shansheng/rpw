package com.company.rpw.bpm;

import com.company.rpw.entity.SysUser;
import com.company.rpw.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程模块用户解析助手：根据 userId 解析昵称，构建 BpmUserInfo。
 */
@Component
@RequiredArgsConstructor
public class BpmUserHelper {

    private final SysUserMapper userMapper;

    public String getNickname(Long id) {
        if (id == null) {
            return "";
        }
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            return "用户" + id;
        }
        return u.getRealName() != null && !u.getRealName().isEmpty() ? u.getRealName() : u.getUsername();
    }

    public BpmUserInfo buildInfo(Long id) {
        BpmUserInfo info = new BpmUserInfo();
        info.setId(id);
        info.setNickname(getNickname(id));
        return info;
    }

    public BpmUserInfo buildInfoByUsername(String username) {
        if (username == null) {
            return null;
        }
        SysUser u = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username).last("LIMIT 1"));
        BpmUserInfo info = new BpmUserInfo();
        if (u == null) {
            info.setId(null);
            info.setNickname(username);
            return info;
        }
        info.setId(u.getId());
        info.setNickname(u.getRealName() != null && !u.getRealName().isEmpty() ? u.getRealName() : u.getUsername());
        return info;
    }

    public String getUsernameById(Long id) {
        if (id == null) return null;
        SysUser u = userMapper.selectById(id);
        return u == null ? null : u.getUsername();
    }

    public List<BpmUserInfo> buildInfos(List<Long> ids) {
        List<BpmUserInfo> result = new ArrayList<>();
        if (ids == null) {
            return result;
        }
        for (Long id : ids) {
            result.add(buildInfo(id));
        }
        return result;
    }
}
