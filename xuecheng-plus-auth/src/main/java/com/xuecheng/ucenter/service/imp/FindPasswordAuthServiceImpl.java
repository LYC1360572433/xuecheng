package com.xuecheng.ucenter.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.FindPasswordAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service("findPassword_authservice")
public class FindPasswordAuthServiceImpl implements FindPasswordAuthService {

    @Autowired
    CheckCodeClient checkCodeClient;

    @Autowired
    XcUserMapper xcUserMapper;

    @Override
    public void findPasswordAuth(AuthParamsDto authParamsDto) {
        //用户输入的验证码
        String checkcode = authParamsDto.getCheckcode();
        //正确的验证码
        String checkcodekey = authParamsDto.getCheckcodekey();
        //判断验证码是否正确
        Boolean verify = checkCodeClient.verify(checkcodekey, checkcode);
        if (!verify){
        XueChengPlusException.cast("验证码有误");
        }
        //判断两次密码是否一致
        //获取第一次输入的密码
        String password = authParamsDto.getPassword();
        //获取第二次输入的确认密码
        String confirmpwd = authParamsDto.getConfirmpwd();
        if (!confirmpwd.equals(password)){
            XueChengPlusException.cast("两次密码输入不一致");
        }
        //根据手机号和邮箱查询用户
        //获取手机号或者邮箱
        String cellphone = authParamsDto.getCellphone();
        String email = authParamsDto.getEmail();
        LambdaQueryWrapper<XcUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcUser::getCellphone,cellphone);
        queryWrapper.eq(XcUser::getEmail,email);
        XcUser xcUser = xcUserMapper.selectOne(queryWrapper);
        if (xcUser == null){
            XueChengPlusException.cast("找不到该用户");
        }
        //如果找到用户更新为新密码
        //将密码转为BCrypt形式 存入数据库
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        String encode = bCryptPasswordEncoder.encode(confirmpwd);
        xcUser.setPassword(encode);
        int i = xcUserMapper.updateById(xcUser);
        if (i < 0){
            XueChengPlusException.cast("更新新密码失败");
        }
        XueChengPlusException.cast("200","找回密码成功");
    }
}
