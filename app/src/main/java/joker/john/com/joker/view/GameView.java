package joker.john.com.joker.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;

import java.util.ArrayList;

import joker.john.com.joker.Card;
import joker.john.com.joker.CardsUtility;
import joker.john.com.joker.JokerMainActivity;
import joker.john.com.joker.R;
import joker.john.com.joker.fromddz.CardsManager;

/**
 * Created by liweifeng on 15/03/2018.
 */

public class GameView extends SurfaceView implements SurfaceHolder.Callback, OnTouchListener {
    private ArrayList<Card> cards;
    private SurfaceHolder holder;
    private Canvas canvas;
    private boolean isDraw;
    private Bitmap bgBitmap;
    private Bitmap redrawBitMap, leftBitMap, frontBitMap, rightBitMap, chupaiDownBitMap, chupaiUpBitMap;
    private Paint paint;
    private ArrayList<Card> leftCardList, frontCardList, rightCardList;
    private ArrayList<Integer> clickedIndexList;
    private Thread fapaiThread = new Thread() {
        @Override
        public void run() {
            while (isDraw) {
                fapai();
            }
        }
    };

    public GameView(Context context) {
        super(context);
        init();

    }

    private void init() {
        leftCardList = new ArrayList<>();
        frontCardList = new ArrayList<>();
        rightCardList = new ArrayList<>();
        clickedIndexList = new ArrayList<>();

        cards = CardsUtility.getSortedCardList();
        ArrayList<ArrayList<Card>> fourCardList = CardsUtility.getFourCardList(cards);
//        leftCardList = CardsUtility.getFourCardList(cards).get(0);
//        frontCardList = CardsUtility.getFourCardList(cards).get(1);
//        rightCardList = CardsUtility.getFourCardList(cards).get(2);  cards is remove
        leftCardList = fourCardList.get(0);
        frontCardList = fourCardList.get(1);
        rightCardList = fourCardList.get(2);
        cards = fourCardList.get(3);

//        cards = CardsUtility.getRandomSortedCardList(cards);
//        cards = CardsUtility.getFourCardList(cards).get(3);
        cards = CardsUtility.getSortedEachCardList(cards);

        leftCardList = CardsUtility.getSortedEachCardList(leftCardList);
        frontCardList = CardsUtility.getSortedEachCardList(frontCardList);
        rightCardList = CardsUtility.getSortedEachCardList(rightCardList);
        paint = new Paint();
        bgBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.game_bg);
        redrawBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.icon_farmer);
        leftBitMap = BitmapFactory.decodeResource(getResources(), R.mipmap.maleface1);
        frontBitMap = BitmapFactory.decodeResource(getResources(), R.mipmap.femaleface1);
        rightBitMap = BitmapFactory.decodeResource(getResources(), R.mipmap.femaleface3);
        chupaiDownBitMap = BitmapFactory.decodeResource(getResources(), R.mipmap.button_chupai_down);
        chupaiUpBitMap = BitmapFactory.decodeResource(getResources(), R.mipmap.button_chupai);

        holder = getHolder();
        holder.addCallback(this);
        setOnTouchListener(this);
    }

    private void fapai() {
        ArrayList<Card> realCardList = new ArrayList<>();
//                canvas = holder.lockCanvas();//放这里没有一张一张的动画
        for (Card card : cards) {
            try {
                canvas = holder.lockCanvas();
                realCardList.add(card);
                drawPaiOnce(realCardList, canvas, new ArrayList<Integer>());
//                i++;
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                holder.unlockCanvasAndPost(canvas);
            }
        }
//                holder.unlockCanvasAndPost(canvas);
        isDraw = false;
    }

    private void drawPaiOnce(ArrayList<Card> cardList, Canvas canvas, ArrayList<Integer> cardIndexList) {
        int i = 0;
        int j = 0;
        int tempIndex = 0;
        int cardIndex = -1;

        if (cardIndexList.size() > 0) {
            cardIndex = cardIndexList.get(0);
        }

        try {
            Rect src = new Rect();
            Rect des = new Rect();
            src.set(0, 0, bgBitmap.getWidth(), bgBitmap.getHeight());
            des.set(0, 0, JokerMainActivity.SCREEN_WIDTH, JokerMainActivity.SCREEN_HEIGHT);
            canvas.drawBitmap(bgBitmap, src, des, null);
            for (Card card : cardList) {
                Bitmap cardBitMap = BitmapFactory.decodeResource(getResources(), card.getImage());
                int tempHeight = JokerMainActivity.SCREEN_HEIGHT / 2 + 80;
                if (70 * i > JokerMainActivity.SCREEN_WIDTH - cardBitMap.getWidth()) {
                    i = 0;
                    j = j + 1;
                    boolean flag = false;

                    if (cardIndex >= 0 && cardIndex == tempIndex) {
                        canvas.drawBitmap(cardBitMap, i * 70, tempHeight + 100 * j + 20, paint);
                        flag = true;
                        break;
                    } else {
                        canvas.drawBitmap(cardBitMap, i * 70, tempHeight + 100 * j + 50, paint);
                    }

//                    for (int n = 0; n < cardIndexList.size(); n++) {
//                        int cardIndex = cardIndexList.get(n);
//                        if (cardIndex >= 0 && cardIndex == tempIndex) {
//                            canvas.drawBitmap(cardBitMap, i * 70, tempHeight + 100 * j + 20, paint);
//                            flag = true;
//                            break;
//                        } else {
////                            canvas.drawBitmap(cardBitMap, i * 70, tempHeight + 100 * j + 50, paint);
//                        }
//                    }
//                    if (!flag) {
//                        canvas.drawBitmap(cardBitMap, i * 70, tempHeight + 100 * j + 50, paint);
//                    }
                } else {

                    if (cardIndex >= 0 && cardIndex == tempIndex) {
                        canvas.drawBitmap(cardBitMap, i * 70, tempHeight + 100 * j + 20, paint);
                        break;
                    } else {
                        canvas.drawBitmap(cardBitMap, i * 70, tempHeight + 100 * j + 50, paint);
                    }
//                    boolean flag = false;
//                    for (int n = 0; n < cardIndexList.size(); n++) {
//                        int cardIndex = cardIndexList.get(n);
//                        if (cardIndex >= 0 && cardIndex == tempIndex) {
//                            canvas.drawBitmap(cardBitMap, i * 70, tempHeight + 100 * j + 20, paint);
//                            flag = true;
//                            break;
//                        } else {
//                        }
//                    }
//                    if (!flag) {
//                        canvas.drawBitmap(cardBitMap, i * 70, tempHeight + 100 * j + 50, paint);
//                    }

                }
                i++;
                tempIndex++;
            }

//            canvas.drawBitmap(redrawBitMap,300,300,paint);
            canvas.drawBitmap(leftBitMap, 5, JokerMainActivity.SCREEN_HEIGHT / 2, paint);
            canvas.drawBitmap(frontBitMap, JokerMainActivity.SCREEN_WIDTH / 2, 5, paint);
            canvas.drawBitmap(rightBitMap, JokerMainActivity.SCREEN_WIDTH - 5 - rightBitMap.getWidth(),
                    JokerMainActivity.SCREEN_HEIGHT / 2, paint);
            Paint textPaint = new Paint();
            textPaint.setColor(Color.RED);
            textPaint.setTextSize(30);
            canvas.drawText("这是第一局\n赢0局\n失败0局", 50, 50, textPaint);
            canvas.drawText("亮主 5", JokerMainActivity.SCREEN_WIDTH - 150, 50, textPaint);
            canvas.drawText("庄家 上", JokerMainActivity.SCREEN_WIDTH - 150, 50 + 50, textPaint);
            canvas.drawText("得分 30", JokerMainActivity.SCREEN_WIDTH - 150, 50 + 100, textPaint);
            canvas.drawBitmap(chupaiDownBitMap, JokerMainActivity.SCREEN_WIDTH / 2,
                    JokerMainActivity.SCREEN_HEIGHT / 2 - 100, paint);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        finally {
////            holder.unlockCanvasAndPost(canvas);//
//        }


    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isDraw = true;
        fapaiThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isDraw = false;
        boolean retry = true;
        while (retry) {
            try {
                fapaiThread.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int eventX = (int) event.getX();
        int eventY = (int) event.getY();
        int cardIndex = -1;
        Bitmap cardBitMap = BitmapFactory.decodeResource(getResources(), cards.get(0).getImage());
        int tempIndex = 0;
        //他们的宽度间隔是70，而不是卡片的宽度
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < cards.size(); j++) {

                if (CardsManager.inRect(eventX, eventY, 70 * j, 100 * i + 50 + JokerMainActivity.SCREEN_HEIGHT / 2 + 80,
                        70, 100)) {
                    cardIndex = tempIndex;
                    break;
                }
                tempIndex++;
            }
        }
        if (cardIndex >= 0) {
            if (clickedIndexList.indexOf(cardIndex) == -1) {
                clickedIndexList.add(cardIndex);
            }
            System.out.print("====================== value = " + cards.get(cardIndex).getValue() + "color = " + cards.get(cardIndex).getColor());
            CardsUtility.showToast(getContext(), "you click value " + cards.get(cardIndex).getValue() + " index = " + cardIndex);
//            drawPaiOnce(cards, canvas, cardIndex);
//            if (holder )

//            holder = getHolder();
//            canvas = holder.lockCanvas();
            drawPaiOnce(cards, canvas, clickedIndexList);
//            canvas.drawBitmap(leftBitMap, 500, 300, paint);
//            canvas.drawBitmap(chupaiDownBitMap);
//            holder.unlockCanvasAndPost(canvas);//注释掉这行才可用
        }

        if (CardsManager.inRect(eventX, eventY, JokerMainActivity.SCREEN_WIDTH / 2,
                JokerMainActivity.SCREEN_HEIGHT / 2 - 100, chupaiDownBitMap.getWidth(), chupaiDownBitMap.getHeight())) {
            //chupai
            handleChuPai(cards, clickedIndexList);
        }
        return super.onTouchEvent(event);
    }

    private void handleChuPai(ArrayList<Card> cards, ArrayList<Integer> clickedIndexList) {
        for (Integer number : clickedIndexList) {
            cards.remove(number);
        }
        canvas = holder.lockCanvas();
        drawPaiOnce(cards, canvas, new ArrayList<Integer>());
    }
}
