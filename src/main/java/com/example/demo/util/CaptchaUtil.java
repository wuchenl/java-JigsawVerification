package com.example.demo.util;

import com.example.demo.support.CaptchaConfig;
import com.example.demo.support.CaptchaConst;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * @author wuchenl
 * @date 2018/12/8.
 */
public class CaptchaUtil {
    private static Logger log = LoggerFactory.getLogger(CaptchaUtil.class);

    /**
     * 将我们配置的图片原始路径和数量配置注入进来使用
     */

    /**
     * 阴影宽度
     */
    private static final int SHADOW_WIDTH = 2;
    /**
     * 图片边缘亮色（黄色）宽度。
     */
    private static final int LIGHT_HEIGHT_WIDTH = 2;
    /**
     * 圆弧直径
     */
    private static final int ARC = 10;
    /**
     * 生成图片后缀
     */
    private static final String PIC_SUFFIX = ".png";
    /**
     * 生成图片后缀
     */
    private static final String PNG_SUFFIX = "png";

    private static final Color clrGlowInnerHi = new Color(253, 239, 175, 148);
    private static final Color clrGlowInnerLo = new Color(255, 209, 0);
    private static final Color clrGlowOuterHi = new Color(253, 239, 175, 124);
    private static final Color clrGlowOuterLo = new Color(255, 179, 0);


    //----------------临时全局变量区域----------------------

    /**
     * 小图的宽---剪裁的图的宽度
     */
    private int tailoringWidth = 50;
    /**
     * 小图的高---剪裁的图的高度
     */
    private int tailoringHeight = 50;
    /**
     * 随机X位置---位于原图的X位置
     */
    private int locationX = 0;
    /**
     * 随机Y位置----位于原图的Y位置
     */
    private int locationY = 0;

    public Map<String, String> createCaptchaImage(String host, int size, String sourcePath) throws IOException {
        Map<String, String> resultMap = Maps.newConcurrentMap();
        Random random = new Random();
        log.info("开始创建滑动验证码相关图片----请求地址为:{}", host);
        // 获取原始图片的完整路径，随机采用一张
        int sourceSize = random.nextInt(size);
        String sourceImageName = UtilString.join(sourcePath, sourceSize, PIC_SUFFIX);
        InputStream sourceImageInputStream = Resources.getResourceAsStream(sourceImageName);
        if (Objects.isNull(sourceImageInputStream)) {
            log.warn("读取原始图片异常:{}", sourceImageName);
            return resultMap;
        }

        // 读取原始图片大小。并判断是否符合预设值
        BufferedImage bufferedImage = ImageIO.read(sourceImageInputStream);
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        if (width <= tailoringWidth * 2 || height <= tailoringHeight) {
            log.warn("原始图片不符合默认尺寸:{}*{}", width, height);
            return resultMap;
        }
        // 这里是控制剪裁图片生成的区域。尽量位于中间。同时。图片大于100*50

        this.locationX = random.nextInt(width - tailoringWidth * 2) + tailoringWidth;
        this.locationY = random.nextInt(height - tailoringHeight);
        // 获取裁剪小图
        BufferedImage tailoringImageBuffer = tailoringImage(sourceImageName);

        //创建shape区域
        List<Shape> shapes = createSmallShape();
        if (shapes == null || shapes.size() <= 0) {
            log.error("生成剪裁小图随机形状异常！");
            return resultMap;
        }

        Shape area = shapes.get(0);
        Shape bigarea = shapes.get(1);
        //创建图层用于处理小图的阴影
        BufferedImage bfm1 = new BufferedImage(tailoringWidth, tailoringHeight, BufferedImage.TYPE_INT_ARGB);
        //创建图层用于处理大图的凹槽
        BufferedImage bfm2 = new BufferedImage(tailoringWidth, tailoringHeight, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < tailoringWidth; i++) {
            for (int j = 0; j < tailoringHeight; j++) {
                if (area.contains(i, j)) {
                    bfm1.setRGB(i, j, tailoringImageBuffer.getRGB(i, j));
                }
                if (bigarea.contains(i, j)) {
                    bfm2.setRGB(i, j, Color.black.getRGB());
                }
            }
        }
        //处理图片的边缘高亮及其阴影效果
        BufferedImage resultImgBuff = dealLightAndShadow(bfm1, area);
        //生成大小图随机名称
        String smallFileName = createSmallImg(resultImgBuff);
        //将灰色图当做水印印到原图上
        String bigImgName = createBigImg(bfm2, sourceImageName);
        if (smallFileName == null) {
            return null;
        }
        resultMap.put("smallImgName", smallFileName);
        resultMap.put("bigImgName", bigImgName);
        resultMap.put("location_y", String.valueOf(locationY));
        resultMap.put("sourceImgName", UtilString.join(sourceSize, PIC_SUFFIX));

        // 拼接放入redis的key
        host = UtilString.join(host, CaptchaConst.MIDDLE_LINE, CaptchaConst.CAPTCHA);

        InputStream sourceInput=Resources.getResourceAsStream(sourceImageName);
        String sourcePngBase64 = getBase64FromInputStream(sourceInput);
        CacheManagerHolder.getManager().getCache(CaptchaConst.CACHE_CAPTCHA_IMG).put(UtilString.join(sourceSize, PIC_SUFFIX), sourcePngBase64);

        //将x 轴位置作为验证码 放入到redis中，key为IP-captcha
        CacheManagerHolder.getManager().getCache(CaptchaConst.VERIFICATION_CODE).put(host, locationX);

        return resultMap;
    }

