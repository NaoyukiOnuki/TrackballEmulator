package jp.ac.titech.itpro.sdl.trackballemulator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by onuki on 2017/06/20.
 */

public class CameraSettingsActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    String TAG = "CameraSettingsActivity";
    final int REQUEST_SUB = 1000;
    final int RESULT_SUB = 100;
    Intent intent;

    float inc_alpha, dec_alpha, threshold, scale;

    boolean isDown = true;
    List<Float> vlist = new ArrayList<>();
    List<Float> hlist = new ArrayList<>();

    CameraBridgeViewBase mCameraView;

    MotionDetector detector;

    // OpenCVライブラリのロード
    private BaseLoaderCallback mCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "callback SUCCESS");
                    mCameraView.enableView();
                    break;
                default:
                    Log.d(TAG, "callback DEFAULT");
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_settings);

        Log.d(TAG, "onCreate");

        intent = new Intent();

        getSettings();

        mCameraView = (CameraBridgeViewBase) findViewById(R.id.settingCameraView);
        mCameraView.setCvCameraViewListener(this);
        //mCameraView.setVisibility(View.GONE);

        findViewById(R.id.downButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDown = true;
            }
        });
        findViewById(R.id.upButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDown = false;
            }
        });
        findViewById(R.id.finishButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "start onclick fin");
                setResult(RESULT_SUB, intent);
                finish();
                Log.d(TAG, "finish onclick fin");
            }
        });
        findViewById(R.id.saveButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "start onclick");
                saveSettings();
                Log.d(TAG, "finish onclick");
            }
        });
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d(TAG, "onCameraViewStarted");
        detector = new MotionDetector(width, height, threshold);
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        //Mat mat = new Mat();
        //Imgproc.threshold(inputFrame.gray(), mat, 20.0, 255.0,
        //        Imgproc.THRESH_BINARY);
        //return mat;

        MotionDetector.Motion motion = detector.onCameraFrame(inputFrame.rgba());
        hlist.add(motion.x);
        vlist.add(motion.y);
        Log.d(TAG, "add scroll value: " + motion.x + " " + motion.y);
        return motion.image;
    }
    /*
        private static class OpenCVLoaderCallback extends BaseLoaderCallback {
            private final CameraBridgeViewBase mCameraView;
            private OpenCVLoaderCallback(Context context, CameraBridgeViewBase cameraView) {
                super(context);
                mCameraView = cameraView;
            }

            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                        mCameraView.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        }
    */
    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mCallBack);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCameraView != null) mCameraView.disableView();
    }


    void saveSettings() {
        float x = 0f;
        for (float e: hlist) {
            x += e;
        }
        x /= hlist.size();
        float y = 0f;
        for (float e: vlist) {
            y += e;
        }
        y /= vlist.size();
        SharedPreferences pref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor e = pref.edit();
        if (isDown) {
            intent.putExtra(getString(R.string.key_x1), x);
            intent.putExtra(getString(R.string.key_y1), y);
            Log.d(TAG, "set down " + x + " " + y);
        } else {
            intent.putExtra(getString(R.string.key_x2), x);
            intent.putExtra(getString(R.string.key_y2), y);
            Log.d(TAG, "set notdown " + x + " " + y);
        }
        e.apply();
        hlist.clear();
        vlist.clear();
    }

    private void getSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        TypedValue outValue = new TypedValue();
        getResources().getValue(R.dimen.inc_alpha, outValue, true);
        inc_alpha = sharedPreferences.getFloat("inc_alpha", outValue.getFloat());
        outValue = new TypedValue();
        getResources().getValue(R.dimen.dec_alpha, outValue, true);
        dec_alpha = sharedPreferences.getFloat("dec_alpha", outValue.getFloat());
        outValue = new TypedValue();
        getResources().getValue(R.dimen.threshold, outValue, true);
        threshold = sharedPreferences.getFloat("threshold", outValue.getFloat());
        outValue = new TypedValue();
        getResources().getValue(R.dimen.scale, outValue, true);
        scale = sharedPreferences.getFloat("scale", outValue.getFloat());
    }
}
