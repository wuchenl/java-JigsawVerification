(function ($) {
    $.fn.drag = function (options, sucfun, errfun) {
        var isvalid = false;
        var x, drag = this, isMove = false, defaults = {};
        var options = $.extend(defaults, options);
        //添加背景，文字，滑块
        var html = '<div class="drag_bg"></div>' +
            '<div class="drag_text" onselectstart="return false;" unselectable="on">拖动图片验证登陆</div>' +
            '<div class="handler handler_bg"></div>';
        this.append(html);

        var handler = drag.find('.handler');
        var drag_bg = drag.find('.drag_bg');
        var text = drag.find('.drag_text');
        var maxWidth = drag.width() - handler.width();  //能滑动的最大间距

        //鼠标按下时候的x轴的位置
        handler.mousedown(function (e) {
            if (isvalid) {
                return false;
            }
            $gt_cut.css("display", "none");
            $gt_cut_hidden.show();
            $xy_img.show();
            isMove = true;
            x = e.pageX - parseInt(handler.css('left'), 10);
        });
        var $xy_img = $("#xy_img");
        var $gt_cut = $("#yzm_image_source");
        var $gt_cut_hidden = $("#yzm_image_cut_big");
        //鼠标指针在上下文移动时，移动距离大于0小于最大间距，滑块x轴位置等于鼠标移动距离
        $("#drag").mousemove(function (e) {
            if (isvalid) {
                return false;
            }
            var _x = e.pageX - x;
            if (isMove) {
                if (_x > 0 && _x <= maxWidth) {
                    $xy_img.css({'left': _x});
                    handler.css({'left': _x});
                    drag_bg.css({'width': _x});
                } else if (_x > maxWidth) {  //鼠标指针移动距离达到最大时清空事件
                    //  dragOk();
                }
            }
        }).mouseup(function (e) {
            if (isvalid) {
                return false;
            }
            isMove = false;
            var _x = e.pageX - x;
            console.log(_x);
            $.ajax({
                type: "POST",
                url: "captcha/checkCaptcha",
                dataType: "JSON",
                async: false,
                data: {point: _x},
                success: function (result) {
                    console.log(result);
                    if (result.code == 200) {
                        dragOk(_x);
                    } else {
                        dragErr();
                    }
                },
                error: function (err) {
                    console.log(err);
                    $.ligerDialog.error('服务异常！');
                }
            });
        });

        var errcount = 0;

        function dragErr() {
            errcount = errcount + 1;
            isvalid = false;
            $(".drag_bg").css("background-color", "#C22A0E");
            text.text("验证失败");
            handler.removeClass('handler_bg').addClass('handler_err_bg');
            setTimeout(function () {
                //还原
                text.text("拖动图片验证登陆");
                $(".drag_bg").css("background-color", "#7ac23c");
                handler.removeClass("handler_err_bg").addClass("handler_bg");
                // $xy_img.css("display", "none");
                $xy_img.css({'left': 0});
                handler.css({'left': 0});
                drag_bg.css({'width': 0});
                //验证失败, 拖动错误数大于3次，那么就重置
                if (errcount >= 3) {
                    errfun();
                    errcount = 0;
                    $xy_img.css("display", "none");
                    $gt_cut.show();
                    $gt_cut_hidden.css("display", "none");
                }
            }, 1000);
        }

        //清空事件
        function dragOk(_x) {
            isvalid = true;
            handler.removeClass('handler_bg').addClass('handler_ok_bg');
            text.text('验证通过');
            drag.css({'color': '#fff'});
            handler.unbind('mousedown');
            $(document).unbind('mousemove');
            $(document).unbind('mouseup');
            //验证通过，隐藏刷新
            $("#refreshyzm").css("display", "none");
            $("#passcheck").val("1");
            $("#yzm").val(_x);

            //显示原始图片,并做光线闪过
            $xy_img.css("display", "none");
            $gt_cut_hidden.css("display", "none");
            $gt_cut.show();
            $gt_cut.addClass("run");
        }
    };
})(jQuery);