    private String createSmallImg(BufferedImage resultImgBuff) {
        String smallFileName = randomImgName("small_source_");

        // 图片流先转输入流
        InputStream inputStream = getInputStreamFromBufferedImage(resultImgBuff, PNG_SUFFIX);
        if (Objects.isNull(inputStream)) {
            log.warn("生成小图失败：转inputStream流失败！");
            return null;
        }
        // 然后转为base64编码
        String smallPngBase64 = getBase64FromInputStream(inputStream);
        if (UtilString.isEmpty(smallPngBase64)) {
            log.warn("生成小图失败：转base64编码失败！");
            return null;
        }
        // 最后放入cache
        log.info("即将存入Cache:{}", smallFileName);
        CacheManagerHolder.getManager().getCache(CaptchaConst.CACHE_CAPTCHA_IMG).put(smallFileName, smallPngBase64);
        return smallFileName;
    }


    /**
     * 创建大图，即带小图水印缺口的图片
     *
     * @param sourceImageBuffer 大图，拼图的buffer
     * @param sourceName        源文件
     * @return 大图的buffer
     * @throws IOException
     */
    private String createBigImg(BufferedImage sourceImageBuffer, String sourceName) throws IOException {
        //创建一个灰度化图层， 将生成的小图，覆盖到该图层，使其灰度化，用于作为一个水印图
        String bigImgName = randomImgName("big_source_");
        //如果大图不存在，那么就创建
//        File bigfile = new File(imgPath + File.separator + bigImgName);
//        if (!bigfile.exists()) {
        //将灰度化之后的图片，整合到原有图片上
        BufferedImage bigImg = addWatermark(sourceName, sourceImageBuffer, 0.6F);
        // 转is流
        InputStream inputStream = getInputStreamFromBufferedImage(bigImg, "png");
        if (Objects.isNull(inputStream)) {
            log.warn("生成大图失败：转inputStream流失败！");
            return null;
        }
        //转base64编码
        String bigPngBase64 = getBase64FromInputStream(inputStream);
        if (UtilString.isEmpty(bigPngBase64)) {
            log.warn("生成大图失败：转base64编码失败！");
            return null;
        }
        //存入redis
        CacheManagerHolder.getManager().getCache(CaptchaConst.CACHE_CAPTCHA_IMG).put(bigImgName, bigPngBase64);
//        }
        return bigImgName;
    }

    /**
     * 添加水印
     *
     * @param sourceName
     * @param smallImage
     * @param alpha
     * @return
     * @throws IOException
     */
    private BufferedImage addWatermark(String sourceName, BufferedImage smallImage, float alpha) throws IOException {
        InputStream inputStream = Resources.getResourceAsStream(sourceName);
        BufferedImage source = ImageIO.read(inputStream);
        Graphics2D graphics2D = source.createGraphics();
        graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
        graphics2D.drawImage(smallImage, locationX, locationY, null);
        graphics2D.dispose(); //释放
        return source;
    }

    /**
     * 生成大小图片文件名
     *
     * @param suf 文件名称
     * @return 根据位置生成后的
     */
    private String randomImgName(String suf) {
        //按照坐标位生成图片
        return suf + locationY + "_" + DigestUtils.md5Hex(String.valueOf(locationX)).substring(0, 16) + PIC_SUFFIX;
    }

