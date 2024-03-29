package com.vollo.launcher;

import com.vollo.launcher.HolderLayout.OnFadingListener;
import com.vollo.launcher.LauncherSettings.Applications;
import com.vollo.launcher.LauncherSettings.BaseLauncherColumns;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;

public class SlidingView extends AdapterView<ArrayAdapter> {

    private static final String TAG = "SlidingView";

    private static final int DEFAULT_SCREEN = 0;
    private static final int INVALID_SCREEN = -1;
    private static final int SNAP_VELOCITY = 400;
    private static final int AIMIATE_TIME = 800;
    private static final int WATI_TIME = 810;

    public int mCurrentScreen;
    public int mTotalScreens;
    private int mCurrentHolder = 1;
    private int mPageWidth;
    private final int mDefaultScreen = DEFAULT_SCREEN;
    private int mNextScreen = INVALID_SCREEN;
    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;
    private float mLastMotionX;
    private float mLastMotionY;

    static final int TOUCH_STATE_DOWN = 3;
    static final int TOUCH_STATE_TAP = 4;
    static final int TOUCH_STATE_DONE_WAITING = 5;

    private final static int TOUCH_STATE_REST = 0;
    private final static int TOUCH_STATE_SCROLLING = 1;
    private int mTouchState = TOUCH_STATE_REST;
    private int mTouchSlop;
    private int mMaximumVelocity;

    private boolean mFirstLayout = true;

    private ArrayAdapter mAdapter;
    private boolean mBorderViewScroll = false;
    private final static int BORDER_CHECK_SCROLL = 20;
    private static final float BASELINE_FLING_VELOCITY = 2500.f;
    private static final float FLING_VELOCITY_INFLUENCE = 0.4f;
    /**
     * Should be used by subclasses to listen to changes in the dataset
     */
    AdapterDataSetObserver mDataSetObserver;
    public boolean mDataChanged;
    public int mItemCount;
    public int mOldItemCount;

    private int mPageHorizontalMargin = 0;

    static final int DEFAULT_ROW_NUM = 4;
    static final int DEFAULT_COL_NUM = 4;

    private int mDragViewIndex=0;
    private int mNumColumns;
    private int mNumRows;
    private int mNumPerPage;
//    private int mPageIndicatorSpace = 10;
    static final int LAYOUT_NORMAL = 0;
    static final int LAYOUT_SCROLLING = 1;
    int mLayoutMode = LAYOUT_NORMAL;

    /**
     * Should be used by subclasses to listen to changes in the dataset
     */
    /**
     * Indicates whether the list selector should be drawn on top of the children or behind
     */
    boolean mDrawSelectorOnTop = false;

    /**
     * The drawable used to draw the selector
     */
    Drawable mSelector;

    /**
     * Defines the selector's location and dimension at drawing time
     */
    Rect mSelectorRect = new Rect();

    /**
     * The selection's left padding
     */
    int mSelectionLeftPadding = 0;

    /**
     * The selection's top padding
     */
    int mSelectionTopPadding = 0;

    /**
     * The selection's right padding
     */
    int mSelectionRightPadding = 0;

    /**
     * The selection's bottom padding
     */
    int mSelectionBottomPadding = 0;

    /**
     * The last CheckForLongPress runnable we posted, if any
     */
    private CheckForLongPress mPendingCheckForLongPress;

    /**
     * The last CheckForTap runnable we posted, if any
     */
    private Runnable mPendingCheckForTap;

    /**
     * The last CheckForKeyLongPress runnable we posted, if any
     */
    private CheckForKeyLongPress mPendingCheckForKeyLongPress;
    private int mCheckTapPosition;
    private int mSelectedPosition= INVALID_POSITION;

    /**
     * Acts upon click
     */
    private SlidingView.PerformClick mPerformClick;

    /**
     * The data set used to store unused views that should be reused during the next layout
     * to avoid creating new ones
     */
    final RecycleBin mRecycler = new RecycleBin();

    //ADW:Hack the texture thing to make scrolling faster
    private Paint mPaint;
    private int mCacheColorHint = 0;
    private boolean mBlockLayouts;
    private ScreenIndicator mPager;
    private int mScrollToScreen;
    //ADW: Animation variables
    private boolean isAnimating = false;
    private OnFadingListener mFadingListener;
    //private int mBgAlpha = 255;
    //private int mTargetAlpha = 255;
    private int mAnimationDuration = 800;
    //ADW: speed for new scrolling transitions
    //private final int mScrollingSpeed = 600;
    //ADW: bounce scroll
    private final int mScrollingBounce = 50;
    //ADW:Bg color
    //private int mBgColor = 0xFF000000;
    private float mZoom;

	public SlidingView(Context context) {
		super(context);
		initWorkspace();
	}

	public SlidingView(Context context, AttributeSet attrs) {
		this(context, attrs, com.android.internal.R.attr.absListViewStyle);
		initWorkspace();
	}

	public SlidingView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.obtainStyledAttributes(attrs,
		        R.styleable.SlidingView, defStyle, 0);

		Drawable d = a.getDrawable(R.styleable.SlidingView_listSelector);
		if (d != null) {
		    setSelector(d);
		}

		mDrawSelectorOnTop = true;
		mPageHorizontalMargin = a.getDimensionPixelSize(R.styleable.SlidingView_pager_hormargin, 8);
//		mNumRows = a.getInt(R.styleable.SlidingView_slideNumRows, DEFAULT_ROW_NUM);
//		mNumColumns = a.getInt(R.styleable.SlidingView_slideNumCloums, DEFAULT_COL_NUM);
		mNumRows = Configurator.getIntegerConfig(context, Configurator.CONFIG_SLIDENUMROWS, DEFAULT_ROW_NUM);
		mNumColumns = Configurator.getIntegerConfig(context, Configurator.CONFIG_SLIDENUMCLOUMS, DEFAULT_COL_NUM);
        
		mNumPerPage = mNumRows * mNumColumns;

