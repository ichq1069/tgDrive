package com.skydevs.tgdrive.mapper;

import com.github.pagehelper.Page;
import com.skydevs.tgdrive.entity.FileInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FileMapper {

    /**
     * 插入已上传文件
     * @param fileInfo
     */
    @Insert("INSERT INTO files (file_name, download_url, upload_time, file_id, size, full_size, webdav_path, dir, user_id, is_public, library, content_level, in_random_pool, pool_folder_id) VALUES (#{fileName}, #{downloadUrl}, #{uploadTime}, #{fileId}, #{size}, #{fullSize}, #{webdavPath}, #{dir}, #{userId}, #{isPublic}, #{library}, #{contentLevel}, #{inRandomPool}, #{poolFolderId})")
    void insertFile(FileInfo fileInfo);

    /**
     * 获取全部文件
     * @return
     */
    @Select("SELECT * FROM files order by upload_time desc ")
    Page<FileInfo> getAllFiles();

    @SelectProvider(type = FileSqlProvider.class, method = "getFilteredFilesQuery")
    Page<FileInfo> getFilteredFiles(@Param("keyword") String keyword, @Param("userId") Long userId, @Param("role") String role);

    class FileSqlProvider {
        public String getFilteredFilesQuery(String keyword, Long userId, String role) {
            StringBuilder sql = new StringBuilder("SELECT f.*, u.username as uploader FROM files f LEFT JOIN users u ON f.user_id = u.id WHERE 1=1");
            
            // 关键词过滤
            if (keyword != null && !keyword.isEmpty()) {
                sql.append(" AND f.file_name LIKE '%").append(keyword).append("%'");
            }
            
            // 权限过滤：共享库对登录用户可见，私密库仅授权用户可见
            if ("admin".equals(role)) {
                // admin可以查看所有文件，不添加额外条件
            } else if ("admin_filter".equals(role)) {
                sql.append(" AND f.user_id = ").append(userId);
            } else if ("user".equals(role) || "user_private".equals(role)) {
                sql.append(" AND (f.user_id = ").append(userId)
                        .append(" OR f.library = 'shared'");
                if ("user_private".equals(role)) {
                    sql.append(" OR f.library = 'private'");
                }
                sql.append(")");
            } else if ("visitor".equals(role) || userId == null) {
                sql.append(" AND f.library = 'shared'");
            }
            
            sql.append(" ORDER BY f.upload_time DESC");
            return sql.toString();
        }

        public String getLibraryFilesQuery(@Param("library") String library,
                                           @Param("keyword") String keyword,
                                           @Param("userId") Long userId,
                                           @Param("role") String role,
                                           @Param("tagIds") List<Long> tagIds) {
            StringBuilder sql = new StringBuilder("SELECT f.*, u.username as uploader FROM files f LEFT JOIN users u ON f.user_id = u.id WHERE f.library = #{library}");
            if (keyword != null && !keyword.isEmpty()) {
                sql.append(" AND f.file_name LIKE '%' || #{keyword} || '%'");
            }
            if ("tele".equals(library) && !"admin".equals(role) && userId != null) {
                sql.append(" AND f.user_id = #{userId}");
            }
            appendTagFilter(sql, tagIds);
            sql.append(" ORDER BY f.upload_time DESC");
            return sql.toString();
        }

        public String getGalleryFilesQuery(@Param("keyword") String keyword,
                                           @Param("folderId") Long folderId,
                                           @Param("fileType") String fileType,
                                           @Param("tagIds") List<Long> tagIds) {
            StringBuilder sql = new StringBuilder("SELECT f.*, u.username as uploader FROM files f LEFT JOIN users u ON f.user_id = u.id WHERE f.library = 'shared' AND f.in_random_pool = 1");
            if (keyword != null && !keyword.isEmpty()) {
                sql.append(" AND f.file_name LIKE '%' || #{keyword} || '%'");
            }
            if (folderId != null) {
                sql.append(" AND f.pool_folder_id = #{folderId}");
            }
            appendFileTypeFilter(sql, fileType);
            appendTagFilter(sql, tagIds);
            sql.append(" ORDER BY f.upload_time DESC");
            return sql.toString();
        }

        public String getRandomPoolQuery(@Param("folderId") Long folderId, @Param("tagIds") List<Long> tagIds) {
            StringBuilder sql = new StringBuilder("SELECT f.*, u.username as uploader FROM files f LEFT JOIN users u ON f.user_id = u.id WHERE f.library = 'shared' AND f.in_random_pool = 1");
            if (folderId != null) {
                sql.append(" AND f.pool_folder_id = #{folderId}");
            }
            appendTagFilter(sql, tagIds);
            sql.append(" ORDER BY f.upload_time DESC");
            return sql.toString();
        }

        private void appendTagFilter(StringBuilder sql, List<Long> tagIds) {
            if (tagIds == null || tagIds.isEmpty()) {
                return;
            }
            sql.append(" AND (SELECT COUNT(1) FROM file_tags ft WHERE ft.file_id = f.file_id AND ft.tag_id IN (");
            for (int i = 0; i < tagIds.size(); i++) {
                if (i > 0) {
                    sql.append(",");
                }
                sql.append("#{tagIds[").append(i).append("]}");
            }
            sql.append(")) = ").append(tagIds.size());
        }

        private void appendFileTypeFilter(StringBuilder sql, String fileType) {
            if (fileType == null || fileType.isEmpty()) {
                return;
            }
            switch (fileType) {
                case "image" -> sql.append(" AND lower(f.file_name) GLOB '*.{jpg,jpeg,png,gif,webp,bmp,svg}' = 0 AND (lower(f.file_name) LIKE '%.jpg' OR lower(f.file_name) LIKE '%.jpeg' OR lower(f.file_name) LIKE '%.png' OR lower(f.file_name) LIKE '%.gif' OR lower(f.file_name) LIKE '%.webp' OR lower(f.file_name) LIKE '%.bmp' OR lower(f.file_name) LIKE '%.svg')");
                case "video" -> sql.append(" AND (lower(f.file_name) LIKE '%.mp4' OR lower(f.file_name) LIKE '%.webm' OR lower(f.file_name) LIKE '%.mkv' OR lower(f.file_name) LIKE '%.mov' OR lower(f.file_name) LIKE '%.avi')");
                case "audio" -> sql.append(" AND (lower(f.file_name) LIKE '%.mp3' OR lower(f.file_name) LIKE '%.wav' OR lower(f.file_name) LIKE '%.flac' OR lower(f.file_name) LIKE '%.aac' OR lower(f.file_name) LIKE '%.ogg')");
                default -> {
                }
            }
        }
    }

    @Select("SELECT file_name FROM files where file_id = #{fileId} AND (webdav_path IS NULL OR webdav_path != 'deleted') LIMIT 1")
    String getFileNameByFileId(String fileId);

    @Select("SELECT full_size FROM files where file_id = #{fileId} LIMIT 1")
    Long getFullSizeByFileId(String fileId);

    void updateUrl(String prefix);

    @Select("SELECT * FROM files WHERE webdav_path = #{path}")
    FileInfo getFileByWebdavPath(String path);

    @Select("SELECT * FROM files WHERE webdav_path LIKE #{path} || '%' ORDER BY id DESC")
    List<FileInfo> getFilesByPathPrefix(String path);

    @Select("SELECT * FROM files WHERE file_id = #{fileId}")
    FileInfo getFileByFileId(String fileId);

    @Delete("DELETE FROM files WHERE file_id = #{fileId}")
    void deleteFile(String fileId);

    @Delete("DELETE FROM files WHERE webdav_path LIKE CONCAT(#{path}, '%')")
    void deleteFileByWebDav(String path);

    @Update("UPDATE files SET download_url = #{file.downloadUrl}, upload_time = #{file.uploadTime}, size = #{file.size}, full_size = #{file.fullSize}, file_id = #{file.fileId} WHERE webdav_path = #{target}")
    void updateFileAttributeByWebDav(@Param("file") FileInfo file, @Param("target") String target);

    @Insert("INSERT INTO files (file_name, download_url, upload_time, file_id, size, full_size, webdav_path, dir, user_id, is_public, library, content_level, in_random_pool, pool_folder_id) VALUES (#{file.fileName}, #{file.downloadUrl}, #{file.uploadTime}, #{file.fileId}, #{file.size}, #{file.fullSize}, #{target}, #{file.dir}, #{file.userId}, #{file.isPublic}, #{file.library}, #{file.contentLevel}, #{file.inRandomPool}, #{file.poolFolderId})")
    void moveFile(@Param("file") FileInfo sourceFile, @Param("target") String target);

    @Update("UPDATE files SET is_public = #{isPublic} WHERE file_id = #{fileId}")
    void updateIsPublic(@Param("fileId") String fileId, @Param("isPublic") boolean isPublic);

    @Update("UPDATE files SET library = #{library}, is_public = #{isPublic}, content_level = #{contentLevel}, in_random_pool = #{inRandomPool}, pool_folder_id = #{poolFolderId} WHERE file_id = #{fileId}")
    void updateLibrary(@Param("fileId") String fileId,
                       @Param("library") String library,
                       @Param("isPublic") boolean isPublic,
                       @Param("contentLevel") String contentLevel,
                       @Param("inRandomPool") boolean inRandomPool,
                       @Param("poolFolderId") Long poolFolderId);

    @Update("UPDATE files SET in_random_pool = #{inRandomPool}, pool_folder_id = #{poolFolderId} WHERE file_id = #{fileId}")
    void updateRandomPool(@Param("fileId") String fileId,
                          @Param("inRandomPool") boolean inRandomPool,
                          @Param("poolFolderId") Long poolFolderId);

    @Update("UPDATE files SET pool_folder_id = NULL WHERE pool_folder_id = #{folderId}")
    void clearPoolFolder(Long folderId);

    @SelectProvider(type = FileSqlProvider.class, method = "getLibraryFilesQuery")
    Page<FileInfo> getLibraryFiles(@Param("library") String library,
                                   @Param("keyword") String keyword,
                                   @Param("userId") Long userId,
                                   @Param("role") String role,
                                   @Param("tagIds") List<Long> tagIds);

    @SelectProvider(type = FileSqlProvider.class, method = "getGalleryFilesQuery")
    Page<FileInfo> getGalleryFiles(@Param("keyword") String keyword,
                                   @Param("folderId") Long folderId,
                                   @Param("fileType") String fileType,
                                   @Param("tagIds") List<Long> tagIds);

    @SelectProvider(type = FileSqlProvider.class, method = "getRandomPoolQuery")
    List<FileInfo> getRandomPoolFiles(@Param("folderId") Long folderId, @Param("tagIds") List<Long> tagIds);
}
