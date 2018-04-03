package joker.john.com.joker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by liweifeng on 01/03/2018.
 */

public class HandWriteJavaSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    MyThread thread;
    private SurfaceHolder holder;
//    private boolean isDrawing;
    //    private Thread thread;
    private Canvas canvas;
    private int x, y;
    private Paint mPaint;

    public HandWriteJavaSurfaceView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
//        x =30;
//        y =30;
//        canvas = new Canvas();
//        isDrawing = true;
//        thread = new Thread(this);
//        thread.start();
        thread = new MyThread(holder);
    }


//    public HandWriteJavaSurfaceView(Context context, AttributeSet attrs) {
//        super(context, attrs);
//    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        System.out.print("surfaceCreated====================================================");
////        holder = getHolder();
////        holder.addCallback(this);
////        x =30;
////        y =30;
////        canvas = new Canvas();
//        isDrawing = true;
////        thread = new Thread(this);
////        thread.start();
//        MyThread thread = new MyThread(holder,isDrawing);
        thread.isRun = true;
        thread.start();

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
//        isDrawing = false;
        thread.isRun = false;

    }

//    public void run() {
//        mPaint = new Paint();
//        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
//        mPaint.setStrokeWidth(10f);
//        mPaint.setColor(Color.parseColor("#FF4081"));
//        mPaint.setStyle(Paint.Style.STROKE);
//        mPaint.setStrokeJoin(Paint.Join.ROUND);
//        mPaint.setStrokeCap(Paint.Cap.ROUND);
//        while (isDrawing) {
//            try {
//                canvas = holder.lockCanvas();
//                canvas.drawRect(x, y, x + 30, y + 30, mPaint);
//                x += 5;
//                y += 5;
//                if (x > 480) {
//                    isDrawing = false;
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                holder.unlockCanvasAndPost(canvas);
//            }
//        }
//    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                break;
//            case MotionEvent.ACTION_UP:
//                break;
//            case MotionEvent.ACTION_MOVE:
//                break;
//        }
//        return super.onTouchEvent(event);
//    }

//    @Override
//    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//    }

    class MyThread extends Thread {
        public boolean isRun;
        private SurfaceHolder holder;

        public MyThread(SurfaceHolder holder) {
            this.holder = holder;
        }

        @Override
        public void run() {
            int count = 0;
            while (isRun) {
                Canvas c = null;
                try {
                    synchronized (holder) {
                        c = holder.lockCanvas();//锁定画布，一般在锁定后就可以通过其返回的画布对象Canvas，在其上面画图等操作了。
                        c.drawColor(Color.BLACK);//设置画布背景颜色
                        Paint p = new Paint(); //创建画笔
                        p.setColor(Color.WHITE);
                        p.setTextSize(100);
                        Rect r = new Rect(100, 50, 1000, 250);
                        c.drawRect(r, p);
                        c.drawText("这是第" + (count++) + "秒", 300, 400, p);
                        Thread.sleep(1000);//睡眠时间为1秒
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (c != null) {
                        holder.unlockCanvasAndPost(c);//结束锁定画图，并提交改变。
                    }
                }
            }
        }
    }
}

