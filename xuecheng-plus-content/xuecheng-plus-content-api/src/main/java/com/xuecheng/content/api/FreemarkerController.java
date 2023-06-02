package com.xuecheng.content.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Freemarker入门程序
 */

@Controller//返回页面,不用rest，因为rest返回json数据
public class FreemarkerController {

    @GetMapping("/testfreemarker")
    public ModelAndView test(){
        ModelAndView modelAndView = new ModelAndView();
        //设置模型数据（指定模型）
        modelAndView.addObject("name","小明");
        //设置模板名称（指定模板）
        modelAndView.setViewName("test");//因为nacos配置里面有指明后缀，根据视图名称加.ftl找到模板
        return modelAndView;
    }
}