		a.recycle();
		initWorkspace();		
	}

    @Override
    public boolean isOpaque() {
        return mZoom > 0.999f;
    }

    private void initWorkspace() {
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        mDrawSelectorOnTop = false;
        setFocusable(true);
        setFocusableInTouchMode(true);
        setWillNotDraw(false);
        mScroller = new Scroller(getContext());
        mCurrentScreen = mDefaultScreen;
        mScroller.forceFinished(true);
        mPaint = new Paint();
        mPaint.setDither(false);

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        //mPager = new ScreenIndicator(getContext());
        //ADW: listener to handle holderlayouts animations
        mFadingListener = new OnFadingListener() {

			@Override
			public void onUpdate(int Status) {
				// TODO Auto-generated method stub
				if (Status == OnFadingListener.CLOSE) {
//					setVisibility(View.GONE);
				} else {
					isAnimating = false;
//					mPager.setVisibility(VISIBLE);
					//mBgAlpha = mTargetAlpha;
				}
			}

			@Override
			public void onAlphaChange(float alphaPercent) {
				// TODO Auto-generated method stub
				//mBgAlpha = (int) (mTargetAlpha * alphaPercent);
				//ADW: hack to redraw pager background..... :-(
				//invalidate(mPager.getLeft(), mPager.getTop(), mPager.getRight(), mPager.getBottom());
			}
		};
    }

    @Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);
		//mPager.setLeft(l);
        if (mLayoutMode == LAYOUT_SCROLLING) {
			final int screenWidth = mPageWidth;
	        final int whichScreen = (getScrollX() + (screenWidth / 2)) / screenWidth;
	        if (whichScreen != mScrollToScreen) {
	        	if (mScrollToScreen != INVALID_POSITION) {
	        		addRemovePages(mScrollToScreen, whichScreen);
	        	}
	        	mScrollToScreen = whichScreen;
	        	mPager.setCurrentScreen(whichScreen);
	        }
        }
	}

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        } else if (mNextScreen != INVALID_SCREEN) {
        	mNextScreen = INVALID_SCREEN;
        	mLayoutMode = LAYOUT_NORMAL;
        	findCurrentHolder();
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int saveCount = 0;
        final boolean clipToPadding = (mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK;
	    //canvas.drawARGB(mBgAlpha, Color.red(mBgColor), Color.green(mBgColor), Color.blue(mBgColor));

        if (clipToPadding) {
            saveCount = canvas.save();
            final int scrollX = mScrollX;
            final int scrollY = mScrollY;
            canvas.clipRect(scrollX + mPaddingLeft, scrollY,
                    scrollX + mRight - mLeft,
                    scrollY + mBottom - mTop);
            mGroupFlags &= ~CLIP_TO_PADDING_MASK;
        }

        final boolean drawSelectorOnTop = mDrawSelectorOnTop;
        if (!drawSelectorOnTop) {
            drawSelector(canvas);
        }

        super.dispatchDraw(canvas);

        if (drawSelectorOnTop) {
            drawSelector(canvas);
        }

        if (clipToPadding) {
            canvas.restoreToCount(saveCount);
            mGroupFlags |= CLIP_TO_PADDING_MASK;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(widthSize, heightSize);
        mPageWidth = widthSize;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    	super.onLayout(changed, left, top, right, bottom);
    	if (mFirstLayout) {
    		TextView tvEmpty = new TextView(getContext());
//            mPager.setScreenCount(mTotalScreens);
//            mPager.setAlwaysDrawnWithCacheEnabled(false);
            LinearLayout.LayoutParams params =
            	new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//            mPager.measure(mPageWidth, mPageIndicatorSpace);
//            mPager.layout(0, 0, mPageWidth, mPageIndicatorSpace);
//            addViewInLayout(mPager, getChildCount(), params);
            addViewInLayout(tvEmpty, getChildCount(), params);
            mFirstLayout = false;
    	}
    	if (!mBlockLayouts) {
    		layoutChildren();
    	}
    	invalidate();
    }

    private void layoutChildren() {
        final RecycleBin recycleBin = mRecycler;

        for (int i = 0; i < getChildCount(); i++) {
        	if (getChildAt(i) instanceof HolderLayout) {
	        	final ViewGroup h = (ViewGroup) getChildAt(i);
	        	for (int j = 0; j < h.getChildCount(); j++) {
	        		recycleBin.addScrapView(h.getChildAt(j));
	        	}
        	}
        }

        detachViewsFromParent(1, getChildCount());
        mDragViewIndex = 0;
		makePage(mCurrentScreen - 1);
		makePage(mCurrentScreen);
		makePage(mCurrentScreen + 1);
        requestFocus();
        setFocusable(true);
        mDataChanged = false;
        mBlockLayouts = true;
        findCurrentHolder();
    }

    public void makePage(int pageNum) {
    	if (pageNum < 0 || pageNum > (mTotalScreens - 1)) {
    		return;
    	}

		final int pageSpacing = pageNum * mPageWidth;
		final int startPos = pageNum * mNumPerPage;
		final int marginTop = getPaddingTop();
		final int marginBottom = getPaddingBottom();
		final int marginLeft = getPaddingLeft() + mPageHorizontalMargin;
		final int marginRight = getPaddingRight() + mPageHorizontalMargin;
		final int actualWidth = getMeasuredWidth() - marginLeft - marginRight;
		final int actualHeight = getMeasuredHeight() - marginTop - marginBottom;
		final int columnWidth = actualWidth / mNumColumns;
		final int rowHeight = actualHeight / mNumRows;

        SlidingView.LayoutParams p =
        	new SlidingView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        		ViewGroup.LayoutParams.MATCH_PARENT);
        int pos = startPos;
        int x = marginLeft;
        int y = marginTop;
        HolderLayout holder = new HolderLayout(getContext());

        for (int i = 0; i < mNumRows; i++) {
        	for (int j = 0; j < mNumColumns; j++) {
        		if (pos < mAdapter.getCount()) {
		            View child = obtainView(pos);
		            child.setLayoutParams(p);
		            child.setSelected(false);
		            child.setPressed(false);
		            int childHeightSpec = getChildMeasureSpec(
		                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, p.height);
		            int childWidthSpec = getChildMeasureSpec(
		                    MeasureSpec.makeMeasureSpec(columnWidth, MeasureSpec.EXACTLY), 0, p.width);
		            child.measure(childWidthSpec, childHeightSpec);
        			int left = x;
        			int top = y;
        			int w = columnWidth;
        			int h = child.getMeasuredHeight();
        			//int h = rowHeight;
        			child.layout(left, top, left + w, top + h);
        			if(pos + 1 == mDragViewIndex){
        				child.setVisibility(View.INVISIBLE);
        			}else{
        				child.setVisibility(View.VISIBLE);
        			}
					holder.addViewInLayout(child, holder.getChildCount(), p, true);
					pos++;
		        	x += columnWidth;
        		}
        	}
        	x = marginLeft;
    		y += rowHeight;
        }
        SlidingView.LayoutParams holderParams = new SlidingView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        holder.layout(pageSpacing, (actualHeight % mNumRows) / 2, pageSpacing + mPageWidth, getMeasuredHeight());
        holder.setTag(pageNum);
        holder.setOnFadingListener(mFadingListener);
        addViewInLayout(holder, getChildCount(), holderParams, true);
        if (pageNum == mCurrentScreen && isAnimating) {
        	holder.open(isAnimating, mAnimationDuration);
        }
    }

    private Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			//mLauncher.getAllAppsView().updateAllData();
			mAdapter.notifyDataSetChanged();
		}
	};

	public void removePackage(ApplicationInfoEx slideInfo) {
		final ContentResolver cr = getContext().getContentResolver();
		final Uri updateUri = Applications.getCustomUri("/insertfolder");
		cr.update(updateUri, null, null, new String[] { String.valueOf(slideInfo.position) });

		final Uri deleteUri = Applications.getContentUri(slideInfo.id, true);
		cr.delete(deleteUri, null, null);
	}
	
	private boolean checkAppExist(ApplicationInfo item){
		final PackageManager packageManager = getContext().getPackageManager();
        ResolveInfo resolveInfo = packageManager.resolveActivity(item.intent, 1);
        String intentInfo = resolveInfo.activityInfo.applicationInfo.packageName + "|" + 
        	resolveInfo.activityInfo.name;
        String title = resolveInfo.loadLabel(packageManager).toString();
        
        final ContentResolver cr = getContext().getContentResolver();
        Cursor c = cr.query(Applications.CONTENT_URI_NO_NOTIFICATION,
            new String[] { "title", "intent" }, "title=? and intent=?",
            new String[] { title, intentInfo }, null);
        boolean result = false;
        try {
            result = c.moveToFirst();
        } finally {
            c.close();
        }
        
        Log.d(TAG, "checkAppExist result="+result+", intent="+intentInfo);
        return result;
	}

	public long addPackage(ApplicationInfo item) {
		if(checkAppExist(item)){
			Log.e(TAG, "addPackage fail! intent="+item.intent);
			return -1;
		}
		
        final PackageManager packageManager = getContext().getPackageManager();
        final ContentResolver cr = getContext().getContentResolver();
        final Uri queryUri = Applications.CONTENT_URI_NO_NOTIFICATION;
        ResolveInfo resolveInfo = packageManager.resolveActivity(item.intent, 1);
     	ContentValues values = new ContentValues();
        String intentInfo =
        	resolveInfo.activityInfo.applicationInfo.packageName + "|" + resolveInfo.activityInfo.name;

        if (!values.containsKey(BaseLauncherColumns.TITLE)) {
            values.put(BaseLauncherColumns.TITLE, resolveInfo.loadLabel(packageManager).toString());
        }

        values.put(BaseLauncherColumns.TITLE, resolveInfo.loadLabel(packageManager).toString());
        values.put(BaseLauncherColumns.INTENT, intentInfo);
        values.put(Applications.CONTAINER, Applications.CONTAINER_APPS);
        if (mAdapter != null) {
        	values.put(Applications.POSITION, mAdapter.getCount());
        } else {
            Cursor c = cr.query(queryUri, new String[]{ BaseColumns._ID },
      				Applications.CONTAINER + "=?",
      				new String[] { String.valueOf(Applications.CONTAINER_APPS) },
      				null);
     		if (c != null) {
     			values.put(Applications.POSITION, c.getCount());
     			c.close();
     		}
        }
        values.put(BaseLauncherColumns.ITEM_TYPE, Applications.APPS_TYPE_APP);
     	values.put(Applications.SYSAPP, 0);
     	values.put(Applications.PACKAGENAME, resolveInfo.activityInfo.applicationInfo.packageName);
     	// do not save icon bitmap to db
//     	Bitmap bitmap = Utilities.createIconBitmap(
//     			resolveInfo.activityInfo.loadIcon(packageManager), mContext);
//     	values.put(Applications.ICON_BITMAP, Utilities.getBitmapByte(bitmap));

        Uri insert = cr.insert(queryUri, values);
        if (insert != null) {
        	return Long.parseLong(insert.getLastPathSegment());
        } else {
        	return -1;
        }
	}

	public void deleteFolder(ApplicationFolderInfo foldInfo, ArrayList<ApplicationInfoEx> contents) {
		final int count = contents.size();
		final ContentResolver cr = getContext().getContentResolver();
		final Uri deleteUri = Applications.getContentUri(foldInfo.id, true);
		cr.delete(deleteUri, null, null);

		if (count > 0) {
			int index = 0;
			ApplicationInfoEx infoEx = contents.get(index);
			if (infoEx != null) {
				ContentValues values = new ContentValues();
				values.put(BaseLauncherColumns.ITEM_TYPE, Applications.APPS_TYPE_APP);
				values.put(Applications.POSITION, foldInfo.position);
				values.put(Applications.CONTAINER, Applications.CONTAINER_APPS);

				cr.update(Applications.getContentUri(infoEx.id, true), values, null, null);
			}

			Cursor c = cr.query(Applications.CONTENT_URI,
				new String[] { BaseColumns._ID },
				Applications.CONTAINER + "=?",
				new String[] { String.valueOf(Applications.CONTAINER_APPS) },
				null);
			int lastIndex = 0;
			if (c != null) {
				lastIndex = c.getCount();
				c.close();
			}

			index++;
			for (; index < count; index++) {
				infoEx = contents.get(index);
				if (infoEx != null) {
					ContentValues values = new ContentValues();
					values.put(BaseLauncherColumns.ITEM_TYPE, Applications.APPS_TYPE_APP);
					values.put(Applications.CONTAINER, Applications.CONTAINER_APPS);
					values.put(Applications.POSITION, lastIndex);

					cr.update(Applications.getContentUri(infoEx.id, true), values, null, null);
					lastIndex++;
				}
			}
			return;
		} else {
			View view = getViewAtPosition(foldInfo.position);
			if(view == null){
				Log.e(TAG, "deleteFolder error! position="+foldInfo.position+", mCurrentHolder="+mCurrentHolder);
				return;
			}
			view.setVisibility(GONE);

			final int marginTop = getPaddingTop();
			final int marginBottom = getPaddingBottom();
			final int marginLeft = getPaddingLeft() + mPageHorizontalMargin;
			final int marginRight = getPaddingRight() + mPageHorizontalMargin;
			final int actualWidth = getMeasuredWidth() - marginLeft - marginRight;
			final int actualHeight = getMeasuredHeight() - marginTop - marginBottom;
			final int columnWidth = actualWidth / mNumColumns;
			final int rowHeight = actualHeight / mNumRows;
			HolderLayout holder = (HolderLayout) getChildAt(mCurrentHolder);

			int initPos = foldInfo.position;
			int nCount = mAdapter.getCount();
			int init = initPos + 1;
			int lastPos = (mCurrentScreen + 1) * mNumPerPage - 1;
			if (nCount <= lastPos) {
				lastPos = nCount - 1;
			}

			if (initPos == lastPos) {
				// If the folder is in lastPos, should remove folder's view first
				View childFolder = getViewAtPosition(initPos);
				holder.removeView(childFolder);
			}

	    	// Move first child in next page to last position of current page
	    	for (; init <= lastPos; init++) {
	    		View child = getViewAtPosition(init);
	    		if (child == null) {
	    			continue;
	    		}
	    		int x1 = marginLeft + (init % mNumPerPage) % mNumColumns * columnWidth;
	    		int y1 = marginTop + (init % mNumPerPage) / mNumColumns * rowHeight;
	    		int x2 = marginLeft + ((init - 1) % mNumPerPage) % mNumColumns * columnWidth;
	    		int y2 = marginTop + ((init - 1) % mNumPerPage) / mNumColumns * rowHeight;

	    		ItemAnimate itemAnimate = new ItemAnimate(x1, x2, y1, y2, child);
	    		itemAnimate.setDuration(AIMIATE_TIME);
	    		itemAnimate.setSquare(columnWidth, child.getMeasuredHeight());
	    		itemAnimate.start();
	    	}

	    	if (initPos < lastPos) {
	    		// Else remove the folder's view after animation
		    	View childFolder = getViewAtPosition(initPos);
		    	holder.removeView(childFolder);
	    	}

	    	if (nCount > (lastPos + 1)) {
	    		View child = null;
	    		ViewGroup nextHolder = getNextHolderView();
	    		if (nextHolder != null) {
	 	    		child = nextHolder.getChildAt(0);
	 	    	}
	 	    	if(child != null){
		    		SlidingView.LayoutParams p = (SlidingView.LayoutParams) child.getLayoutParams();
		    		nextHolder.removeView(child);
		    		child.layout(-300, -300, -300 + columnWidth, -300 + child.getMeasuredHeight());
		    		holder.addViewInLayout(child, mNumPerPage - 1, p, true);
		    		int x2 = marginLeft + (lastPos % mNumPerPage) % mNumColumns * columnWidth;
		    		int y2 = marginTop + (lastPos % mNumPerPage) / mNumColumns * rowHeight;
	
		    		ItemAnimate itemAnimate = new ItemAnimate(x2 + columnWidth, x2, y2 - rowHeight * 2, y2, child);
		    		itemAnimate.setDuration(AIMIATE_TIME);
		    		itemAnimate.setSquare(columnWidth, child.getMeasuredHeight());
		    		itemAnimate.start();
	 	    	}else{
	 	    		Log.w(TAG, "deleteFolder error! cant get next page view, nCount > (lastPos + 1), initPos="+initPos);
	 	    	}
	    	}

    		final Uri updateUri = Applications.getCustomUri("/insertfolder");
			cr.update(updateUri, null, null, new String[] { String.valueOf(initPos) });
		}

		Thread myThread = new Thread() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					Thread.sleep(WATI_TIME);
					myHandler.sendEmptyMessage(0);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		myThread.start();
	}

    public void moveFolderItemToPosition(int insertPos, ApplicationInfoEx slideInfo) {
    	final int marginTop = getPaddingTop();
        final int marginBottom = getPaddingBottom();
        final int marginLeft = getPaddingLeft() + mPageHorizontalMargin;
        final int marginRight = getPaddingRight() + mPageHorizontalMargin;
        final int actualWidth = getMeasuredWidth() - marginLeft - marginRight;
        final int actualHeight = getMeasuredHeight() - marginTop - marginBottom;
        final int columnWidth = actualWidth / mNumColumns;
        final int rowHeight = actualHeight / mNumRows;
        HolderLayout holder = (HolderLayout) getChildAt(mCurrentHolder);

        int nCount = mAdapter.getCount();
        int lastPos = (mCurrentScreen + 1) * mNumPerPage - 1;
        int init = insertPos;

        //the last page, the current page last view no need to show page turning animation
        if (nCount <= lastPos) {
        	lastPos = nCount - 1;
        	for (; init <= lastPos; init++) {
        		View child = getViewAtPosition(init);
        		int x1 = marginLeft + (init % mNumPerPage) % mNumColumns * columnWidth;
        		int y1 = marginTop + (init % mNumPerPage) / mNumColumns * rowHeight;
        		int x2 = marginLeft + ((init + 1) % mNumPerPage) % mNumColumns * columnWidth;
        		int y2 = marginTop + ((init + 1) % mNumPerPage) / mNumColumns * rowHeight;

        		ItemAnimate itemAnimate = new ItemAnimate(x1, x2, y1, y2, child);
        		itemAnimate.setDuration(AIMIATE_TIME);
        		itemAnimate.setSquare(columnWidth, child.getMeasuredHeight());
        		itemAnimate.start();
        	}
        } else {
        	for (; init < lastPos; init++) {
        		View child = getViewAtPosition(init);
        		int x1 = marginLeft + (init % mNumPerPage) % mNumColumns * columnWidth;
        		int y1 = marginTop + (init % mNumPerPage) / mNumColumns * rowHeight;
        		int x2 = marginLeft + ((init + 1) % mNumPerPage) % mNumColumns * columnWidth;
        		int y2 = marginTop + ((init + 1) % mNumPerPage) / mNumColumns * rowHeight;

        		ItemAnimate itemAnimate = new ItemAnimate(x1, x2, y1, y2, child);
        		itemAnimate.setDuration(AIMIATE_TIME);
        		itemAnimate.setSquare(columnWidth, child.getMeasuredHeight());
        		itemAnimate.start();
        	}

        	View child = getViewAtPosition(lastPos);
        	int x1 = marginLeft + (lastPos % mNumPerPage) % mNumColumns * columnWidth;
    		int y1 = marginTop + (lastPos % mNumPerPage) / mNumColumns * rowHeight;

    		ItemAnimate itemAnimate = new ItemAnimate(x1, x1 + columnWidth, y1, y1 - rowHeight * 2, child);
    		itemAnimate.setDuration(AIMIATE_TIME);
    		itemAnimate.setSquare(columnWidth, child.getMeasuredHeight());
    		itemAnimate.start();
        }

    	//create a new view
    	final LayoutInflater mInflater = LayoutInflater.from(getContext());
    	TextView textView = (TextView) mInflater.inflate(R.layout.application_boxed, this, false);
		textView.setCompoundDrawablesWithIntrinsicBounds(
		 		null, 
		  		new BitmapDrawable(slideInfo.iconBitmap), 
		  		null, 
		  		null);
		textView.setText(slideInfo.title);

		int left = marginLeft + (insertPos % mNumPerPage) % mNumColumns * columnWidth;
		int top  = marginTop + (insertPos % mNumPerPage) / mNumColumns * rowHeight;

		SlidingView.LayoutParams p =
			new SlidingView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
	        		ViewGroup.LayoutParams.MATCH_PARENT);
	    textView.setLayoutParams(p);
	    textView.setSelected(false);
	    textView.setPressed(false);
	    textView.setVisibility(View.VISIBLE);
	    int childHeightSpec = getChildMeasureSpec(
	            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, p.height);
        int childWidthSpec = getChildMeasureSpec(
                MeasureSpec.makeMeasureSpec(columnWidth, MeasureSpec.EXACTLY), 0, p.width);
        textView.measure(childWidthSpec, childHeightSpec);

		textView.layout(left, top, left + columnWidth, top + textView.getMeasuredHeight());	
		holder.addViewInLayout(textView, insertPos % mNumPerPage, p, true);
		holder.invalidate();

		//database operator
   		final ContentResolver cr = getContext().getContentResolver();
   		final Uri updateUri = Applications.getCustomUri("/addfolder");
		cr.update(updateUri, null, null, new String[] { String.valueOf(insertPos) });

   		ContentValues values = new ContentValues();
		values.put(Applications.CONTAINER, Applications.CONTAINER_APPS);
		values.put(Applications.POSITION, insertPos);
		values.put(BaseLauncherColumns.ITEM_TYPE, Applications.APPS_TYPE_APP);
		cr.update(Applications.getContentUri(slideInfo.id, true), values, null, null);

		Thread myThread = new Thread() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					Thread.sleep(WATI_TIME);
					myHandler.sendEmptyMessage(0);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		myThread.start();
    }

    public void moveFolderItemToFolder(ApplicationInfoEx initInfo, ApplicationFolderInfo lastInfo) {
    	if (initInfo.container != lastInfo.id) {
            final ContentResolver cr = getContext().getContentResolver();            
       		ContentValues values = new ContentValues();
   			values.put(Applications.CONTAINER, lastInfo.id);
   			values.put(BaseLauncherColumns.ORDERID, initInfo.orderId);
   			cr.update(Applications.getContentUri(initInfo.id, true), values, null, null);
    	}
    }

    public void moveItemToFolder(ApplicationInfoEx initInfo, long lastID) {
    	final int marginTop = getPaddingTop();
        final int marginBottom = getPaddingBottom();
        final int marginLeft = getPaddingLeft() + mPageHorizontalMargin;
        final int marginRight = getPaddingRight() + mPageHorizontalMargin;
        final int actualWidth = getMeasuredWidth() - marginLeft - marginRight;
        final int actualHeight = getMeasuredHeight() - marginTop - marginBottom;
        final int columnWidth = actualWidth / mNumColumns;
        final int rowHeight = actualHeight / mNumRows;
        HolderLayout holder = (HolderLayout) getChildAt(mCurrentHolder);

        int initPos = initInfo.position;
        int nCount = mAdapter.getCount();
    	int init = initPos + 1;
    	int lastPos = (mCurrentScreen + 1) * mNumPerPage - 1;

    	if (nCount <= lastPos) {
    		lastPos = nCount - 1;
    	}

    	for (; init <= lastPos; init++) {
    		View child = getViewAtPosition(init);
    		int x1 = marginLeft + (init % mNumPerPage) % mNumColumns * columnWidth;
    		int y1 = marginTop + (init % mNumPerPage) / mNumColumns * rowHeight;
    		int x2 = marginLeft + ((init - 1) % mNumPerPage) % mNumColumns * columnWidth;
    		int y2 = marginTop + ((init - 1) % mNumPerPage) / mNumColumns * rowHeight;

    		ItemAnimate itemAnimate = new ItemAnimate(x1, x2, y1, y2, child);
    		itemAnimate.setDuration(AIMIATE_TIME);
    		itemAnimate.setSquare(columnWidth, child.getMeasuredHeight());
    		itemAnimate.start();
    	}

    	View lastView = getViewAtPosition(initPos);
    	holder.removeView(lastView);

    	if (nCount > (lastPos + 1)) {
    		View child = null;
    		ViewGroup nextHolder = getNextHolderView();
    		if (nextHolder != null) {
 	    		child = nextHolder.getChildAt(0);
 	    	}
    		
 	    	if(child != null){
 	    		SlidingView.LayoutParams p = (SlidingView.LayoutParams) child.getLayoutParams();
 	    		nextHolder.removeView(child);
 	    		child.setVisibility(View.VISIBLE);
 	    		child.layout(-300, -300, -300 + columnWidth, -300 + child.getMeasuredHeight());
 	    		holder.addViewInLayout(child, mNumPerPage - 1, p, true);
 	    		int x2 = marginLeft + (lastPos % mNumPerPage) % mNumColumns * columnWidth;
 	    		int y2 = marginTop + (lastPos % mNumPerPage) / mNumColumns * rowHeight;

 	    		ItemAnimate itemAnimate = new ItemAnimate(x2 + columnWidth, x2, y2 - rowHeight * 2, y2, child);
 	    		itemAnimate.setDuration(AIMIATE_TIME);
 	    		itemAnimate.setSquare(columnWidth, child.getMeasuredHeight());
 	    		itemAnimate.start();
 	    	}else{
 	    		Log.w(TAG, "moveItemToFolder error! cant get next page view, nCount > (lastPos + 1), initPos="+initPos);
 	    	}

    	}

    	// Operate database
		final ContentResolver cr = getContext().getContentResolver();
		final Uri updateUri = Applications.getCustomUri("/insertfolder");
		cr.update(updateUri, null, null, new String[] { String.valueOf(initPos) });

		ContentValues values = new ContentValues();
		values.put(Applications.CONTAINER, lastID);
		values.put(BaseLauncherColumns.ORDERID, initInfo.orderId);
		values.put(BaseLauncherColumns.ITEM_TYPE, initInfo.itemType);

		cr.update(Applications.getContentUri(initInfo.id, false), values, null, null);

    	Thread myThread = new Thread() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					Thread.sleep(WATI_TIME);
					myHandler.sendEmptyMessage(0);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		myThread.start();
    }

    public void moveItemToPosition(int initPos, int lastPos, long initID) {
    	final int marginTop = getPaddingTop();
        final int marginBottom = getPaddingBottom();
        final int marginLeft = getPaddingLeft() + mPageHorizontalMargin;
        final int marginRight = getPaddingRight() + mPageHorizontalMargin;
        final int actualWidth = getMeasuredWidth() - marginLeft - marginRight;
        final int actualHeight = getMeasuredHeight() - marginTop - marginBottom;
        final int columnWidth = actualWidth / mNumColumns;
        final int rowHeight = actualHeight / mNumRows;
        HolderLayout holder = (HolderLayout) getChildAt(mCurrentHolder);

        if (lastPos > initPos) {
    	    int init = initPos + 1;
    		for (; init <= lastPos; init++) {
        	    View child = getViewAtPosition(init);
        		int x1 = marginLeft + (init % mNumPerPage) % mNumColumns * columnWidth;
        		int y1 = marginTop + (init % mNumPerPage) / mNumColumns * rowHeight;
        		
        		int x2 = marginLeft + ((init - 1) % mNumPerPage) % mNumColumns * columnWidth;
        		int y2 = marginTop + ((init - 1) % mNumPerPage) / mNumColumns * rowHeight;

        		ItemAnimate itemAnimate = new ItemAnimate(x1, x2, y1, y2, child);
        		itemAnimate.setDuration(AIMIATE_TIME);
        		itemAnimate.setSquare(columnWidth, child.getMeasuredHeight());
        		itemAnimate.start();
        	}

        	View child = getViewAtPosition(initPos);
			int left = marginLeft + (lastPos % mNumPerPage) % mNumColumns * columnWidth;
			int top = marginTop + (lastPos % mNumPerPage) / mNumColumns * rowHeight;
			SlidingView.LayoutParams p = (SlidingView.LayoutParams) child.getLayoutParams();

 			child.layout(left, top, left + columnWidth, top + child.getMeasuredHeight());
 			child.setVisibility(View.VISIBLE);
			holder.removeView(child);
            holder.addViewInLayout(child, lastPos % mNumPerPage, p, true);

        	//database operator
       		final ContentResolver cr = getContext().getContentResolver();
       		final Uri updateUri = Applications.getCustomUri("/movebehind");
			cr.update(updateUri, null, null, new String[] { String.valueOf(initPos), String.valueOf(lastPos) });

       		ContentValues values = new ContentValues();
   			values.put(Applications.POSITION, lastPos);
            cr.update(Applications.getContentUri(initID, true), values, null, null);
    	} else if (lastPos == initPos) {
    		View child = getViewAtPosition(initPos);
    		int left = marginLeft + (lastPos % mNumPerPage) % mNumColumns * columnWidth;
			int top = marginTop + (lastPos % mNumPerPage) / mNumColumns * rowHeight;
			
			child.setVisibility(View.VISIBLE);
 			child.layout(left, top, left + columnWidth, top + child.getMeasuredHeight());
    	} else {
    		int init = initPos - 1;
    		for (; init >= lastPos; init--) {
        		View child = getViewAtPosition(init);
        		int x1 = marginLeft + ((init) % mNumPerPage) % mNumColumns * columnWidth;
        		int y1 = marginTop + ((init) % mNumPerPage) / mNumColumns * rowHeight;
        		int x2 = marginLeft + ((init + 1) % mNumPerPage) % mNumColumns * columnWidth;
        		int y2 = marginTop + ((init + 1) % mNumPerPage) / mNumColumns * rowHeight;
        		
        		ItemAnimate itemAnimate = new ItemAnimate(x1, x2, y1, y2, child);
        		itemAnimate.setDuration(AIMIATE_TIME);
        		itemAnimate.setSquare(columnWidth, child.getMeasuredHeight());
        		itemAnimate.start();
        	}

        	View child = getViewAtPosition(initPos);
			int left = marginLeft + (lastPos % mNumPerPage) % mNumColumns * columnWidth;
			int top = marginTop + (lastPos % mNumPerPage) / mNumColumns * rowHeight;
			SlidingView.LayoutParams p = (SlidingView.LayoutParams) child.getLayoutParams();

 			child.layout(left, top, left + columnWidth, top + child.getMeasuredHeight());
 			child.setVisibility(View.VISIBLE);
			holder.removeView(child);
			holder.addViewInLayout(child, lastPos % mNumPerPage, p, true);

        	//database operator
       		final ContentResolver cr = getContext().getContentResolver();
       		final Uri updateUri = Applications.getCustomUri("/movefront");
			cr.update(updateUri, null, null, new String[] { String.valueOf(lastPos), String.valueOf(initPos) });

       		ContentValues values = new ContentValues();
   			values.put(Applications.POSITION, lastPos);
   			cr.update(Applications.getContentUri(initID, true), values, null, null);
    	}
    }

    public void moveFolderItemToPositionSlide(int lastPos, ApplicationInfoEx slideInfo) {
    	final int marginTop = getPaddingTop();
        final int marginBottom = getPaddingBottom();
        final int marginLeft = getPaddingLeft() + mPageHorizontalMargin;
        final int marginRight = getPaddingRight() + mPageHorizontalMargin;
        final int actualWidth = getMeasuredWidth() - marginLeft - marginRight;
        final int actualHeight = getMeasuredHeight() - marginTop - marginBottom;
        final int columnWidth = actualWidth / mNumColumns;
        final int rowHeight = actualHeight / mNumRows;
        HolderLayout holder = (HolderLayout) getChildAt(mCurrentHolder);

        int count = mAdapter.getCount();
        int initPos = slideInfo.position;
        int currentLastPos = (mCurrentScreen + 1) * mNumPerPage - 1;
        if (initPos < lastPos) {	
        	if (currentLastPos > (count - 1)) {
        		currentLastPos = count - 1;
        	}
        }

    	int init = lastPos;
		for (; init < currentLastPos; init++) {
    		View child = getViewAtPosition(init);
    		int x1 = marginLeft + (init % mNumPerPage) % mNumColumns * columnWidth;
    		int y1 = marginTop + (init % mNumPerPage) / mNumColumns * rowHeight;	
    		int x2 = marginLeft + ((init + 1) % mNumPerPage) % mNumColumns * columnWidth;
    		int y2 = marginTop + ((init + 1) % mNumPerPage) / mNumColumns * rowHeight;

    		ItemAnimate itemAnimate = new ItemAnimate(x1, x2, y1, y2, child);
    		itemAnimate.setDuration(AIMIATE_TIME);
    		itemAnimate.setSquare(columnWidth, child.getMeasuredHeight());
    		itemAnimate.start();
    	}

		//animate the last view
		if (currentLastPos < (count - 1)) {
			View child = getViewAtPosition(currentLastPos);
        	int x1 = marginLeft + (currentLastPos % mNumPerPage) % mNumColumns * columnWidth;
    		int y1 = marginTop + (currentLastPos % mNumPerPage) / mNumColumns * rowHeight;

    		ItemAnimate itemAnimate = new ItemAnimate(x1, x1 + columnWidth, y1, y1 - rowHeight * 2, child);
    		itemAnimate.setDuration(AIMIATE_TIME);
    		itemAnimate.setSquare(columnWidth, child.getMeasuredHeight());
    		itemAnimate.start();	 
		}

		//insert initView
		final LayoutInflater mInflater = LayoutInflater.from(getContext());
    	TextView textView = (TextView)mInflater.inflate(R.layout.application_boxed, this, false);
		textView.setCompoundDrawablesWithIntrinsicBounds(
		        null, 
		  		new BitmapDrawable(slideInfo.iconBitmap), 
		  		null, 
		  		null);
		textView.setText(slideInfo.title);

		int left = marginLeft + (lastPos % mNumPerPage) % mNumColumns * columnWidth;
		int top = marginTop + (lastPos % mNumPerPage) / mNumColumns * rowHeight;
		SlidingView.LayoutParams p =
			new SlidingView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
	        		ViewGroup.LayoutParams.MATCH_PARENT);

	    textView.setLayoutParams(p);
	    textView.setSelected(false);
	    textView.setPressed(false);
	    textView.setVisibility(View.VISIBLE);
	    int childHeightSpec = getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, p.height);
        int childWidthSpec = getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(columnWidth, MeasureSpec.EXACTLY), 0, p.width);
        textView.measure(childWidthSpec, childHeightSpec);
		textView.layout(left, top, left+columnWidth, top+textView.getMeasuredHeight());	
		holder.addViewInLayout(textView, lastPos / mNumPerPage, p, true);

		//database operator
   		final ContentResolver cr = getContext().getContentResolver();
   		final Uri updateUri = Applications.getCustomUri("/addfolder");
		cr.update(updateUri, null, null, new String[] { String.valueOf(slideInfo.position) });

   		ContentValues values = new ContentValues();
		values.put(Applications.POSITION, lastPos);
		values.put(Applications.CONTAINER, Applications.CONTAINER_APPS);
		values.put(BaseLauncherColumns.ITEM_TYPE, Applications.APPS_TYPE_APP);
		cr.update(Applications.getContentUri(slideInfo.id, true), values, null, null);

        Thread myThread = new Thread() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					Thread.sleep(WATI_TIME);
					myHandler.sendEmptyMessage(0);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		myThread.start();
    }

    public void moveItemToPositionSlide(int initPos, int lastPos, long initID, View initView) {
    	final int marginTop = getPaddingTop();
        final int marginBottom = getPaddingBottom();
        final int marginLeft = getPaddingLeft() + mPageHorizontalMargin;
        final int marginRight = getPaddingRight() + mPageHorizontalMargin;
        final int actualWidth = getMeasuredWidth() - marginLeft - marginRight;
        final int actualHeight = getMeasuredHeight() - marginTop - marginBottom;
        final int columnWidth = actualWidth / mNumColumns;
        final int rowHeight = actualHeight / mNumRows;
        HolderLayout holder = (HolderLayout) getChildAt(mCurrentHolder);

        if (initPos > lastPos) {
        	int currentLastPos = (mCurrentScreen + 1) * mNumPerPage - 1;
        	int init = lastPos;

    		for (; init < currentLastPos; init++) {
        		View child = getViewAtPosition(init);
        		int x1 = marginLeft + (init % mNumPerPage) % mNumColumns * columnWidth;
        		int y1 = marginTop + (init % mNumPerPage) / mNumColumns * rowHeight;	
        		int x2 = marginLeft + ((init + 1) % mNumPerPage) % mNumColumns * columnWidth;
        		int y2 = marginTop + ((init + 1) % mNumPerPage) / mNumColumns * rowHeight;

        		ItemAnimate itemAnimate = new ItemAnimate(x1, x2, y1, y2, child);
        		itemAnimate.setDuration(AIMIATE_TIME);
        		itemAnimate.setSquare(columnWidth, child.getMeasuredHeight());
        		itemAnimate.start();
        	}

    		//animate the last view
			View child = getViewAtPosition(currentLastPos);
        	int x1 = marginLeft + (currentLastPos % mNumPerPage) % mNumColumns * columnWidth;
    		int y1 = marginTop + (currentLastPos % mNumPerPage) / mNumColumns * rowHeight;

    		ItemAnimate itemAnimate = new ItemAnimate(x1, x1 + columnWidth, y1, y1 - rowHeight * 2, child);
    		itemAnimate.setDuration(AIMIATE_TIME);
    		itemAnimate.setSquare(columnWidth, child.getMeasuredHeight());
    		itemAnimate.start();
    		
    		//insert initView
			SlidingView.LayoutParams p =
				new SlidingView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
	        		ViewGroup.LayoutParams.MATCH_PARENT);

			int left = marginLeft + (lastPos % mNumPerPage) % mNumColumns * columnWidth;
    		int top = marginTop + (lastPos % mNumPerPage) / mNumColumns * rowHeight;
    		initView.setLayoutParams(p);
    		initView.setSelected(false);
    		initView.setPressed(false);
    		initView.setVisibility(View.VISIBLE);
    		int childHeightSpec = getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, p.height);
            int childWidthSpec = getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(columnWidth, MeasureSpec.EXACTLY), 0, p.width);
            initView.measure(childWidthSpec, childHeightSpec);
            initView.layout(left, top, left+columnWidth, top + initView.getMeasuredHeight());
		    holder.addViewInLayout(initView, lastPos % mNumPerPage, p, true);

    		//database operator
       		final ContentResolver cr = getContext().getContentResolver();
       		final Uri updateUri = Applications.getCustomUri("/movefront");
			cr.update(updateUri, null, null, new String[] { String.valueOf(lastPos), String.valueOf(initPos) });

       		ContentValues values = new ContentValues();
   			values.put(Applications.POSITION, lastPos);
   			cr.update(Applications.getContentUri(initID, true), values, null, null);
        } else {
        	int currentFirstPos = mCurrentScreen * mNumPerPage;
        	int init = currentFirstPos + 1;

    		for (; init <= lastPos; init++) {
           		View child = getViewAtPosition(init);
           		int x1 = marginLeft + (init % mNumPerPage) % mNumColumns * columnWidth;
           		int y1 = marginTop + (init % mNumPerPage) / mNumColumns * rowHeight;
           		int x2 = marginLeft + ((init - 1) % mNumPerPage) % mNumColumns * columnWidth;
           		int y2 = marginTop + ((init - 1) % mNumPerPage) / mNumColumns * rowHeight;

           		ItemAnimate itemAnimate = new ItemAnimate(x1, x2, y1, y2, child);
           		itemAnimate.setDuration(AIMIATE_TIME);
           		itemAnimate.setSquare(columnWidth, child.getMeasuredHeight());
           		itemAnimate.start();
       	 	}

    		//animate the first view
   			View child = getViewAtPosition(currentFirstPos);
           	int x1 = marginLeft + (currentFirstPos % mNumPerPage) % mNumColumns * columnWidth;
       		int y1 = marginTop + (currentFirstPos % mNumPerPage) / mNumColumns * rowHeight;

       		ItemAnimate itemAnimate = new ItemAnimate(x1, x1 - columnWidth, y1, y1 + rowHeight * 2, child);
       		itemAnimate.setDuration(AIMIATE_TIME);
       		itemAnimate.setSquare(columnWidth, child.getMeasuredHeight());
       		itemAnimate.start();

       	    //insert initView
			SlidingView.LayoutParams p =
				new SlidingView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
	        		ViewGroup.LayoutParams.MATCH_PARENT);

			int left = marginLeft + (lastPos % mNumPerPage) % mNumColumns * columnWidth;
    		int top = marginTop + (lastPos % mNumPerPage) / mNumColumns * rowHeight;
    		initView.setLayoutParams(p);
    		initView.setSelected(false);
    		initView.setPressed(false);
    		initView.setVisibility(View.VISIBLE);
    		int childHeightSpec = getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, p.height);
            int childWidthSpec = getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(columnWidth, MeasureSpec.EXACTLY), 0, p.width);
            initView.measure(childWidthSpec, childHeightSpec);
            initView.layout(left, top, left + columnWidth, top + initView.getMeasuredHeight());
		    holder.addViewInLayout(initView, lastPos % mNumPerPage, p, true);

       		final ContentResolver cr = getContext().getContentResolver();
       		final Uri updateUri = Applications.getCustomUri("/movebehind");
			cr.update(updateUri, null, null, new String[] { String.valueOf(initPos), String.valueOf(lastPos) });

       		ContentValues values = new ContentValues();
   			values.put(Applications.POSITION, lastPos);
   			cr.update(Applications.getContentUri(initID, true), values, null, null);
        }

        Thread myThread = new Thread() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					Thread.sleep(WATI_TIME);
					myHandler.sendEmptyMessage(0);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		myThread.start();
    }

    public void moveItemToFolderSlide(ApplicationInfoEx initInfo, int lastPos, long lastID, View nextView) {
    	final int marginTop = getPaddingTop();
        final int marginBottom = getPaddingBottom();
        final int marginLeft = getPaddingLeft() + mPageHorizontalMargin;
        final int marginRight = getPaddingRight() + mPageHorizontalMargin;
        final int actualWidth = getMeasuredWidth() - marginLeft - marginRight;
        final int actualHeight = getMeasuredHeight() - marginTop - marginBottom;
        final int columnWidth = actualWidth / mNumColumns;
        final int rowHeight = actualHeight / mNumRows;
        HolderLayout holder = (HolderLayout) getChildAt(mCurrentHolder);
        int initPos = initInfo.position;

        if (initPos > lastPos) {
    		final ContentResolver cr = getContext().getContentResolver();
    		final Uri updateUri = Applications.getCustomUri("/insertfolder");
			cr.update(updateUri, null, null, new String[] { String.valueOf(initPos) });

    		ContentValues values = new ContentValues();
			values.put(Applications.CONTAINER, lastID);
			values.put(BaseLauncherColumns.ORDERID, initInfo.orderId);
   			values.put(BaseLauncherColumns.ITEM_TYPE, initInfo.itemType);
			cr.update(Applications.getContentUri(initInfo.id, true), values, null, null);

			myHandler.sendEmptyMessage(0);
			return;
        } else {
        	int count = mAdapter.getCount();
        	int currentFirstPos = mCurrentScreen * mNumPerPage;
        	int currentLastPos = (mCurrentScreen + 1) * mNumPerPage;
        	int init = currentFirstPos + 1;

        	if ((count - 1) < currentLastPos) {
        		currentLastPos = count;
        	}

    		for (; init < currentLastPos; init++) {
           		View child = getViewAtPosition(init);
           		int x1 = marginLeft + (init % mNumPerPage) % mNumColumns * columnWidth;
           		int y1 = marginTop + (init % mNumPerPage) / mNumColumns * rowHeight;
           		int x2 = marginLeft + ((init - 1) % mNumPerPage) % mNumColumns * columnWidth;
           		int y2 = marginTop + ((init - 1) % mNumPerPage) / mNumColumns * rowHeight;

           		ItemAnimate itemAnimate = new ItemAnimate(x1, x2, y1, y2, child);
           		itemAnimate.setDuration(AIMIATE_TIME);
           		itemAnimate.setSquare(columnWidth, child.getMeasuredHeight());
           		itemAnimate.start();
        	}

    		//animate the first view
   			View child = getViewAtPosition(currentFirstPos);
           	int x1 = marginLeft + (currentFirstPos % mNumPerPage) % mNumColumns * columnWidth;
       		int y1 = marginTop + (currentFirstPos % mNumPerPage) / mNumColumns * rowHeight;

       		ItemAnimate itemAnimate = new ItemAnimate(x1, x1 - columnWidth, y1, y1 + rowHeight * 2, child);
       		itemAnimate.setDuration(AIMIATE_TIME);
       		itemAnimate.setSquare(columnWidth, child.getMeasuredHeight());
       		itemAnimate.start();

	       	//insert lastView
       		if (nextView != null) {
       			int pos = currentLastPos - 1;
	   			SlidingView.LayoutParams p =
	   				new SlidingView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
			        	ViewGroup.LayoutParams.MATCH_PARENT);

	   			int left = marginLeft + (pos % mNumPerPage) % mNumColumns * columnWidth;
	       		int top = marginTop + (pos % mNumPerPage) / mNumColumns * rowHeight;
	       		nextView.setLayoutParams(p);
	       		nextView.setSelected(false);
	       		nextView.setPressed(false);
	       		nextView.setVisibility(View.VISIBLE);
	       		int childHeightSpec = getChildMeasureSpec(
		             MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, p.height);
		        int childWidthSpec = getChildMeasureSpec(
		             MeasureSpec.makeMeasureSpec(columnWidth, MeasureSpec.EXACTLY), 0, p.width);
		        nextView.measure(childWidthSpec, childHeightSpec);
		        nextView.layout(left, top, left + columnWidth, top + nextView.getMeasuredHeight());
	   		    holder.addViewInLayout(nextView, pos % mNumPerPage, p, true);
	       	}

       		// Operate database
       		final ContentResolver cr = getContext().getContentResolver();
       		final Uri updateUri = Applications.getCustomUri("/insertfolder");
   			cr.update(updateUri, null, null, new String[] { String.valueOf(initPos) });

       		ContentValues values = new ContentValues();
   			values.put(Applications.CONTAINER, lastID);
   			values.put(BaseLauncherColumns.ORDERID, initInfo.orderId);
   			values.put(BaseLauncherColumns.ITEM_TYPE, initInfo.itemType);
   			cr.update(Applications.getContentUri(initInfo.id, true), values, null, null);
        }

	   	Thread myThread = new Thread() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					Thread.sleep(WATI_TIME);
					myHandler.sendEmptyMessage(0);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		myThread.start();
    }

    private void addRemovePages(int current, int next) {
    	int addPage;
    	int removePage;
    	if (current > next) {
    		//Going left
    		addPage = next - 1;
    		removePage = current + 1;
    	} else {
    		//Going right
    		addPage = next + 1;
    		removePage = current - 1;
    	}
    	if (removePage >= 0 && removePage < mTotalScreens) {
    		HolderLayout h = null;
    		for (int i = 1; i < getChildCount(); i++) {
    			if (getChildAt(i).getTag().equals(removePage)) {
    				h = (HolderLayout) getChildAt(i);
    				break;
    			}
    		}
    		if (h != null) {
				for (int i = 0; i < h.getChildCount(); i++) {
					mRecycler.addScrapView(h.getChildAt(i));
				}
				detachViewFromParent(h);
				removeDetachedView(h, false);
    		}
    	}
		makePage(addPage);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
    	/*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onTouchEvent will be called and we do the actual
         * scrolling there.
         */

        /*
         * Shortcut the most recurring case: the user is in the dragging
         * state and he is moving his finger.  We want to intercept this
         * motion.
         */
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mTouchState != TOUCH_STATE_REST)) {
            return true;
        }

        final float x = ev.getX();
        final float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */

                /*
                 * Locally do absolute value. mLastMotionX is set to the y value
                 * of the down event.
                 */
                final int xDiff = (int) Math.abs(x - mLastMotionX);
                final int yDiff = (int) Math.abs(y - mLastMotionY);
                final int touchSlop = mTouchSlop;
                boolean xMoved = xDiff > touchSlop;
                boolean yMoved = yDiff > touchSlop;

                if (xMoved || yMoved) {
                    if (xMoved) {
                        // Scroll if the user moved far enough along the X axis
                        mTouchState = TOUCH_STATE_SCROLLING;
                    }
                }
                break;

            case MotionEvent.ACTION_DOWN:
                // Remember location of down touch
                mLastMotionX = x;
                mLastMotionY = y;

                /*
                 * If being flinged and user touches the screen, initiate drag;
                 * otherwise don't.  mScroller.isFinished should be false when
                 * being flinged.
                 */
            	mTouchState = mScroller.isFinished() ? TOUCH_STATE_DOWN : TOUCH_STATE_SCROLLING;
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // Release the drag
                mTouchState = TOUCH_STATE_REST;
                break;
        }

        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
        return mTouchState != TOUCH_STATE_REST;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
    	if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();
        final float x = ev.getX();
        final float y = ev.getY();
        final View child;

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            /*
             * If being flinged and user touches, stop the fling. isFinished
             * will be false if being flinged.
             */
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            //mTouchState = TOUCH_STATE_DOWN;
            child = pointToView((int) x, (int) y);
            if (child != null) {
	            // FIXME Debounce
	            if (mPendingCheckForTap == null) {
	                mPendingCheckForTap = new CheckForTap();
	            }
	            postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());
	            // Remember where the motion event started
	            mCheckTapPosition = getPositionForView(child);
            }
            // Remember where the motion event started
            mLastMotionX = x;
            break;
        case MotionEvent.ACTION_MOVE:
        	if (mTouchState == TOUCH_STATE_SCROLLING || mTouchState == TOUCH_STATE_DOWN 
        			|| mTouchState == TOUCH_STATE_TAP) {
            	// Scroll to follow the motion event
                final int deltaX = (int) (mLastMotionX - x);
                if (Math.abs(deltaX) > mTouchSlop || mTouchState == TOUCH_STATE_SCROLLING) {
                	if (mTouchState == TOUCH_STATE_TAP && mCheckTapPosition != INVALID_POSITION) {
                		 child = getViewAtPosition(mCheckTapPosition);
                		 if (child != null && !child.hasFocusable()) {
                			 setSelection(INVALID_POSITION);
                			 child.setPressed(false);                         		 
                		 }
                	}
                	mTouchState = TOUCH_STATE_SCROLLING;
	                mLastMotionX = x;

	                if (deltaX < 0) {
	                    if (getScrollX() > -mScrollingBounce) {
	                        scrollBy(Math.min(deltaX, mScrollingBounce), 0);
	                    }
	                } else if (deltaX > 0) {
	                	final int availableToScroll = ((mTotalScreens) * mPageWidth) - getScrollX() - mPageWidth + mScrollingBounce;
	                	if (availableToScroll > 0) {
	                        scrollBy(deltaX, 0);
	                    }
	                }
                }
//                final int deltaY = (int) (mLastMotionY - y);
//                if(Math.abs(deltaY)>mTouchSlop || mTouchState == TOUCH_STATE_SCROLLING){
//                	mTouchState = TOUCH_STATE_SCROLLING;
//                }
            }
            break;
        case MotionEvent.ACTION_UP:
            if (mTouchState == TOUCH_STATE_SCROLLING) {
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocityX = (int) velocityTracker.getXVelocity();
                //ADW: remove for now the "multi-page scrolling", is causing a lot of mess...
                /*int moveScreens=Math.round(velocityX/1000);
                int destinationScreen=mCurrentScreen-moveScreens;
                if(destinationScreen<0) destinationScreen=0;
                if(destinationScreen>mTotalScreens-1)destinationScreen=mTotalScreens-1;*/

                if (velocityX > SNAP_VELOCITY && mCurrentScreen > 0) {
                    // Fling hard enough to move left
                    //snapToScreen(destinationScreen);
                	snapToScreen(mCurrentScreen - 1, velocityX);
                } else if (velocityX < -SNAP_VELOCITY && mCurrentScreen < (mTotalScreens - 1)) {
                    // Fling hard enough to move right
                	//snapToScreen(destinationScreen);
                	snapToScreen(mCurrentScreen + 1, velocityX);
                } else {
                    snapToDestination();
                }

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
            } else {
            	child = getViewAtPosition(mCheckTapPosition);
            	if (child != null && child.equals(pointToView((int)x, (int)y))) {
            		mBorderViewScroll = false;
            		if (x < BORDER_CHECK_SCROLL || x >= mPageWidth-BORDER_CHECK_SCROLL) {
            			int deltaX = Math.abs((int) (x - mLastMotionX));
            			if (deltaX > mTouchSlop / 2) {
            				mBorderViewScroll = true;
            			}
            		}
	            	if (mPerformClick == null) {
	                    mPerformClick = new PerformClick();
	                }

	                final SlidingView.PerformClick performClick = mPerformClick;
	                performClick.mChild = child;
	                performClick.mClickMotionPosition = mCheckTapPosition;
	                performClick.rememberWindowAttachCount();
	                if (mTouchState == TOUCH_STATE_DOWN || mTouchState == TOUCH_STATE_TAP) {
		                final Handler handler = getHandler();
		                if (handler != null) {
		                    handler.removeCallbacks(mTouchState == TOUCH_STATE_DOWN ?
		                            mPendingCheckForTap : mPendingCheckForLongPress);
		                }
		                mLayoutMode = LAYOUT_NORMAL;

		                if (!mDataChanged) {
			                mTouchState = TOUCH_STATE_TAP;
	                        child.setPressed(true);
	                        setPressed(true);
	                        setSelection(mCheckTapPosition);
	                        positionSelector(child);

		                    if (mSelector != null) {
		                        Drawable d = mSelector.getCurrent();
		                        if (d != null && d instanceof TransitionDrawable) {
		                            ((TransitionDrawable)d).resetTransition();
		                        }
		                    }
		                    postDelayed(new Runnable() {
		                        @Override
								public void run() {
		                            child.setPressed(false);
		                            if (!mDataChanged && !mBorderViewScroll) {
		                                post(performClick);
		                            }
//		                            mTouchState = TOUCH_STATE_REST;
		                        }
		                    }, ViewConfiguration.getPressedStateDuration());
		                }
		                return true;
	                }
                } else {
                	resurrectSelection();
                }
            }
            mTouchState = TOUCH_STATE_REST;
            mCheckTapPosition = INVALID_POSITION;
            hideSelector();
            invalidate();

            final Handler handler = getHandler();
            if (handler != null) {
                handler.removeCallbacks(mPendingCheckForLongPress);
            }
            break;
        case MotionEvent.ACTION_CANCEL:
            if (mTouchState == TOUCH_STATE_SCROLLING) {
                snapToDestination();
            }
            mTouchState = TOUCH_STATE_REST;
        }
        return true;
    }

    public void onTouchModeChanged(boolean isInTouchMode) {
        if (isInTouchMode) {
            // Get rid of the selection when we enter touch mode
            hideSelector();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	return commonKey(keyCode, 1, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return commonKey(keyCode, repeatCount, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean handled = commonKey(keyCode, 1, event);
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_ENTER:
	        if (isPressed() && mSelectedPosition >= 0 && mAdapter != null &&
	                mSelectedPosition < mAdapter.getCount()) {
	            final HolderLayout h = (HolderLayout) getChildAt(mCurrentHolder);
	        	final View view = h.getChildAt(mSelectedPosition);
	        	final int realPosition = getPositionForView(view);

	        	performItemClick(view, realPosition, mAdapter.getItemId(realPosition));
	            setPressed(false);
	            if (view != null) {
	            	view.setPressed(false);
	            }
	            return true;
	        }
        }
        return handled;
    }

    private boolean commonKey(int keyCode, int count, KeyEvent event) {
    	if (mAdapter == null) {
            return false;
        }

        if (mDataChanged) {
            layoutChildren();
        }

        boolean handled = false;
        int action = event.getAction();

        if (action != KeyEvent.ACTION_UP) {
            if (mSelectedPosition < 0) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_SPACE:
                    case KeyEvent.KEYCODE_ENTER:
                        resurrectSelection();
                        return true;
                }
            }
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    handled = arrowScroll(FOCUS_LEFT);
                    break;

                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    handled = arrowScroll(FOCUS_RIGHT);
                    break;

                case KeyEvent.KEYCODE_DPAD_UP:
                    handled = arrowScroll(FOCUS_UP);
                    break;

                case KeyEvent.KEYCODE_DPAD_DOWN:
                    handled = arrowScroll(FOCUS_DOWN);
                    break;

                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER: {
                    if (getChildCount() > 0 && event.getRepeatCount() == 0) {
                        keyPressed();
                    }
                    return true;
                }
            }
        }

        if (handled) {
            return true;
        } else {
            switch (action) {
                case KeyEvent.ACTION_DOWN:
                    return super.onKeyDown(keyCode, event);
                case KeyEvent.ACTION_UP:
                    return super.onKeyUp(keyCode, event);
                case KeyEvent.ACTION_MULTIPLE:
                    return super.onKeyMultiple(keyCode, count, event);
                default:
                    return false;
            }
        }
    }
    /**
     * Scrolls to the next or previous item, horizontally or vertically.
     *
     * @param direction either {@link View#FOCUS_LEFT}, {@link View#FOCUS_RIGHT},
     *        {@link View#FOCUS_UP} or {@link View#FOCUS_DOWN}
     *
     * @return whether selection was moved
     */
    boolean arrowScroll(int direction) {
        final int selectedPosition = (mSelectedPosition == INVALID_POSITION) ? 0 : mSelectedPosition;
        final int numColumns = mNumColumns;
        final int numRows = mNumRows;
        int rowPos;
        int colPos;

        boolean moved = false;
        final HolderLayout h = (HolderLayout) getChildAt(mCurrentHolder);

        colPos = (selectedPosition % numColumns);
        int lastColPos = mNumColumns;//(h.getChildCount()-1)%numColumns;
        rowPos = (selectedPosition / numColumns);
        int lastRowPos = mNumRows;//(h.getChildCount()-1)/numColumns;
        switch (direction) {
            case FOCUS_UP:
                if (rowPos > 0) {
                	rowPos--;
                    moved = true;
                }
                break;
            case FOCUS_DOWN:
                if (rowPos < (numRows - 1) && rowPos < lastRowPos) {
                	rowPos++;
                    moved = true;
                }
                break;
            case FOCUS_LEFT:
                if (colPos > 0) {
                	colPos--;
                    moved = true;
                } else {
                	if (mCurrentScreen > 0) {
                    	setSelection(INVALID_POSITION);
                		snapToScreen(mCurrentScreen - 1);
                		invalidate();
                		return true;
                	}
                }
                break;
            case FOCUS_RIGHT:
                if (colPos < (numColumns - 1) && colPos < lastColPos) {
                	colPos++;
                    moved = true;
                } else {
                	if (mCurrentScreen < (mTotalScreens - 1)) {
                    	setSelection(INVALID_POSITION);
                		snapToScreen(mCurrentScreen + 1);
                		invalidate();
                		return true;
                	}
                }
                break;
        }
        if (moved) {
            int pos = ((rowPos * numColumns) + colPos);
            if (pos < h.getChildCount()) {
	        	playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
	            setSelection(Math.max(0, pos));
	            positionSelector(h.getChildAt(pos));
	            invalidate();
            }
        }
        return moved;
    }

    /**
     * Attempt to bring the selection back if the user is switching from touch
     * to trackball mode
     * @return Whether selection was set to something.
     */
    boolean resurrectSelection() {
    	if (getChildCount() <= 0) {
    		return false;
    	}
    	final HolderLayout h = (HolderLayout) getChildAt(mCurrentHolder);
    	if (h != null && h instanceof HolderLayout) {
	        final int childCount = h.getChildCount();

	        if (childCount <= 0) {
	            return false;
	        }
	        for (int i = 0; i < childCount; i++) {
	        	h.getChildAt(i).setPressed(false);
	        }
	        positionSelector(h.getChildAt(0));
	        setSelection(0);
    	}
        return true;
    }

    public View getViewAtPosition(int pos) {
    	View v = null;
    	int position = pos;
        int realScreen = mCurrentHolder;

    	if (mCurrentScreen > 0) {
    		position -= mCurrentScreen * mNumPerPage;
    	}
    	final ViewGroup h = (ViewGroup) getChildAt(realScreen);
    	if (h != null && h instanceof HolderLayout) {
    		v = h.getChildAt(position);
    	}
    	return v;
    }

    @Override
    public int getPositionForView(View view) {
        View listItem = view;
        int realScreen = mCurrentHolder;
    	int pos = 0;

        if (mCurrentScreen > 0) {
    		pos += mCurrentScreen * mNumPerPage;
    	}
    	final ViewGroup h = (ViewGroup) getChildAt(realScreen);
    	for (int i = 0; i < h.getChildCount(); i++) {
            if (h.getChildAt(i).equals(listItem)) {
                return (i + pos);
            }
    	}
        // Child not found!
        return INVALID_POSITION;
    }

    public View pointToView(int x, int y) {
    	if (getChildCount() > 1) {
	    	Rect frame = new Rect();
	    	int realScreen = mCurrentHolder;
	    	final ViewGroup h = (ViewGroup) getChildAt(realScreen);
	    	//ADW: fix possible nullPointerException when flinging too fast
	    	if (h != null) {
		    	Rect frameHolder = new Rect();
		    	h.getHitRect(frameHolder);
		    	final int xHolder = x;
		    	final int yHolder = y - frameHolder.top - getPaddingTop();
		    	for (int i = 0; i < h.getChildCount(); i++) {
		        	final View child = h.getChildAt(i);
		            if (child.getVisibility() != View.GONE) {
		                child.getHitRect(frame);
		                if (frame.contains(xHolder, yHolder)) {
		                    return child;
		                }
		            }
		        }
	    	}
    	}
        return null;
    }

    public View pointToViewEx(int x, int y, int initPos){
    	
    	if (getChildCount() > 1) {
    		
    		final int marginTop = getPaddingTop();
    		final int marginBottom = getPaddingBottom();
    		final int marginLeft = getPaddingLeft() + mPageHorizontalMargin;
    		final int marginRight = getPaddingRight() + mPageHorizontalMargin;
    		final int actualWidth = getMeasuredWidth() - marginLeft - marginRight;
    		final int actualHeight = getMeasuredHeight() - marginTop - marginBottom;
    		final int columnWidth = actualWidth / mNumColumns;
    		final int rowHeight = actualHeight / mNumRows;
            
	    	Rect frame = new Rect();
	    	int realScreen = mCurrentHolder;
	    	final ViewGroup h = (ViewGroup) getChildAt(realScreen);
	    	//ADW: fix possible nullPointerException when flinging too fast
	    	if (h != null) {
		    	Rect frameHolder = new Rect();
		    	h.getHitRect(frameHolder);
		    	
		    	final int xHolder = x;
		    	final int yHolder = y - frameHolder.top;
		    	
		    	if(frameHolder.bottom < yHolder){
		    		return getViewAtPosition(initPos);
		    	}
		    	
		    	for (int i = 0; i < h.getChildCount(); i++) {
		        	final View child = h.getChildAt(i);
		            if (child.getVisibility() != View.GONE) {
		            	frame.left = marginLeft + (i % mNumColumns) * columnWidth;
		            	frame.top = marginTop + (i/mNumColumns) * rowHeight;
		            	frame.right = frame.left + columnWidth;
		            	frame.bottom = frame.top + rowHeight;
		                if (frame.contains(xHolder, yHolder)) {
		                    return child;
		                }
		            }
		        }
	    	}
    	}
    	
        return null;
    }
    
    private void snapToDestination() {
        final int screenWidth = mPageWidth;
        final int whichScreen = (getScrollX() + (screenWidth / 2)) / screenWidth;
        snapToScreen(whichScreen);
    }

    void snapToScreen(int whichScreen) {
    	snapToScreen(whichScreen, 0);
    }

    private void snapToScreen(int whichScreen, int velocity){
//        if (!mScroller.isFinished()) return;
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }

        whichScreen = Math.max(0, Math.min(whichScreen, mTotalScreens - 1));
        boolean changingScreens = whichScreen != mCurrentScreen;

        mNextScreen = whichScreen;
