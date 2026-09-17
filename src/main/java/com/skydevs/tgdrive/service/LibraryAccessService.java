package com.skydevs.tgdrive.service;

import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.entity.User;

public interface LibraryAccessService {

    boolean canList(String library, User user);

    boolean canDownload(FileInfo file, User user);

    boolean isPrivateAuthorized(User user);

    User getCurrentUserOrNull();

    User requireCurrentUser();
}
