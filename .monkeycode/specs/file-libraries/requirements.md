# Requirements Document

## Introduction

将 telegram-r2-worker 的 Tele 库、共享库、私密库、标签、随机池与画廊能力落地到 tgDrive。文件本体仍存储在 Telegram；tgDrive 负责库归属、会员等级、私密库白名单、标签、随机展示与权限控制。

当前 tgDrive 仅有 `files.is_public` 二元可见性与 `admin` / `user` / `visitor` 三角色。本需求引入三库模型：上传默认进入 Tele 库；仅管理员可将文件转入共享库或私密库，转入时从 Tele 库搬走（改 library，原列表不再显示）。共享库对全部已登录用户开放。私密库仅 `admin`、`vvip` 与白名单用户可进入，且进入后可见全部私密文件；`content_level` 仅供管理员分类。画廊与随机接口只从共享库随机池抽文件。

## Glossary

- **System**: tgDrive 网盘应用（Spring Boot 后端 + Vue 前端）
- **Tele 库**: 上传后的默认文件集合，文件仍按上传者归属
- **共享库 (Shared Pool)**: 管理员从 Tele 库转入的公开集合，全部已登录用户可浏览、预览、下载
- **私密库 (Private Pool)**: 管理员从 Tele 库转入的受限集合，仅 `vvip` 用户与私密库白名单用户可进入
- **私密库白名单用户**: 管理员指定的用户，可进入私密库，并与 vvip 一样看到全部私密文件
- **随机池**: 共享库中标记为可随机抽取的条目，供画廊与随机接口使用
- **画廊**: 瀑布流展示页，按标签、类型、文件夹筛选共享库随机池内容
- **库归属 (library)**: `tele`、`shared`、`private`
- **内容等级 (content_level)**: 管理员给私密库文件打的分类标记，取值 `pt`、`vip`、`svip`、`vvip`；不用于拦截 vvip 或白名单用户的浏览与下载
- **会员等级 (member_level)**: 用户账号等级，取值 `pt`、`vip`、`svip`、`vvip`；`admin` 可访问全部库
- **等级顺序**: `pt` < `vip` < `svip` < `vvip`
- **标签 (tag)**: 管理员维护的分类关键字，可挂到共享库与私密库条目
- **转入**: 将 Tele 库文件搬走到共享库或私密库：更新 library，引用同一 Telegram `file_id`，不重复上传；该文件从 Tele 库列表消失
- **转出**: 将文件从共享库或私密库搬回 Tele 库：library 改回 `tele`，该文件重新出现在 Tele 库
- **管理员 (admin)**: 角色为 `admin` 的已登录用户
- **普通用户 (user)**: 角色为 `user` 的已登录用户
- **访客 (visitor)**: 角色为 `visitor` 或未登录用户
- **一次性兑换码**: `code_type = once`，全站成功兑换一次后即失效
- **重复性兑换码**: `code_type = repeatable`，全站可兑至 `max_uses`；同一用户对同一码仅能成功兑换一次

## Requirements

### Requirement 1: 三库数据模型

**User Story:** AS 管理员, I want 每个文件具备库归属, so that 文件可以按 Tele 库、共享库、私密库分别管理

#### Acceptance Criteria

1. THE System SHALL persist a library field for each file record, with allowed values `tele`, `shared`, and `private`
2. WHEN a user uploads a file through the existing upload API, THE System SHALL set that file's library to `tele`
3. WHEN an admin transfers a file into 共享库, THE System SHALL set that file's library to `shared` and SHALL exclude that file from Tele 库 listings
4. WHEN an admin transfers a file into 私密库, THE System SHALL set that file's library to `private` and SHALL exclude that file from Tele 库 listings
5. Existing files with `is_public = 1` SHALL be migrated to library `shared`; remaining files SHALL be migrated to library `tele`

### Requirement 2: Tele 库

**User Story:** AS 登录用户, I want 在 Tele 库中查看自己上传的文件, so that 我能管理存储在 Telegram 中的个人文件

#### Acceptance Criteria

1. WHEN an admin opens Tele 库, THE System SHALL list all files whose library is `tele`
2. WHEN a user opens Tele 库, THE System SHALL list files whose library is `tele` and whose owner is that user
3. WHILE a file is in Tele 库, THE System SHALL allow the owner and admin to preview, download, copy link, and delete that file
4. THE System SHALL keep the existing Telegram upload, chunked transfer, and download URL behavior for Tele 库 files

### Requirement 3: 共享库

**User Story:** AS 管理员, I want 把 Tele 库文件转入共享库, so that 全部已登录用户可以浏览这些文件

#### Acceptance Criteria

