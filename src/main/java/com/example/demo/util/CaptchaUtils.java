package com.example.demo.util;

import com.example.demo.support.CaptchaConst;
import com.google.common.collect.Maps;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * @author itw_wangjb03
 * @date 2018-9-3 14:00:24
 * 用于生成拼图验证码工具类
 */
public class CaptchaUtils {

    private static Logger log = LoggerFactory.getLogger(CaptchaUtils.class);

    //小图的宽
    private int tailoring_w = 50;
    //小图的高
    private int tailoring_h = 50;
    //随机X位置
    private int location_x = 0;
    //随机Y位置
    private int location_y = 0;

    //生成图片名称
    private String picSuffix = ".png";

    //指定web下的目录,
    private static String imgPath = "";
    private static String sourceImgPath = "";
    // 临时验证码目录
    private static final String tempYzmImg = "public/images/captcha/temp";
    // 源目录
    private static final String sourceYzmImg = "public/images/captcha/source";

    private static final int shadowWidth = 4; //阴影宽度
    private static final int lightHeightWidth = 5; //图片边缘亮色（黄色）宽度。

    private static final int arc = 10; //圆弧直径

    /**
     * 制定源图片位置及其生成的临时图位置
     *
     * @param context
     */
    public static void init(ServletContext context) {
        imgPath = context.getRealPath("/") + tempYzmImg;
        sourceImgPath = context.getRealPath("/") + sourceYzmImg;
        log.info("tempPath:{}", imgPath);
        log.info("sourcePath:{}", sourceImgPath);
    }

    public CaptchaUtils() {
    }

    public static void cleanTemp(ServletContext servletContext) {

    }

    /**
     * 创建图片到临时目录， 并返回图片的文件名称
     *
     * @param request
     * @param havingfilename
     * @return
     * @throws IOException
     */
    public Map<String, String> create(HttpServletRequest request, String havingfilename) throws IOException {
        log.info("readPath:{}", sourceImgPath);
        //本地原始图片路径,
        File file = new File(sourceImgPath);
        String[] list = file.list();
        if (Objects.isNull(list)) {
            log.error("读取文件列表失败:{}", file);
            throw new RuntimeException("读取文件列表失败");
        }
        String filename;
        //获取随机图片， 每次获取到的图片与已有的图片要不同。
        while (true) {
            int randowval = RandomUtils.nextInt(list.length);
            filename = list[randowval];
            if (!filename.equals(havingfilename)) {
                break;
            }
        }
        File sourceFile = new File(sourceImgPath + File.separator + filename);
        //从原始图片中随机截取小图，同时处理背景大图
        Map<String, String> result = createImg(sourceFile, filename);

        // 获取当前访问的ip，
        String host = UtilWeb.getIpAddr((HttpServletRequest) request);
        // 拼接放入redis的key
        host = UtilString.join(host, CaptchaConst.MIDDLE_LINE, CaptchaConst.CAPTCHA);
        //将x 轴位置作为验证码 放入到redis中，key为IP-captcha
        CacheManagerHolder.getManager().getCache(CaptchaConst.VERIFICATION_CODE).put(host, location_x);
        return result;
    }

