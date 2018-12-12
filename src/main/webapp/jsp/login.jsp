<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>登1录</title>
    <link rel="stylesheet" href="css/captcha.css">
    <script src="js/jquery.1.12.4.min.js"></script>
    <script src="js/drag.js"></script>
    <style>
        body {
            width: 100%;
        }

        .form {
            width: 400px;
            margin-left: 30%;
            margin-top: 10%;
        }

        .form-item {
            text-align: left;
            padding: 10px;
        }

        .form-item label {
            float: left;
            width: 60px !important;
            text-align: right;
            height: 30px;
            padding-top: 5px;
        }

        .form-item input {
            margin-left: 20px;
            width: 270px;
            height: 30px;
            line-height: 30px;
        }

        .btm {
            width: 120px !important;
            margin-left: 120px !important;
            background-color: #7ac23c;
            border: none;
            cursor: pointer;
        }
    </style>
</head>
<body>
<form class="form" action="login">
    <input id="yzm" name="yzm" type="hidden">
    <input id="passcheck" name="passcheck" type="hidden">
    <div class="form-item">
        <label>用户名</label>
        <input name="username" type="text"/>
    </div>
    <div class="form-item">
        <label>密码</label>
        <input name="password" type="password"/>
    </div>
    <div class="form-item">
        <input name="">
        <div class="yzm">
            <!--展示原图-->
            <div id="yzm_image_source" class="yzm_image_source"></div>
            <!--展示凹图-->
            <div id="yzm_image_cut_big" class="yzm_image_cut_big"></div>
            <!--加载中..-->
            <div id="yzm_image_cut_loading" class="yzm_image_cut_loading"></div>
            <!--拼图-->
            <div id="yzm_image_cut_small" class="yzm_image_cut_small"></div>
            <div style="top: 37px; left: 0px;display: none;"
                 id="xy_img"></div>
            <img id="refreshyzm" src="img/refresh.png"
                 style="position:relative;top: -60px;/* top:120px; */left: 280px;width: 20px;height: 20px;cursor: pointer;"
                 onclick="initYzm()">
        </div>
        <div id="drag" style="width: 260px;"></div>
    </div>

    <div class="form-item">
        <input type="button" class="btm" value="登  录" onclick="login()">
    </div>
</form>

<script>
    $(function () {
        //初始化图形验证码
        initYzm();
        //注册验证码拖动事件
        $('#drag').drag(null, null, initYzm);
    })

    function initYzm() {
        //加载中
        $("#xy_img").css("display", "none");
        $("#yzm_image_source").css("display", "none");
        $(".yzm_image_cut_big").css("display", "none");
        $(".yzm_image_cut_loading").show();
        $.ajax({
            type: "POST",
            async: true,
            url: "captcha/captchaImage",
            dataType: 'json',
            success: function (result) {
                if (result) {
                    //设置大图，小图，及其位置
                    $(".yzm_image_source").css("background-image", "url(captcha/image/" + result.sourceImgName + ")");
                    $(".yzm_image_cut_big").css("background-image", "url(captcha/image/" + result.bigImgName + ")");
                    $("#xy_img").css("background-image", "url(captcha/image/" + result.smallImgName + ")");
                    $("#xy_img").css("top", Number(result.location_y) + "px");
                    $(".yzm_image_cut_loading").css("display", "none");
                    $(".yzm_image_source").show();
                    $(".yzm_image_cut_big").css("display", "none");
                } else {
                    $.ligerDialog.error('获取图形验证码失败！');
                }
            },
            error: function (errormsg) {
                $.ligerDialog.error("获取图形验证码失败！");
            }
        });
    }

    function login() {
//        if($("#")})
    }
</script>
</body>
</html>
