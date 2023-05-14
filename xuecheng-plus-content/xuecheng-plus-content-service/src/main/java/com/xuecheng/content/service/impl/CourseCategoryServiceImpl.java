package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CourseCategoryServiceImpl implements CourseCategoryService {

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        //调用mapper递归查询出分类信息
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);

        //找到每个节点的子节点，最终封装成List<CourseCategoryTreeDto>
        //先将list转成map，key就是结点的id，value就是CourseCategoryTreeDto对象，目的就是为了方便从map获取节点
        //先变 流 再转为map map里面的key对应元素的id  value对应其它字段 (key1,key2)->key2):当key重复时，取后者的key
        Map<String, CourseCategoryTreeDto> mapTemp = courseCategoryTreeDtos.stream()
                .filter(item->!id.equals(item.getId()))//过滤掉根节点的元素
                .collect(Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key2));

        //定义一个list作为最终返回的list
        List<CourseCategoryTreeDto> courseCategoryList = new ArrayList<>();

        //从头遍历List<CourseCategoryTreeDto>，一边遍历一边找子节点，把子节点放在父节点的childrenTreeNodes属性中
        courseCategoryTreeDtos.stream().filter(item->!id.equals(item.getId()))
                .forEach(item->{
                //向list写入元素
                    if (item.getParentid().equals(id)){
                        courseCategoryList.add(item);
                    }
                    //找到节点的父节点
                    CourseCategoryTreeDto courseCategoryParent = mapTemp.get(item.getParentid());
                    if (courseCategoryParent != null){
                        if (courseCategoryParent.getChildrenTreeNodes() == null){
                            //如果该父节点的属性childrenTreeNodes为空 要new一个集合，因为我们要向该集合放入它的子节点
                            courseCategoryParent.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                        }
                        //找到每个字节点放在父节点的childrenTreeNodes属性中
                        courseCategoryParent.getChildrenTreeNodes().add(item);
                    }
        });
        return courseCategoryList;
    }
}