    /**
     * 生成 点随机形状
     *
     * @return
     */
    private List<Shape> createSmallShape() {
        //处理小图，在4个方向上 随机找到2个方向添加凸出
        int face1 = RandomUtils.nextInt(3); //凸出1
        int face2; //凸出2
        //使凸出1 与 凸出2不在同一个方向
        while (true) {
            face2 = RandomUtils.nextInt(3);
            if (face1 != face2) {
                break;
            }
        }
        //生成随机区域值， （10-20）之间
        int position1 = RandomUtils.nextInt((tailoringHeight - ARC * 2) / 2) + (tailoringHeight - ARC * 2) / 2;
        Shape shape1 = createShape(face1, 0, position1);
        Shape bigshape1 = createShape(face1, 2, position1);

        //生成中间正方体Shape, (具体边界+弧半径 = x坐标位)
        Shape centre = new Rectangle2D.Float(ARC, ARC, tailoringWidth - 2 * 10, tailoringHeight - 2 * 10);
        int position2 = RandomUtils.nextInt((tailoringHeight - ARC * 2) / 2) + (tailoringHeight - ARC * 2) / 2;
        Shape shape2 = createShape(face2, 0, position2);

        //因为后边图形需要生成阴影， 所以生成的小图shape + 阴影宽度 = 灰度化的背景小图shape（即大图上的凹槽）
        Shape bigshape2 = createShape(face2, SHADOW_WIDTH / 2, position2);
        Shape bigcentre = new Rectangle2D.Float(10 - SHADOW_WIDTH / 2, 10 - SHADOW_WIDTH / 2, 30 + SHADOW_WIDTH, 30 + SHADOW_WIDTH);

        //合并Shape
        Area area = new Area(centre);
        area.add(new Area(shape1));
        area.add(new Area(shape2));
        //合并大Shape
        Area bigarea = new Area(bigcentre);
        bigarea.add(new Area(bigshape1));
        bigarea.add(new Area(bigshape2));
        List<Shape> list = new ArrayList<>();
        list.add(area);
        list.add(bigarea);
        return list;
    }

    /**
     * 对图片进行裁剪
     *
     * @param sourceImageName 剪裁前原始图片名称以及完整路径
     * @return 裁剪之后的图片Buffered
     * @throws IOException
     */
    private BufferedImage tailoringImage(String sourceImageName) throws IOException {
        Iterator iterator = ImageIO.getImageReadersByFormatName(PNG_SUFFIX);
        ImageReader render = (ImageReader) iterator.next();
        InputStream sourceInputStream = Resources.getResourceAsStream(sourceImageName);
        ImageInputStream in = ImageIO.createImageInputStream(sourceInputStream);
        render.setInput(in, true);
        BufferedImage tailoringImageBuffer;
        try {
            ImageReadParam param = render.getDefaultReadParam();
            Rectangle rect = new Rectangle(locationX, locationY, tailoringWidth, tailoringHeight);
            param.setSourceRegion(rect);
            tailoringImageBuffer = render.read(0, param);
        } finally {
            if (in != null) {
                in.close();
            }
            try {
                sourceInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return tailoringImageBuffer;
    }


    /**
     * 创建圆形区域, 半径为5  type , 0：上方，1：右方 2：下方，3：左方
     *
     * @param type
     * @param size
     * @param position
     * @return
     */
    private Shape createShape(int type, int size, int position) {
        Arc2D.Float d;
        if (type == 0) {
            //上
            d = new Arc2D.Float(position, 5, 10 + size, 10 + size, 0, 190, Arc2D.CHORD);
        } else if (type == 1) {
            //右
            d = new Arc2D.Float(35, position, 10 + size, 10 + size, 270, 190, Arc2D.CHORD);
        } else if (type == 2) {
            //下
            d = new Arc2D.Float(position, 35, 10 + size, 10 + size, 180, 190, Arc2D.CHORD);
        } else if (type == 3) {
            //左
            d = new Arc2D.Float(5, position, 10 + size, 10 + size, 90, 190, Arc2D.CHORD);
        } else {
            d = new Arc2D.Float(5, position, 10 + size, 10 + size, 90, 190, Arc2D.CHORD);
        }
        return d;
    }


    /**
     * 处理小图的边缘灯光及其阴影效果
     *
     * @param bfm
     * @param shape
     * @return
     */
    private BufferedImage dealLightAndShadow(BufferedImage bfm, Shape shape) {
        //创建新的透明图层，该图层用于边缘化阴影， 将生成的小图合并到该图上
        BufferedImage buffimg = ((Graphics2D) bfm.getGraphics()).getDeviceConfiguration().createCompatibleImage(50, 50, Transparency.TRANSLUCENT);
        Graphics2D graphics2D = buffimg.createGraphics();
        Graphics2D g2 = (Graphics2D) bfm.getGraphics();
        //原有小图，边缘亮色处理
        paintBorderGlow(g2, LIGHT_HEIGHT_WIDTH, shape);
        //新图层添加阴影
        paintBorderShadow(graphics2D, SHADOW_WIDTH, shape);
        graphics2D.drawImage(bfm, 0, 0, null);
        return buffimg;
    }

    /**
     * 处理边缘亮色
     *
     * @param g2
     * @param glowWidth
     * @param clipShape
     */
    private void paintBorderGlow(Graphics2D g2, int glowWidth, Shape clipShape) {
        int gw = glowWidth * 2;
        for (int i = gw; i >= 2; i -= 2) {
            float pct = (float) (gw - i) / (gw - 1);
            Color mixHi = getMixedColor(clrGlowInnerHi, pct, clrGlowOuterHi, 1.0f - pct);
            Color mixLo = getMixedColor(clrGlowInnerLo, pct, clrGlowOuterLo, 1.0f - pct);
            g2.setPaint(new GradientPaint(0.0f, 35 * 0.25f, mixHi, 0.0f, 35, mixLo));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, pct));
            g2.setStroke(new BasicStroke(i));
            g2.draw(clipShape);
        }
    }

