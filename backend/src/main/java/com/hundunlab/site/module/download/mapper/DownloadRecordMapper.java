package com.hundunlab.site.module.download.mapper;

import com.hundunlab.site.module.download.pojo.Entity.DownloadRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DownloadRecordMapper {

    @Insert("""
            INSERT INTO download_record
                (user_id, software_id, release_id, asset_id, platform, ip_hash, user_agent)
            VALUES
                (#{userId}, #{softwareId}, #{releaseId}, #{assetId}, #{platform}, #{ipHash}, #{userAgent})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(DownloadRecord record);

    /**
     * 用户中心的「我的下载」：同一安装包只留最近一次，避免列表被重复记录刷屏。
     * <p>
     * 用 MAX(id) 子查询而不是 GROUP BY + MAX(created_at)：后者在默认的
     * ONLY_FULL_GROUP_BY 模式下会被 MySQL 8 拒绝。
     */
    @Select("""
            SELECT d.id, d.user_id, d.software_id, d.release_id, d.asset_id, d.platform,
                   d.ip_hash, d.user_agent, d.created_at
            FROM download_record d
                     INNER JOIN (SELECT asset_id, MAX(id) AS max_id
                                 FROM download_record
                                 WHERE user_id = #{userId}
                                 GROUP BY asset_id) t ON t.max_id = d.id
            ORDER BY d.created_at DESC, d.id DESC
            LIMIT #{limit}
            """)
    List<DownloadRecord> selectRecentByUser(@Param("userId") Long userId, @Param("limit") int limit);
}
