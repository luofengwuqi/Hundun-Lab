package com.hundunlab.site.module.user.pojo.VO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class PageVO<T> implements Serializable {
    private Long total;
//    private Integer pageNum;
//    private Integer pageSize;
    private List<T> list;

}
