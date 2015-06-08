package wwckl.projectmiki.activity;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.os.Handler;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import wwckl.projectmiki.R;
import wwckl.projectmiki.models.Receipt;

/**
 * Created by Aryn on 5/24/15.
 */
public class LoadingActivity extends AppCompatActivity {
    private Bitmap mReceiptPicture = null;
    private String mRecognizedText = "start";

    private ImageView mImageView;
    private ProgressBar mProgressBar;
    private TextView mTextView;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            stopProgressBar();
        }
    };
    private Thread mThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        // Set receipt image in background.
        mReceiptPicture = Receipt.receiptBitmap;

        mImageView = (ImageView) findViewById(R.id.imageViewLoading);
        mImageView.setImageBitmap(mReceiptPicture);

        // Progress bar
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mTextView = (TextView) findViewById(R.id.tvRecognisedText);

        // START thread to do back ground operations
        startOperation();

        //Receipt.recognizedText = mRecognizedText;

        // completed job. stop thread & hide progress bar
        //startBillSplitting();
    }

    public void stopProgressBar(){
        mProgressBar.setVisibility(View.GONE);

        if(!mRecognizedText.isEmpty())
            mTextView.setText(mRecognizedText);
        mImageView.setVisibility(View.GONE);
    }

    public void startBillSplitting(){
        // TODO: STORE RECEIPT RESULT?
        Intent intent = new Intent(this, BillSplitterActivity.class);
        startActivity(intent);
    }

    // Start thread to run Tesseract
    public void startOperation(){

        mThread = new Thread(new Runnable() {
            public void run() {
                // start Tesseract thread to detect text.
                TesseractDetectText();

                // TODO: CLEAN UP DATA

                // Post message to handler to signal complete operation
                mHandler.sendEmptyMessage(0);
            }
        });
        mThread.start();
    }

    public void TesseractDetectText() {
        // create tessdata directory
        File tessDir = new File(Environment.getExternalStorageDirectory().getPath() + "/tessdata");
        if (!tessDir.exists()) {
            tessDir.mkdir();
        }

        // get data path
        String path = Environment.getExternalStorageDirectory().getPath();
        String lang = "eng";

        File tessData = new File(path + "/tessdata/" + lang + ".traineddata");

        // Copy tessdata language file
        if (!tessData.exists()) {
            try {
                AssetManager assetManager = getAssets();
                InputStream in = assetManager.open(lang + ".traineddata");
                OutputStream out = new FileOutputStream(path
                        + "/tessdata/" + lang + ".traineddata");

                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            } catch (IOException e) {}
        }

        TessBaseAPI tessBaseAPI = new TessBaseAPI();
        tessBaseAPI.setDebug(true);
        tessBaseAPI.init(path, lang); //Init the Tess with the trained data file, with english language

        // Set the Receipt image
        tessBaseAPI.setImage(mReceiptPicture);
        // Retrieve text detected
        mRecognizedText = tessBaseAPI.getUTF8Text();

        tessBaseAPI.end();
    }
}