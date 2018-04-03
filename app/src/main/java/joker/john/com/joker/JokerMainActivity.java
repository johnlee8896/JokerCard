package joker.john.com.joker;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.widget.Toast;

import joker.john.com.joker.view.MenuView;

//import joker.john.com.joker.fromddz.MenuView;


/**
 * Created by liweifeng on 14/03/2018.
 */

public class JokerMainActivity extends AppCompatActivity  {
    public static int SCREEN_WIDTH;
    public static int SCREEN_HEIGHT;
    private OnMenuSelectListener onMenuSelectListener = new OnMenuSelectListener() {
        @Override
        public void onSelect(int index) {
//            Toast.makeText(JokerMainActivity.this, "you click" + index, Toast.LENGTH_LONG).show();
            switch (index) {
                case 0:
                    gotoGame();
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    break;
                case 5:
                    break;
                case 6:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(new MenuView(JokerMainActivity.this));
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        SCREEN_WIDTH = dm.widthPixels;
        SCREEN_HEIGHT = dm.heightPixels;
//        setContentView(new MenuView(JokerMainActivity.this));
        MenuView menuView = new MenuView(JokerMainActivity.this);
        setContentView(menuView);
        menuView.setOnMenuSelectListener(onMenuSelectListener);
    }

//    @Override
//    public void onSelect(int index) {
//
//    }

    private void gotoGame() {
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }
}
