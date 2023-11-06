package com.xuecheng.content.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 
 * @TableName exam
 */
@TableName(value ="exam")
@Data
public class Exam implements Serializable {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 
     */
    @TableField(value = "question")
    private String question;

    /**
     * 
     */
    @TableField(value = "optionA")
    private String optionA;

    /**
     * 
     */
    @TableField(value = "optionB")
    private String optionB;

    /**
     * 
     */
    @TableField(value = "optionC")
    private String optionC;

    /**
     * 
     */
    @TableField(value = "optionD")
    private String optionD;

    /**
     * 
     */
    @TableField(value = "rightOption")
    private String rightOption;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}