package joker.john.com.joker.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import joker.john.com.joker.Card;
import joker.john.com.joker.CardColor;
import joker.john.com.joker.CardsUtility;
import joker.john.com.joker.R;
import joker.john.com.joker.game.GamePhase;
import joker.john.com.joker.game.PlayedHand;
import joker.john.com.joker.game.PlayedTrick;
import joker.john.com.joker.game.RuleConfig;
import joker.john.com.joker.game.SimpleShengJiGame;

public class GameView extends View {
    private static final int SETTING_ROW_COUNT = 15;
    private static final int MAX_HAND_ROWS = 2;

    private final SparseArray<Bitmap> cardBitmapCache = new SparseArray<>();
    private final ArrayList<Integer> selectedIndexes = new ArrayList<>();
    private final ArrayList<Rect> handCardRects = new ArrayList<>();
    private final Rect playButtonRect = new Rect();
    private final Rect restartButtonRect = new Rect();
    private final Rect undoButtonRect = new Rect();
    private final Rect hintButtonRect = new Rect();
    private final Rect historyButtonRect = new Rect();
    private final Rect settingsButtonRect = new Rect();
    private final Rect menuButtonRect = new Rect();
    private final Rect revealNoticeRect = new Rect();
    private final Rect revealNoticeConfirmRect = new Rect();
    private final Rect overlayCloseRect = new Rect();
    private final Rect overlaySaveRect = new Rect();
    private final Rect overlayCancelRect = new Rect();
    private final Rect[] settingRowRects = new Rect[SETTING_ROW_COUNT];

    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonAltPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedGroupPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint infoPanelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint infoErrorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint phasePanelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subtleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Bitmap backgroundBitmap;
    private final Bitmap leftAvatarBitmap;
    private final Bitmap topAvatarBitmap;
    private final Bitmap rightAvatarBitmap;
    private final Bitmap playButtonBitmap;
    private final Bitmap restartButtonBitmap;
    private final Bitmap cardBackBitmap;
    private final Bitmap sampleCardBitmap;

    private RuleConfig pendingRuleConfig;
    private RuleConfig editingRuleConfig;
    private SimpleShengJiGame game;
    private boolean showHistoryPanel;
    private boolean showSettingsPanel;
    private boolean showSettlementPanel;
    private boolean showRestartConfirmPanel;
    private boolean settlementPanelShownForRound;
    private String selectionFeedbackMessage = "";
    private boolean selectionFeedbackIsError;
    private float historyScrollOffset;
    private float settingsScrollOffset;
    private float overlayTouchLastY;
    private boolean overlayDragging;
    private boolean toolMenuExpanded;
    private int dealVisibleCount;
    private boolean dealAnimating;
    private boolean showRevealNotice;
    private String revealNoticeSummary = "";
    private final ArrayList<Card> revealNoticeCards = new ArrayList<>();

