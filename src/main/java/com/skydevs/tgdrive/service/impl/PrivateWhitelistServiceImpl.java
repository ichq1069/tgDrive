package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.entity.User;
import com.skydevs.tgdrive.exception.BadRequestException;
import com.skydevs.tgdrive.mapper.UserMapper;
import com.skydevs.tgdrive.mapper.WhitelistMapper;
import com.skydevs.tgdrive.service.PrivateWhitelistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PrivateWhitelistServiceImpl implements PrivateWhitelistService {

    private final WhitelistMapper whitelistMapper;
    private final UserMapper userMapper;

    @Override
    public List<Long> listUserIds() {
        return whitelistMapper.listUserIds();
    }

    @Override
    public void add(Long userId) {
        User user = userMapper.getUserById(userId);
        if (user == null) {
            throw new BadRequestException("用户不存在");
        }
        if (whitelistMapper.countByUserId(userId) > 0) {
            return;
        }
        whitelistMapper.insert(userId);
        log.info("将用户 {} 加入私密库白名单", userId);
    }

    @Override
    public void remove(Long userId) {
        whitelistMapper.delete(userId);
        log.info("将用户 {} 从私密库白名单移除", userId);
    }
}
