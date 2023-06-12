package com.xuecheng.ucenter.service.imp;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcMenuMapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcMenu;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import springfox.documentation.spring.web.json.Json;

import java.util.ArrayList;
import java.util.List;

/**
 * @description 自定义UserDetailsService用来对接Spring Security，查询用户信息
 */
@Component
@Slf4j
@Service
//spring security提供的UserDetailsService
public class UserServiceImpl implements UserDetailsService {

    @Autowired
    XcUserMapper xcUserMapper;//注入，查用户表

    @Autowired//注入spring容器，从容器里根据beanname去找对应的bean
    ApplicationContext applicationContext;

    @Autowired
    XcMenuMapper xcMenuMapper;

    /**
     * @param s  AuthParamsDto类型的json数据
     * @return org.springframework.security.core.userdetails.UserDetails
     * @description 根据账号查询用户信息  查询用户信息组成用户身份信息
     */

    //传入的请求认证的参数就是AuthParamsDto模型类  参数为json数据
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {

//        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, s));
//        if(user==null){
//            //返回空表示用户不存在
//            return null;
//        }
//        //取出数据库存储的正确密码
//        String password  =user.getPassword();
//        //用户权限,如果不加报Cannot pass a null GrantedAuthority collection
//        String[] authorities= {"test"};
//        //创建UserDetails对象,权限信息待实现授权功能时再向UserDetail中加入
//        UserDetails userDetails = User.withUsername(user.getUsername()).password(password).authorities(authorities).build();
//
//        return userDetails;

        //将传入的json转成AuthParamsDto对象
        AuthParamsDto authParamsDto = null;
        try {
            //万一转失败了，捕捉个异常
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        } catch (Exception e) {
            throw new RuntimeException("请求认证参数不符合要求");
        }

        //认证方法
        //认证类型，有password，wx。。。
        String authType = authParamsDto.getAuthType();

        //根据认证类型，拼一个bean
        String beanName = authType + "_authservice";
        //根据认证类型从spring容器取出指定的bean
        AuthService authService = applicationContext.getBean(beanName, AuthService.class);
        //调用统一execute方法完成认证
        XcUserExt xcUserExt = authService.execute(authParamsDto);

        UserDetails userPrincipal = getUserPrincipal(xcUserExt);
        return userPrincipal;
    }

    /**
     * @param user 用户id，主键
     * @return com.xuecheng.ucenter.model.po.XcUser 用户信息
     * @description 查询用户信息
     */
    public UserDetails getUserPrincipal(XcUserExt user) {

        String[] authorities = {"p1"};

        String password = user.getPassword();

        //用户权限,如果不加报Cannot pass a null GrantedAuthority collection
        //根据用户id查询用户的权限
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(user.getId());

        if (xcMenus.size()>0){
            //一个用户可以有多个权限
            List<String> permissions = new ArrayList<>();
            xcMenus.forEach(m->{
                //拿到了用户拥有的权限标识符
                permissions.add(m.getCode());
            });
            //将permissions转成数组
            authorities = permissions.toArray(new String[0]);
        }
        //为了安全在令牌中不放密码
        user.setPassword(null);
        //将user对象转json
        String userString = JSON.toJSONString(user);
        //创建UserDetails对象
        UserDetails userDetails = User.withUsername(userString).password(password).authorities(authorities).build();
        return userDetails;
    }


}