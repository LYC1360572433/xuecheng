package com.xuecheng.base.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

/**
 * 分页查询分页参数   通用的类
 */
@Data//get set 方法
@ToString//为了数据输出方便，打印日志
public class PageParams {

    //因为mybatis-plus的接口分页的参数类型就Long
    //当前页码
    @ApiModelProperty("页码")//swagger接口文档 提供的api 参数注释
    private Long pageNo = 1L;
    //每页显示记录数
    @ApiModelProperty("每页记录数")
    private Long pageSize = 30L;

    public PageParams() {
    }

    public PageParams(Long pageNo, Long pageSize) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }
}
