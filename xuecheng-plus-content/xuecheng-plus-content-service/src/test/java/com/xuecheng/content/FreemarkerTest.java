package com.xuecheng.content;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CoursePublishService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @description 测试freemarker页面静态化方法 了解即可 如果以后需要，将其做成工具类
 */
@SpringBootTest
public class FreemarkerTest {

    @Autowired
    CoursePublishService coursePublishService;

    //测试页面静态化
    @Test
    public void testGenerateHtmlByTemplate() throws IOException, TemplateException {

        //配置freemarker    指定freemarker版本
        Configuration configuration = new Configuration(Configuration.getVersion());

        //加载模板
        //选指定模板路径,classpath下templates下

        //得到classpath路径
        String classpath = this.getClass().getResource("/").getPath();

        //要从哪个目录来加载模版   指定目录
        configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/"));

        //设置字符编码
        configuration.setDefaultEncoding("utf-8");

        //指定模板文件名称 得到模版 要写全
        Template template = configuration.getTemplate("course_template1.ftl");

        //准备数据 拿到课程信息 不要把这个直接扔到processTemplateIntoString里面，会有问题(虽然不会报错)
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(2L);

        Map<String, Object> map = new HashMap<>();
        //模版都是以“model”打头
        map.put("model", coursePreviewInfo);

        //静态化
        //参数1：模板，参数2：数据模型  该方法作用：生成一个页面，转成字符串
        String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
        System.out.println(content);

        //使用流，将字符串变成文件
        //将静态化内容输出到文件中

        //输入流
        InputStream inputStream = IOUtils.toInputStream(content);

        //输出流
        FileOutputStream outputStream = new FileOutputStream("D:\\upload\\test.html");

        //流拷贝
        IOUtils.copy(inputStream, outputStream);

    }

}