    private final Runnable dealAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            if (!dealAnimating) {
                return;
            }
            dealVisibleCount++;
            if (dealVisibleCount >= game.getHumanHand().size()) {
                dealVisibleCount = game.getHumanHand().size();
                dealAnimating = false;
                invalidate();
                scheduleNextAction();
                return;
            }
            invalidate();
            postDelayed(this, 70);
        }
    };

    private final Runnable aiTurnRunnable = new Runnable() {
        @Override
        public void run() {
            if (game.isRoundFinished() || game.hasPendingTrickResolution() || showAnyOverlay()) {
                return;
            }
            if (game.getPhase() != GamePhase.REVEAL_TRUMP && game.isHumanTurn()) {
                return;
            }
            game.playNextAiTurn();
            syncRevealNoticeFromGame();
            invalidate();
            scheduleNextAction();
        }
    };

    private final Runnable nextTrickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!game.hasPendingTrickResolution() || game.isRoundFinished() || showAnyOverlay()) {
                return;
            }
            game.beginNextTrick();
            invalidate();
            scheduleNextAction();
        }
    };

    private final Runnable nextRoundRunnable = new Runnable() {
        @Override
        public void run() {
            if (!game.shouldAutoStartNextRound() || showAnyOverlay()) {
                return;
            }
            selectedIndexes.clear();
            selectionFeedbackMessage = "";
            selectionFeedbackIsError = false;
            settlementPanelShownForRound = false;
            game.startNextRound();
            beginDealAnimation();
            syncRevealNoticeFromGame();
            invalidate();
            scheduleNextAction();
        }
    };

    private final Runnable dismissSettlementRunnable = new Runnable() {
        @Override
        public void run() {
            if (!showSettlementPanel) {
                return;
            }
            showSettlementPanel = false;
            invalidate();
            postDelayed(nextRoundRunnable, 700);
        }
    };

    public GameView(Context context) {
        super(context);
        pendingRuleConfig = RuleConfig.load(context);
        game = new SimpleShengJiGame(pendingRuleConfig.copy());

        for (int i = 0; i < settingRowRects.length; i++) {
            settingRowRects[i] = new Rect();
        }

        backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.game_bg);
        leftAvatarBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.maleface1);
        topAvatarBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.femaleface1);
        rightAvatarBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.femaleface3);
        playButtonBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.button_chupai);
        restartButtonBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.btn_redo);
        cardBackBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.card_bg);
        sampleCardBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.spade_3);

        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(38f);

        bodyPaint.setColor(Color.WHITE);
        bodyPaint.setTextSize(30f);

        smallPaint.setColor(Color.WHITE);
        smallPaint.setTextSize(24f);

        panelPaint.setColor(Color.argb(150, 0, 0, 0));

        overlayPaint.setColor(Color.argb(240, 18, 35, 42));

        dimPaint.setColor(Color.argb(165, 0, 0, 0));

        buttonPaint.setColor(Color.argb(220, 206, 100, 46));
        buttonAltPaint.setColor(Color.argb(220, 47, 86, 94));

        buttonBorderPaint.setColor(Color.argb(235, 248, 221, 163));
        buttonBorderPaint.setStyle(Paint.Style.STROKE);
        buttonBorderPaint.setStrokeWidth(3f);

        selectedPaint.setColor(Color.argb(220, 255, 215, 0));
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(4f);

        selectedFillPaint.setColor(Color.argb(88, 255, 215, 0));
        selectedGroupPaint.setColor(Color.argb(108, 255, 215, 0));

        infoPanelPaint.setColor(Color.argb(195, 33, 56, 45));
        infoErrorPaint.setColor(Color.argb(210, 120, 36, 26));
        phasePanelPaint.setColor(Color.argb(210, 23, 48, 62));

        hintPaint.setColor(Color.rgb(255, 220, 120));
        hintPaint.setTextSize(24f);

        subtleTextPaint.setColor(Color.argb(220, 220, 230, 235));
        subtleTextPaint.setTextSize(20f);

        beginDealAnimation();
        syncRevealNoticeFromGame();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
        drawTopPanels(canvas);
        drawOpponents(canvas);
        drawCurrentTrick(canvas);
        drawPhaseSummary(canvas);
        drawButtons(canvas);
        drawHand(canvas);
        drawRevealNotice(canvas);
        if (showHistoryPanel) {
            drawHistoryOverlay(canvas);
        }
        if (showSettingsPanel) {
            drawSettingsOverlay(canvas);
        }
        if (showSettlementPanel) {
            drawSettlementOverlay(canvas);
        }
        if (showRestartConfirmPanel) {
            drawRestartConfirmOverlay(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        if (showSettingsPanel) {
            handleOverlayTouch(event, true);
            return true;
        }
        if (showHistoryPanel) {
            handleOverlayTouch(event, false);
            return true;
        }
        if (showSettlementPanel) {
            handleSettlementTouch(event);
            return true;
        }
        if (showRestartConfirmPanel) {
            handleRestartConfirmTouch(event);
            return true;
        }
        if (showRevealNotice && event.getAction() == MotionEvent.ACTION_UP && revealNoticeConfirmRect.contains(x, y)) {
            performClick();
            showRevealNotice = false;
            invalidate();
            return true;
        }
        if (dealAnimating) {
            return true;
        }

        if (event.getAction() != MotionEvent.ACTION_UP) {
            return true;
        }
        performClick();

        if (restartButtonRect.contains(x, y)) {
            showRestartConfirmPanel = true;
            toolMenuExpanded = false;
            invalidate();
            return true;
        }
        if (undoButtonRect.contains(x, y)) {
            handleUndoClick();
            return true;
        }
        if (playButtonRect.contains(x, y)) {
            handlePrimaryClick();
            return true;
        }
        if (hintButtonRect.contains(x, y)) {
            handleHintClick();
            return true;
        }
        if (menuButtonRect.contains(x, y)) {
            toolMenuExpanded = !toolMenuExpanded;
            invalidate();
            return true;
        }
        if (historyButtonRect.contains(x, y)) {
            showHistoryPanel = true;
            toolMenuExpanded = false;
            historyScrollOffset = 0f;
            invalidate();
            return true;
        }
        if (settingsButtonRect.contains(x, y)) {
            toolMenuExpanded = false;
            openSettingsPanel();
            return true;
        }

        if (!game.isHumanTurn()) {
            return true;
        }

        int touchedIndex = findTouchedCardIndex(x, y);
        if (touchedIndex >= 0) {
            toggleSelectedIndex(touchedIndex);
            invalidate();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(aiTurnRunnable);
        removeCallbacks(nextTrickRunnable);
        removeCallbacks(nextRoundRunnable);
        removeCallbacks(dealAnimationRunnable);
        removeCallbacks(dismissSettlementRunnable);
        super.onDetachedFromWindow();
    }

    private void handlePrimaryClick() {
        GamePhase phaseBeforeAction = game.getPhase();
        if (!game.canHumanUsePrimaryAction()) {
            String message;
            if (game.getPhase() == GamePhase.REVEAL_TRUMP) {
                message = "当前没有可亮的级牌，等待其他人亮主。";
            } else if (game.getPhase() == GamePhase.BURY_KITTY) {
                message = "当前不是你放底牌。";
            } else {
                message = "当前由电脑玩家出牌。";
            }
            CardsUtility.showToast(getContext(), message);
            setSelectionFeedback(message, true);
            return;
        }
        SimpleShengJiGame.MoveResult result = game.handleHumanPrimaryAction(selectedIndexes);
        if (!result.success) {
            CardsUtility.showToast(getContext(), result.message);
            setSelectionFeedback(result.message, true);
            return;
        }
        selectedIndexes.clear();
        if (phaseBeforeAction == GamePhase.REVEAL_TRUMP) {
            String revealMessage = game.getLastRevealSummary();
            setSelectionFeedback(revealMessage.length() > 0 ? revealMessage : "亮主成功。", false);
            syncRevealNoticeFromGame();
        } else if (phaseBeforeAction == GamePhase.BURY_KITTY) {
            setSelectionFeedback(result.message == null || result.message.length() == 0 ? "底牌已放好。" : result.message, false);
        } else {
            String successMessage = result.message == null || result.message.length() == 0
                    ? "出牌成功。"
                    : result.message;
            setSelectionFeedback(successMessage, false);
        }
        invalidate();
        scheduleNextAction();
    }

    private void handleHintClick() {
        if (game.getPhase() == GamePhase.REVEAL_TRUMP && game.canHumanUsePrimaryAction()) {
            SimpleShengJiGame.MoveResult passResult = game.passHumanReveal();
            setSelectionFeedback(passResult.success ? "这次先不抢亮，继续看其他人。" : passResult.message, !passResult.success);
            syncRevealNoticeFromGame();
            invalidate();
            scheduleNextAction();
            return;
        }
        SimpleShengJiGame.MoveSuggestion suggestion = game.getSuggestedHumanMove();
        if (!suggestion.success) {
            CardsUtility.showToast(getContext(), suggestion.message);
            setSelectionFeedback(suggestion.message, true);
            return;
        }
        selectedIndexes.clear();
        selectedIndexes.addAll(suggestion.indexes);
        setSelectionFeedback("已根据当前规则帮你选牌。", false);
        invalidate();
    }

    private void handleUndoClick() {
        SimpleShengJiGame.MoveResult result = game.undoHumanMove();
        setSelectionFeedback(result.message, !result.success);
        if (result.success) {
            selectedIndexes.clear();
        }
        invalidate();
        scheduleNextAction();
    }

    private void restartGame() {
        removeCallbacks(aiTurnRunnable);
        removeCallbacks(nextTrickRunnable);
        removeCallbacks(nextRoundRunnable);
        selectedIndexes.clear();
        game = new SimpleShengJiGame(pendingRuleConfig.copy());
        showHistoryPanel = false;
        showSettingsPanel = false;
        showSettlementPanel = false;
        showRestartConfirmPanel = false;
        settlementPanelShownForRound = false;
        toolMenuExpanded = false;
        selectionFeedbackMessage = "";
        selectionFeedbackIsError = false;
        historyScrollOffset = 0f;
        settingsScrollOffset = 0f;
        showRevealNotice = false;
        revealNoticeSummary = "";
        revealNoticeCards.clear();
        removeCallbacks(dismissSettlementRunnable);
        beginDealAnimation();
        syncRevealNoticeFromGame();
        invalidate();
        scheduleNextAction();
    }

    private void scheduleNextAction() {
        removeCallbacks(aiTurnRunnable);
        removeCallbacks(nextTrickRunnable);
        removeCallbacks(nextRoundRunnable);
        maybeAutoSelectForcedMove();
        if (dealAnimating) {
            return;
        }
        if (game.shouldAutoStartNextRound()) {
            if (!settlementPanelShownForRound) {
                settlementPanelShownForRound = true;
                showSettlementPanel = true;
                invalidate();
                removeCallbacks(dismissSettlementRunnable);
                int delaySeconds = pendingRuleConfig.getSettlementDelaySeconds();
                if (delaySeconds > 0) {
                    postDelayed(dismissSettlementRunnable, delaySeconds * 1000L);
                }
                return;
            }
            if (showSettlementPanel) {
                return;
            }
            postDelayed(nextRoundRunnable, 700);
            return;
        }
        if (showAnyOverlay()) {
            return;
        }
        if (game.hasPendingTrickResolution()) {
            postDelayed(nextTrickRunnable, 1300);
            return;
        }
        if (game.getPhase() == GamePhase.REVEAL_TRUMP) {
            if (game.canHumanUsePrimaryAction()) {
                return;
            }
            postDelayed(aiTurnRunnable, 520);
            return;
        }
        if (game.getPhase() == GamePhase.BURY_KITTY && game.canHumanUsePrimaryAction()) {
            return;
        }
        if (!game.isHumanTurn() || game.getPhase() == GamePhase.BURY_KITTY) {
            postDelayed(aiTurnRunnable, 700);
        }
    }

    private void openSettingsPanel() {
        editingRuleConfig = pendingRuleConfig.copy();
        showSettingsPanel = true;
        settingsScrollOffset = 0f;
        invalidate();
    }

    private void handleOverlayTouch(MotionEvent event, boolean settings) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            overlayTouchLastY = y;
            overlayDragging = false;
            return;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float delta = y - overlayTouchLastY;
            overlayTouchLastY = y;
            if (Math.abs(delta) > 2f) {
                overlayDragging = true;
                if (settings) {
                    settingsScrollOffset = clampSettingsScroll(settingsScrollOffset + delta);
                } else {
                    historyScrollOffset = clampHistoryScroll(historyScrollOffset + delta);
                }
                invalidate();
            }
            return;
        }
        if (event.getAction() != MotionEvent.ACTION_UP) {
            return;
        }
        performClick();
        if (overlayDragging) {
            overlayDragging = false;
            return;
        }
        if (settings) {
            handleSettingsTouch(x, y);
        } else {
            handleHistoryTouch(x, y);
        }
    }

    private void handleHistoryTouch(int x, int y) {
        if (overlayCloseRect.contains(x, y)) {
            showHistoryPanel = false;
            invalidate();
            scheduleNextAction();
            return;
        }
        if (!insideOverlayPanel(x, y)) {
            showHistoryPanel = false;
            invalidate();
            scheduleNextAction();
        }
    }

    private void handleSettingsTouch(int x, int y) {
        if (overlaySaveRect.contains(x, y)) {
            pendingRuleConfig = editingRuleConfig.copy();
            pendingRuleConfig.save(getContext());
            game.updateBaseRuleConfig(pendingRuleConfig.copy());
            showSettingsPanel = false;
            CardsUtility.showToast(getContext(), "规则已保存，将在下一局生效。");
            setSelectionFeedback("规则已保存，将在下一局生效。", false);
            invalidate();
            scheduleNextAction();
            return;
        }
        if (overlayCancelRect.contains(x, y) || overlayCloseRect.contains(x, y)) {
            showSettingsPanel = false;
            invalidate();
            scheduleNextAction();
            return;
        }

        for (int i = 0; i < settingRowRects.length; i++) {
            Rect rowRect = settingRowRects[i];
            if (rowRect.contains(x, y)) {
                boolean forward = x >= rowRect.centerX();
                cycleSetting(i, forward);
                invalidate();
                return;
            }
        }

        if (!insideOverlayPanel(x, y)) {
            showSettingsPanel = false;
            invalidate();
            scheduleNextAction();
        }
    }

    private boolean insideOverlayPanel(int x, int y) {
        int panelLeft = getWidth() / 7;
        int panelTop = getHeight() / 8;
        int panelRight = getWidth() - panelLeft;
        int panelBottom = getHeight() - panelTop;
        return x >= panelLeft && x <= panelRight && y >= panelTop && y <= panelBottom;
    }

    private boolean showAnyOverlay() {
        return showHistoryPanel || showSettingsPanel || showSettlementPanel || showRestartConfirmPanel;
    }

    private void cycleSetting(int rowIndex, boolean forward) {
        if (editingRuleConfig == null) {
            return;
        }
        if (rowIndex == 0) {
            editingRuleConfig.setRankValue(nextRankValue(editingRuleConfig.getRankValue(), forward));
            return;
        }
        if (rowIndex == 1) {
            editingRuleConfig.setTrumpSuit(nextTrumpSuit(editingRuleConfig.getTrumpSuit(), forward));
            return;
        }
        if (rowIndex == 2) {
            editingRuleConfig.setTractorEnabled(!editingRuleConfig.isTractorEnabled());
            return;
        }
        if (rowIndex == 3) {
            int history = editingRuleConfig.getHistoryLimit() + (forward ? 1 : -1);
            if (history > 10) {
                history = 3;
            } else if (history < 3) {
                history = 10;
            }
            editingRuleConfig.setHistoryLimit(history);
            return;
        }
        if (rowIndex == 4) {
            editingRuleConfig.setHintEnabled(!editingRuleConfig.isHintEnabled());
            return;
        }
        if (rowIndex == 5) {
            editingRuleConfig.setRevealStealEnabled(!editingRuleConfig.isRevealStealEnabled());
            return;
        }
        if (rowIndex == 6) {
            editingRuleConfig.setSpecialThreeCardShuaiEnabled(!editingRuleConfig.isSpecialThreeCardShuaiEnabled());
            return;
        }
        if (rowIndex == 7) {
            editingRuleConfig.setTractorCarryAceEnabled(!editingRuleConfig.isTractorCarryAceEnabled());
            return;
        }
        if (rowIndex == 8) {
            editingRuleConfig.setSpecialTopSplitEnabled(!editingRuleConfig.isSpecialTopSplitEnabled());
            return;
        }
        if (rowIndex == 9) {
            editingRuleConfig.setPublicInfoSemiShuaiEnabled(!editingRuleConfig.isPublicInfoSemiShuaiEnabled());
            return;
        }
        if (rowIndex == 10) {
            editingRuleConfig.setCounterTrumpSingleJokerPairEnabled(!editingRuleConfig.isCounterTrumpSingleJokerPairEnabled());
            return;
        }
        if (rowIndex == 11) {
            editingRuleConfig.setCounterTrumpDoubleJokerCopyEnabled(!editingRuleConfig.isCounterTrumpDoubleJokerCopyEnabled());
            return;
        }
        if (rowIndex == 12) {
            editingRuleConfig.setCounterNoTrumpEnabled(!editingRuleConfig.isCounterNoTrumpEnabled());
            return;
        }
        if (rowIndex == 13) {
            editingRuleConfig.setSettlementDelaySeconds(nextSettlementDelaySeconds(editingRuleConfig.getSettlementDelaySeconds(), forward));
            return;
        }
        editingRuleConfig.setUndoChancesPerRound(nextUndoChances(editingRuleConfig.getUndoChancesPerRound(), forward));
    }

    private int nextRankValue(int current, boolean forward) {
        int[] values = RuleConfig.AVAILABLE_RANK_VALUES;
        int currentIndex = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                currentIndex = i;
                break;
            }
        }
        int nextIndex = forward ? currentIndex + 1 : currentIndex - 1;
        if (nextIndex >= values.length) {
            nextIndex = 0;
        }
        if (nextIndex < 0) {
            nextIndex = values.length - 1;
        }
        return values[nextIndex];
    }

    private CardColor nextTrumpSuit(CardColor current, boolean forward) {
        CardColor[] suits = RuleConfig.AVAILABLE_TRUMP_SUITS;
        int currentIndex = 0;
        for (int i = 0; i < suits.length; i++) {
            if (suits[i] == current) {
                currentIndex = i;
                break;
            }
        }
        int nextIndex = forward ? currentIndex + 1 : currentIndex - 1;
        if (nextIndex >= suits.length) {
            nextIndex = 0;
        }
        if (nextIndex < 0) {
            nextIndex = suits.length - 1;
        }
        return suits[nextIndex];
    }

    private int nextSettlementDelaySeconds(int current, boolean forward) {
        int[] values = RuleConfig.AVAILABLE_SETTLEMENT_DELAY_SECONDS;
        int currentIndex = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                currentIndex = i;
                break;
            }
        }
        int nextIndex = forward ? currentIndex + 1 : currentIndex - 1;
        if (nextIndex >= values.length) {
            nextIndex = 0;
        }
        if (nextIndex < 0) {
            nextIndex = values.length - 1;
        }
        return values[nextIndex];
    }

    private int nextUndoChances(int current, boolean forward) {
        int[] values = RuleConfig.AVAILABLE_UNDO_CHANCES;
        int currentIndex = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                currentIndex = i;
                break;
            }
        }
        int nextIndex = forward ? currentIndex + 1 : currentIndex - 1;
        if (nextIndex >= values.length) {
            nextIndex = 0;
        }
        if (nextIndex < 0) {
            nextIndex = values.length - 1;
        }
        return values[nextIndex];
    }

    private void drawBackground(Canvas canvas) {
        Rect dest = new Rect(0, 0, getWidth(), getHeight());
        canvas.drawBitmap(backgroundBitmap, null, dest, null);
    }

    private void drawTopPanels(Canvas canvas) {
        int panelHeight = 84;
        drawPanel(canvas, 16, 16, getBoardRight(), 16 + panelHeight);
        String trumpText = game.getTrumpLabel();
        String dealerText = game.getDealerPlayer() >= 0 ? "庄家: " + game.getPlayerName(game.getDealerPlayer()) : "庄家: 待定";
        float totalWidth = getBoardRight() - 64f;
        canvas.drawText("双升单机", 32, 44, smallPaint);
        canvas.drawText(trimTextToWidth(trumpText + "  " + dealerText, totalWidth - 120f), 150, 44, smallPaint);
        String secondLine = "第 " + game.getRoundNumber() + " 局 / 第 " + game.getTrickNumber() + " 墩  "
                + game.getDefenderScoreLabel() + "  " + game.getNextDealerLabel() + "  " + getTopSelectionText();
        canvas.drawText(trimTextToWidth(secondLine, totalWidth), 32, 76, selectionFeedbackIsError ? bodyPaint : hintPaint);
        drawSidebar(canvas);
    }

    private void drawPhaseSummary(Canvas canvas) {
        // Phase info has been folded into the top header to free the center board area.
    }

    private void drawOpponents(Canvas canvas) {
        int avatarSize = Math.max(84, getWidth() / 14);
        int cardBackWidth = avatarSize / 2;
        int cardBackHeight = cardBackWidth * 3 / 2;
        int middleY = getMiddleBoardCenterY();

        drawPlayerBadge(canvas, leftAvatarBitmap, 24, middleY - avatarSize / 2, avatarSize,
                getAnimatedHandCount(SimpleShengJiGame.PLAYER_LEFT), cardBackWidth, cardBackHeight, "左家");

        drawPlayerBadge(canvas, topAvatarBitmap, getBoardRight() / 2 - avatarSize / 2, 164, avatarSize,
                getAnimatedHandCount(SimpleShengJiGame.PLAYER_TOP), cardBackWidth, cardBackHeight, "对家");

        drawPlayerBadge(canvas, rightAvatarBitmap, getBoardRight() - avatarSize - 112, middleY - avatarSize / 2, avatarSize,
                getAnimatedHandCount(SimpleShengJiGame.PLAYER_RIGHT), cardBackWidth, cardBackHeight, "右家");
    }

    private void drawPlayerBadge(Canvas canvas, Bitmap avatarBitmap, int left, int top, int size,
                                 int handCount, int cardBackWidth, int cardBackHeight, String title) {
        drawPanel(canvas, left - 8, top - 8, left + size + 96, top + size + 58);
        Rect avatarRect = new Rect(left, top, left + size, top + size);
        canvas.drawBitmap(avatarBitmap, null, avatarRect, null);

        int stackLeft = left + size + 8;
        int stackTop = top + 8;
        int stackCount = Math.max(1, Math.min(4, handCount / 5));
        for (int i = 0; i < stackCount; i++) {
            Rect backRect = new Rect(stackLeft + i * 6, stackTop + i * 4,
                    stackLeft + i * 6 + cardBackWidth, stackTop + i * 4 + cardBackHeight);
            canvas.drawBitmap(cardBackBitmap, null, backRect, null);
        }

        canvas.drawText(title, left, top + size + 24, smallPaint);
        canvas.drawText(handCount + " 张", left, top + size + 50, smallPaint);
    }

    private void drawCurrentTrick(Canvas canvas) {
        List<PlayedHand> trick = game.getCurrentTrick();
        if (trick.isEmpty()) {
            return;
        }

        int cardWidth = getCardWidth();
        int cardHeight = getCardHeight(cardWidth);
        int leadPlayer = trick.get(0).getPlayer();
        for (PlayedHand playedHand : trick) {
            ArrayList<Rect> slots = getTrickCardSlots(playedHand.getPlayer(), playedHand.getCards().size(), cardWidth, cardHeight);
            for (int i = 0; i < playedHand.getCards().size() && i < slots.size(); i++) {
                canvas.drawBitmap(getCardBitmap(playedHand.getCards().get(i)), null, slots.get(i), null);
            }
            if (!slots.isEmpty()) {
                Rect anchor = slots.get(0);
                String label = game.getPlayerName(playedHand.getPlayer()) + (playedHand.getPlayer() == leadPlayer ? " 先手" : "");
                canvas.drawText(label, anchor.left, anchor.top - 10, subtleTextPaint);
            }
        }
    }

    private ArrayList<Rect> getTrickCardSlots(int player, int count, int cardWidth, int cardHeight) {
        ArrayList<Rect> rects = new ArrayList<>();
        int gap = cardWidth / 4;
        int centerX = getBoardRight() / 2;
        int centerY = getMiddleBoardCenterY() + 18;

        int baseLeft;
        int baseTop;
        switch (player) {
            case SimpleShengJiGame.PLAYER_LEFT:
                baseLeft = 166;
                baseTop = centerY - cardHeight / 2;
                break;
            case SimpleShengJiGame.PLAYER_TOP:
                baseLeft = centerX - ((cardWidth * count) + (gap * (count - 1))) / 2;
                baseTop = centerY - cardHeight - 60;
                break;
            case SimpleShengJiGame.PLAYER_RIGHT:
                baseLeft = getBoardRight() - 166 - (cardWidth * count + gap * Math.max(0, count - 1));
                baseTop = centerY - cardHeight / 2;
                break;
            default:
                baseLeft = centerX - ((cardWidth * count) + (gap * (count - 1))) / 2;
                baseTop = centerY + 18;
                break;
        }

        for (int i = 0; i < count; i++) {
            rects.add(new Rect(baseLeft + i * (cardWidth + gap), baseTop,
                    baseLeft + i * (cardWidth + gap) + cardWidth, baseTop + cardHeight));
        }
        return rects;
    }

    private void drawButtons(Canvas canvas) {
        // Selection text has been folded into the top header to free more table space.
    }

    private void drawRevealNotice(Canvas canvas) {
        if (!showRevealNotice) {
            return;
        }

        int panelWidth = Math.min(getBoardRight() - 44, Math.max(520, getBoardRight() - 96));
        int left = Math.max(22, (getBoardRight() - panelWidth) / 2);
        int top = 88;
        int right = left + panelWidth;
        int bottom = top + 214;
        revealNoticeRect.set(left, top, right, bottom);

        RectF rect = new RectF(revealNoticeRect);
        canvas.drawRect(0, 0, getBoardRight(), getHeight(), dimPaint);
        canvas.drawRoundRect(new RectF(left - 10, top - 10, right + 10, bottom + 10), 24f, 24f, dimPaint);
        canvas.drawRoundRect(rect, 18f, 18f, overlayPaint);
        canvas.drawRoundRect(rect, 18f, 18f, buttonBorderPaint);

        canvas.drawText("亮主提示", left + 24, top + 40, bodyPaint);
        String summary = trimTextToWidth(revealNoticeSummary, panelWidth - 56f);
        canvas.drawText(summary, left + 24, top + 82, subtleTextPaint);
        if (!revealNoticeCards.isEmpty()) {
            String cardsText = "亮牌: " + game.describeCards(revealNoticeCards);
            canvas.drawText(trimTextToWidth(cardsText, panelWidth - 56f), left + 24, top + 126, hintPaint);
        } else {
            canvas.drawText(trimTextToWidth("亮主后会自动继续起底和出牌。", panelWidth - 56f), left + 24, top + 126, hintPaint);
        }
        canvas.drawText(trimTextToWidth("点“确定”或“知道了”关闭提示，不影响当前亮主、起底和后续出牌流程。", panelWidth - 56f), left + 24, top + 164, smallPaint);
        canvas.drawText(trimTextToWidth("这只是一个亮牌提醒，大框只负责提示，不会影响后续自动流程。", panelWidth - 56f), left + 24, top + 196, subtleTextPaint);

        revealNoticeConfirmRect.set(right - 132, bottom - 60, right - 20, bottom - 16);
        drawTextButton(canvas, revealNoticeConfirmRect, "知道了", buttonPaint);
    }

    private void drawHand(Canvas canvas) {
        ArrayList<Card> hand = game.getHumanHand();
        handCardRects.clear();
        int visibleCount = dealAnimating ? Math.min(hand.size(), dealVisibleCount) : hand.size();
        if (visibleCount <= 0) {
            return;
        }

        int cardWidth = getCardWidth();
        int cardHeight = getCardHeight(cardWidth);
        int margin = getHandSideInset();
        int rowGap = 18;
        int raiseOffset = 34;
        int rowCount = getHandRowCount(visibleCount);
        int cardsPerRow = (int) Math.ceil(visibleCount / (float) rowCount);
        int bottomRowTop = getHeight() - cardHeight - 18;
        int topMostRowTop = bottomRowTop - (rowCount - 1) * (cardHeight + rowGap);
        drawPanel(canvas, margin - 10, topMostRowTop - 14, getBoardRight() - margin + 10, bottomRowTop + cardHeight + 10);

        for (int i = 0; i < visibleCount; i++) {
            int rowIndex = i / cardsPerRow;
            int rowTop = topMostRowTop + rowIndex * (cardHeight + rowGap);
            int indexInRow = i % cardsPerRow;
            int countInRow = Math.min(cardsPerRow, visibleCount - rowIndex * cardsPerRow);
            int left = calculateCardLeft(indexInRow, countInRow, cardWidth, margin);
            int top = rowTop - (selectedIndexes.contains(i) ? raiseOffset : 0);
            Rect dst = new Rect(left, top, left + cardWidth, top + cardHeight);
            handCardRects.add(dst);
        }

        drawSelectedGroups(canvas);

        for (int i = 0; i < visibleCount; i++) {
            Rect dst = handCardRects.get(i);
            if (selectedIndexes.contains(i)) {
                canvas.drawRoundRect(new RectF(dst.left - 6, dst.top - 6, dst.right + 6, dst.bottom + 6), 14f, 14f, selectedFillPaint);
            }
            canvas.drawBitmap(getCardBitmap(hand.get(i)), null, dst, null);
            if (selectedIndexes.contains(i)) {
                canvas.drawRect(dst, selectedPaint);
            }
        }
    }

    private void drawSelectionSummary(Canvas canvas, int left, int right, int top) {
        int panelHeight = 44;
        Rect panelRect = new Rect(left, top, right, top + panelHeight);
        RectF rectF = new RectF(panelRect);
        canvas.drawRoundRect(rectF, 14f, 14f, selectionFeedbackIsError ? infoErrorPaint : infoPanelPaint);
        canvas.drawRoundRect(rectF, 14f, 14f, buttonBorderPaint);

        String countText = selectedIndexes.isEmpty()
                ? "当前未选牌"
                : "已选 " + selectedIndexes.size() + " 张";
        canvas.drawText(countText, left + 16, top + 28, smallPaint);

        String cardsText = selectionFeedbackMessage.length() > 0 ? selectionFeedbackMessage : getSelectedCardsText();
        if (cardsText.length() > 0) {
            float startX = left + 126;
            float maxWidth = right - startX - 18;
            canvas.drawText(trimTextToWidth(cardsText, maxWidth), startX, top + 28, subtleTextPaint);
        }
    }

    private void drawHistoryOverlay(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), dimPaint);
        int left = getWidth() / 7;
        int top = getHeight() / 8;
        int right = getWidth() - left;
        int bottom = getHeight() - top;
        drawOverlayPanel(canvas, left, top, right, bottom, "已出牌记录");
        overlayCloseRect.set(right - 120, top + 18, right - 24, top + 68);
        drawTextButton(canvas, overlayCloseRect, "关闭", buttonAltPaint);

        List<PlayedTrick> history = game.getCompletedTricks();
        if (history.isEmpty()) {
            canvas.drawText("本局还没有完成的牌墩。", left + 34, top + 120, bodyPaint);
            return;
        }

        int contentTop = top + 96;
        int contentBottom = bottom - 56;
        canvas.save();
        canvas.clipRect(left + 18, contentTop, right - 18, contentBottom);
        int lineY = (int) (top + 112 + historyScrollOffset);
        int shown = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            PlayedTrick trick = history.get(i);
            canvas.drawText(
                    "第 " + trick.getTrickNumber() + " 墩  " + game.getPlayerName(trick.getWinner()) + " 收 " + trick.getPoints() + " 分",
                    left + 28, lineY, bodyPaint
            );
            lineY += 30;
            for (PlayedHand hand : trick.getPlays()) {
                canvas.drawText(
                        game.getPlayerName(hand.getPlayer()) + ": " + game.describeCards(hand.getCards()),
                        left + 52, lineY, smallPaint
                );
                lineY += 28;
            }
            lineY += 20;
            shown++;
        }
        canvas.restore();
        canvas.drawText("上下拖动可滚动查看，共 " + shown + " 墩。", left + 28, bottom - 22, smallPaint);
    }

    private void drawSettingsOverlay(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), dimPaint);
        int left = getWidth() / 7;
        int top = getHeight() / 8;
        int right = getWidth() - left;
        int bottom = getHeight() - top;
        drawOverlayPanel(canvas, left, top, right, bottom, "规则设置");

        overlayCloseRect.set(right - 120, top + 18, right - 24, top + 68);
        overlaySaveRect.set(right - 260, bottom - 84, right - 116, bottom - 24);
        overlayCancelRect.set(right - 412, bottom - 84, right - 268, bottom - 24);

        drawTextButton(canvas, overlayCloseRect, "关闭", buttonAltPaint);
        drawTextButton(canvas, overlayCancelRect, "取消", buttonAltPaint);
        drawTextButton(canvas, overlaySaveRect, "保存", buttonPaint);

        int contentTop = top + 96;
        int contentBottom = bottom - 126;
        canvas.save();
        canvas.clipRect(left + 18, contentTop, right - 18, contentBottom);
        int baseY = (int) (top + 110 + settingsScrollOffset);
        canvas.drawText("点每一行左半边减少，右半边增加或切换。", left + 28, baseY, smallPaint);

        String[] labels = {
                "起始级牌",
                "默认主花色",
                "拖拉机",
                "历史条数",
                "提示",
                "允许抢亮",
                "特殊三张甩牌",
                "4466算拖拉机",
                "高牌拆分特例",
                "公开信息半甩",
                "单王带对反主",
                "双王抄底反主",
                "双王反无主",
                "结算停留",
                "撤销次数"
        };
        String[] values = {
                editingRuleConfig.getRankLabel(),
                editingRuleConfig.getTrumpSuitLabel(),
                editingRuleConfig.isTractorEnabled() ? "开启" : "关闭",
                String.valueOf(editingRuleConfig.getHistoryLimit()),
                editingRuleConfig.isHintEnabled() ? "开启" : "关闭",
                editingRuleConfig.isRevealStealEnabled() ? "开启" : "关闭",
                editingRuleConfig.isSpecialThreeCardShuaiEnabled() ? "开启" : "关闭",
                editingRuleConfig.isTractorCarryAceEnabled() ? "开启" : "关闭",
                editingRuleConfig.isSpecialTopSplitEnabled() ? "开启" : "关闭",
                editingRuleConfig.isPublicInfoSemiShuaiEnabled() ? "开启" : "关闭",
                editingRuleConfig.isCounterTrumpSingleJokerPairEnabled() ? "开启" : "关闭",
                editingRuleConfig.isCounterTrumpDoubleJokerCopyEnabled() ? "开启" : "关闭",
                editingRuleConfig.isCounterNoTrumpEnabled() ? "开启" : "关闭",
                editingRuleConfig.getSettlementDelayLabel(),
                editingRuleConfig.getUndoChancesLabel()
        };

        int rowTop = baseY + 22;
        for (int i = 0; i < labels.length; i++) {
            int rowY = rowTop + i * 76;
            settingRowRects[i].set(left + 24, rowY, right - 24, rowY + 56);
            drawSettingRow(canvas, settingRowRects[i], labels[i], values[i]);
        }
        canvas.restore();
        canvas.drawText("保存后只影响下一局，当前牌局继续按本局规则进行。", left + 28, bottom - 110, smallPaint);
        canvas.drawText("后面新增的地方规则会继续收进这里，当前已支持抢亮、公开信息半甩、反主、无主和结算停留。", left + 28, bottom - 82, smallPaint);
        canvas.drawText("该界面支持上下拖动。", left + 28, bottom - 54, smallPaint);
    }

    private void drawRestartConfirmOverlay(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), dimPaint);
        int left = getWidth() / 5;
        int top = getHeight() / 4;
        int right = getWidth() - left;
        int bottom = getHeight() - top;
        drawOverlayPanel(canvas, left, top, right, bottom, "确认重选");

        overlayCancelRect.set(left + 36, bottom - 84, left + 180, bottom - 24);
        overlaySaveRect.set(right - 180, bottom - 84, right - 36, bottom - 24);
        drawTextButton(canvas, overlayCancelRect, "取消", buttonAltPaint);
        drawTextButton(canvas, overlaySaveRect, "确定", buttonPaint);

        canvas.drawText("确定后会立即重新洗牌并开始新一局。", left + 34, top + 116, bodyPaint);
        canvas.drawText("当前这局的进度会直接丢失。", left + 34, top + 156, smallPaint);
    }

    private void drawSettlementOverlay(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), dimPaint);
        int left = getWidth() / 7;
        int top = getHeight() / 8;
        int right = getWidth() - left;
        int bottom = getHeight() - top;
        drawOverlayPanel(canvas, left, top, right, bottom, "本局结算");

        overlayCloseRect.set(right - 150, top + 18, right - 24, top + 68);
        drawTextButton(canvas, overlayCloseRect, "继续", buttonPaint);

        int lineY = top + 112;
        canvas.drawText(trimTextToWidth(game.getRoundSettlementSummary(), right - left - 56f), left + 28, lineY, smallPaint);
        lineY += 40;

        String kittyLabel = "底牌: " + game.describeCards(game.getKitty());
        canvas.drawText(trimTextToWidth(kittyLabel, right - left - 56f), left + 28, lineY, smallPaint);
        lineY += 40;

        String calcText = buildSettlementCalcText();
        canvas.drawText(trimTextToWidth(calcText, right - left - 56f), left + 28, lineY, bodyPaint);
        lineY += 44;

        String scoreText = "闲家最终抓分: " + game.getNonDealerScore()
                + "    " + game.getNextDealerLabel();
        canvas.drawText(trimTextToWidth(scoreText, right - left - 56f), left + 28, lineY, smallPaint);
        lineY += 40;

        String hint = pendingRuleConfig.getSettlementDelaySeconds() > 0
                ? "结算会按设置停留后自动进入下一局，也可以点“继续”马上开始。"
                : "当前设置为手动停留，点“继续”后再进入下一局。";
        canvas.drawText(trimTextToWidth(hint, right - left - 56f), left + 28, bottom - 36, subtleTextPaint);
    }

    private void drawSettingRow(Canvas canvas, Rect rect, String label, String value) {
        RectF rectF = new RectF(rect);
        canvas.drawRoundRect(rectF, 14f, 14f, panelPaint);
        canvas.drawRoundRect(rectF, 14f, 14f, buttonBorderPaint);
        canvas.drawText(label, rect.left + 22, rect.centerY() + 10, bodyPaint);
        canvas.drawText("<  " + value + "  >", rect.right - 220, rect.centerY() + 10, bodyPaint);
    }

    private void drawPanel(Canvas canvas, int left, int top, int right, int bottom) {
        RectF rect = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(rect, 16f, 16f, panelPaint);
        canvas.drawRoundRect(rect, 16f, 16f, buttonBorderPaint);
    }

    private String buildSettlementCalcText() {
        int basePoints = game.getKittyBasePoints();
        int multiplier = game.getKittyMultiplier();
        int bonusPoints = game.getKittyBonusPoints();
        String typeLabel = game.getKittyWinTypeLabel();
        if (bonusPoints <= 0) {
            return typeLabel.length() == 0
                    ? "本局无额外扣底分。"
                    : typeLabel + "，底牌 " + basePoints + " 分，本次未追加。";
        }
        return typeLabel + "，底牌 " + basePoints + " 分，"
                + basePoints + " × " + multiplier + " = " + bonusPoints + " 分。";
    }

    private void drawOverlayPanel(Canvas canvas, int left, int top, int right, int bottom, String title) {
        RectF rect = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(rect, 20f, 20f, overlayPaint);
        canvas.drawRoundRect(rect, 20f, 20f, buttonBorderPaint);
        canvas.drawText(title, left + 28, top + 54, titlePaint);
    }

    private void drawTextButton(Canvas canvas, Rect rect, String label, Paint fillPaint) {
        RectF rectF = new RectF(rect);
        canvas.drawRoundRect(rectF, 14f, 14f, fillPaint);
        canvas.drawRoundRect(rectF, 14f, 14f, buttonBorderPaint);
        float textWidth = bodyPaint.measureText(label);
        canvas.drawText(label, rect.centerX() - textWidth / 2f, rect.centerY() + 10, bodyPaint);
    }

    private void drawCircleButton(Canvas canvas, Rect rect, String label) {
        if (rect.isEmpty()) {
            return;
        }
        RectF rectF = new RectF(rect);
        canvas.drawOval(rectF, buttonAltPaint);
        canvas.drawOval(rectF, buttonBorderPaint);
        float textWidth = bodyPaint.measureText(label);
        canvas.drawText(label, rect.centerX() - textWidth / 2f, rect.centerY() + 10, bodyPaint);
    }

    private void setSmallButtonRect(Rect rect, int left, int top, int width, int height) {
        rect.set(left, top, left + width, top + height);
    }

    private int calculateCardLeft(int index, int count, int cardWidth, int margin) {
        if (count <= 1) {
            return (getBoardRight() - cardWidth) / 2;
        }
        int usableWidth = getBoardRight() - margin * 2 - cardWidth;
        int step = Math.min(cardWidth - cardWidth / 3, usableWidth / (count - 1));
        return margin + index * step;
    }

    private int getHandSideInset() {
        return Math.max(126, getBoardRight() / 7);
    }

    private int getReservedHandHeight() {
        int cardHeight = getCardHeight(getCardWidth());
        return cardHeight * 3 + 76;
    }

    private int getHandRowCount(int cardCount) {
        if (cardCount <= 10) {
            return 1;
        }
        if (cardCount <= 20) {
            return 2;
        }
        return MAX_HAND_ROWS;
    }

    private int getCardWidth() {
        return Math.max(48, Math.min(78, getBoardRight() / 12));
    }

    private int getCardHeight(int cardWidth) {
        return sampleCardBitmap.getHeight() * cardWidth / sampleCardBitmap.getWidth();
    }

    private Bitmap getCardBitmap(Card card) {
        Bitmap bitmap = cardBitmapCache.get(card.getImage());
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(getResources(), card.getImage());
            cardBitmapCache.put(card.getImage(), bitmap);
        }
        return bitmap;
    }

    private int findTouchedCardIndex(int x, int y) {
        for (int i = handCardRects.size() - 1; i >= 0; i--) {
            if (handCardRects.get(i).contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    private void toggleSelectedIndex(int touchedIndex) {
        int selectedPosition = selectedIndexes.indexOf(touchedIndex);
        if (selectedPosition >= 0) {
            selectedIndexes.remove(selectedPosition);
            updateSelectionFeedbackFromCards();
            return;
        }
        selectedIndexes.add(touchedIndex);
        updateSelectionFeedbackFromCards();
    }

    private String getSelectedCardsText() {
        if (selectedIndexes.isEmpty()) {
            return "";
        }
        ArrayList<Card> hand = game.getHumanHand();
        ArrayList<Card> selectedCards = new ArrayList<>();
        for (Integer index : selectedIndexes) {
            if (index != null && index >= 0 && index < hand.size()) {
                selectedCards.add(hand.get(index));
            }
        }
        return game.describeCards(selectedCards);
    }

    private String trimTextToWidth(String text, float maxWidth) {
        if (text == null) {
            return "";
        }
        if (smallPaint.measureText(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        for (int i = text.length() - 1; i > 0; i--) {
            String candidate = text.substring(0, i) + suffix;
            if (smallPaint.measureText(candidate) <= maxWidth) {
                return candidate;
            }
        }
        return suffix;
    }

    private float clampHistoryScroll(float offset) {
        float contentHeight = 20f;
        List<PlayedTrick> history = game.getCompletedTricks();
        for (int i = 0; i < history.size(); i++) {
            PlayedTrick trick = history.get(i);
            contentHeight += 50f + trick.getPlays().size() * 28f + 20f;
        }
        float visibleHeight = getHeight() - (getHeight() / 8f) * 2f - 160f;
        float minOffset = Math.min(0f, visibleHeight - contentHeight);
        if (offset < minOffset) {
            return minOffset;
        }
        if (offset > 0f) {
            return 0f;
        }
        return offset;
    }

    private float clampSettingsScroll(float offset) {
        float contentHeight = SETTING_ROW_COUNT * 76f + 150f;
        float visibleHeight = getHeight() - (getHeight() / 8f) * 2f - 200f;
        float minOffset = Math.min(0f, visibleHeight - contentHeight);
        if (offset < minOffset) {
            return minOffset;
        }
        if (offset > 0f) {
            return 0f;
        }
        return offset;
    }

    private void drawSelectedGroups(Canvas canvas) {
        if (selectedIndexes.isEmpty() || handCardRects.isEmpty()) {
            return;
        }
        ArrayList<Integer> sortedSelected = new ArrayList<>(selectedIndexes);
        java.util.Collections.sort(sortedSelected);
        Rect currentGroup = null;

        for (int i = 0; i < sortedSelected.size(); i++) {
            int selectedIndex = sortedSelected.get(i);
            if (selectedIndex < 0 || selectedIndex >= handCardRects.size()) {
                continue;
            }
            Rect rect = handCardRects.get(selectedIndex);
            if (currentGroup == null) {
                currentGroup = new Rect(rect.left - 12, rect.top - 10, rect.right + 12, rect.bottom + 10);
                continue;
            }

            Rect previousRect = handCardRects.get(sortedSelected.get(i - 1));
            boolean sameRow = Math.abs(rect.top - previousRect.top) < 10;
            boolean closeEnough = rect.left - previousRect.right < getCardWidth() / 2;
            if (sameRow && closeEnough) {
                currentGroup.right = rect.right + 12;
                currentGroup.top = Math.min(currentGroup.top, rect.top - 10);
                currentGroup.bottom = Math.max(currentGroup.bottom, rect.bottom + 10);
            } else {
                canvas.drawRoundRect(new RectF(currentGroup), 18f, 18f, selectedGroupPaint);
                currentGroup = new Rect(rect.left - 12, rect.top - 10, rect.right + 12, rect.bottom + 10);
            }
        }

        if (currentGroup != null) {
            canvas.drawRoundRect(new RectF(currentGroup), 18f, 18f, selectedGroupPaint);
        }
    }

    private void updateSelectionFeedbackFromCards() {
        if (selectedIndexes.isEmpty()) {
            if (game.getPhase() == GamePhase.BURY_KITTY && game.isHumanDealerBuryPhase()) {
                selectionFeedbackMessage = "请选 8 张底牌后点击“放底牌”。";
            } else if (game.getPhase() == GamePhase.REVEAL_TRUMP && game.canHumanUsePrimaryAction()) {
                selectionFeedbackMessage = "你可以点击“我亮”抢庄。";
            } else {
                selectionFeedbackMessage = "";
            }
            selectionFeedbackIsError = false;
            return;
        }
        selectionFeedbackMessage = getSelectedCardsText();
        selectionFeedbackIsError = false;
    }

    private void setSelectionFeedback(String message, boolean isError) {
        selectionFeedbackMessage = message == null ? "" : message;
        selectionFeedbackIsError = isError;
        invalidate();
    }

    private String getTopSelectionText() {
        if (selectionFeedbackMessage != null && selectionFeedbackMessage.length() > 0) {
            return selectionFeedbackMessage;
        }
        if (selectedIndexes.isEmpty()) {
            return "当前未选牌";
        }
        return "已选 " + selectedIndexes.size() + " 张 " + getSelectedCardsText();
    }

    private void drawSidebar(Canvas canvas) {
        int left = getBoardRight() + 12;
        int right = getWidth() - 16;
        int top = 16;
        int bottom = getHeight() - 16;
        drawPanel(canvas, left, top, right, bottom);

        int buttonLeft = left + 14;
        int buttonWidth = right - left - 28;
        int currentTop = top + 116;
        int circleSize = 52;
        menuButtonRect.set(right - circleSize - 14, top + 16, right - 14, top + 16 + circleSize);

        if (toolMenuExpanded) {
            hintButtonRect.set(menuButtonRect.left - 64, menuButtonRect.top + 2, menuButtonRect.left - 12, menuButtonRect.bottom - 2);
            historyButtonRect.set(menuButtonRect.left - 122, menuButtonRect.top + 20, menuButtonRect.left - 70, menuButtonRect.top + 72);
            settingsButtonRect.set(menuButtonRect.left - 122, menuButtonRect.top - 18, menuButtonRect.left - 70, menuButtonRect.top + 34);
            drawCircleButton(canvas, hintButtonRect, game.getPhase() == GamePhase.REVEAL_TRUMP && game.canHumanUsePrimaryAction() ? "过" : "提");
            drawCircleButton(canvas, historyButtonRect, "牌");
            drawCircleButton(canvas, settingsButtonRect, "规");
        } else {
            hintButtonRect.setEmpty();
            historyButtonRect.setEmpty();
            settingsButtonRect.setEmpty();
        }

        int playHeight = buttonWidth * playButtonBitmap.getHeight() / playButtonBitmap.getWidth();
        int restartHeight = buttonWidth * restartButtonBitmap.getHeight() / restartButtonBitmap.getWidth();
        playButtonRect.set(buttonLeft, currentTop, buttonLeft + buttonWidth, currentTop + playHeight);
        currentTop += playHeight + 18;
        restartButtonRect.set(buttonLeft, currentTop, buttonLeft + buttonWidth, currentTop + restartHeight);
        currentTop += restartHeight + 16;
        undoButtonRect.set(buttonLeft, currentTop, buttonLeft + buttonWidth, currentTop + 58);

        drawCircleButton(canvas, menuButtonRect, toolMenuExpanded ? "收" : "工");
        canvas.drawBitmap(playButtonBitmap, null, playButtonRect, null);
        canvas.drawBitmap(restartButtonBitmap, null, restartButtonRect, null);
        drawTextButton(canvas, undoButtonRect, "撤销(" + game.getHumanUndoRemaining() + ")", game.canUndoHumanMove() ? buttonAltPaint : panelPaint);
        if (game.getPhase() == GamePhase.REVEAL_TRUMP) {
            Paint revealBorderPaint = new Paint(buttonBorderPaint);
            revealBorderPaint.setColor(Color.argb(235, 255, 112, 72));
            revealBorderPaint.setStrokeWidth(5f);
            canvas.drawRoundRect(new RectF(playButtonRect), 18f, 18f, revealBorderPaint);
        } else if (game.getPhase() == GamePhase.BURY_KITTY) {
            Paint buryBorderPaint = new Paint(buttonBorderPaint);
            buryBorderPaint.setColor(Color.argb(235, 86, 186, 255));
            buryBorderPaint.setStrokeWidth(5f);
            canvas.drawRoundRect(new RectF(playButtonRect), 18f, 18f, buryBorderPaint);
        }
        Paint primaryPaint;
        if ("抄底".equals(game.getPrimaryActionText()) || "反主".equals(game.getPrimaryActionText())) {
            primaryPaint = new Paint(hintPaint);
            primaryPaint.setColor(Color.rgb(255, 230, 140));
            primaryPaint.setTextSize(38f);
        } else if (game.getPhase() == GamePhase.BURY_KITTY) {
            primaryPaint = new Paint(titlePaint);
            primaryPaint.setColor(Color.rgb(210, 244, 255));
            primaryPaint.setTextSize(34f);
        } else if (game.getPhase() == GamePhase.REVEAL_TRUMP) {
            primaryPaint = new Paint(hintPaint);
            primaryPaint.setTextSize(36f);
        } else {
            primaryPaint = new Paint(titlePaint);
            primaryPaint.setColor(Color.rgb(86, 45, 8));
            primaryPaint.setTextSize(38f);
        }
        float primaryWidth = primaryPaint.measureText(game.getPrimaryActionText());
        canvas.drawText(game.getPrimaryActionText(), playButtonRect.centerX() - primaryWidth / 2f, playButtonRect.centerY() + 12, primaryPaint);
        float restartWidth = bodyPaint.measureText("重开");
        canvas.drawText("重开", restartButtonRect.centerX() - restartWidth / 2f, restartButtonRect.centerY() + 10, bodyPaint);
        canvas.drawText("操作区", buttonLeft, bottom - 44, subtleTextPaint);
        canvas.drawText(trimTextToWidth("点“工”展开更多。", buttonWidth - 8f), buttonLeft, bottom - 18, subtleTextPaint);
    }

    private int getSidebarWidth() {
        return Math.max(160, getWidth() / 6);
    }

    private int getBoardRight() {
        return getWidth() - getSidebarWidth() - 16;
    }

    private int getHandTop() {
        return getHeight() - getReservedHandHeight() - 18;
    }

    private int getMiddleBoardCenterY() {
        return (228 + getHandTop()) / 2;
    }

    private void beginDealAnimation() {
        removeCallbacks(dealAnimationRunnable);
        dealVisibleCount = 0;
        dealAnimating = true;
        postDelayed(dealAnimationRunnable, 110);
    }

    private int getAnimatedHandCount(int player) {
        if (!dealAnimating) {
            return game.getHandCount(player);
        }
        return Math.min(game.getHandCount(player), dealVisibleCount);
    }

    private void maybeAutoSelectForcedMove() {
        if (dealAnimating || showAnyOverlay() || !selectedIndexes.isEmpty()) {
            return;
        }
        SimpleShengJiGame.MoveSuggestion suggestion = game.getForcedHumanMove();
        if (!suggestion.success || suggestion.indexes.isEmpty()) {
            return;
        }
        selectedIndexes.clear();
        selectedIndexes.addAll(suggestion.indexes);
        setSelectionFeedback(suggestion.message, false);
    }

    private void handleSettlementTouch(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) {
            return;
        }
        performClick();
        removeCallbacks(dismissSettlementRunnable);
        if (overlayCloseRect.contains((int) event.getX(), (int) event.getY()) || !insideOverlayPanel((int) event.getX(), (int) event.getY())) {
            showSettlementPanel = false;
            invalidate();
            postDelayed(nextRoundRunnable, 700);
        }
    }

    private void handleRestartConfirmTouch(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) {
            return;
        }
        performClick();
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (overlaySaveRect.contains(x, y)) {
            showRestartConfirmPanel = false;
            restartGame();
            return;
        }
        if (overlayCancelRect.contains(x, y) || !insideOverlayPanel(x, y)) {
            showRestartConfirmPanel = false;
            invalidate();
            scheduleNextAction();
        }
    }

    private String getPhaseTitle() {
        if (game.getPhase() == GamePhase.REVEAL_TRUMP) {
            return "当前阶段：抢庄亮主";
        }
        if (game.getPhase() == GamePhase.BURY_KITTY) {
            return "当前阶段：庄家埋底";
        }
        if (game.getPhase() == GamePhase.ROUND_END) {
            return "当前阶段：本局结算";
        }
        return "当前阶段：正常出牌";
    }

    private String getPhaseDetailText() {
        if (game.getPhase() == GamePhase.REVEAL_TRUMP) {
            return game.canHumanUsePrimaryAction()
                    ? "你现在可以点“我亮”抢主；如果先不亮，系统稍后会自动判定更合适的亮主方。"
                    : "系统正在按双方牌力自动判断谁来亮主，亮牌后会马上继续起底。";
        }
        if (game.getPhase() == GamePhase.BURY_KITTY) {
            return game.isHumanDealerBuryPhase()
                    ? "你已经拿到底牌，请从手牌中选 8 张放回底牌。"
                    : "庄家正在整理底牌，完成后会自动开始出牌。";
        }
        if (game.getPhase() == GamePhase.ROUND_END) {
            return "系统会在短暂停留后自动进入下一局，你也可以先看一下本局结算和升降级。";
        }
        return "支持单张、对子、拖拉机和严格跟牌，已出牌记录可在上方按钮里滚动查看。";
    }

    private void syncRevealNoticeFromGame() {
        String summary = game.getLastRevealSummary();
        if (summary == null || summary.length() == 0) {
            showRevealNotice = false;
            revealNoticeSummary = "";
            revealNoticeCards.clear();
            return;
        }
        if (!summary.equals(revealNoticeSummary)) {
            revealNoticeSummary = summary;
            revealNoticeCards.clear();
            revealNoticeCards.addAll(game.getLastRevealCards());
            showRevealNotice = true;
        }
    }
}
