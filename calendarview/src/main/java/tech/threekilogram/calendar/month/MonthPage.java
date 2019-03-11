package tech.threekilogram.calendar.month;

import static tech.threekilogram.calendar.month.MonthDayView.SELECTED;
import static tech.threekilogram.calendar.month.MonthDayView.UNSELECTED;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import java.util.Date;
import tech.threekilogram.calendar.util.CalendarUtils;

/**
 * {@link MonthLayout}的一个页面,用来显示一个月日期或者一周的日期,通过手势分发可以在月模式和周模式之间转换
 *
 * @author Liujin 2019/2/21:21:32:09
 */
public class MonthPage extends ViewGroup implements OnClickListener {

      /**
       * 当前状态之一:已经展开
       */
      protected static final int STATE_EXPAND  = 0;
      /**
       * 当前状态之一:已经折叠
       */
      protected static final int STATE_FOLDED  = 1;
      /**
       * 当前状态之一:正在折叠/展开
       */
      protected static final int STATE_MOVING  = 2;
      /**
       * 当前状态之一:使用动画在手指离开后,自动折叠或者展开
       */
      protected static final int STATE_ANIMATE = 3;

      /**
       * 记录当前页面日期
       */
      protected Date    mDate;
      /**
       * 记录当前页面位于{@link MonthLayout#getAdapter()}中的位置
       */
      protected int     mPosition;
      /**
       * 记录当前页面第一天是否是周一
       */
      protected boolean isFirstDayMonday;
      /**
       * {@link #mDate}代表的日期在当前布局中的位置索引,例如:日期是2019/3/7,那么在布局中第10个子view处于选中状态,该值为10
       */
      protected int     mCurrentSelectedPosition;
      /**
       * {@link #mDate}代表的月份的总天数
       */
      protected int     mMonthDayCount;
      /**
       * {@link #mDate}位于的月份第一天在布局中的位置索引
       */
      protected int     mFirstDayOffset;

      /**
       * 显示天的view的宽度
       */
      protected int          mCellWidth  = -1;
      /**
       * 显示天的view的高度
       */
      protected int          mCellHeight = -1;
      /**
       * 根据总天数计算的页面高度,有的月份占据4行有的月份占据6行,根据天数和起始的日期是周几计算
       */
      protected int          mPageHeight;
      /**
       * 辅助管理当前状态,当从折叠状态改变为其他状态时,改变非本月的日期的子view的可见性,折叠时可见,展开时不可见
       */
      protected StateManager mStateManager;
      /**
       * 辅助类用于界面展开折叠
       */
      protected MoveHelper   mMoveHelper;

      public MonthPage ( Context context ) {

            super( context );
            init();
      }

      protected void init ( ) {
            /*每个月最多使用7列6行个子view就能包含所有日期*/
            for( int i = 0; i < 6 * 7; i++ ) {
                  View child = generateItemView();
                  addView( child );
                  child.setOnClickListener( this );
            }

            mMoveHelper = new MoveHelper();
            mStateManager = new StateManager();
      }

      /**
       * 创建子view
       *
       * @return 子view
       */
      protected View generateItemView ( ) {

            return new MonthDayView( getContext() );
      }

      protected Date getDate ( ) {

            return mDate;
      }

      protected int getPosition ( ) {

            return mPosition;
      }

      /**
       * 设置页面显示信息
       *
       * @param isFirstDayMonday 每周第一天是周一或者周日,true:周一
       * @param monthMode true:月显示模式,false:周显示模式
       * @param date 显示日期
       * @param position 位于pager的位置
       */
      protected void setInfo ( Date date, int position, boolean isFirstDayMonday, boolean monthMode ) {

            mDate = date;
            mPosition = position;
            this.isFirstDayMonday = isFirstDayMonday;

            if( monthMode ) {
                  mStateManager.setState( STATE_EXPAND );
            } else {
                  mStateManager.setState( STATE_FOLDED );
            }

            calculateMonthInfo( isFirstDayMonday, date );
            bindChildren();
            requestLayout();
      }

      /**
       * 计算总天数,这个月第一天是周几
       *
       * @param isFirstDayMonday 每周的第一天是否是周一,true:是周一
       * @param date 需要判断的日期
       */
      protected void calculateMonthInfo ( boolean isFirstDayMonday, Date date ) {

            mMonthDayCount = CalendarUtils.getDayCountOfMonth( date );
            int dayOfWeek = CalendarUtils.getDayOfWeekAtMonthFirstDay( date );
            if( isFirstDayMonday ) {
                  if( dayOfWeek == 1 ) {
                        mFirstDayOffset = 6;
                  } else {
                        mFirstDayOffset = dayOfWeek - 2;
                  }
            } else {
                  mFirstDayOffset = dayOfWeek - 1;
            }
      }

