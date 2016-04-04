package net.jongrakko.sensortech;

import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;

/**
 * Created by MoonJongRak on 2016. 3. 20..
 */
public class SensorTechApplication extends Application {

    private Thread.UncaughtExceptionHandler mUncaughtExceptionHandler;

    @Override
    public void onCreate() {
        mUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandlerApplication());
        super.onCreate();
    }

    private class UncaughtExceptionHandlerApplication implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {

            //예외상황이 발행 되는 경우 작업
            File log = new File(Environment.getExternalStorageDirectory(), "SENSOR_TECH.log");
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(log, true));
                writer.newLine();
                writer.write(new SimpleDateFormat("yyyy년MM월dd일 HH시mm분ss초 로그입니다.").format(System.currentTimeMillis()));
                writer.newLine();
                writer.write(getStackTrace(ex));
                writer.newLine();
                writer.write("============================================");
                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            //예외처리를 하지 않고 DefaultUncaughtException으로 넘긴다.
            mUncaughtExceptionHandler.uncaughtException(thread, ex);
        }

        private String getStackTrace(Throwable th) {

            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);

            Throwable cause = th;
            while (cause != null) {
                cause.printStackTrace(printWriter);
                cause = cause.getCause();
            }
            final String stacktraceAsString = result.toString();
            printWriter.close();

            return stacktraceAsString;
        }

    }
}
