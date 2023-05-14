package com.xuecheng.content;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest//单元测试需要加的注解
public class CourseCategoryMapperTests {

    @Autowired
    CourseCategoryMapper courseCategoryMapper;//默认接口不能注入 设置 警告去掉

    @Test
    public void testCourseCategoryMapper(){
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes("1");
        System.out.println(courseCategoryTreeDtos);
    }
}