//        final int screenDelta = Math.abs(whichScreen - mCurrentScreen);
        mCurrentScreen = whichScreen;
        mPager.setCurrentScreen(mCurrentScreen);

        if (changingScreens) {
        	mLayoutMode = LAYOUT_SCROLLING;
        }
        View focusedChild = getFocusedChild();
        if (focusedChild != null && changingScreens && focusedChild == getChildAt(mCurrentHolder)) {
            focusedChild.clearFocus();
        }

//        int durationOffset = 1;
//		// Faruq: Added to allow easing even when Screen doesn't changed (when revert happens)
//		if (screenDelta == 0) {
//			durationOffset = 200;
//		}
//		final int duration = mScrollingSpeed + durationOffset;

		final int newX = whichScreen * mPageWidth;
        final int delta = newX - getScrollX();

        final int screenDelta = Math.max(1, Math.abs(whichScreen - mCurrentScreen));
        int duration = (screenDelta + 1) * 100;

        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration += (duration / (velocity / BASELINE_FLING_VELOCITY))
                    * FLING_VELOCITY_INFLUENCE;
        } else {
            duration += 100;
        }
        if (delta != 0) {
        	mScroller.startScroll(getScrollX(), 0, delta, 0, duration);
        }
        invalidate();
    }

	@Override
