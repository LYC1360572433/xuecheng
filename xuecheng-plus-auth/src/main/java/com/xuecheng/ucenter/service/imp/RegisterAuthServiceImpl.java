package com.xuecheng.ucenter.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.FindPasswordAuthService;
import com.xuecheng.ucenter.service.RegisterAuthService;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.keystore.BC;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service("register_authservice")
public class RegisterAuthServiceImpl implements RegisterAuthService {

    @Autowired
    CheckCodeClient checkCodeClient;

    @Autowired
    XcUserMapper xcUserMapper;

    @Autowired
    XcUserRoleMapper xcUserRoleMapper;

    @Override
    public RestResponse registerAuth(AuthParamsDto authParamsDto) {
        //用户输入的验证码
        String checkcode = authParamsDto.getCheckcode();
        //正确的验证码
        String checkcodekey = authParamsDto.getCheckcodekey();
        //判断验证码是否正确
        Boolean verify = checkCodeClient.verify(checkcodekey, checkcode);
        if (!verify) {
            return RestResponse.validfail("验证码有误");
        }
        //判断两次密码是否一致
        //获取第一次输入的密码
        String password = authParamsDto.getPassword();
        //获取第二次输入的确认密码
        String confirmpwd = authParamsDto.getConfirmpwd();
        if (!confirmpwd.equals(password)) {
            return RestResponse.validfail("两次密码输入不一致");
        }
        //根据手机号和邮箱查询用户(所以默认一个手机号只能有一个用户)
        //获取手机号或者邮箱
        String cellphone = authParamsDto.getCellphone();
        String email = authParamsDto.getEmail();
        LambdaQueryWrapper<XcUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcUser::getCellphone, cellphone);
        queryWrapper.eq(XcUser::getEmail, email);
        XcUser xcUser = xcUserMapper.selectOne(queryWrapper);
        if (xcUser != null) {
            return RestResponse.validfail("该用户已存在");
        }
        //向用户表添加数据
        xcUser = addUser(authParamsDto, confirmpwd);
        //向用户角色关系表添加数据
        addUserRole(xcUser);
        return new RestResponse(200,"注册成功");
    }

    /**
     * 向用户角色关系表添加数据
     * @param xcUser 用户
     */
    private void addUserRole(XcUser xcUser) {
        //向用户角色关系表添加数据
        XcUserRole xcUserRole = new XcUserRole();
        BeanUtils.copyProperties(xcUser, xcUserRole);
        xcUserRole.setRoleId("17");//角色id 学生用户 为17
        int insert = xcUserRoleMapper.insert(xcUserRole);
        if (insert < 0) {
            XueChengPlusException.cast("新增用户角色关系失败");
        }
    }

    /**
     * 向用户表添加数据
     * @param authParamsDto
     * @param confirmpwd
     */
    private XcUser addUser(AuthParamsDto authParamsDto, String confirmpwd) {
        //如果没找到用户，则添加数据
        XcUser xcUser = new XcUser();
        BeanUtils.copyProperties(authParamsDto, xcUser);
        xcUser.setName("学生用户");
        xcUser.setUtype("101001");//不知道是什么，数据字典里面没查到
        xcUser.setStatus("1");//用户状态
//        xcUser1.setId("1");
        xcUser.setCreateTime(LocalDateTime.now());
        //将密码转为BCrypt形式 存入数据库
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        String encode = bCryptPasswordEncoder.encode(confirmpwd);
        xcUser.setPassword(encode);
        int i = xcUserMapper.insert(xcUser);
        if (i < 0) {
            XueChengPlusException.cast("新增用户失败");
        }
        return xcUser;
    }
}
