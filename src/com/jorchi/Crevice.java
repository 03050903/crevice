package com.jorchi;

import org.apache.commons.lang3.StringUtils;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.*;
import org.opencv.imgproc.Imgproc;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.*;
import static org.opencv.imgproc.Imgproc.*;
import static org.opencv.imgproc.Imgproc.MORPH_CLOSE;
import static org.opencv.imgproc.Imgproc.MORPH_OPEN;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;

/**
 * Update by Lizenn on 2017/10/2.
 */


public class Crevice {
    static{
//        System.loadLibrary("jniLib//opencv_java330");
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args)throws IOException{
        //image to byte
        String filePath = "test//img//3.jpeg";
        //Mat imgMat= Imgcodecs.imread(filePath);
        byte[] imgData = image2byte(filePath);

        Integer x =100;
        Integer y = 270;
        List list = new ArrayList();
        list  = getCreviceWidth(false,imgData,x,y);
        System.out.println("list:"+list.get(1));
//        double totalTime = 0;
//        for(int i = 0; i < 100; i++) {
//            long start = System.currentTimeMillis();
//            System.out.println(getPicCenterYDistance(imgData,x,y));
//            long end = System.currentTimeMillis();
//            System.out.print((double)(end - start) / 1000 + "秒");
//            totalTime += (double)(end - start) / 1000;
//            System.out.println(" <===================================>");
//        }
//        System.out.println("总时间" + totalTime);
//        System.out.println("平均时间" + totalTime / 100 );

    }

    /**
     * 形态学处理
     * @param imgMat
     * @return
     */
    public static Mat getCannyMat(boolean flag,Mat imgMat) {
        //转灰度图
        Mat mat_gray = new Mat();
        Imgproc.cvtColor(imgMat, mat_gray, Imgproc.COLOR_BGRA2GRAY);
//        Imgcodecs.imwrite("E:\\image\\mat_gray_1.jpg",mat_gray);
        int kSize = 7;
        if(flag){
            kSize = 1;
        }
        Mat ezh_img = new Mat();
        //中值滤波
        Mat medBlur = imgMat.clone();
        Imgproc.medianBlur(mat_gray,medBlur,kSize);
//        Imgcodecs.imwrite("E:\\image\\medBlur.jpg",medBlur);
        //二值化
        Imgproc.threshold(medBlur, ezh_img, 80, 255, Imgproc.THRESH_TOZERO);
//        Imgcodecs.imwrite("E:\\image\\ezh_img.jpg",ezh_img);
        //开运算
        Mat openOut = new Mat();
        Mat elementOpen = Imgproc.getStructuringElement(MORPH_RECT, new Size(5, 5));
        Imgproc.morphologyEx(ezh_img,openOut,MORPH_OPEN,elementOpen,new Point(-1,-1),6);
//        Imgcodecs.imwrite("E:\\image\\kai.jpg",openOut);
        //闭运算
        Mat closeOut = new Mat();
        Mat elementClose = Imgproc.getStructuringElement(MORPH_RECT, new Size(6, 6));
        Imgproc.morphologyEx(openOut, closeOut,MORPH_CLOSE, elementClose,new Point(-1,-1),1);
//        Imgcodecs.imwrite("E:\\image\\BI.jpg",closeOut);
        //高斯滤波
        Mat cannyMat= new Mat();
        Imgproc.Canny(closeOut, cannyMat, 255, 255);
//        Imgcodecs.imwrite("E:\\image\\cannyMat_7.jpg",cannyMat);
        return cannyMat;
    }

    /**
     * image to byte data
     * @param path
     * @return
     */
    public static byte[] image2byte(String path){
        byte[] data = null;
        FileImageInputStream input = null;
        try {
            input = new FileImageInputStream(new File(path));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int numBytesRead = 0;
            while ((numBytesRead = input.read(buf)) != -1) {
                output.write(buf, 0, numBytesRead);
            }
            data = output.toByteArray();
            output.close();
            input.close();
        }
        catch (FileNotFoundException ex1) {
            ex1.printStackTrace();
        }
        catch (IOException ex1) {
            ex1.printStackTrace();
        }
        return data;
    }

