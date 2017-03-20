package com.bakerj.infinitecards;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;

import com.bakerj.infinitecards.lib.R;
import com.bakerj.infinitecards.transformer.DefaultCommonTransformer;
import com.bakerj.infinitecards.transformer.DefaultTransformerToBack;
import com.bakerj.infinitecards.transformer.DefaultTransformerToFront;
import com.bakerj.infinitecards.transformer.DefaultZIndexTransformerCommon;
import com.bakerj.infinitecards.transformer.DefaultZIndexTransformerToFront;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ValueAnimator;

import java.util.LinkedList;

/**
 * @author BakerJ
 */
public class InfiniteCardView extends FrameLayout implements Animator.AnimatorListener,
        ValueAnimator.AnimatorUpdateListener {
    /*
     * Three types of animation
     * ANIM_TYPE_FRONT:custom animation for chosen card, common animation for other cards
     * ANIM_TYPE_SWITCH:switch the position by custom animation of the first card and the chosen card
     * ANIM_TYPE_FRONT_TO_LAST:moving the first card to last position by custom animation, common animation for others
     */
    public static final int ANIM_TYPE_FRONT = 0, ANIM_TYPE_SWITCH = 1, ANIM_TYPE_FRONT_TO_LAST = 2;
    //cardHeight / cardWidth = CARD_SIZE_RATIO
    private static final float CARD_SIZE_RATIO = 0.5f;
    //animation duration
    private static final int ANIM_DURATION = 1000;
    //animation type
    private int mAnimType = ANIM_TYPE_FRONT;
    //cardHeight / cardWidth = mCardRatio
    private float mCardRatio = CARD_SIZE_RATIO;
    //animation duration
    private int mAnimDuration = ANIM_DURATION;
    //view adapter
    private BaseAdapter mAdapter;
    //card item list
    private LinkedList<CardItem> mCards;
    //total card count
    private int mCardCount;
    //card width, card height
    private int mCardWidth, mCardHeight;
    //for judge Z index
    //    private ArrayList<CardItem> mCards4JudgeZIndex;
    //current card moving to back, current card moving to front
    private CardItem mCardToBack, mCardToFront;
    //current card position moving to front, current card position moving to front
    private int mPositionToBack = 0, mPositionToFront = 0;
    //is doing animation now
    private boolean mIsAnim = false;
    //animator
    private ValueAnimator mValueAnimator;
    //custom animation transformer for card moving to front, card moving to back, and common card
    private AnimationTransformer mTransformerToFront, mTransformerToBack, mTransformerCommon;
    //custom Z index transformer for card moving to front, card moving to back, and common card
    private ZIndexTransformer mZIndexTransformerToFront, mZIndexTransformerToBack, mZIndexTransformerCommon;
    //animation interpolator
    private Interpolator mAnimInterpolator;

    public InfiniteCardView(@NonNull Context context) {
        this(context, null);
    }

    public InfiniteCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InfiniteCardView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
        setClickable(true);
        mAnimInterpolator = new LinearInterpolator();
        mTransformerToFront = new DefaultTransformerToFront();
        mTransformerToBack = new DefaultTransformerToBack();
        mTransformerCommon = new DefaultCommonTransformer();
        mZIndexTransformerToFront = new DefaultZIndexTransformerToFront();
        mZIndexTransformerToBack = new DefaultZIndexTransformerCommon();
        mZIndexTransformerCommon = new DefaultZIndexTransformerCommon();
        initAnimator();
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.InfiniteCardView);
            mAnimType = ta.getInt(R.styleable.InfiniteCardView_animType, ANIM_TYPE_FRONT);
            mCardRatio = ta.getFloat(R.styleable.InfiniteCardView_cardRatio, CARD_SIZE_RATIO);
            mAnimDuration = ta.getInt(R.styleable.InfiniteCardView_animDuration, ANIM_DURATION);
            ta.recycle();
        }
    }

    /**
     * setup animator
     */
    private void initAnimator() {
        mValueAnimator = ValueAnimator.ofFloat(0, 1).setDuration(mAnimDuration);
        mValueAnimator.addUpdateListener(this);
        mValueAnimator.addListener(this);
    }

    /**
     * do animation while update
     *
     * @param animation animation
     */
    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float fraction = (float) animation.getAnimatedValue();
        float fractionInterpolated = fraction;
        if (mAnimInterpolator != null) {
            fractionInterpolated = mAnimInterpolator.getInterpolation(fraction);
        }
        doAnimationBackToFront(fraction, fractionInterpolated);
        doAnimationFrontToBack(fraction, fractionInterpolated);
        doAnimationCommon(fraction, fractionInterpolated);
        bringToFrontByZIndex();
    }

    /**
     * do animation for card moving from back to front
     *
     * @param fraction             animation progress from 0.0f to 1.0f
     * @param fractionInterpolated interpolated animation progress
     */
    private void doAnimationBackToFront(float fraction, float fractionInterpolated) {
        mTransformerToFront.transformAnimation(mCardToFront.view,
                fraction, mCardWidth, mCardHeight, mPositionToFront, 0);
        if (mAnimInterpolator != null) {
            mTransformerToFront.transformInterpolatedAnimation(mCardToFront.view,
                    fractionInterpolated, mCardWidth, mCardHeight, mPositionToFront, 0);
        }
        doAnimationZIndex(mZIndexTransformerToFront, mCardToFront, fraction, fractionInterpolated,
                mPositionToFront, 0);
    }

    /**
     * do animation for card moving from from to back
     *
     * @param fraction             animation progress from 0.0f to 1.0f
     * @param fractionInterpolated interpolated animation progress
     */
    private void doAnimationFrontToBack(float fraction, float fractionInterpolated) {
        if (mAnimType == ANIM_TYPE_FRONT) {
            return;
        }
        mTransformerToBack.transformAnimation(mCardToBack.view, fraction, mCardWidth,
                mCardHeight, 0, mPositionToBack);
        if (mAnimInterpolator != null) {
            mTransformerToBack.transformInterpolatedAnimation(mCardToBack.view,
                    fractionInterpolated, mCardWidth, mCardHeight, 0, mPositionToBack);
        }
        doAnimationZIndex(mZIndexTransformerToBack, mCardToBack, fraction, fractionInterpolated,
                0, mPositionToBack);
    }

    /**
     * do animation for common card items
     *
     * @param fraction             animation progress from 0.0f to 1.0f
     * @param fractionInterpolated interpolated animation progress
     */
    private void doAnimationCommon(float fraction, float fractionInterpolated) {
        if (mAnimType == ANIM_TYPE_FRONT) {
            for (int i = 0; i < mPositionToFront; i++) {
                CardItem card = mCards.get(i);
                doAnimationCommonView(card.view, fraction, fractionInterpolated, i, i + 1);
                doAnimationZIndex(mZIndexTransformerCommon, card, fraction, fractionInterpolated,
                        i, i + 1);
            }
        } else if (mAnimType == ANIM_TYPE_FRONT_TO_LAST) {
            for (int i = mPositionToFront + 1; i < mCardCount; i++) {
                CardItem card = mCards.get(i);
                doAnimationCommonView(card.view, fraction, fractionInterpolated, i, i - 1);
                doAnimationZIndex(mZIndexTransformerCommon, card, fraction, fractionInterpolated,
                        i, i - 1);
            }
        }
    }

    /**
     * do animation for common card views
     *
     * @param view                 card view
     * @param fraction             animation progress from 0.0f to 1.0f
     * @param fractionInterpolated interpolated animation progress
     * @param fromPosition         card moving from
     * @param toPosition           card moving to
     */
    private void doAnimationCommonView(View view, float fraction, float fractionInterpolated, int
            fromPosition, int toPosition) {
        mTransformerCommon.transformAnimation(view, fraction, mCardWidth,
                mCardHeight, fromPosition, toPosition);
        if (mAnimInterpolator != null) {
            mTransformerCommon.transformInterpolatedAnimation(view, fractionInterpolated, mCardWidth,
                    mCardHeight, fromPosition, toPosition);
        }
    }

    /**
     * do calculation for card Z index
     *
     * @param transformer          Z index transformer
     * @param card                 card item
     * @param fraction             animation progress from 0.0f to 1.0f
     * @param fractionInterpolated interpolated animation progress
     * @param fromPosition         card moving from
     * @param toPosition           card moving to
     */
    private void doAnimationZIndex(ZIndexTransformer transformer, CardItem card, float fraction,
                                   float fractionInterpolated, int fromPosition, int toPosition) {
        transformer.transformAnimation(card, fraction, mCardWidth,
                mCardHeight, fromPosition, toPosition);
        if (mAnimInterpolator != null) {
            transformer.transformInterpolatedAnimation(card, fractionInterpolated, mCardWidth,
                    mCardHeight, fromPosition, toPosition);
        }
    }

    /**
     * bring card to front by Z index, the card with smaller Z index is in front of the card with
     * bigger Z index
     */
    private void bringToFrontByZIndex() {
        if (mAnimType == ANIM_TYPE_FRONT) {
            //if the animation type is ANIM_TYPE_FRONT, which means other cards are under common
            // animation, so start cycling the card items from the position before the moving
            // card, and while the moving card's Z index is smaller than an other card, we
            // call bringToFront for it, otherwise we call bringToFront for other cards
            for (int i = mPositionToFront - 1; i >= 0; i--) {
                CardItem card = mCards.get(i);
                if (card.zIndex > mCardToFront.zIndex) {
                    mCardToFront.view.bringToFront();
                } else {
                    card.view.bringToFront();
                }
            }
        } else {
            //sort the card items by Z index and call bringToFront foreach view
//            Collections.sort(mCards4JudgeZIndex, this);
//            for (int i = mCardCount - 1; i >= 0; i--) {
//                mCards4JudgeZIndex.get(i).view.bringToFront();
//            }
            //##########################for better performance#########################
            boolean cardToFrontBrought = false;//is card moving to front called bringToFront
            //cycling the card items
            for (int i = mCardCount - 1; i > 0; i--) {
                CardItem card = mCards.get(i);
                //get the card before current card
                CardItem cardPre = i > 1 ? mCards.get(i - 1) : null;
                //is card moving to back behind the card before current card
                boolean cardToBackBehindCardPre = cardPre == null ||
                        mCardToBack.zIndex > cardPre.zIndex;
                //if the card moving to back Z index is smaller than current card, and is behind
                // the card before current card, we should call bringToFront for it
                boolean bringCardToBackViewToFront = mCardToBack.zIndex < card.zIndex && cardToBackBehindCardPre;
                //is card moving to front behind the current card
                boolean cardToFrontBehindCardPre = cardPre == null ||
                        mCardToFront.zIndex > cardPre.zIndex;
                //if the card moving to front Z index is smaller than current card, and is behind
                // the card before current card, we should call bringToFront for it
                boolean bringCardToFrontViewToFront = mCardToFront.zIndex < card.zIndex && cardToFrontBehindCardPre;
                //if current card is not the card moving to front
                if (i != mPositionToFront) {
                    //call bringToFront for it
                    card.view.bringToFront();
                    //if we should bring the card moving to back to front, just do it
                    if (bringCardToBackViewToFront) {
                        mCardToBack.view.bringToFront();
                    }
                    //if we should bring the card moving to front to front, just do it
                    if (bringCardToFrontViewToFront) {
                        mCardToFront.view.bringToFront();
                        cardToFrontBrought = true;
                    }
                    //if we has both bring the card moving to front and back to front, and the
                    // card moving to back Z index is smaller than the card moving to front Z
                    // index, we call bringToFront for the card moving to back
                    if (bringCardToBackViewToFront && bringCardToFrontViewToFront &&
                            mCardToBack.zIndex < mCardToFront.zIndex) {
                        mCardToBack.view.bringToFront();
                    }
                } else {
                    //if current card is the card moving to front, and behind the card before
                    if (cardToFrontBehindCardPre) {
                        mCardToFront.view.bringToFront();
                        cardToFrontBrought = true;
                        //if the card moving to back Z index is smaller than the card moving to
                        // front, call bringToFront for it
                        if (cardToBackBehindCardPre && mCardToBack.zIndex < mCardToFront.zIndex) {
                            mCardToBack.view.bringToFront();
                        }
                    }
                }
            }
            // it the card moving to front has not call bringToFront yet, which means it is
            // already in the first position, call bringToFront for it
            if (!cardToFrontBrought) {
                mCardToFront.view.bringToFront();
            }
        }
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    /**
     * animation end
     *
     * @param animation animation
     */
    @Override
    public void onAnimationEnd(Animator animation) {
        if (mAnimType == ANIM_TYPE_FRONT) {
            //move the card moving to front to the first position
            mCards.remove(mPositionToFront);
            mCards.addFirst(mCardToFront);
        } else if (mAnimType == ANIM_TYPE_SWITCH) {
            //switch the position of the card moving to front and back
            mCards.remove(mPositionToFront);
            mCards.removeFirst();
            mCards.addFirst(mCardToFront);
            mCards.add(mPositionToFront, mCardToBack);
        } else {
            //moving the first position card to last
            mCards.remove(mPositionToFront);
            mCards.removeFirst();
            mCards.addFirst(mCardToFront);
            mCards.addLast(mCardToBack);
        }
        mPositionToFront = 0;
        mPositionToBack = 0;
        mIsAnim = false;
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mCardWidth == 0) {
            mCardWidth = getMeasuredWidth();
            mCardHeight = (int) (mCardWidth * mCardRatio);
            initAdapterView();
        }
    }

    private void initAdapterView() {
        if (mCardWidth > 0 && mCardHeight > 0 && mCards == null) {
            mCards = new LinkedList<>();
//            mCards4JudgeZIndex = new ArrayList<>();
            mCardCount = mAdapter.getCount();
            for (int i = mCardCount - 1; i >= 0; i--) {
                View child = mAdapter.getView(i, null, this);
                CardItem cardItem = new CardItem(child, 0);
                addCardView(cardItem);
                mZIndexTransformerCommon.transformAnimation(cardItem, 1, mCardWidth, mCardHeight, i, i);
                mTransformerCommon.transformAnimation(child, 1, mCardWidth, mCardHeight, i, i);
                mCards.addFirst(cardItem);
//                mCards4JudgeZIndex.add(cardItem);
            }
        }
    }

    private void addCardView(final CardItem card) {
        View view = card.view;
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(mCardWidth,
                mCardHeight);
        layoutParams.gravity = Gravity.CENTER;
        view.setLayoutParams(layoutParams);
        addView(view);
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                bringCardToFront(card);
            }
        });
    }

    private void bringCardToFront(CardItem card) {
        if (!isClickable() || mCards == null || mTransformerCommon == null || mTransformerToFront ==
                null || mTransformerToBack == null) {
            return;
        }
        int position = mCards.indexOf(card);
        bringCardToFront(position);
    }

    /**
     * bring the specific position card to front
     *
     * @param position position
     */
    public void bringCardToFront(int position) {
        if (position >= 0 && position != mPositionToFront && !mIsAnim) {
            mPositionToFront = position;
            //if the animation type is not ANIM_TYPE_SWITCH, the card to back post is the last
            // position
            mPositionToBack = mAnimType == ANIM_TYPE_SWITCH ? mPositionToFront :
                    (mCardCount - 1);
            mCardToBack = mCards.getFirst();
            mCardToFront = mCards.get(mPositionToFront);
            if (mValueAnimator.isRunning()) {
                mValueAnimator.end();
            }
            mIsAnim = true;
            mValueAnimator.start();
        }
    }

    private void notifyDataSetChanged() {

    }

    /**
     * set view adapter
     *
     * @param adapter adapter
     */
    public void setAdapter(BaseAdapter adapter) {
        removeAllViews();
        this.mAdapter = adapter;
        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                notifyDataSetChanged();
            }
        });
        initAdapterView();
    }

    public void setTransformerToFront(AnimationTransformer toFrontTransformer) {
        this.mTransformerToFront = toFrontTransformer;
    }

    public void setTransformerToBack(AnimationTransformer toBackTransformer) {
        this.mTransformerToBack = toBackTransformer;
    }

    public void setCommonSwitchTransformer(AnimationTransformer commonTransformer) {
        this.mTransformerCommon = commonTransformer;
    }

    public void setTransformerCommon(AnimationTransformer transformerCommon) {
        this.mTransformerCommon = transformerCommon;
    }

    public void setZIndexTransformerToFront(ZIndexTransformer zIndexTransformerToFront) {
        this.mZIndexTransformerToFront = zIndexTransformerToFront;
    }

    public void setZIndexTransformerToBack(ZIndexTransformer zIndexTransformerToBack) {
        this.mZIndexTransformerToBack = zIndexTransformerToBack;
    }

    public void setZIndexTransformerCommon(ZIndexTransformer zIndexTransformerCommon) {
        this.mZIndexTransformerCommon = zIndexTransformerCommon;
    }

    public void setAnimInterpolator(Interpolator animInterpolator) {
        this.mAnimInterpolator = animInterpolator;
    }

    public void setmAnimType(int mAnimType) {
        this.mAnimType = mAnimType;
    }

//    @Override
//    public int compare(CardItem o1, CardItem o2) {
//        return o1.zIndex < o2.zIndex ? -1 : 1;
//    }
}