      /**
       * 设置显示状态,绑定数据
       */
      protected void bindChildren ( ) {

            int childCount = getChildCount();
            int offset = -mFirstDayOffset;

            Date firstDayOfMonth = CalendarUtils.getFirstDayOfMonth( mDate );

            for( int i = 0; i < childCount; i++ ) {
                  MonthDayView child = (MonthDayView) getChildAt( i );
                  Date day = CalendarUtils.getDateByAddDay( firstDayOfMonth, offset );
                  child.bind( day );

                  if( offset < 0 || offset > mMonthDayCount - 1 ) {
                        if( mStateManager.getState() == STATE_FOLDED ) {
                              child.setVisibility( VISIBLE );
                        } else {
                              child.setVisibility( INVISIBLE );
                        }
                  } else {
                        child.setVisibility( VISIBLE );
                  }

                  if( mDate.equals( day ) ) {
                        child.setState( SELECTED );
                        mCurrentSelectedPosition = i;
                  } else {
                        child.setState( UNSELECTED );
                  }

                  offset++;
            }
      }

      @Override
      protected void onMeasure ( int widthMeasureSpec, int heightMeasureSpec ) {

            int widthSize = MeasureSpec.getSize( widthMeasureSpec );

            /* 从父布局获取基础尺寸,保证所有页面基础尺寸一致 */
            MonthLayout parent = (MonthLayout) getParent();
            mCellWidth = parent.getCellWidth();
            mCellHeight = parent.getCellHeight();

            /* 如果需要测量,那么测量所有child */
            View view = getChildAt( 0 );
            if( view.getMeasuredWidth() != mCellWidth || view.getMeasuredHeight() != mCellHeight ) {
                  int cellWidthSpec = MeasureSpec.makeMeasureSpec( mCellWidth, MeasureSpec.EXACTLY );
                  int cellHeightSpec = MeasureSpec.makeMeasureSpec( mCellHeight, MeasureSpec.EXACTLY );

                  int childCount = getChildCount();
                  for( int i = 0; i < childCount; i++ ) {
                        View child = getChildAt( i );
                        child.measure( cellWidthSpec, cellHeightSpec );
                  }
            }

            /* 设置高度信息 */
            int count = mMonthDayCount + mFirstDayOffset;
            int lines = count % 7 == 0 ? count / 7 : count / 7 + 1;
            int resultHeight = lines * mCellHeight;
            mPageHeight = resultHeight;

            /* 当折叠或者展开时,计算偏移量 */
            int state = mStateManager.getState();
            if( state == STATE_FOLDED ) {
                  mMoveHelper.calculateFoldMoved();
            } else if( state == STATE_EXPAND ) {
                  mMoveHelper.calculateExpandMoved();
            }

            /* 根据偏移量设置自身尺寸 */
            int measuredHeight = mMoveHelper.calculateMeasuredHeight( resultHeight );
            setMeasuredDimension( widthSize, measuredHeight );
      }

      @Override
      protected void onLayout ( boolean changed, int l, int t, int r, int b ) {

            View child = getChildAt( 0 );
            int cellWidth = child.getMeasuredWidth();
            int cellHeight = child.getMeasuredHeight();

            int count = getChildCount();
            int topMoved = (int) mMoveHelper.mTopMoved;
            for( int i = 0; i < count; i++ ) {
                  View view = getChildAt( i );
                  int left = ( i % 7 ) * cellWidth;
                  int top = i / 7 * cellHeight;
                  view.layout(
                      left,
                      top + topMoved,
                      left + view.getMeasuredWidth(),
                      top + view.getMeasuredHeight() + topMoved
                  );
            }
      }

      /**
       * 用于手势释放时,展开或者折叠到最终状态
       */
      @Override
      public void computeScroll ( ) {

            super.computeScroll();
            mMoveHelper.animateIfNeed();
      }

      /**
       * 子view点击事件
       */
      @Override
      public void onClick ( View v ) {

            int state = mStateManager.getState();
            if( state == STATE_MOVING || state == STATE_ANIMATE ) {
                  return;
            }

            MonthDayView itemView = (MonthDayView) v;
            Date date = itemView.getDate();
            /* 日期变化了 */
            if( !date.equals( mDate ) ) {
                  MonthLayout parent = (MonthLayout) getParent();
                  parent.onNewDateClicked( date, mPosition );
            }
      }