    /**
     * byte数组到图片
     * @param data
     * @param path
     */
    public static void byte2image(byte[] data,String path){
        if(data.length<3||path.equals("")) return;
        try{
            FileImageOutputStream imageOutput = new FileImageOutputStream(new File(path));
            imageOutput.write(data, 0, data.length);
            imageOutput.close();
        } catch(Exception ex) {
            System.out.println("Exception: " + ex);
            ex.printStackTrace();
        }
    }

    /**
     * 判断文件是否存在，不存在则创建
     * @param filePath
     */
    public static void checkDirAndFileExist(String filePath){
        String dirPath = StringUtils.substringBeforeLast(filePath,"//");
        File dir =new File(dirPath);
        //如果文件夹不存在则创建
        if  (!dir .exists() && !dir .isDirectory()) {
            dir .mkdir();
        }
        //创建文件
        File file=new File(filePath);
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //多线程共用静态变量
    public static int nrows, ncols, mid_rows1, mid_rows, cols_times, point1, point2,
            start_cols, run_cols, P,stepLeft,stepRight;

    /**
     * 裂缝识别主入口
     * @param imgData
     * @param x
     * @param y
     * @return
     */
    public static List getCreviceWidth(boolean flag,byte[] imgData,Integer x,Integer y){
        List list = new ArrayList();
        int[] array = new int[5];
        //byte to image
        //filePath不要改动，将byte[]转成图片写入该路径，
        //每写入一张图片都会覆盖原来的，根据该路径读取矩阵。
        String filePath = "image//draw.jpeg";
        checkDirAndFileExist(filePath);
        byte2image(imgData,filePath);
        Mat imgMat= Imgcodecs.imread(filePath);
        //调用形态学方法
        Mat cannyMat = getCannyMat(flag,imgMat);
        //取滤波后的像素长和宽
        nrows =cannyMat.rows();
        ncols =cannyMat.cols();
        mid_rows1=nrows/2;
        mid_rows=mid_rows1;
        cols_times = 0;
        point1 = 0;
        point2 = 0;
        start_cols = 0;
        stepLeft = 0;
        stepRight = 0;
        int startY = y;
        P = 1;
        if(x == null && y == null) {
            long beginTime1 = System.currentTimeMillis();
            while(((System.currentTimeMillis() - beginTime1)/1000 < 2) && cols_times == 0) {
                for (int i = 0; i < ncols; i++) {
                    //获取某行某列的像素值  只有一个值
                    double[] p=cannyMat.get(mid_rows,start_cols + i);
                    if (p[0] > 0) {
                        cols_times++;
                        run_cols = start_cols + i;
                        if (cols_times == 1) {
                            point1 = run_cols;
                        }
                        if (cols_times == 2) {
                            point2 = run_cols;
                        }
                    }
                }
                if (cols_times > 2){
                    if ((mid_rows < (nrows - 20)) && (P == 1)){
                        cols_times = 0;
                        mid_rows = mid_rows + 1;
                    }else if (mid_rows >= (nrows - 20)){
                        mid_rows = mid_rows1;
                        P = -1;
                    }

                    if ((mid_rows >= 20) && (P == -1)){
                        cols_times = 0;
                        mid_rows = mid_rows - 1;
                    }else if ((mid_rows < 20) && (P == -1)){
                        P = 0;
                    }
                }
                if (P == 0){
                    cols_times = 10;
                }
            }
            if(Math.abs(point2 - point1) != 0)
            drawImage(filePath,point1,point2,cannyMat.rows()/2);
        } else {
            long beginTime2 = System.currentTimeMillis();
            while(((System.currentTimeMillis() - beginTime2)/1000 < 2)&&(cols_times == 0)) {
                if((x > ncols || y > nrows)||(x ==0 && y == 0)){
                    x = ncols/2;
                    y = nrows/2;
                    startY = nrows/2;
                }
//                // 左扫描z
//                new Thread() {
//                   public void run() {
//                           for(int i = x; i > -1; i--) {
//                               stepLeft = i;
//                               double[] p=cannyMat.get(y, i);
//                               if (p[0] > 0) {
//                                   System.out.println("左扫描边界："+p[0]+" X:"+i);
//                                   cols_times++;
//                                   run_cols = i;
//                                   if (cols_times == 1) {
//                                       point1 = run_cols;
//                                   }
//                                   if (cols_times == 2) {
//                                       point2 = run_cols;
//                                   }
//                                   if(cols_times == 3) {
//                                       Thread.interrupted();
//                                   }
//                               }
//                               if(stepLeft == 0 && stepRight == x && point2 == 0) {
//                                   // 另起一行
//                               }
//
//                           }
//                       }
//               }.start();
//                // 向右扫描
//                new Thread() {
//                    public void run() {
//
//                        if(point2 == 0) {
//                            for(int i = x + 1; i < ncols + 1; i++) {
//                                double[] p=cannyMat.get(y, i);
//                                //System.out.println("像素值2："+p[0]);
//                                if(p != null)
//                                    if (p[0] > 0) {
//                                        System.out.println("右扫描边界："+p[0]+" X:"+i);
//                                        cols_times++;
//                                        run_cols = i;
//                                        if (cols_times == 1) {
//                                            point1 = run_cols;
//                                        }
//                                        if (cols_times == 2) {
//                                            point2 = run_cols;
//                                        }
//                                        if(cols_times == 3) {
//                                            Thread.interrupted();
//                                        }
//                                    }
//                            }
//                            if(stepRight == x && stepLeft == 0 && point2 == 0) {
//                                // 另起一行
//                            }
//                        }
//                    }
//
//                }.start();
                // 左扫描z
                for(int i = x; i > -1; i--) {
                    stepLeft = i;
//                    System.out.println("左扫描");
                    double[] p=cannyMat.get(y, i);
                    if (p[0] > 0) {
//                        System.out.println("左扫描边界："+p[0]+" X:"+i);
                        cols_times++;
                        run_cols = i;
                        if (cols_times == 1 && point1 == 0) {
                            point1 = run_cols;
                        }
                        if (cols_times == 2 && point2 == 0) {
                            point2 = run_cols;
                        }
                        if(cols_times == 3) {
                            break;
                        }
                    }
                }
                //右扫描
                for(int i = x + 1; i < ncols + 1; i++) {
                    if(cols_times >= 2){break;}
//                    System.out.println("you扫描");
                    double[] p=cannyMat.get(y, i);
                    if(p != null)
                        if (p[0] > 0) {
//                            System.out.println("右扫描边界："+p[0]+" X:"+i);
                            cols_times++;
                            run_cols = i;
                            if (cols_times == 1&& point1 == 0) {
                                point1 = run_cols;
                                //System.out.println("in if point1：" +point1);
                            }
                            if (cols_times == 2 && point2 == 0) {
                                point2 = run_cols;
                                //System.out.println("in if point2：" +point2);

                            }
                        }
                }
                if (cols_times < 2){
                    if ((y < (nrows - 50)) && (P == 1)){
                        cols_times = 0;
                        y = y + 1;
                    }else if (y >= (nrows - 50)){
                        y = startY;
                        P = -1;
                    }

                    if ((y >= 50) && (P == -1)){
                        cols_times = 0;
                        y = y - 1;
                    }else if ((y < 50) && (P == -1)){
                        P = 0;
                    }
                }
                if (P == 0){
                    cols_times = 100;
                }
            }
            if(Math.abs(point2 - point1) != 0)
            drawImage(filePath,point1,point2,y);
        }
//            array[0] = Math.abs(point2-point1);
//            array[1] = point1;
//            array[2] = point2;
            list.add(0,image2byte(filePath));
            list.add(1,Math.abs(point2-point1));
            return list;
    }
    public static void drawImage(String filePath,int point1,int point2,int y){
        Mat imgMat= Imgcodecs.imread(filePath);
        Imgproc.line(imgMat,new Point(0,y),new Point(point1,y),new Scalar(0,0,255));
        Imgproc.line(imgMat,new Point(point2,y),new Point(imgMat.cols(),y),new Scalar(0,0,255));
        Imgproc.line(imgMat,new Point(point1,y-30),new Point(point1,y+30),new Scalar(0,0,255));
        Imgproc.line(imgMat,new Point(point2,y-30),new Point(point2,y+30),new Scalar(0,0,255));
        Imgcodecs.imwrite(filePath,imgMat);
    }
}
