package joker.john.com.joker.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import joker.john.com.joker.JokerMainActivity;
import joker.john.com.joker.OnMenuSelectListener;
import joker.john.com.joker.R;
import joker.john.com.joker.fromddz.CardsManager;

/**
 * Created by liweifeng on 14/03/2018.
 */

public class MenuView extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener {
    //    private ArrayList<Bitmap> menus;
    private Bitmap[] menus;
    private SurfaceHolder surfaceHolder;
    private boolean isDraw;
    private Bitmap backgroudBitmap;
    private OnMenuSelectListener onMenuSelectListener;
    //    private Paint paint;
    private Canvas canvas;
    private Thread drawThread = new Thread() {
        @Override
        public void run() {
//            super.run();
            while (isDraw) {

                try {
                    canvas = surfaceHolder.lockCanvas();
                    synchronized (this) {
                        startDraw();
                    }

                } catch (Exception e) {

                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
                //            startDraw();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    };

    public MenuView(Context context) {
        super(context);
        backgroudBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.menu_bg);
        menus = new Bitmap[]{
                BitmapFactory.decodeResource(getResources(), R.drawable.menu1),
                BitmapFactory.decodeResource(getResources(), R.drawable.menu2),
                BitmapFactory.decodeResource(getResources(), R.drawable.menu3),
                BitmapFactory.decodeResource(getResources(), R.drawable.menu4),
                BitmapFactory.decodeResource(getResources(), R.drawable.menu5),
                BitmapFactory.decodeResource(getResources(), R.drawable.menu6),
                BitmapFactory.decodeResource(getResources(), R.drawable.menu7),
        };
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        setOnTouchListener(this);
//        paint = new Paint();
    }

    private void startDraw() {
//        src.set(0, 0, background.getWidth(), background.getHeight());
//        // System.out.println("menu:" + background.getWidth() + "X" +
//        // background.getHeight());
//        des.set(0, 0, MainActivity.SCREEN_WIDTH, MainActivity.SCREEN_HEIGHT);
//        canvas.drawBitmap(background, src, des, paint);
//        for (int i = 0; i < menuItems.length; i++) {
//            canvas.drawBitmap(menuItems[i], (int) (x * MainActivity.SCALE_HORIAONTAL),
//                    (int) ((y + i * 43) * MainActivity.SCALE_VERTICAL), paint);
//        }
//        while (isDraw){


        Rect src = new Rect();
        Rect des = new Rect();
        src.set(0, 0, backgroudBitmap.getWidth(), backgroudBitmap.getHeight());
        des.set(0, 0, JokerMainActivity.SCREEN_WIDTH, JokerMainActivity.SCREEN_HEIGHT);
        Paint paint = new Paint();
        canvas.drawBitmap(backgroudBitmap, 0, 0, paint);
        int i = 0;
        for (Bitmap bitmap : menus) {
//               try {
            canvas.drawBitmap(bitmap, JokerMainActivity.SCREEN_WIDTH - 50 - bitmap.getWidth(), 40 + 80 * i, paint);
            i++;
//               }catch (Exception e){
//
//               }finally {
//
//               }
        }
//        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isDraw = true;
        if (drawThread != null && !drawThread.isAlive()) {

            drawThread.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isDraw = false;
        boolean retry = true;
        while (retry) {// 循环
            try {
                drawThread.join();// 等待线程结束
                retry = false;// 停止循环
            } catch (InterruptedException e) {
            }// 不断地循环，直到刷帧线程结束
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int eventX = (int) event.getX();
        int eventY = (int) event.getY();
        int index = -1;
        for (int i = 0; i < menus.length; i++) {
            if (CardsManager.inRect(eventX, eventY,
                    JokerMainActivity.SCREEN_WIDTH - 50 - menus[i].getWidth(), 40 + 80 * i, menus[i].getWidth(), menus[i].getHeight()
            )) {
                index = i;
                break;
            }
        }

        if (onMenuSelectListener != null) {
            onMenuSelectListener.onSelect(index);
        }


        return super.onTouchEvent(event);
    }

    public void setOnMenuSelectListener(OnMenuSelectListener onMenuSelectListener) {
        this.onMenuSelectListener = onMenuSelectListener;
    }
}
