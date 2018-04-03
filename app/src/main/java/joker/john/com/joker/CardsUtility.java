package joker.john.com.joker;

import android.content.Context;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import joker.john.com.joker.fromddz.CardImage;

/**
 * Created by liweifeng on 16/03/2018.
 */

public class CardsUtility {

    public static Random rand = new Random();
    public static CardColor colorMain = CardColor.Spade;
    public static int liangzhuIndex = -1;
    public static int pubMainValue = 5;
    static Comparator c = new Comparator<Card>() {

        @Override
        public int compare(Card o1, Card o2) {
//                if (o1.getValue() < o2.getValue()){//from large to small
            if (o1.getValue() == 15 || o2.getValue() == 15){
                if (o1.getValue() == o2.getValue()){
                    return 0;
                }else if (o1.getValue() == 15){
                    return 1;
                }else {
                    return -1;
                }
            }

            if (o1.getValue() == 14 || o2.getValue() == 14){
                if (o1.getValue() == o2.getValue()){
                    return 0;
                }else if (o1.getValue() == 14){
                    return 1;
                }else {
                    return -1;
                }
            }
            if (o1.getValue() == pubMainValue || o2.getValue() == pubMainValue){
                if (o1.getValue() == o2.getValue()){
                    if (o1.getColor() == colorMain && o2.getColor() == colorMain){
                        return 0;
                    }else if (o1.getColor() == colorMain){
                        return 1;
                    }else {
                        return -1;
                    }

                }else if (o1.getValue() == pubMainValue){
                    return 1;
                }else {
                    return -1;
                }
            }
            if (o1.getValue() == 1 || o2.getValue() == 1) {
                if (o1.getValue() == o2.getValue()) {
                    return 0;
                } else if (o1.getValue() == 1) {
                    return 1;
                } else {
                    return -1;
                }
            }
            //1 fronter bigger 0 equal
            if (o1.getValue() > o2.getValue()) {
                return 1;
            }
            return -1;
        }
    };

    // 洗牌，cards[0]~cards[53]表示54张牌:3333444455556666...KKKKAAAA2222小王大王
    public static void shuffle(int[] cards) {
        int len = cards.length;
        // 对于54张牌中的任何一张，都随机找一张和它互换，将牌顺序打乱。
        for (int l = 0; l < len; l++) {
            int des = rand.nextInt(54 * 2);//two cards
            int temp = cards[l];
            cards[l] = cards[des];
            cards[des] = temp;
        }
    }


//    public static void shuffle() {
//        // 定义变量
//        ArrayList list = new ArrayList();
//        Random random = new Random();
//        int i = 0;
//
//        // 向数组中增加数字
//        for (i = 0; i < 54; i++) {
//            list.add(poker.poker(i));
//        }
//
//        // 随机输出数组中的每个数字
//        int length = list.size();
//        for (i = 0; i < length; i++) {
//            //随机生成数组下标
//            int num = random.nextInt(list.size());
//            //取出数字
//            System.out.println(list.get(num));
//            //将数字从数组中移除
//            list.remove(num);
//        }
//    }

    public static ArrayList<Card> getSortedCardList() {
        ArrayList<Card> cardList = new ArrayList<>();
//        for (int i = 1; i < 15; i++){
//            for
//            Card card = new Card();
//            card.setValue(i);
//        }
        int[][] cardImages = CardImage.cardImages;
        for (int i = 0; i < cardImages.length - 1; i++) {
            for (int j = 0; j < cardImages[i].length; j++) {
                Card card = new Card();
//                card.setValue(j + 1);
                card.setValue(i + 1);
//                card.setImage(BitmapFactory.decodeResource(App.g));
                card.setImage(cardImages[i][j]);
                int colorIndex = j % 4;
                switch (colorIndex) {
                    case 0:
                        card.setColor(CardColor.Spade);
                        break;
                    case 1:
                        card.setColor(CardColor.Heart);
                        break;
                    case 2:
                        card.setColor(CardColor.Club);
                        break;
                    case 3:
                        card.setColor(CardColor.Diamond);
                        break;
                }
                cardList.add(card);
            }
        }
        Card card = new Card();
        card.setImage(cardImages[13][0]);
        card.setValue(14);
        card.setColor(CardColor.SmallJoker);
        cardList.add(card);

        Card card1 = new Card();
        card1.setImage(cardImages[13][1]);
        card1.setValue(15);
        card1.setColor(CardColor.BigJoker);
        cardList.add(card1);

//        cardList.clone();
        ArrayList<Card> anotherList = new ArrayList<>();
        anotherList.addAll(cardList);
        anotherList.addAll(cardList);
        return anotherList;
    }