    /**
     * 对图片进行裁剪
     *
     * @param file 图片
     * @param x    裁剪图左上方X位置
     * @param y    裁剪图左上方Y位置
     * @param w    裁剪的宽
     * @param h    裁剪的宽
     * @return 裁剪之后的图片Buffered
     * @throws IOException
     */
    private static BufferedImage cutImg(File file, int x, int y, int w, int h) throws IOException {
        Iterator iterator = ImageIO.getImageReadersByFormatName("png");
        ImageReader render = (ImageReader) iterator.next();
        ImageInputStream in = ImageIO.createImageInputStream(new FileInputStream(file));
        render.setInput(in, true);
        BufferedImage bufferedImage;
        try {
            ImageReadParam param = render.getDefaultReadParam();
            Rectangle rect = new Rectangle(x, y, w, h);
            param.setSourceRegion(rect);
            bufferedImage = render.read(0, param);
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return bufferedImage;
    }

    /**
     * 创建大图，即带小图水印缺口的图片
     *
     * @param smllImage 大图，拼图的buffer
     * @param file      源文件
     * @param filename  文件名称
     * @return 大图的buffer
     * @throws IOException
     */
    private String createBigImg(BufferedImage smllImage, File file, String filename) throws IOException {
        //创建一个灰度化图层， 将生成的小图，覆盖到该图层，使其灰度化，用于作为一个水印图
        String bigImgName = randomImgName("big_" + filename.replaceAll(".png", "") + "_");
        //如果大图不存在，那么就创建
        File bigfile = new File(imgPath + File.separator + bigImgName);
        if (!bigfile.exists()) {
            //将灰度化之后的图片，整合到原有图片上
            BufferedImage bigImg = addWatermark(file, smllImage, 0.6F);
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
        }
        return bigImgName;
    }

    /**
     * 添加水印
     *
     * @param file
     * @param smallImage
     */
    private BufferedImage addWatermark(File file, BufferedImage smallImage, float alpha) throws IOException {
        BufferedImage source = ImageIO.read(file);
        Graphics2D graphics2D = source.createGraphics();
        graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
        graphics2D.drawImage(smallImage, location_x, location_y, null);
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
        return suf + location_y + "_" + DigestUtils.md5Hex(String.valueOf(location_x)).substring(0, 16) + picSuffix;
    }

    /**
     * 生成文件到对应路径
     *
     * @param file     文件
     * @param filename 文件名
     * @return
     * @throws IOException
     */
    private Map<String, String> createImg(File file, String filename) throws IOException {
//        ImageIO.read()
        BufferedImage sourceBuff = ImageIO.read(file);
        int width = sourceBuff.getWidth();
        int height = sourceBuff.getHeight();
        //生成随机x，y
        Random random = new Random();
        //X轴距离右端tailoring_w 以上）  Y轴距离底部tailoring_y以上
        this.location_x = random.nextInt(width - tailoring_w * 2) + tailoring_w;
        this.location_y = random.nextInt(height - tailoring_h);
        //裁剪小图
        BufferedImage sourceSmall = cutImg(file, location_x, location_y, tailoring_w, tailoring_h);
        //创建shape区域
        List<Shape> shapes = createSmallShape();
        Shape area = shapes.get(0);
        Shape bigarea = shapes.get(1);
        //创建图层用于处理小图的阴影
        BufferedImage bfm1 = new BufferedImage(tailoring_w, tailoring_h, BufferedImage.TYPE_INT_ARGB);
        //创建图层用于处理大图的凹槽
        BufferedImage bfm2 = new BufferedImage(tailoring_w, tailoring_h, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < tailoring_w; i++) {
            for (int j = 0; j < tailoring_h; j++) {
                if (area.contains(i, j)) {
                    bfm1.setRGB(i, j, sourceSmall.getRGB(i, j));
                }
                if (bigarea.contains(i, j)) {
                    bfm2.setRGB(i, j, Color.black.getRGB());
                }
            }
        }
        //处理图片的边缘高亮及其阴影效果
        BufferedImage resultImgBuff = dealLightAndShadow(bfm1, area);
        //生成随机名称
        String smallFileName = randomImgName("small_" + filename.replaceAll(".png", "") + "_");

        // 图片流先转输入流
        InputStream inputStream = getInputStreamFromBufferedImage(resultImgBuff, "png");
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
        Map<String, String> result = Maps.newConcurrentMap();
        result.put("smallImgName", smallFileName);
        //将灰色图当做水印印到原图上
        String bigImgName = createBigImg(bfm2, new File(sourceImgPath + File.separator + filename), filename);
        result.put("bigImgName", bigImgName);
        result.put("location_y", String.valueOf(location_y));
        result.put("sourceImgName", filename);
        return result;
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
        int position1 = RandomUtils.nextInt((tailoring_h - arc * 2) / 2) + (tailoring_h - arc * 2) / 2;
        Shape shape1 = createShape(face1, 0, position1);
        Shape bigshape1 = createShape(face1, 2, position1);

        //生成中间正方体Shape, (具体边界+弧半径 = x坐标位)
        Shape centre = new Rectangle2D.Float(arc, arc, tailoring_w - 2 * 10, tailoring_h - 2 * 10);
        int position2 = RandomUtils.nextInt((tailoring_h - arc * 2) / 2) + (tailoring_h - arc * 2) / 2;
        Shape shape2 = createShape(face2, 0, position2);

        //因为后边图形需要生成阴影， 所以生成的小图shape + 阴影宽度 = 灰度化的背景小图shape（即大图上的凹槽）
        Shape bigshape2 = createShape(face2, shadowWidth / 2, position2);
        Shape bigcentre = new Rectangle2D.Float(10 - shadowWidth / 2, 10 - shadowWidth / 2, 30 + shadowWidth, 30 + shadowWidth);

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


    //处理小图的边缘灯光及其阴影效果
    private BufferedImage dealLightAndShadow(BufferedImage bfm, Shape shape) {
        //创建新的透明图层，该图层用于边缘化阴影， 将生成的小图合并到该图上
        BufferedImage buffimg = ((Graphics2D) bfm.getGraphics()).getDeviceConfiguration().createCompatibleImage(50, 50, Transparency.TRANSLUCENT);
        Graphics2D graphics2D = buffimg.createGraphics();
        Graphics2D g2 = (Graphics2D) bfm.getGraphics();
        //原有小图，边缘亮色处理
        paintBorderGlow(g2, lightHeightWidth, shape);
        //新图层添加阴影
        paintBorderShadow(graphics2D, shadowWidth, shape);
        graphics2D.drawImage(bfm, 0, 0, null);
        return buffimg;
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

    private static final Color clrGlowInnerHi = new Color(253, 239, 175, 148);
    private static final Color clrGlowInnerLo = new Color(255, 209, 0);
    private static final Color clrGlowOuterHi = new Color(253, 239, 175, 124);
    private static final Color clrGlowOuterLo = new Color(255, 179, 0);

    /**
     * 处理边缘亮色
     *
     * @param g2
     * @param glowWidth
     * @param clipShape
     */
    public void paintBorderGlow(Graphics2D g2, int glowWidth, Shape clipShape) {
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

    //创建圆形区域, 半径为5  type , 0：上方，1：右方 2：下方，3：左方
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
        return new String(Base64.encodeBase64(data));
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
    public static InputStream getInputStreamFromBufferedImage(BufferedImage bufferedImage, String fileType) {
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
