package kr.kinow.gpsdemo;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Vibrator;

/**
 * Created by kinow on 2015-12-23.
 */
public class RepeatViberator extends AsyncTask<String, Void, String> {
    private Context mContext = null;
    private Vibrator mVibrator = null;

    public RepeatViberator(Context context) {
        mContext = context;
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    protected String doInBackground(String... params) {
        long millisecON = 100;
        long millisecOFF = 100;
        int time = 1;

        long [] vPattern = {millisecON, millisecOFF};

        while (true) {
            mVibrator.vibrate(vPattern, -1);
            try {
                if (this.isCancelled()) {
                    break;
                }
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.cancel(true);
        mVibrator.cancel();
        return "";
    }
}
