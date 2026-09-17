package com.skydevs.tgdrive.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.skydevs.tgdrive.constants.Libraries;
import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.entity.User;
import com.skydevs.tgdrive.exception.ForbiddenException;
import com.skydevs.tgdrive.exception.UnauthorizedException;
import com.skydevs.tgdrive.mapper.WhitelistMapper;
import com.skydevs.tgdrive.service.LibraryAccessService;
import com.skydevs.tgdrive.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LibraryAccessServiceImpl implements LibraryAccessService {

    private final UserService userService;
    private final WhitelistMapper whitelistMapper;

    @Override
    public boolean canList(String library, User user) {
        if (!Libraries.isValid(library)) {
            return false;
        }
        if (user == null) {
            return Libraries.SHARED.equals(library);
        }
        if ("admin".equals(user.getRole())) {
            return true;
        }
        return switch (library) {
            case Libraries.TELE -> true;
            case Libraries.SHARED -> true;
            case Libraries.PRIVATE -> isPrivateAuthorized(user);
            default -> false;
        };
    }

    @Override
    public boolean canDownload(FileInfo file, User user) {
        return switch (file.getLibrary()) {
            case Libraries.SHARED -> true;
            case Libraries.TELE -> user != null && (
                    "admin".equals(user.getRole())
                            || (file.getUserId() != null && file.getUserId().equals(user.getId()))
            );
            case Libraries.PRIVATE -> user != null && (
                    "admin".equals(user.getRole())
                            || isPrivateAuthorized(user)
            );
            default -> false;
        };
    }

    @Override
    public boolean isPrivateAuthorized(User user) {
        if (user == null) {
            return false;
        }
        if ("admin".equals(user.getRole())) {
            return true;
        }
        if ("vvip".equals(user.getMemberLevel())) {
            return true;
        }
        return whitelistMapper.countByUserId(user.getId()) > 0;
    }

    @Override
    public User getCurrentUserOrNull() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        try {
            return userService.getById(StpUtil.getLoginIdAsLong());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public User requireCurrentUser() {
        User user = getCurrentUserOrNull();
        if (user == null) {
            throw new UnauthorizedException();
        }
        return user;
    }
}
