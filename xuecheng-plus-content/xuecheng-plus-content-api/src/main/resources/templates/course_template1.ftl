<!DOCTYPE html>
<html lang="zh-CN">

<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <!-- 上述3个meta标签*必须*放在最前面，任何其他内容都*必须*跟随其后！ -->
    <meta name="description" content="">
    <meta name="author" content="">
    <link rel="icon" href="/static/img/asset-favicon.ico">
    <title>学成在线-${model.courseBase.name!''}</title>
<#--    http://www.51xuecheng.cn/plugins/normalize-css/normalize.css-->
<#--    <link rel="stylesheet" href="/static/plugins/normalize-css/normalize.css" />-->
    <link rel="stylesheet" href="http://www.51xuecheng.cn/plugins/normalize-css/normalize.css" />
<#--    <link rel="stylesheet" href="/static/plugins/bootstrap/dist/css/bootstrap.css" />-->
    <link rel="stylesheet" href="http://www.51xuecheng.cn/plugins/bootstrap/dist/css/bootstrap.css" />
<#--    <link rel="stylesheet" href="/static/css/page-learing-article.css" />-->
    <link rel="stylesheet" href="http://www.51xuecheng.cn/css/page-learing-article.css" />
</head>

<body data-spy="scroll" data-target="#articleNavbar" data-offset="150">
<!-- 页面头部 -->
<!--#include virtual="/include/header.html"-->
<!--页面头部结束sss-->
<div id="learningArea">
<div class="article-banner">
    <div class="banner-bg"></div>
    <div class="banner-info">
        <div class="banner-left">
            <p>${model.courseBase.mtName!''}<span>\ ${model.courseBase.stName!''}</span></p>
            <p class="tit">${model.courseBase.name!''}</p>
            <p class="pic">
                <#if model.courseBase.charge=='201000'>
                    <span class="new-pic">免费</span>
                <#else>
                    <span class="new-pic">特惠价格￥${model.courseBase.price!''}</span>
                    <span class="old-pic">原价￥${model.courseBase.originalPrice!''}</span>
                </#if>
            </p>
            <p class="info">
                <a href="#" @click.prevent="startLearning()">马上学习</a>
                <span><em>难度等级</em>
                <#if model.courseBase.grade=='204001'>
                    初级
                 <#elseif model.courseBase.grade=='204002'>
                    中级
                <#elseif model.courseBase.grade=='204003'>
                    高级
                </#if>
                </span>
<#--                <span><em>课程时长</em>2小时27分</span>-->
<#--                <span><em>评分</em>4.7分</span>-->
                <span><em>授课模式</em>
                 <#if model.courseBase.teachmode=='200002'>
                     录播
                 <#elseif model.courseBase.teachmode=='200003'>
                     直播
                 </#if>
                </span>
            </p>
        </div>
        <div class="banner-rit">
            <p>
                <a href="http://www.51xuecheng.cn/course/preview/learning.html?id=${model.courseBase.id!''}" target="_blank">
                    <#if model.courseBase.pic??>
                        <img src="http://file.51xuecheng.cn${model.courseBase.pic!''}" alt="" width="270" height="156">
                    <#else>
                        <img src="http://www.51xuecheng.cn/static/img/widget-video.png" alt="" width="270" height="156">
                    </#if>

                </a>
            </p>
<#--            <p class="vid-act"><span> <i class="i-heart"></i>收藏 23 </span> <span>分享 <i class="i-weixin"></i><i class="i-qq"></i></span></p>-->
        </div>
    </div>