    /**
     * 处理阴影
     *
     * @param g2
     * @param shadowWidth
     * @param clipShape
     */
    private void paintBorderShadow(Graphics2D g2, int shadowWidth, Shape clipShape) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int sw = shadowWidth * 2;
        for (int i = sw; i >= 2; i -= 2) {
            float pct = (float) (sw - i) / (sw - 1);
            //pct<03. 用于去掉阴影边缘白边，  pct>0.8用于去掉过深的色彩， 如果使用Color.lightGray. 可去掉pct>0.8
            if (pct < 0.3 || pct > 0.8) {
                continue;
            }
            g2.setColor(getMixedColor(new Color(54, 54, 54), pct, Color.WHITE, 1.0f - pct));
            g2.setStroke(new BasicStroke(i));
            g2.draw(clipShape);
        }
    }

    /**
     * 加点颜色更明显
     *
     * @param c1
     * @param pct1
     * @param c2
     * @param pct2
     * @return
     */
    private static Color getMixedColor(Color c1, float pct1, Color c2, float pct2) {
        float[] clr1 = c1.getComponents(null);
        float[] clr2 = c2.getComponents(null);
        for (int i = 0; i < clr1.length; i++) {
            clr1[i] = (clr1[i] * pct1) + (clr2[i] * pct2);
        }
        return new Color(clr1[0], clr1[1], clr1[2], clr1[3]);
    }

    /**
     * 图片转base64
     *
     * @param inputStream 图片的输入流
     * @return 字符串
     */
    public static String getBase64FromInputStream(InputStream inputStream) {
        if (Objects.isNull(inputStream)) {
            return null;
        }
        byte[] data;
        try {
            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[100];
            int rc = 0;
            while ((rc = inputStream.read(buffer, 0, 100)) > 0) {
                arrayOutputStream.write(buffer, 0, rc);
            }
            data = arrayOutputStream.toByteArray();
        } catch (IOException e) {
            log.error("图片流转base64编码异常：{}", e);
            return null;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return new String(org.apache.commons.codec.binary.Base64.encodeBase64(data));
    }

    /**
     * base64转字节流
     *
     * @param base64Str base64字符串
     * @return 流
     */
    public static InputStream getInputStreamFromBase64Str(String base64Str) {
        if (UtilString.isEmpty(base64Str)) {
            return null;
        }
        byte[] bytes = Base64.decodeBase64(base64Str);
        if (Objects.isNull(bytes) || bytes.length == 0) {
            return null;
        }
        return new ByteArrayInputStream(bytes);
    }

    /**
     * bufferedImage 转为普通的InputStream
     *
     * @param bufferedImage bufferedImage流
     * @param fileType      图片类型
     * @return InputStream流
     */
    private static InputStream getInputStreamFromBufferedImage(BufferedImage bufferedImage, String fileType) {
        if (Objects.isNull(bufferedImage)) {
            log.warn("BufferImage转InputStream异常：传入bufferedImage为空！");
            return null;
        }
        //默认为png
        if (UtilString.isEmpty(fileType)) {
            fileType = "png";
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(bufferedImage, fileType, outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());
        } catch (IOException e) {
            log.error("BufferImage转InputStream异常：{}", e);
            return null;
        }
    }
}