//	public ApplicationsAdapter getAdapter() {
	public ArrayAdapter getAdapter() {
		// TODO Auto-generated method stub
		return mAdapter;
	}

	@Override
//	public void setAdapter(ApplicationsAdapter adapter) {
	public void setAdapter(ArrayAdapter adapter) {
		// TODO Auto-generated method stub
        if (null != mAdapter) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }

        mRecycler.clear();
        mAdapter = adapter;

        if (mAdapter != null) {
            mOldItemCount = mItemCount;
            mItemCount = mAdapter.getCount();
            mTotalScreens = getPageCount();
    		mPager.setScreenCount(mTotalScreens);
            mDataChanged = true;

            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);

            mRecycler.setViewTypeCount(mAdapter.getViewTypeCount());
        }

        if (!mFirstLayout && !mScroller.isFinished()) {
        	Log.i(TAG, "setAdapter scroller forceFinished");
        	scrollTo(mScroller.getFinalX(), mScroller.getFinalY());
        }

        mScroller.forceFinished(true);
        mBlockLayouts = false;
        requestLayout();
	}

    void hideSelector() {
        if (mSelectedPosition != INVALID_POSITION) {
        	setSelection(INVALID_POSITION);
            mSelectorRect.setEmpty();
        }
    }

	@Override
	public View getSelectedView() {
    	final ViewGroup h = (ViewGroup) getChildAt(0);

        if (mItemCount > 0 && mSelectedPosition >= 0) {
            return h.getChildAt(mSelectedPosition);
        } else {
            return null;
        }
	}

	@Override
	public void setSelection(int position) {
		// TODO Auto-generated method stub
		mSelectedPosition = position;
		invalidate();
	}

    View obtainView(int position) {
        View scrapView = mRecycler.getScrapView(position);
        View child;

        if (scrapView != null) {
            child = mAdapter.getView(position, scrapView, this);
            if (child != scrapView) {
                mRecycler.addScrapView(scrapView);
            }
        } else {
            child = mAdapter.getView(position, null, this);
        }
        return child;
    }

    public int getPageCount() {
    	int pages = mAdapter.getCount() / mNumPerPage;
    	if (mAdapter.getCount() % mNumPerPage > 0) {
    		pages++;
    	}
    	return pages;
    }

    //TODO:ADW Focus things :)
    /**
     * @return True if the current touch mode requires that we draw the selector in the pressed
     *         state.
     */
    boolean touchModeDrawsInPressedState() {
        // FIXME use isPressed for this
        switch (mTouchState) {
        case TOUCH_STATE_TAP:
        case TOUCH_STATE_DONE_WAITING:
            return true;
        default:
            return false;
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mSelector != null) {
            mSelector.setState(getDrawableState());
        }
    }

    void positionSelector(View sel) {
        final Rect selectorRect = mSelectorRect;
        final int actualHeight = getMeasuredHeight() - getPaddingTop()-getPaddingBottom();
        final int offsetHolderY = (actualHeight % mNumRows) / 2;
        final int offHeight = sel.getHeight() - sel.getMeasuredHeight();
        selectorRect.set(sel.getLeft(),
                sel.getTop() + offsetHolderY + getPaddingTop(),
                sel.getRight(),
                sel.getBottom() + offsetHolderY + getPaddingTop());

        selectorRect.bottom = selectorRect.bottom - offHeight;
        positionSelector(selectorRect.left, selectorRect.top, selectorRect.right,
                selectorRect.bottom);
        refreshDrawableState();
    }

    private void positionSelector(int l, int t, int r, int b) {
        mSelectorRect.set(l - mSelectionLeftPadding + getScrollX(), t - mSelectionTopPadding + getScrollY(),
        		r + mSelectionRightPadding + getScrollX(), b + mSelectionBottomPadding + getScrollY());
    }

    /**
     * Indicates whether this view is in a state where the selector should be drawn. This will
     * happen if we have focus but are not in touch mode, or we are in the middle of displaying
     * the pressed state for an item.
     *
     * @return True if the selector should be shown
     */
    boolean shouldShowSelector() {
    	return (hasFocus() && !isInTouchMode()) || touchModeDrawsInPressedState();
    }

    private void drawSelector(Canvas canvas) {
        if (shouldShowSelector() && mSelectorRect != null && !mSelectorRect.isEmpty()) {
        	final Drawable selector = mSelector;
            selector.setBounds(mSelectorRect);
            selector.setState(getDrawableState());
            selector.draw(canvas);
        }
    }

    /**
     * Controls whether the selection highlight drawable should be drawn on top of the item or
     * behind it.
     *
     * @param onTop If true, the selector will be drawn on the item it is highlighting. The default
     *        is false.
     *
     * @attr ref android.R.styleable#AbsListView_drawSelectorOnTop
     */
    public void setDrawSelectorOnTop(boolean onTop) {
        mDrawSelectorOnTop = onTop;
    }

    /**
     * Set a Drawable that should be used to highlight the currently selected item.
     *
     * @param resID A Drawable resource to use as the selection highlight.
     *
     * @attr ref android.R.styleable#AbsListView_listSelector
     */
    public void setSelector(int resID) {
        setSelector(getResources().getDrawable(resID));
    }

    public void setSelector(Drawable sel) {
        if (mSelector != null) {
            mSelector.setCallback(null);
            unscheduleDrawable(mSelector);
        }
        mSelector = sel;
        Rect padding = new Rect();
        sel.getPadding(padding);
        sel.setCallback(this);
        sel.setState(getDrawableState());
    }

    /**
     * Returns the selector {@link android.graphics.drawable.Drawable} that is used to draw the
     * selection in the list.
     *
     * @return the drawable used to display the selector
     */
    public Drawable getSelector() {
        return mSelector;
    }

    @Override
    public int getSolidColor() {
        return mCacheColorHint;
    }

    /**
     * When set to a non-zero value, the cache color hint indicates that this list is always drawn
     * on top of a solid, single-color, opaque background
     *
     * @param color The background color
     */
    public void setCacheColorHint(int color) {
        mCacheColorHint = color;
    }

    /**
     * When set to a non-zero value, the cache color hint indicates that this list is always drawn
     * on top of a solid, single-color, opaque background
     *
     * @return The cache color hint
     */
    public int getCacheColorHint() {
        return mCacheColorHint;
    }

    /**
     * Sets the recycler listener to be notified whenever a View is set aside in
     * the recycler for later reuse. This listener can be used to free resources
     * associated to the View.
     *
     * @param listener The recycler listener to be notified of views set aside
     *        in the recycler.
     *
     * @see android.widget.AbsListView.RecycleBin
     * @see android.widget.AbsListView.RecyclerListener
     */
    public void setRecyclerListener(RecyclerListener listener) {
        mRecycler.mRecyclerListener = listener;
    }

    /**
     * A RecyclerListener is used to receive a notification whenever a View is placed
     * inside the RecycleBin's scrap heap. This listener is used to free resources
     * associated to Views placed in the RecycleBin.
     *
     * @see android.widget.AbsListView.RecycleBin
     * @see android.widget.AbsListView#setRecyclerListener(android.widget.AbsListView.RecyclerListener)
     */
    public static interface RecyclerListener {
        /**
         * Indicates that the specified View was moved into the recycler's scrap heap.
         * The view is not displayed on screen any more and any expensive resource
         * associated with the view should be discarded.
         *
         * @param view
         */
        void onMovedToScrapHeap(View view);
    }

    /**
     * The RecycleBin facilitates reuse of views across layouts. The RecycleBin has two levels of
     * storage: ActiveViews and ScrapViews. ActiveViews are those views which were onscreen at the
     * start of a layout. By construction, they are displaying current information. At the end of
     * layout, all views in ActiveViews are demoted to ScrapViews. ScrapViews are old views that
     * could potentially be used by the adapter to avoid allocating views unnecessarily.
     *
     * @see android.widget.AbsListView#setRecyclerListener(android.widget.AbsListView.RecyclerListener)
     * @see android.widget.AbsListView.RecyclerListener
     */

    class RecycleBin {
        private RecyclerListener mRecyclerListener;

        /**
         * The position of the first view stored in mActiveViews.
         */
        private int mFirstActivePosition;

        /**
         * Views that were on screen at the start of layout. This array is populated at the start of
         * layout, and at the end of layout all view in mActiveViews are moved to mScrapViews.
         * Views in mActiveViews represent a contiguous range of Views, with position of the first
         * view store in mFirstActivePosition.
         */
        private View[] mActiveViews = new View[0];

        /**
         * Unsorted views that can be used by the adapter as a convert view.
         */
        private ArrayList<View>[] mScrapViews;

        private int mViewTypeCount;

        private ArrayList<View> mCurrentScrap;

        @SuppressWarnings("unchecked")
        public void setViewTypeCount(int viewTypeCount) {
            if (viewTypeCount < 1) {
                throw new IllegalArgumentException("Can't have a viewTypeCount < 1");
            }
            //noinspection unchecked
            ArrayList<View>[] scrapViews = new ArrayList[viewTypeCount];
            for (int i = 0; i < viewTypeCount; i++) {
                scrapViews[i] = new ArrayList<View>();
            }
            mViewTypeCount = viewTypeCount;
            mCurrentScrap = scrapViews[0];
            mScrapViews = scrapViews;
        }

        public boolean shouldRecycleViewType(int viewType) {
            return viewType >= 0;
        }

        /**
         * Clears the scrap heap.
         */
        void clear() {
            if (mViewTypeCount == 1) {
                final ArrayList<View> scrap = mCurrentScrap;
                final int scrapCount = scrap.size();
                for (int i = 0; i < scrapCount; i++) {
                    removeDetachedView(scrap.remove(scrapCount - 1 - i), false);
                }
            } else {
                final int typeCount = mViewTypeCount;
                for (int i = 0; i < typeCount; i++) {
                    final ArrayList<View> scrap = mScrapViews[i];
                    final int scrapCount = scrap.size();
                    for (int j = 0; j < scrapCount; j++) {
                        removeDetachedView(scrap.remove(scrapCount - 1 - j), false);
                    }
                }
            }
        }

        /**
         * Fill ActiveViews with all of the children of the AbsListView.
         *
         * @param childCount The minimum number of views mActiveViews should hold
         * @param firstActivePosition The position of the first view that will be stored in
         *        mActiveViews
         */
        void fillActiveViews(int childCount, int firstActivePosition) {
            if (mActiveViews.length < childCount) {
                mActiveViews = new View[childCount];
            }
            mFirstActivePosition = firstActivePosition;

            final View[] activeViews = mActiveViews;
            for (int i = 0; i < childCount; i++) {
            	View child = getChildAt(i);
                SlidingView.LayoutParams lp = (SlidingView.LayoutParams)child.getLayoutParams();
                // Don't put header or footer views into the scrap heap
                if (lp != null && lp.viewType != AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
                    // Note:  We do place AdapterView.ITEM_VIEW_TYPE_IGNORE in active views.
                    //        However, we will NOT place them into scrap views.
                    activeViews[i] = child;
                }
            }
//            for (int i = 0; i < activeViews.length; i++) {
//            	Log.d("MyRecycler","We have recycled activeview "+i);
//            	Log.d("MyRecycler","So whe we call it will be "+(i-mFirstActivePosition));
//            }
        }

        /**
         * Get the view corresponding to the specified position. The view will be removed from
         * mActiveViews if it is found.
         *
         * @param position The position to look up in mActiveViews
         * @return The view if it is found, null otherwise
         */
        View getActiveView(int position) {
            int index = position - mFirstActivePosition;
            final View[] activeViews = mActiveViews;
            //Log.d("MyRecycler","We're recovering view "+index+" of a list of "+activeViews.length);
            if (index >= 0 && index < activeViews.length) {
                final View match = activeViews[index];
                activeViews[index] = null;
                return match;
            }
            return null;
        }

        /**
         * @return A view from the ScrapViews collection. These are unordered.
         */
        View getScrapView(int position) {
            ArrayList<View> scrapViews;
            if (mViewTypeCount == 1) {
                scrapViews = mCurrentScrap;
                int size = scrapViews.size();
                if (size > 0) {
                    return scrapViews.remove(size - 1);
                } else {
                    return null;
                }
            } else {
                int whichScrap = mAdapter.getItemViewType(position);
                if (whichScrap >= 0 && whichScrap < mScrapViews.length) {
                    scrapViews = mScrapViews[whichScrap];
                    int size = scrapViews.size();
                    if (size > 0) {
                        return scrapViews.remove(size - 1);
                    }
                }
            }
            return null;
        }

        /**
         * Put a view into the ScapViews list. These views are unordered.
         *
         * @param scrap The view to add
         */
        void addScrapView(View scrap) {
            SlidingView.LayoutParams lp = (SlidingView.LayoutParams) scrap.getLayoutParams();
            if (lp == null) {
                return;
            }

            // Don't put header or footer views or views that should be ignored
            // into the scrap heap
            int viewType = lp.viewType;
            if (!shouldRecycleViewType(viewType)) {
                return;
            }

            if (mViewTypeCount == 1) {
                mCurrentScrap.add(scrap);
            } else {
                mScrapViews[viewType].add(scrap);
            }

            if (mRecyclerListener != null) {
                mRecyclerListener.onMovedToScrapHeap(scrap);
            }
        }

        /**
         * Move all views remaining in mActiveViews to mScrapViews.
         */
        void scrapActiveViews() {
            final View[] activeViews = mActiveViews;
            final boolean hasListener = mRecyclerListener != null;
            final boolean multipleScraps = mViewTypeCount > 1;

            ArrayList<View> scrapViews = mCurrentScrap;
            final int count = activeViews.length;
            for (int i = 0; i < count; ++i) {
                final View victim = activeViews[i];
                if (victim != null) {
                    int whichScrap = ((SlidingView.LayoutParams) victim.getLayoutParams()).viewType;

                    activeViews[i] = null;

                    if (whichScrap == AdapterView.ITEM_VIEW_TYPE_IGNORE) {
                        // Do not move views that should be ignored
                        continue;
                    }

                    if (multipleScraps) {
                        scrapViews = mScrapViews[whichScrap];
                    }
                    scrapViews.add(victim);

                    if (hasListener) {
                        mRecyclerListener.onMovedToScrapHeap(victim);
                    }
                }
            }

            pruneScrapViews();
        }

        /**
         * Makes sure that the size of mScrapViews does not exceed the size of mActiveViews.
         * (This can happen if an adapter does not recycle its views).
         */
        private void pruneScrapViews() {
            final int maxViews = mActiveViews.length;
            final int viewTypeCount = mViewTypeCount;
            final ArrayList<View>[] scrapViews = mScrapViews;
            for (int i = 0; i < viewTypeCount; ++i) {
                final ArrayList<View> scrapPile = scrapViews[i];
                int size = scrapPile.size();
                final int extras = size - maxViews;

                size--;
                for (int j = 0; j < extras; j++) {
                    removeDetachedView(scrapPile.remove(size--), false);
                }
            }
        }

        /**
         * Puts all views in the scrap heap into the supplied list.
         */
        void reclaimScrapViews(List<View> views) {
            if (mViewTypeCount == 1) {
                views.addAll(mCurrentScrap);
            } else {
                final int viewTypeCount = mViewTypeCount;
                final ArrayList<View>[] scrapViews = mScrapViews;

                for (int i = 0; i < viewTypeCount; ++i) {
                    final ArrayList<View> scrapPile = scrapViews[i];
                    views.addAll(scrapPile);
                }
            }
        }
    }

    //TODO:ADW Helper classes
    final class CheckForTap implements Runnable {
        @Override
		public void run() {
            if (mTouchState == TOUCH_STATE_DOWN) {
                mTouchState = TOUCH_STATE_TAP;
                final View child = getViewAtPosition(mCheckTapPosition);
                if (child != null && !child.hasFocusable()) {
                    mLayoutMode = LAYOUT_NORMAL;

                    if (!mDataChanged) {
                        child.setPressed(true);
                        setPressed(true);
                        setSelection(mCheckTapPosition);
                        positionSelector(child);
                        final int longPressTimeout = ViewConfiguration.getLongPressTimeout() + 500;
                        final boolean longClickable = isLongClickable();

                        if (mSelector != null) {
                            Drawable d = mSelector.getCurrent();
                            if (d != null && d instanceof TransitionDrawable) {
                                if (longClickable) {
                                    ((TransitionDrawable) d).startTransition(longPressTimeout);
                                } else {
                                    ((TransitionDrawable) d).resetTransition();
                                }
                            }
                        }

                        if (longClickable) {
                            if (mPendingCheckForLongPress == null) {
                                mPendingCheckForLongPress = new CheckForLongPress();
                            }
                            mPendingCheckForLongPress.rememberWindowAttachCount();
                            postDelayed(mPendingCheckForLongPress, longPressTimeout);
                        } else {
                            mTouchState = TOUCH_STATE_DONE_WAITING;
                        }
                    } else {
                        mTouchState = TOUCH_STATE_DONE_WAITING;
                    }
                }
            }
        }
    }

    /**
     * Sets the selector state to "pressed" and posts a CheckForKeyLongPress to see if
     * this is a long press.
     */
    void keyPressed() {
        Drawable selector = mSelector;
        Rect selectorRect = mSelectorRect;
        if (selector != null && (isFocused() || touchModeDrawsInPressedState())
                && selectorRect != null && !selectorRect.isEmpty()) {
            final View v = getViewAtPosition(mSelectedPosition);

            if (v != null) {
                if (v.hasFocusable()) return;
                v.setPressed(true);
            }
            setPressed(true);

            final boolean longClickable = isLongClickable();
            Drawable d = selector.getCurrent();
            if (d != null && d instanceof TransitionDrawable) {
                if (longClickable) {
                    ((TransitionDrawable) d).startTransition(ViewConfiguration
                            .getLongPressTimeout());
                } else {
                    ((TransitionDrawable) d).resetTransition();
                }
            }
            if (longClickable && !mDataChanged) {
                if (mPendingCheckForKeyLongPress == null) {
                    mPendingCheckForKeyLongPress = new CheckForKeyLongPress();
                }
                int nTimeDelay = ViewConfiguration.getLongPressTimeout();
                mPendingCheckForKeyLongPress.rememberWindowAttachCount();
                postDelayed(mPendingCheckForKeyLongPress, nTimeDelay);
            }
        }
    }

    /**
     * A base class for Runnables that will check that their view is still attached to
     * the original window as when the Runnable was created.
     *
     */
    private class WindowRunnnable {
        private int mOriginalAttachCount;

        public void rememberWindowAttachCount() {
            mOriginalAttachCount = getWindowAttachCount();
        }

        public boolean sameWindow() {
            return hasWindowFocus() && getWindowAttachCount() == mOriginalAttachCount;
        }
    }

    private class PerformClick extends WindowRunnnable implements Runnable {
        View mChild;
        int mClickMotionPosition;

        @Override
		public void run() {
            // The data has changed since we posted this action in the event queue,
            // bail out before bad things happen
            if (mDataChanged || mTouchState != TOUCH_STATE_TAP) {
            	return;
            }

            final int realPosition = mClickMotionPosition;
            if (realPosition == INVALID_POSITION) {
            	return;
            }

            if (mAdapter != null &&  realPosition < mAdapter.getCount() && sameWindow()) {
                performItemClick(mChild, realPosition, mAdapter.getItemId(realPosition));
                setSelection(INVALID_POSITION);
            }
            mTouchState = TOUCH_STATE_REST;
        }
    }

    private class CheckForLongPress extends WindowRunnnable implements Runnable {

        @Override
		public void run() {
        	if (mDataChanged || mTouchState != TOUCH_STATE_TAP) {
        		return;
        	}
            final int motionPosition = mCheckTapPosition;
            final View child = getViewAtPosition(motionPosition);
            if (child != null && mAdapter != null) {
                final int longPressPosition = motionPosition;
                final long longPressId = mAdapter.getItemId(motionPosition);

                boolean handled = false;
                if (sameWindow() && !mDataChanged) {
                    handled = performLongPress(child, longPressPosition, longPressId);
                }
                if (handled) {
                    mTouchState = TOUCH_STATE_REST;
                    child.setPressed(false);
                    snapToDestination();
                } else {
                    mTouchState = TOUCH_STATE_DONE_WAITING;
                }
            }
        }
    }

    private class CheckForKeyLongPress extends WindowRunnnable implements Runnable {
        @Override
		public void run() {
            if (isPressed() && mCheckTapPosition >= 0) {
                int index = mCheckTapPosition;
                View v = getChildAt(index);

                if (!mDataChanged && mTouchState == TOUCH_STATE_TAP) {
                    boolean handled = false;
                    if (sameWindow()) {
                        handled = performLongPress(v, mCheckTapPosition, mCheckTapPosition);
                    }
                    if (handled) {
                        v.setPressed(false);
                    }
                } else {
                    v.setPressed(false);
                    if (v != null) v.setPressed(false);
                }
            }
        }
    }

    private boolean performLongPress(final View child,
            final int longPressPosition, final long longPressId) {
        boolean handled = false;

        if (getOnItemLongClickListener() != null) {
            handled = getOnItemLongClickListener().onItemLongClick(SlidingView.this, child,
                    longPressPosition, longPressId);
        }
        if (handled) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
        return handled;
    }
    /**
     * AbsListView extends LayoutParams to provide a place to hold the view type.
     */
    public class LayoutParams extends AdapterView.LayoutParams {
        /**
         * View type for this view, as returned by
         * {@link android.widget.Adapter#getItemViewType(int) }
         */
        int viewType;

        /**
         * When this boolean is set, the view has been added to the AbsListView
         * at least once. It is used to know whether headers/footers have already
         * been added to the list view and whether they should be treated as
         * recycled views or not.
         */
        boolean recycledHeaderFooter;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int w, int h) {
            super(w, h);
        }

        public LayoutParams(int w, int h, int viewType) {
            super(w, h);
            this.viewType = viewType;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new SlidingView.LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof SlidingView.LayoutParams;
    }

    //TODO:ADW DATA HANDLING
    class AdapterDataSetObserver extends DataSetObserver {

        private Parcelable mInstanceState = null;

        @Override
        public void onChanged() {
            mDataChanged = true;
            mOldItemCount = mItemCount;
            mItemCount = getAdapter().getCount();
            mTotalScreens = getPageCount();
    		mPager.setScreenCount(mTotalScreens);

    		if ((mTotalScreens - 1) < mCurrentScreen) {
                scrollTo(0, 0);
                mCurrentScreen = 0;
                mCurrentHolder = 1;
                mPager.setCurrentScreen(mCurrentScreen);
                mBlockLayouts = false;
                mScrollToScreen = 0;
                mLayoutMode = LAYOUT_NORMAL;
    		}
            // Detect the case where a cursor that was previously invalidated has
            // been repopulated with new data.
            if (SlidingView.this.getAdapter().hasStableIds() && mInstanceState != null
                    && mOldItemCount == 0 && mItemCount > 0) {
            	SlidingView.this.onRestoreInstanceState(mInstanceState);
                mInstanceState = null;
            }
            mBlockLayouts = false;
            requestLayout();
        }

        @Override
        public void onInvalidated() {
            mDataChanged = true;

            if (SlidingView.this.getAdapter().hasStableIds()) {
                // Remember the current state for the case where our hosting activity is being
                // stopped and later restarted
                mInstanceState = SlidingView.this.onSaveInstanceState();
            }

            // Data is invalid so we should reset our state
            mOldItemCount = mItemCount;
            mItemCount = 0;
            mSelectedPosition = INVALID_POSITION;
        }

        public void clearSavedState() {
            mInstanceState = null;
        }
    }

	public int getNumColumns() {
		return mNumColumns;
	}

	public void setPageHorizontalMargin(int margin) {
		if (margin != mPageHorizontalMargin) {
			this.mPageHorizontalMargin = margin;
			if (mAdapter != null) {
				scrollTo(0, 0);
				mTotalScreens = getPageCount();
				mCurrentScreen = 0;
				mCurrentHolder = 1;
				mPager.setScreenCount(mTotalScreens);
				mPager.setCurrentScreen(mCurrentScreen);
				mBlockLayouts = false;
				mScrollToScreen = 0;
				mLayoutMode = LAYOUT_NORMAL;
				requestLayout();
			}
		}
	}

	public void setNumColumns(int numColumns) {
		if (mNumColumns != numColumns) {
			this.mNumColumns = numColumns;
			if (mAdapter != null) {
				scrollTo(0, 0);
				mTotalScreens = getPageCount();
				mCurrentScreen = 0;
				mCurrentHolder = 1;
	    		mPager.setScreenCount(mTotalScreens);
	    		mPager.setCurrentScreen(mCurrentScreen);
	    		mBlockLayouts = false;
	    		mScrollToScreen = 0;
	    		mLayoutMode = LAYOUT_NORMAL;
				requestLayout();
			}
		}
	}

	public int getNumRows() {
		return mNumRows;
	}

	public void setNumRows(int numRows) {
		if (mNumRows != numRows) {
			this.mNumRows = numRows;
			if (mAdapter != null) {
				scrollTo(0, 0);
				mTotalScreens = getPageCount();
				mCurrentScreen = 0;
				mCurrentHolder = 1;
	    		mPager.setScreenCount(mTotalScreens);
	    		mPager.setCurrentScreen(mCurrentScreen);
				mBlockLayouts = false;
				mScrollToScreen = 0;
				mLayoutMode = LAYOUT_NORMAL;
	    		requestLayout();
			}
		}
	}
	
	public int getPerPageCount(){
		return mNumPerPage;
	}

	public void setDragViewIndex(int index){
		mDragViewIndex = index;
	}
	
	public int getDragViewIndex(){
		return mDragViewIndex;
	}
	
	public void open(boolean animate) {
		mScroller.forceFinished(true);
		setVisibility(View.VISIBLE);
		findCurrentHolder();
        final HolderLayout holder = (HolderLayout) getChildAt(mCurrentHolder);
        if (getAdapter() == null) {
        	animate = false;
        } else if (getAdapter().getCount() <= 0) {
        	animate = false;
        }
//        if (animate) {
//            ListAdapter adapter = getAdapter();
//            if (adapter instanceof ApplicationsAdapter)
//                ((ApplicationsAdapter)adapter).setChildDrawingCacheEnabled(true);
//        	mPager.setVisibility(INVISIBLE);
//    		mBgAlpha = 0;
//    	} else {
//    		mPager.setVisibility(VISIBLE);
//    		mBgAlpha = mTargetAlpha;
//    	}
		if (holder == null) {
			isAnimating = animate;
		} else {
			if (mBlockLayouts) {
				snapToDestination();
				holder.open(animate, mAnimationDuration);
			} else {
				isAnimating = animate;
			}
		}
	}

	public void close(boolean animate) {
		setPressed(false);
        if (getAdapter() == null) {
        	animate = false;
        } else if (getAdapter().getCount() <= 0) {
        	animate = false;
        }
    	if (animate) {
    		findCurrentHolder();
    		HolderLayout holder = (HolderLayout) getChildAt(mCurrentHolder);
    		if (holder != null) {
    			holder.close(animate, mAnimationDuration);
    		} else {
//    			setVisibility(View.GONE);
    		}
    	} else {
//    		setVisibility(View.GONE);
    	}
	}

	public void setAnimationSpeed(int speed) {
		mAnimationDuration = speed;
	}

	/**
	 * ADW: find the current child page
	 */
	private void findCurrentHolder() {
    	for (int i = 1; i < getChildCount(); i++) {
    		if (getChildAt(i).getTag().equals(mCurrentScreen)) {
    			mCurrentHolder = i;
    			break;
    		}
    	}
	}
	
	private HolderLayout getNextHolderView() {
		int nextHolder = -1;
		int count = getChildCount();
		if(mCurrentHolder == count -1 && mCurrentHolder > 0){
			nextHolder = (mCurrentHolder - 1);
		}else{
			nextHolder = (mCurrentHolder + 1);
		}

		View holderView = getChildAt(nextHolder);
		if (!(holderView instanceof HolderLayout && holderView.getTag().equals(mCurrentScreen + 1))) {
	    	for (int i = 1; i < count; i++) {
	    		View child = getChildAt(i);
	    		if (child instanceof HolderLayout && child.getTag().equals(mCurrentScreen+1)) {
	    			holderView = child;
	    			break;
	    		}
	    	}
		}
		
		if (holderView != null) {
			return (HolderLayout)holderView;
		}
		
    	return null;
	}

	public void updateAppGrp() {
		if (getAdapter() != null) {
			//(getAdapter()).updateDataSet();
			scrollTo(0, 0);
			mTotalScreens = getPageCount();
			mCurrentScreen = 0;
			mCurrentHolder = 1;
			mPager.setScreenCount(mTotalScreens);
			mPager.setCurrentScreen(mCurrentScreen);
			mBlockLayouts = false;
			mScrollToScreen = 0;
			mLayoutMode = LAYOUT_NORMAL;
			requestLayout();
		}
	}

	public View getViewAtIndex(int index) {
		return getViewAtPosition(index);
	}

	public void setTextFilterEnabled(boolean textFilterEnabled) {}
	public void clearTextFilter() {}

	public void setScreenIndicator(ScreenIndicator mScreenIndicator) {
		// TODO Auto-generated method stub
		mPager = mScreenIndicator;
		mPager.setScreenCount(mTotalScreens);
		mPager.setCurrentScreen(mCurrentScreen);
	}

	public void adjustOrderIdInFolder(ApplicationFolderInfo folderInfo, int orderId) {
		// TODO Auto-generated method stub
		// If the item is not the last item in source folder
		// should adjust rest items' orderId
		if (orderId < (folderInfo.getSize() - 1)) {
			final ContentResolver cr = getContext().getContentResolver();
			Uri uri = Applications.getCustomUri("/adjustOrderId");
			cr.update(uri,
					null, null, new String[] { String.valueOf(folderInfo.id), String.valueOf(orderId) });
		}
	}
}