</div>
<div class="article-cont">
    <div class="tit-list">
        <a href="javascript:;" id="articleClass" class="active">课程介绍</a>
    </div>
    <div class="article-box">
        <div class="articleClass" style="display: block">
            <!--<div class="rit-title">评价</div>-->
            <div class="article-cont">
                <div class="article-left-box">
                    <div class="content">
                        <div class="content-com suit">
                            <div class="title"><span>适用人群</span></div>
                            <div class="cont cktop">
                                <div >
                                    <p>${model.courseBase.users!''}</p>
                                </div>
                            </div>
                        </div>
                        <div class="content-com course">
                            <div class="title"><span>课程制作</span></div>
                            <div class="cont">
                                <div class="img-box"><img src="http://file.51xuecheng.cn${model.courseTeacher.photograph!''}" alt="" width="100" height="156"></div>
                                <div class="info-box">
                                    <p class="info">${model.courseTeacher.position!''}</p>
                                </div>
                            </div>

                        </div>
                        <div class="content-com about">
                            <div class="title"><span>课程介绍</span></div>
                            <div class="cont cktop">
                                <div >
                                    <p>${model.courseBase.description!""}</p>
                                </div>
                            </div>
                        </div>
                        <div class="content-com prob">
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="articleItem" style="display: none">
            <div class="article-cont-catalog">
                <div class="article-left-box">
                    <div class="content">
                        <#list model.teachplans as firstNode>
                            <div class="item">
                                <div class="title act"><i class="i-chevron-top"></i>${firstNode.pname!''}</div>
                                <div class="drop-down" style="height: 260px;">
                                    <ul class="list-box">
                                        <#list firstNode.teachPlanTreeNodes as secondNode>
                                            <li><a href="http://www.51xuecheng.cn/course/preview/learning.html?id=${model.courseBase.id!''}&chapter=${secondNode.teachplanMedia.teachplanId!''}" target="_blank">${secondNode.pname!''}</a></li>
                                        </#list>
                                    </ul>
                                </div>
                            </div>
                        </#list>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
    <div class="popup-course">
        <div class="mask"></div>
        <!--欢迎访问课程弹窗- start -->
        <div class="popup-course-box">
            <div class="title">${model.courseBase.name!''} <span class="close-popup-course-box">×</span></div>
            <div class="content">
                <p>欢迎学习本课程，本课程免费您可以立即学习，也可加入我的课程表享受更优质的服务。</p>
                <p><a href="#" @click.prevent="addCourseTable()">加入我的课程表</a>  <a href="#" @click.prevent="startLearngin()">立即学习</a></p>
            </div>
        </div>
    </div>
    <div class="popup-box">
        <div class="mask"></div>
        <!--支付弹窗- start -->
        <div class="popup-pay-box">
            <div class="title">${model.courseBase.name!''} <span class="close-popup-pay-box">×</span></div>
            <div class="content">
                <img :src="qrcode" width="200" height="200" alt="请点击支付宝支付按钮，并完成扫码支付。"/>

                <div class="info">
                    <p class="info-tit">${model.courseBase.name!''}<span>课程有效期:${model.courseBase.validDays!''}天</span></p>
                    <p class="info-pic">课程价格 : <span>￥${model.courseBase.originalPrice!''}元</span></p>
                    <p class="info-new-pic">优惠价格 : <span>￥${model.courseBase.price!''}元</span></p>
                </div>
            </div>
            <div class="fact-pic">实际支付: <span>￥${model.courseBase.price!''}元</span></div>
            <div class="go-pay"><a href="#" @click.prevent="wxPay()">微信支付</a><a href="#" @click.prevent="aliPay()">支付宝支付</a><a href="#" @click.prevent="querypayresult()">支付完成</a><a href="#" @click.prevent="startLearngin()">试学</a></div>
        </div>
        <!--支付弹窗- end -->
        <div class="popup-comment-box">

        </div>
    </div>
    <!-- 页面底部 -->
    <!--底部版权-->
    <!--#include virtual="/include/footer.html"-->
    <!--底部版权-->
</div>
<script>var courseId = "${model.courseBase.id!''}";var courseCharge = "${model.courseBase.charge!''}"</script>
<!--#include virtual="/include/course_detail_dynamic.html"-->
</body>
