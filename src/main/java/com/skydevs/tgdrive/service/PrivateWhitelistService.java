package com.skydevs.tgdrive.service;

import java.util.List;

public interface PrivateWhitelistService {

    List<Long> listUserIds();

    void add(Long userId);

    void remove(Long userId);
}