      /**
       * 滑动一段距离,直至展开至月显示模式,或者折叠到周显示模式
       *
       * @param dy 距离
       */
      void onVerticalMoveBy ( float dy ) {

            mMoveHelper.move( dy );
      }

      void moveToExpand ( ) {

            mMoveHelper.setAnimateState( 1 );
      }

      void moveToFold ( ) {

            mMoveHelper.setAnimateState( -1 );
      }

      void onDownTouchEvent ( ) {

            mMoveHelper.forceStopAnimateIfRunning();
      }

      boolean isMoving ( ) {

            int state = mStateManager.getState();
            return state == STATE_MOVING || state == STATE_ANIMATE;
      }

      /**
       * 辅助方法当手指抬起后调用
       *
       * @param totalDy 一共移动的竖直距离
       * @param isMonthMode 当前是否是月显示模式
       */
      void onUpTouchEvent ( float totalDy, boolean isMonthMode ) {

            if( totalDy > 24 ) {
                  moveToExpand();
                  return;
            }

            if( totalDy < -24 ) {
                  moveToFold();
                  return;
            }

            mMoveHelper.checkAnimateState( isMonthMode );
      }

      /**
       * 辅助类管理当前状态
       */
      @SuppressWarnings("WeakerAccess")
      protected class StateManager {

            /**
             * 当前状态
             */
            protected int mState;

            protected void setState ( int newState ) {

                  int old = mState;
                  mState = newState;
                  if( old == STATE_FOLDED && newState != STATE_FOLDED ) {
                        updateChildrenVisibility();
                  }
            }

            protected int getState ( ) {

                  return mState;
            }

            /**
             * {@link}
             */
            protected void updateChildrenVisibility ( ) {

                  for( int i = 0; i < mFirstDayOffset; i++ ) {
                        MonthDayView child = (MonthDayView) getChildAt( i );
                        if( mStateManager.getState() == STATE_FOLDED ) {
                              child.setVisibility( VISIBLE );
                        } else {
                              child.setVisibility( INVISIBLE );
                        }
                  }

                  int childCount = getChildCount();
                  for( int i = mMonthDayCount + mFirstDayOffset; i < childCount; i++ ) {
                        MonthDayView child = (MonthDayView) getChildAt( i );
                        if( mStateManager.getState() == STATE_FOLDED ) {
                              child.setVisibility( VISIBLE );
                        } else {
                              child.setVisibility( INVISIBLE );
                        }
                  }
            }
      }

      /**
       * 辅助类用于手势分发之后,计算页面需要移动的距离,当页面移动之后,
       * 会竖直方向偏移所有子view{@link MoveHelper#mTopMoved}距离,
       * 同时页面的底部收缩{@link MoveHelper#mBottomMoved}距离,
       * 页面会逐渐变化为月显示模式或者周显示模式
       */
      @SuppressWarnings("WeakerAccess")
      protected class MoveHelper {

            /**
             * 所有子view的top偏移
             */
            protected float mTopMoved;
            /**
             * 当前页面bottom偏移
             */
            protected float mBottomMoved;
            /**
             * 手势释放后,需要收缩或者折叠时,用于计算方向,只有两个值1或者-1
             */
            protected int   mDirection = 0;

            /**
             * 将页面移动一段距离,按照比例分配给上下需要移动的距离
             *
             * @param dy 手势滑动的距离
             */
            protected void move ( float dy ) {

                  mStateManager.setState( STATE_MOVING );
                  if( calculateMovedByDy( dy ) ) {
                        requestLayout();
                        ( (MonthLayout) getParent() ).onCurrentItemVerticalMove( mTopMoved + mBottomMoved );
                  }
            }

            /**
             * 计算需要移动的偏移量
             *
             * @param dy 手势滑动的距离
             *
             * @return true:偏移量发生变化,需要重新布局
             */
            protected boolean calculateMovedByDy ( float dy ) {

                  /* 记录原始尺寸,用于后续决定是否需要重新布局 */
                  float topMoved = mTopMoved;
                  float bottomMoved = mBottomMoved;

                  /* 上下部分可以移动的距离 */
                  int topDis = mCurrentSelectedPosition / 7 * mCellHeight;
                  int bottomDis = mPageHeight - ( topDis + mCellHeight );
                  /* 上下可移动距离之比 */
                  float topRadio = topDis * 1f / ( topDis + bottomDis );
                  /* 根据比例分配移动的距离 */
                  float topNeedMove = dy * topRadio;
                  float bottomNeedMove = dy - topNeedMove;

                  /* 重新计算上部移动的距离 */
                  mTopMoved += topNeedMove;
                  if( mTopMoved < -topDis ) {
                        mTopMoved = -topDis;
                  }
                  if( mTopMoved > 0 ) {
                        mTopMoved = 0;
                  }

                  /* 重新计算下部移动的距离 */
                  mBottomMoved += bottomNeedMove;
                  if( mBottomMoved < -bottomDis ) {
                        mBottomMoved = -bottomDis;
                  }
                  if( mBottomMoved > 0 ) {
                        mBottomMoved = 0;
                  }

                  /* 判断是否需要重新布局 */
                  return topMoved != mTopMoved || bottomMoved != mBottomMoved;
            }

