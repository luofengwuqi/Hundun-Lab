package com.hundunlab.site.module.user.pojo.Entity;

//import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperateLog implements Serializable {
        private Long id;
        private Long operateUserId; // 操作人ID
        private String operateUserName;
        private String operateType; // 新增/修改/删除/查询
        private String moduleName; // 操作模块
        private String operateFunctionName; //操作的方法名字
//        private String requestParams; // 请求参数JSON
//        private String responseResult; // 响应结果
        private String ipAddress; // 操作IP
        private LocalDateTime operateAt; // 操作时间
        private String operateTime;//操作时长
        private Integer operateStatus; // 1成功 0失败
        //新增字段:操作的sql
        private String executedSql;
}
