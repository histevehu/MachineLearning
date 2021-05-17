package util;

import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import ann.DataNode;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import static org.opencv.highgui.HighGui.waitKey;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import static org.opencv.imgproc.Imgproc.*;

public class ImageUtil
{
    private Mat[] im_ori_train, im_ori_test, im_proceed_train, im_proceed_test;
    private List<List<DataNode>> dataNodes_train, dataNodes_test;
    private URL path_dll_opencv;
    private String path_data, path_ori_train, path_ori_test, path_train, path_test;
    private int w_uTrain, h_uTrain, w_uTest, h_uTest;

    CLIProgressPrinter clipp;
    private static ImageUtil instance = null;

    public enum dataCate
    {
        TRAIN, TEST
    }

    public Mat[] getIm_ori_train()
    {
        return im_ori_train;
    }

    public Mat[] getIm_ori_test()
    {
        return im_ori_test;
    }

    public Mat[] getIm_proceed_train()
    {
        return im_proceed_train;
    }

    public Mat[] getIm_proceed_test()
    {
        return im_proceed_test;
    }

    public int getW_uTrain()
    {
        return w_uTrain;
    }

    public void setW_uTrain(int w_uTrain)
    {
        this.w_uTrain = w_uTrain;
    }

    public int getH_uTrain()
    {
        return h_uTrain;
    }

    public void setH_uTrain(int h_uTrain)
    {
        this.h_uTrain = h_uTrain;
    }

    public int getW_uTest()
    {
        return w_uTest;
    }

    public void setW_uTest(int w_uTest)
    {
        this.w_uTest = w_uTest;
    }

    public int getH_uTest()
    {
        return h_uTest;
    }

    public void setH_uTest(int h_uTest)
    {
        this.h_uTest = h_uTest;
    }

    public List<List<DataNode>> getDataNodes_train()
    {
        return dataNodes_train;
    }

    public List<List<DataNode>> getDataNodes_test()
    {
        return dataNodes_test;
    }

    public static synchronized ImageUtil getInstance(int w_uTrain, int h_uTrain, int w_uTest, int h_uTest)
    {
        if (instance == null)
            instance = new ImageUtil(w_uTrain, h_uTrain, w_uTest, h_uTest);
        return instance;
    }

    public ImageUtil(int w_uTrain, int h_uTrain, int w_uTest, int h_uTest)
    {
        this.w_uTrain = w_uTrain;
        this.h_uTrain = h_uTrain;
        this.w_uTest = w_uTest;
        this.h_uTest = h_uTest;
        im_ori_train = new Mat[10];
        im_ori_test = new Mat[10];
        im_proceed_train = new Mat[10];
        im_proceed_test = new Mat[10];
        dataNodes_train = new LinkedList<>();
        dataNodes_test = new LinkedList<>();
        for (int i = 0; i <= 9; i++)
        {
            dataNodes_train.add(new LinkedList<DataNode>());
            dataNodes_test.add(new LinkedList<DataNode>());
        }
        // 加载动态库
        path_dll_opencv = ClassLoader.getSystemResource("lib/OpenCV/opencv_java452.dll");
        System.load(path_dll_opencv.getPath());
        path_data = ClassLoader.getSystemResource("data").getPath().substring(1) + "/";
        path_ori_train = path_data + "MNIST/train/";
        path_ori_test = path_data + "MNIST/test/";
        path_train = path_data + "MNIST_proceed/train/";
        path_test = path_data + "MNIST_proceed/test/";
        for (int i = 0; i <= 9; i++)
        {
            File file = new File(path_train + i + "/");
            file.mkdirs();
            file = new File(path_test + i + "/");
            file.mkdirs();
        }
    }

