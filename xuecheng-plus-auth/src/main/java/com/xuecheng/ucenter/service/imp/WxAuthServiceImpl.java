package com.xuecheng.ucenter.service.imp;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.AuthService;
import com.xuecheng.ucenter.service.WxAuthService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 微信扫码认证
 */
@Slf4j
@Service("wx_authservice")
public class WxAuthServiceImpl implements AuthService, WxAuthService {

    @Autowired
    XcUserRoleMapper xcUserRoleMapper;
    @Autowired
    XcUserMapper xcUserMapper;
    //和第三方对接，又不是使用网关，就用这个
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    WxAuthServiceImpl currentPorxy;

    //在微信开放平台，只要审核通过，就会给你appid和app密钥
    //我们将其配在nacos上边
    @Value("${weixin.appid}")
    String appid;
    @Value("${weixin.secret}")
    String secret;

    public XcUser wxAuth(String code) {

        //申请令牌
        //收到code调用微信接口申请access_token
        Map<String, String> access_token_map = getAccess_token(code);
        if (access_token_map == null) {
            return null;
        }
        System.out.println(access_token_map);
        //从令牌里面获取openid
        String openid = access_token_map.get("openid");
        //从令牌里面获取access_token
        String access_token = access_token_map.get("access_token");
        //携带令牌access_token查询用户信息
        Map<String,String> userinfo = getUserinfo(access_token, openid);
        if (userinfo == null) {
            return null;
        }
        //保存用户信息到数据库
        XcUser xcUser = currentPorxy.addWxUser(userinfo);

        return xcUser;
    }

    @Transactional
    public XcUser addWxUser(Map<String,String> userInfo_map){
        //用户表当中，主键不是自增的，而是unionid
        String unionid = userInfo_map.get("unionid").toString();
        //根据unionid查询数据库
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getWxUnionid, unionid));
        //如果用户已经存在，就直接返回，不新增
        if(xcUser!=null){
            return xcUser;
        }
        //向数据库新增记录 主要是新增不为空的字段
        String userId = UUID.randomUUID().toString();//uuid生成主键
        xcUser = new XcUser();
        xcUser.setId(userId);
        xcUser.setWxUnionid(unionid);//账号用唯一的unionid
        //记录从微信得到的昵称
        xcUser.setNickname(userInfo_map.get("nickname"));
        xcUser.setUserpic(userInfo_map.get("headimgurl"));
        xcUser.setName(userInfo_map.get("nickname"));
        xcUser.setUsername(unionid);
        xcUser.setPassword(unionid);
        xcUser.setUtype("101001");//学生类型
        xcUser.setStatus("1");//用户状态
        xcUser.setCreateTime(LocalDateTime.now());
        xcUserMapper.insert(xcUser);
        //向用户角色关系表新增记录
        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setId(UUID.randomUUID().toString());
        xcUserRole.setUserId(userId);//对应用户表的主键
        xcUserRole.setRoleId("17");//学生角色
        xcUserRole.setCreateTime(LocalDateTime.now());
        xcUserRoleMapper.insert(xcUserRole);
        //返回用户信息
        return xcUser;
    }

    /**
     * 携带授权码申请访问令牌,响应示例
     * https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code
     * 响应的内容如下：
     * {
     * "access_token":"ACCESS_TOKEN",
     * "expires_in":7200,
     * "refresh_token":"REFRESH_TOKEN",
     * "openid":"OPENID",
     * "scope":"SCOPE",
     * "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
     * }
     *
     * @param code 授权码
     * @return
     */
    private Map<String, String> getAccess_token(String code) {

        //接口地址模版 参数 用占位符%s占位
        String wxUrl_template = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
        //请求微信地址 最终的请求路径 填参数
        String wxUrl = String.format(wxUrl_template, appid, secret, code);

        log.info("调用微信接口申请access_token, url:{}", wxUrl);

        //远程调用url                                             url       请求方式           请求内容     响应内容类型：字符串
        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.POST, null, String.class);

        //获取响应的结果 body就是返回值，上面那些json数据
        String result = exchange.getBody();
         log.info("调用微信接口申请access_token: 返回值:{}", result);
        //将result转成map
        Map<String, String> resultMap = JSON.parseObject(result, Map.class);

        return resultMap;
    }

    /**
     * 携带令牌查询用户信息
     *
     * https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s
     *
     * {
     * "openid":"OPENID",
     * "nickname":"NICKNAME",
     * "sex":1,
     * "province":"PROVINCE",
     * "city":"CITY",
     * "country":"COUNTRY",
     * "headimgurl": "https://thirdwx.qlogo.cn/mmopen/g3MonUZtNHkdmzicIlibx6iaFqAc56vxLSUfpb6n5WKSYVY0ChQKkiaJSgQ1dZuTOgvLLrhJbERQQ4eMsv84eavHiaiceqxibJxCfHe/0",
     * "privilege":[
     * "PRIVILEGE1",
     * "PRIVILEGE2"
     * ],
     * "unionid": " o6_bmasdasdsad6_2sgVt7hMZOPfL"
     *
     *  }
     * @param access_token
     * @param openid
     * @return
     */
    private Map<String, String> getUserinfo(String access_token, String openid) {
        //地址模版
        String wxUrl_template = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        //请求微信地址
        String wxUrl = String.format(wxUrl_template, access_token, openid);

        log.info("调用微信接口申请access_token, url:{}", wxUrl);

        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.POST, null, String.class);

        //防止乱码进行转码
        String result = new String(exchange.getBody().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        log.info("调用微信接口申请access_token: 返回值:{}", result);
        Map<String, String> resultMap = JSON.parseObject(result, Map.class);

        return resultMap;
    }

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        //得到账号
        String username = authParamsDto.getUsername();
        //查询数据库
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername,username));
        if (xcUser == null){
            throw new RuntimeException("用户不存在");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser,xcUserExt);

        return xcUserExt;
    }
}