            /**
             * 计算折叠时需要移动的尺寸
             */
            protected void calculateFoldMoved ( ) {

                  int topDis = mCurrentSelectedPosition / 7 * mCellHeight;
                  int bottomDis = mPageHeight - ( topDis + mCellHeight );
                  mTopMoved = -topDis;
                  mBottomMoved = -bottomDis;
            }

            /**
             * 计算展开时需要移动的尺寸
             */
            protected void calculateExpandMoved ( ) {

                  mTopMoved = 0;
                  mBottomMoved = 0;
            }

            /**
             * {@link #onMeasure(int, int)}中根据偏移量计算该view高度
             *
             * @param linesHeight 页面显示完整需要的高度
             *
             * @return 修正后的高度
             */
            protected int calculateMeasuredHeight ( int linesHeight ) {

                  return (int) ( linesHeight + mTopMoved + mBottomMoved );
            }

            /**
             * 用于手指释放后,自动折叠或者展开
             *
             * @param direction 方向,1为展开方向,-1为折叠方向
             */
            protected void setAnimateState ( int direction ) {

                  mDirection = direction;
                  mStateManager.setState( STATE_ANIMATE );
                  animateIfNeed();
            }

            /**
             * 用于判断是否已经处于需要的状态,即:已经折叠或者展开;如果没有完成,按照每次移动基础距离的1/5的速度继续进行,
             * 该方法的正确运行需要{@link #computeScroll()}中不断判断
             */
            protected void animateIfNeed ( ) {

                  if( needMockMove() ) {
                        if( calculateMovedByDy( mDirection * mCellHeight / 5f ) ) {
                              requestLayout();
                              ( (MonthLayout) getParent() ).onCurrentItemVerticalMove( mTopMoved + mBottomMoved );
                        }
                  }
            }

            /**
             * 判断是否已经完成折叠或者展开
             *
             * @return true :没有完成,需要继续布局直至完成
             */
            protected boolean needMockMove ( ) {

                  int state = mStateManager.getState();
                  if( state == STATE_ANIMATE ) {
                        if( mDirection == 1 ) {
                              boolean result = mTopMoved != 0 || mBottomMoved != 0;
                              if( !result ) {
                                    mDirection = 0;
                                    mStateManager.setState( STATE_EXPAND );
                                    MonthLayout parent = (MonthLayout) getParent();
                                    parent.onMonthModeChange( mDate, mPosition, true );
                              }
                              return result;
                        }

                        if( mDirection == -1 ) {
                              int topDis = mCurrentSelectedPosition / 7 * mCellHeight;
                              int bottomDis = mPageHeight - ( topDis + mCellHeight );
                              boolean result = mTopMoved != -topDis || mBottomMoved != -bottomDis;
                              if( !result ) {
                                    mDirection = 0;
                                    mStateManager.setState( STATE_FOLDED );
                                    MonthLayout parent = (MonthLayout) getParent();
                                    parent.onMonthModeChange( mDate, mPosition, false );
                              }
                              return result;
                        }
                  }

                  return false;
            }

            /**
             * 强制停止没有完成的展开折叠动画
             */
            protected void forceStopAnimateIfRunning ( ) {

                  if( mStateManager.getState() == STATE_ANIMATE ) {
                        mStateManager.setState( STATE_MOVING );
                  }
            }

            /**
             * 手势释放后,当无法判断最终需要折叠或者展开时使用此方法设置最终状态
             *
             * @param isMonthMode {@link MonthLayout}当前是否是月显示模式
             */
            protected void checkAnimateState ( boolean isMonthMode ) {

                  if( mDirection != 0 ) {
                        setAnimateState( mDirection );
                        return;
                  }

                  if( isMonthMode ) {
                        setAnimateState( 1 );
                  } else {
                        setAnimateState( -1 );
                  }
            }
      }
}