    public void initial()
    {
        System.out.println("</>Reading train images:");
        clipp = new CLIProgressPrinter(10);
        // 读取训练图像
        for (int i = 0; i <= 9; i++)
        {
            Mat image = imread(path_ori_train + "train" + i + ".jpg");
            if (image.empty())
            {
                try
                {
                    throw new Exception("<!>Can't find train" + i + ".jpg");
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            im_ori_train[i] = image;
            clipp.print(clipp.value + 1);
            clipp.value += 1;
        }
        // 读取测试图像
        clipp.reset();
        System.out.println("</>Reading test images:");
        for (int i = 0; i <= 9; i++)
        {
            Mat image = imread(path_ori_test + "test" + i + ".jpg");
            if (image.empty())
            {
                try
                {
                    throw new Exception("<!>Can't find test" + i + ".jpg");
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            im_ori_test[i] = image;
            clipp.print(clipp.value + 1);
            clipp.value += 1;
        }

        // 转灰度图像
        clipp.reset(20);
        System.out.println("</>Converting to gray scale images");
        for (int i = 0; i <= 9; i++)
        {
            Mat grayImgTrain = new Mat();
            Mat grayImgTest = new Mat();
            cvtColor(im_ori_train[i], grayImgTrain, COLOR_RGB2GRAY);
            im_proceed_train[i] = grayImgTrain;
            cvtColor(im_ori_test[i], grayImgTest, COLOR_RGB2GRAY);
            im_proceed_test[i] = grayImgTest;
            imwrite(path_train + i + "-grey.jpg", grayImgTrain);
            imwrite(path_test + i + "-grey.jpg", grayImgTest);
            clipp.print(clipp.value + 2);
            clipp.value += 2;
        }

        // 二值化图像
        clipp.reset(20);
        System.out.println("</>Converting to binary images");
        for (int i = 0; i <= 9; i++)
        {
            Mat binImgTrain = new Mat();
            Mat binImgTest = new Mat();
            threshold(im_proceed_train[i], binImgTrain, 125, 255, THRESH_BINARY);//灰度图像二值化
            im_proceed_train[i] = binImgTrain;
            threshold(im_proceed_test[i], binImgTest, 125, 255, THRESH_BINARY);//灰度图像二值化
            im_proceed_test[i] = binImgTest;
            imwrite(path_train + i + "-grey-bin.jpg", binImgTrain);
            imwrite(path_test + i + "-grey-bin.jpg", binImgTest);
            clipp.print(clipp.value + 2);
            clipp.value += 2;
        }
        clipp.reset();
    }

    public void fetchImgsInfo(dataCate dataCate, Mat[] im_array, List<List<DataNode>> data, int wPixcel, int hPixcel, boolean saveUImgs)
    {
        clipp.reset(im_array.length);
        for (int i = 0; i <= 9; i++)
        {
            for (int h = 0, j = 0; h < im_array[i].rows(); h += hPixcel)
            {
                for (int w = 0; w < im_array[i].cols(); w += wPixcel, j++)
                {
                    Rect rect = new Rect(w, h, wPixcel, hPixcel);
                    Mat cutImage = cutImg(im_array[i], rect);
                    if (hasContent(cutImage))
                    {
                        if (saveUImgs)
                            imwrite(((dataCate == ImageUtil.dataCate.TRAIN) ? path_train : path_test) + i + "/" + j + ".jpg", cutImage);
                        /*for (int e = 0; e < cutImage.rows(); e++)
                        {
                            for (int t = 0; t < cutImage.cols(); t++)
                            {
                                System.out.print((cutImage.get(e, t)[0] == 255 ? 1 : 0) + "  ");
                            }
                            System.out.println();
                        }*/
                        DataNode dataNode = new DataNode();
                        for (int r = 0; r < cutImage.rows(); r += 4)
                        {
                            for (int c = 0; c < cutImage.cols(); c += 4)
                            {
                                //dataNode.addAttr((cutImage.get(r, c)[0] == 255) ? 1 : 0);
                                double cr = calcContentRatio(cutImage, r, c, 4);
                                //System.out.println(cr + "     ");
                                dataNode.addAttr(cr);
                            }
                            //System.out.println();
                        }
                        dataNode.setCategory(i);
                        data.get(i).add(dataNode);
                        clipp.print(clipp.value, "(" + j + ")");
                    }
                }
            }
            //System.out.println("============================");
            clipp.print(clipp.value + 1, "");
            clipp.value += 1;
        }
        clipp.reset();
    }

    protected Mat cutImg(Mat src, Rect rect)
    {
        //图片裁剪
        Mat src_roi = new Mat(src, rect);
        Mat cutImage = new Mat();
        src_roi.copyTo(cutImage);
        return cutImage;
    }

    public boolean hasContent(Mat img)
    {
        double flag = img.get(0, 0)[0];
        for (int i = 0; i < img.rows(); i += 4)
        {
            for (int j = 0; j < img.cols(); j += 4)
            {
                if (img.get(i, j)[0] != flag)
                    return true;
            }
        }
        return false;
    }

    protected double calcContentRatio(Mat img, int r, int c, int scale)
    {
        int count = 0;
        for (int i = r; i < r + scale; i++)
        {
            for (int j = c; j < c + scale; j++)
            {
                //System.out.print((img.get(i, j)[0] == 255 ? 1 : 0) + "  ");
                //System.out.println(i+","+j+" ");
                if (img.get(i, j)[0] == 255)
                    count++;
            }
            //System.out.println();
        }
        return 1.0 * count / (scale * scale);
    }

    public void printDataList(List<DataNode> dataNodeList)
    {
        for (DataNode node : dataNodeList)
        {
            for (int i = 0; i < 49; i++)
            {
                String num = node.getAttrList().get(i).toString();
                num = String.format("%-10s", num);
                System.out.print(num);
                if (((i+1) % 7 == 0) && i > 0)
                    System.out.println();
            }
            System.out.println("====================");
        }

    }

    public static void main(String[] args)
    {
        ImageUtil imageUtil = new ImageUtil(28, 28, 28, 28);
        imageUtil.initial();
        System.out.print("</>Fetching train images info");
        imageUtil.fetchImgsInfo(dataCate.TRAIN, imageUtil.getIm_proceed_train(), imageUtil.getDataNodes_train(), imageUtil.getW_uTrain(), imageUtil.getH_uTrain(), false);
        System.out.print("</>Fetching test images info");
        imageUtil.fetchImgsInfo(dataCate.TEST, imageUtil.getIm_proceed_test(), imageUtil.getDataNodes_test(), imageUtil.getW_uTest(), imageUtil.getH_uTest(), false);
        System.out.println();
    }
}
