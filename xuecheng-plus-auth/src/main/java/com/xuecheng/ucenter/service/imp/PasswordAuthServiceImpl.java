package com.xuecheng.ucenter.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @description 账号密码认证方式   一个验证方式一个实现类 都去实现AuthService接口
 */
//一个接口下有多个实现类
//beanName:password_authservice
@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {

    @Autowired
    XcUserMapper xcUserMapper;

    //注入密码编码对象
    @Autowired
    PasswordEncoder passwordEncoder;


    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {

        //账号
        String username = authParamsDto.getUsername();
        //todo:校验验证码

        //账号是否存在
        //根据username账号查询数据库
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        //查询到用户不存在，抛出异常
        if(user==null){
            //返回空表示用户不存在
            throw new RuntimeException("账号不存在");
        }

        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(user,xcUserExt);

        //校验密码
        //取出数据库存储的正确密码
        String passwordDb  =user.getPassword();
        //拿到用户输入的密码
        String passwordForm = authParamsDto.getPassword();
        //校验密码
        boolean matches = passwordEncoder.matches(passwordForm, passwordDb);
        if(!matches){
            //false 密码错误
            throw new RuntimeException("账号或密码错误");
        }
        //返回用户扩展类型
        return xcUserExt;
    }
}