1. WHEN an admin selects one or more Tele 库 files and confirms transfer to 共享库, THE System SHALL set those files' library to `shared`
2. WHEN a user or visitor requests transfer into 共享库, THE System SHALL reject the request unless the requester role is `admin`
3. WHEN a logged-in user opens 共享库, THE System SHALL list files whose library is `shared`
4. WHILE a file is in 共享库, THE System SHALL allow logged-in users to preview, download, and copy the download link
5. WHEN a visitor requests the shared-library list, THE System SHALL return HTTP 401
6. WHEN any client requests download of a `shared` file through `/d/{fileId}`, THE System SHALL allow the request without login
7. WHEN an admin confirms removal of a file from 共享库, THE System SHALL set that file's library back to `tele` and SHALL show that file in Tele 库 listings again

### Requirement 4: 会员等级、私密库与白名单

**User Story:** AS 管理员, I want 用 vvip 等级和白名单控制私密库入口, so that 高敏感内容只对授权用户开放

#### Acceptance Criteria

1. THE System SHALL persist a member_level on each user account, with allowed values `pt`, `vip`, `svip`, and `vvip`
2. WHEN an admin creates or updates a user, THE System SHALL allow the admin to set that user's member_level
3. Existing users SHALL be migrated to member_level `pt`; existing admin accounts SHALL retain role `admin` and receive member_level `vvip`
4. THE System SHALL persist a private-library whitelist of user IDs
5. WHEN an admin adds or removes a user on the private-library whitelist, THE System SHALL update that whitelist
6. WHEN an admin transfers files into 私密库, THE System SHALL require a content_level of `pt`, `vip`, `svip`, or `vvip`
7. WHEN an admin opens 私密库, THE System SHALL list all private files
8. WHEN a vvip user opens 私密库, THE System SHALL list all private files
9. WHEN a whitelist user opens 私密库, THE System SHALL list all private files
10. WHEN a logged-in user who is neither admin, nor vvip, nor on the whitelist requests the private-library list, THE System SHALL return HTTP 403
11. WHEN a visitor requests the private-library list, THE System SHALL return HTTP 401
12. WHEN an admin confirms removal of a file from 私密库, THE System SHALL set that file's library back to `tele` and SHALL show that file in Tele 库 listings again
13. THE user interface SHALL hide the 私密库 navigation entry unless the current user is admin, vvip, or on the whitelist

### Requirement 5: 标签

**User Story:** AS 用户, I want 在文件转入前给自己的 Tele 库文件打标签, so that 管理员转入后仍能按主题筛选

#### Acceptance Criteria

1. THE System SHALL persist a tag catalog with unique tag names
2. WHEN an admin creates, renames, or deletes a tag, THE System SHALL update the tag catalog
3. WHEN a logged-in user sets tags on a Tele 库 file owned by that user, THE System SHALL persist those tag associations
4. WHEN a user attempts to create, rename, or delete a catalog tag, THE System SHALL reject the request unless the user is admin
5. WHEN an admin sets tags on a shared or private file, THE System SHALL persist those tag associations
6. WHEN a file is transferred from Tele 库 to 共享库 or 私密库, THE System SHALL keep the existing tag associations on that file
7. WHEN a user lists 共享库 or 私密库 with a tag filter, THE System SHALL return files that contain all requested tags and remain within that user's visibility
8. WHEN an admin deletes a tag, THE System SHALL remove that tag from all file associations

### Requirement 6: 随机池（仅共享库）

**User Story:** AS 管理员, I want 把共享库文件加入随机池, so that 画廊和随机接口只从共享库抽取

#### Acceptance Criteria

1. THE System SHALL persist a random-pool flag and optional folder on each shared-library file
2. WHEN an admin enables random-pool on selected shared-library files, THE System SHALL include those files in random selection
3. WHEN an admin attempts to add a Tele 库 or 私密库 file to the random pool, THE System SHALL reject the request
4. WHEN a logged-in user calls the random API, THE System SHALL return one file from enabled shared-library random-pool entries
5. WHEN the shared-library random-pool has zero entries, THE System SHALL return HTTP 404 with an empty-result message
6. THE System SHALL allow admin to create, rename, and delete random-pool folders and to move shared-pool files between folders

### Requirement 7: 画廊

**User Story:** AS 已登录用户, I want 用瀑布流浏览共享库随机池, so that 我可以按标签和文件夹翻看公开运营内容

#### Acceptance Criteria

1. THE System SHALL provide a gallery page for logged-in users
2. WHEN a user opens the gallery, THE System SHALL load shared-library random-pool files with pagination
3. WHEN a user applies tag, file-type, or folder filters on the gallery, THE System SHALL return matching shared-library random-pool files
4. WHILE rendering gallery items, THE System SHALL use existing Telegram download URLs for preview
5. THE gallery page SHALL exclude Tele 库 files and 私密库 files

