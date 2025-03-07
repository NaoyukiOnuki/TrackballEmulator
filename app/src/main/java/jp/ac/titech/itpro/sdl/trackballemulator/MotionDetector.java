package jp.ac.titech.itpro.sdl.trackballemulator;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by onuki on 2017/06/24.
 */

class MotionDetector {

    int select = 1;

    Boolean button = false;

    class Motion {
        float x;
        float y;
        Mat image;
        Motion(float x, float y, Mat image) {
            this.x = x;
            this.y = y;
            this.image = image;
        }
    }

    final private static String TAG = "MotionDetector";

    private float threshold;

    private Mat image_small, high_image, low_image, dark_image;
    private Mat rect;
    private Mat image1, image2;

    private Point point_prev;

    MotionDetector(int width, int height, float threshold) {
        image_small = new Mat(height/8, width/8, CvType.CV_8UC3);
        high_image = new Mat(height/8, width/8, CvType.CV_8UC1);
        low_image = new Mat(height/8, width/8, CvType.CV_8UC1);
        dark_image = new Mat(height/8, width/8, CvType.CV_8UC1);

        rect = new Mat(height/8, width/8, CvType.CV_8UC3);

        image1 = new Mat(height, width, CvType.CV_8UC3);
        image2 = new Mat(height, width, CvType.CV_8UC3);

        this.threshold = threshold;

    }

    void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    Motion onCameraFrame(Mat input) {

        float h_value = 0;
        float v_value = 0;

        rect = new Mat(input.rows(), input.cols(), input.type());

        // 縮小
        Imgproc.resize(input, image_small, image_small.size(), 0, 0, Imgproc.INTER_NEAREST);

        // RGBからHSVに変更
        //Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2HSV);
        Imgproc.cvtColor(image_small, image_small, Imgproc.COLOR_RGB2HSV);

        // debug用
        if (button) {
            for (int y = 0; y*2 < image_small.rows(); y++) {
                for (int x = 0; x*2 < image_small.cols(); x++) {
                    double[] data = image_small.get(y*2, x*2);
                    if (data.length >= 3) {
                        Log.d(TAG, "color is " +data[0] + " " + data[1] + " " + data[2]);
                    }
                }
            }
            button = false;
        }

        // 赤くてある程度明るい：low,high
        // すごく暗い：dark
        Core.inRange(image_small, new Scalar(0, 0, 0), new Scalar(4, 255, 40), low_image);
        Core.inRange(image_small, new Scalar(176, 0, 0), new Scalar(180, 255, 40), high_image);
        Core.inRange(image_small, new Scalar(0, 0, 0), new Scalar(180, 255, 2), dark_image);
        // orで合成
        Core.bitwise_or(low_image, high_image, image_small);
        Core.bitwise_or(image_small, dark_image, image_small);

        // image1に保存
        Imgproc.resize(image_small, image1, image1.size(), 0, 0, Imgproc.INTER_NEAREST);

        // 輪郭を検出
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat(image_small.rows(), image_small.cols(), CvType.CV_8UC1);
        Imgproc.findContours(image_small, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_L1);

        // 一定以上の大きさの輪郭があるかどうか
        boolean exist = false;
        int index = 0;
        double max_area = image_small.rows() * image_small.cols();
        //Log.d(TAG, "contour size is " + contours.size());
        while (!exist && index < contours.size()) {
            double area = Imgproc.contourArea(contours.get(index), false);
            if (area * 3 > max_area) exist = true;
            else index++;
        }

        // 一定以上の大きさの輪郭があれば
        if (exist) {
            MatOfPoint max_contour = contours.get(index);
            Moments mu = MyImgproc.contourMoments(max_contour);
            // 輪郭の重心p
            Point p = new Point(mu.get_m10()/mu.get_m00(), mu.get_m01()/mu.get_m00());
            if (point_prev != null) {
                if (Math.abs(p.x - point_prev.x) > threshold) h_value += p.x - point_prev.x;
                if (Math.abs(p.y - point_prev.y) > threshold) v_value += p.y - point_prev.y;
            }
            point_prev = p;
            Imgproc.drawContours(rect, contours, index, new Scalar(128, 128, 128));
            Imgproc.line(rect, p, p, new Scalar(192, 192, 192), 5);
        } else {
            point_prev = null;
        }

        // image2に保存
        Imgproc.resize(rect, image2, image2.size(), 0, 0, Imgproc.INTER_NEAREST);

        Motion motion;
        switch (select) {
            case 1:
                motion = new Motion(h_value, v_value, input);
                break;
            case 2:
                motion = new Motion(h_value, v_value, image1);
                break;
            case 3:
                motion = new Motion(h_value, v_value, image2);
                break;
            default:
                motion = new Motion(h_value, v_value, input);
        }
        return motion;
    }
}