    public static ArrayList<Card> getRandomSortedCardList(ArrayList<Card> originalList) {
        ArrayList<Card> cardList = new ArrayList<>();
        //attentaion when i = 54,not execute any more
        int length = originalList.size();
        for (int i = 0; i < length; i++) {
            int num = rand.nextInt(originalList.size());
            cardList.add(originalList.get(num));
            originalList.remove(num);
        }

        int[] a = new int[4];
        return cardList;
    }

    public static ArrayList<ArrayList<Card>> getFourCardList(ArrayList<Card> originalList) {
        ArrayList<ArrayList<Card>> allCardList = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ArrayList<Card> cardList = new ArrayList<>();
            allCardList.add(cardList);
        }
//        ArrayList<Card>[] allCardList;
//        allCardList = new ArrayList<Card>;
//        int length = originalList.size();
        int length = originalList.size() - 8;

        for (int i = 0; i < length; i++) {
            int num = rand.nextInt(originalList.size());

            allCardList.get(i % 4).add(originalList.get(num));
            originalList.remove(num);
        }
        return allCardList;
    }

    public static ArrayList<ArrayList<Card>> getFourCardListCardByCard(ArrayList<Card> originalList,int mainValue) {
        ArrayList<ArrayList<Card>> allCardList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ArrayList<Card> cardList = new ArrayList<>();
            allCardList.add(cardList);
        }
        int length = originalList.size() - 8;
        for (int i = 0; i < length; i++) {
            int num = rand.nextInt(originalList.size());
            allCardList.get(i % 4).add(originalList.get(num));
            parseEachListAndGetLiangZhu(true,mainValue,allCardList);
            originalList.remove(num);
        }
        allCardList.get(4).addAll(originalList);//保留底牌
        return allCardList;
    }

    public static ArrayList<Card> getSortedEachCardList(ArrayList<Card> originalList) {
        ArrayList<Card> list = new ArrayList<>();
        ArrayList<ArrayList<Card>> allList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {//include the joker
            ArrayList<Card> newList = new ArrayList<>();
            allList.add(newList);
        }
        int length = originalList.size();
        for (int i = 0; i < length; i++) {
            if (originalList.get(i).getValue() == pubMainValue || originalList.get(i).getValue() == 14 || originalList.get(i).getValue() == 15){
                allList.get(0).add(originalList.get(i));
            }else{

                if (originalList.get(i).getColor() == CardColor.Spade) {
                    allList.get(0).add(originalList.get(i));
                } else if (originalList.get(i).getColor() == CardColor.Heart) {
                    allList.get(1).add(originalList.get(i));
                } else if (originalList.get(i).getColor() == CardColor.Club) {
                    allList.get(2).add(originalList.get(i));
                } else if (originalList.get(i).getColor() == CardColor.Diamond) {
                    allList.get(3).add(originalList.get(i));
                } else {//joker
                    allList.get(4).add(originalList.get(i));
                }
            }


        }
        for (int i = 0; i < allList.size(); i++) {
            Collections.sort(allList.get(i), c);
        }
        for (int i = 0; i < allList.size(); i++) {
            for (int j = 0; j < allList.get(i).size(); j++) {
                list.add(allList.get(i).get(j));
            }
        }
        return list;

    }

    private static void parseEachListAndGetLiangZhu(boolean isFirst, int mainValue, ArrayList<ArrayList<Card>> allList) {
        ArrayList<ArrayList<Card>> liangzhuRelateList = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ArrayList<Card> list = new ArrayList<>();
            liangzhuRelateList.add(list);
        }

        int length = allList.get(0).size();
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < 4; j++) {
                Card card = allList.get(j).get(i);
                if (card.getValue() == mainValue || card.getValue() == 14 || card.getValue() == 15) {
                    allList.get(j).add(card);
//                    if (isFirst) {
                    if (liangzhuIndex == -1){
                        if (allList.get(j).size() > 1) {
                            if (judgeCanLiangzhu(allList.get(j), mainValue)) {
                                liangzhuIndex = j;
                            }
                        }
                    }
//                    }
                }
            }
        }
    }

    private static boolean judgeCanLiangzhu(ArrayList<Card> cards, int mainValue) {
        if (getJokerList(cards).size() == 0 || getMainValueList(cards, mainValue).size() == 0) {
            return false;
        }
        if (getJokerList(cards).size() > 0) {
            if (getSmallJokerList(cards).size() > 0) {
                if (getBlackMainValueList(cards, mainValue).size() > 0) {
                    colorMain = cards.get(0).getColor();
                    return true;
                }
            }
            if (getBigJokerList(cards).size() > 0) {
                if (getRedMainValueList(cards, mainValue).size() > 0) {
                    colorMain = cards.get(0).getColor();
                    return true;
                }
            }
        }
        return false;
    }

    private static ArrayList<Card> getJokerList(ArrayList<Card> cards) {
        ArrayList<Card> list = new ArrayList<>();
        for (Card card : cards) {
            if (card.getValue() == 14 || card.getValue() == 15) {
                list.add(card);
            }
        }
        return list;
    }

    private static ArrayList<Card> getSmallJokerList(ArrayList<Card> cards) {
        ArrayList<Card> list = new ArrayList<>();
        for (Card card : cards) {
            if (card.getValue() == 14) {
                list.add(card);
            }
        }
        return list;
    }

    private static ArrayList<Card> getBigJokerList(ArrayList<Card> cards) {
        ArrayList<Card> list = new ArrayList<>();
        for (Card card : cards) {
            if (card.getValue() == 15) {
                list.add(card);
            }
        }
        return list;
    }

    private static ArrayList<Card> getMainValueList(ArrayList<Card> cards, int mainValue) {
        ArrayList<Card> list = new ArrayList<>();
        for (Card card : cards) {
            if (card.getValue() == mainValue) {
                list.add(card);
            }
        }
        return list;
    }

    private static ArrayList<Card> getBlackMainValueList(ArrayList<Card> cards, int mainValue) {
        ArrayList<Card> list = new ArrayList<>();
        for (Card card : cards) {
            if (card.getValue() == mainValue) {
                if (card.getColor() == CardColor.Spade || card.getColor() == CardColor.Club) {
                    list.add(card);
                }
            }
        }
        return list;
    }

    private static ArrayList<Card> getRedMainValueList(ArrayList<Card> cards, int mainValue) {
        ArrayList<Card> list = new ArrayList<>();
        for (Card card : cards) {
            if (card.getValue() == mainValue) {
                if (card.getColor() == CardColor.Heart || card.getColor() == CardColor.Diamond) {
                    list.add(card);
                }
            }
        }
        return list;
    }


//    private static ArrayList<Card> sortCardList(ArrayList<Card> originalList){
//
////        ArrayList<Card> resultList = new ArrayList<>();
////        Collections.shuffle();
//        Collections.sort(originalList,c);
//        return resultList;
//
//    }

    public static void showToast(Context context,String message){
        Toast.makeText(context,message,Toast.LENGTH_LONG).show();
    }


}