### Requirement 8: 管理端界面

**User Story:** AS 管理员, I want 在管理后台分别管理三库、标签、随机池、白名单和用户等级, so that 我可以完成转入、分级和运营操作

#### Acceptance Criteria

1. THE System SHALL add admin navigation entries: Tele 库, 共享库, 私密库, 标签管理, 画廊, 私密库白名单
2. WHEN the admin is on Tele 库, THE System SHALL provide batch actions "转入共享库" and "转入私密库"
3. WHEN the admin transfers files to 私密库, THE System SHALL prompt for content_level
4. WHEN the admin is on 共享库, THE System SHALL provide batch actions: 转回 Tele 库, 设置标签, 加入/移出随机池, 移动到文件夹
5. WHEN the admin is on 私密库, THE System SHALL provide batch actions: 转回 Tele 库, 设置标签, 设置 content_level
6. THE user-management page SHALL display and edit member_level
7. THE existing `/fileList` page SHALL display a library column and a content_level column

### Requirement 9: 用户端界面

**User Story:** AS 普通用户, I want 进入 Tele 库、共享库、画廊，并在获权后进入私密库, so that 我能管理个人文件并按授权浏览运营内容

#### Acceptance Criteria

1. THE System SHALL add user navigation entries: Tele 库, 共享库, 画廊
2. WHEN the current user is vvip or on the private-library whitelist, THE System SHALL also show a 私密库 navigation entry
3. WHEN a user opens Tele 库, THE System SHALL show only that user's `tele` files
4. WHEN a user opens 共享库, THE System SHALL show all `shared` files
5. WHEN an authorized user opens 私密库, THE System SHALL show private files according to Requirement 4
6. THE user interface SHALL display the current user's member_level

### Requirement 10: 下载与删除权限

**User Story:** AS 系统, I want 下载和删除接口按库归属、vvip 与白名单鉴权, so that 私密库文件无法被未授权用户通过直链获取

#### Acceptance Criteria

1. WHEN a user requests download of a `tele` file, THE System SHALL allow the request if the user is the owner or an admin
2. WHEN any client requests download of a `shared` file through `/d/{fileId}`, THE System SHALL allow the request without login
3. WHEN an admin, a vvip user, or a whitelist user requests download of a `private` file, THE System SHALL allow the request
4. WHEN a visitor requests download of a `private` file, THE System SHALL return HTTP 401
5. WHEN a logged-in user who is neither admin, nor vvip, nor on the whitelist requests download of a `private` file, THE System SHALL return HTTP 403
6. WHEN a user requests deletion of a file, THE System SHALL allow deletion if the user is the owner of a `tele` file or an admin
7. WHEN a file is deleted, THE System SHALL remove the database record, remove pool and tag associations, and request Telegram deletion using the existing delete flow

### Requirement 11: WebDAV 兼容

**User Story:** AS WebDAV 客户端用户, I want 现有 WebDAV 路径继续可用, so that 三库上线后已挂载的客户端仍能访问文件

#### Acceptance Criteria

1. WHILE a file is accessed through WebDAV, THE System SHALL continue to resolve the file by `webdav_path`
2. WHEN a new file is uploaded through WebDAV, THE System SHALL set library to `tele`
3. WebDAV listing SHALL include the authenticated user's `tele` files and all `shared` files
4. WebDAV listing SHALL include `private` files only when the authenticated credentials belong to admin, a vvip user, or a whitelist user

### Requirement 12: 会员升级

**User Story:** AS 用户, I want 通过兑换码或由管理员调整会员等级, so that 我可以获得 vvip 并进入私密库

#### Acceptance Criteria

1. THE System SHALL persist redeem codes with fields: code, code_type (`once` or `repeatable`), target_member_level, max_uses, used_count, expires_at, and enabled
2. WHEN an admin creates, updates, enables, disables, or deletes a redeem code, THE System SHALL persist that change
3. WHEN a logged-in user submits a valid redeem code, THE System SHALL set that user's member_level to the code's target_member_level if the new level is higher than the current level
4. WHEN a logged-in user submits a code that user already redeemed, THE System SHALL reject the request and keep the current member_level
5. WHEN a logged-in user submits an expired, disabled, exhausted, or unknown redeem code, THE System SHALL reject the request and keep the current member_level
6. WHEN an `once` code is successfully redeemed, THE System SHALL mark that code as exhausted
7. WHEN an admin sets a higher member_level on the user-management page, THE System SHALL persist that member_level immediately
8. WHEN an admin sets a lower member_level on the user-management page, THE System SHALL persist that change only after the admin confirms the demotion in a second confirmation step
9. THE user interface SHALL provide a redeem-code entry on the user account page
10. THE admin interface SHALL provide redeem-code management
