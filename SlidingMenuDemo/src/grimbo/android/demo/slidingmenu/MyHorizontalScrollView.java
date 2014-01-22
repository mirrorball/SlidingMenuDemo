/*
 * #%L
 * SlidingMenuDemo
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2012 Paul Grime
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package grimbo.android.demo.slidingmenu;

import java.sql.Savepoint;

import android.content.Context;
import android.database.DataSetObservable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.Toast;

/**
 * A HorizontalScrollView (HSV) implementation that disallows touch events (so no scrolling can be done by the user).
 * 
 * This HSV MUST contain a single ViewGroup as its only child, and this ViewGroup will be used to display the children Views
 * passed in to the initViews() method.
 */
public class MyHorizontalScrollView extends HorizontalScrollView {
    public MyHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public MyHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MyHorizontalScrollView(Context context) {
        super(context);
        init(context);
    }

    void init(Context context) {
        // remove the fading as the HSV looks better without it
        setHorizontalFadingEdgeEnabled(false);
        setVerticalFadingEdgeEnabled(false);
    }

    /**
     * @param children
     *            The child Views to add to parent.
     * @param scrollToViewIdx
     *            The index of the View to scroll to after initialisation.
     * @param sizeCallback
     *            A SizeCallback to interact with the HSV.
     */
    public void initViews(View[] children, int scrollToViewIdx, SizeCallback sizeCallback) {
        // A ViewGroup MUST be the only child of the HSV
        ViewGroup parent = (ViewGroup) getChildAt(0);

        // Add all the children, but add them invisible so that the layouts are calculated, but you can't see the Views
        for (int i = 0; i < children.length; i++) {
            children[i].setVisibility(View.INVISIBLE);
            parent.addView(children[i]);
        }

        // Add a layout listener to this HSV
        // This listener is responsible for arranging the child views.
        OnGlobalLayoutListener listener = new MyOnGlobalLayoutListener(parent, children, scrollToViewIdx, sizeCallback);
        getViewTreeObserver().addOnGlobalLayoutListener(listener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Do not allow touch events.
    	
    	final int action = ev.getAction();
    	final float x = ev.getX();
    	final float y = ev.getY();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			Log.i("dddddddd", "ACTION_DOWN x: " + x + ", y: " + y);
			break;
		case MotionEvent.ACTION_MOVE:
			Log.i("mmmmmmm", "ACTION_MOVE x: " + x + ", y: " + y);
			break;

		case MotionEvent.ACTION_UP:
			Log.i("uuuuuuuu", "ACTION_UP x: " + x + ", y: " + y);
			break;

		case MotionEvent.ACTION_CANCEL:
			Log.i("ccccccccc", "ACTION_CANCEL x: " + x + ", y: " + y);
			break;
		}
		
		
    	 // 터치 up이 되었을 때 화면을 갱신한다.        
		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		// Do not allow touch events.
        return false;
    }

    /**
     * An OnGlobalLayoutListener impl that passes on the call to onGlobalLayout to a SizeCallback, before removing all the Views
     * in the HSV and adding them again with calculated widths and heights.
     */
    class MyOnGlobalLayoutListener implements OnGlobalLayoutListener {
        ViewGroup parent;
        View[] children;
        int scrollToViewIdx;
        int scrollToViewPos = 0;
        SizeCallback sizeCallback;

        /**
         * @param parent
         *            The parent to which the child Views should be added.
         * @param children
         *            The child Views to add to parent.
         * @param scrollToViewIdx
         *            The index of the View to scroll to after initialisation.
         * @param sizeCallback
         *            A SizeCallback to interact with the HSV.
         */
        public MyOnGlobalLayoutListener(ViewGroup parent, View[] children, int scrollToViewIdx, SizeCallback sizeCallback) {
            this.parent = parent;
            this.children = children;
            this.scrollToViewIdx = scrollToViewIdx;
            this.sizeCallback = sizeCallback;
        }

        @Override
        public void onGlobalLayout() {
            // System.out.println("onGlobalLayout");

            final HorizontalScrollView me = MyHorizontalScrollView.this;

            // The listener will remove itself as a layout listener to the HSV
            me.getViewTreeObserver().removeGlobalOnLayoutListener(this);

            // Allow the SizeCallback to 'see' the Views before we remove them and re-add them.
            // This lets the SizeCallback prepare View sizes, ahead of calls to SizeCallback.getViewSize().
            sizeCallback.onGlobalLayout();

            parent.removeViewsInLayout(0, children.length);

            final int w = me.getMeasuredWidth();
            final int h = me.getMeasuredHeight();

            // System.out.println("w=" + w + ", h=" + h);

            // Add each view in turn, and apply the width and height returned by the SizeCallback.
            int[] dims = new int[2];
            scrollToViewPos = 0;
            for (int i = 0; i < children.length; i++) {
                sizeCallback.getViewSize(i, w, h, dims);
                // System.out.println("addView w=" + dims[0] + ", h=" + dims[1]);
                children[i].setVisibility(View.VISIBLE);
                parent.addView(children[i], dims[0], dims[1]);
                if (i < scrollToViewIdx) {
                    scrollToViewPos += dims[0];
                }
            }

            // For some reason we need to post this action, rather than call immediately.
            // If we try immediately, it will not scroll.
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    me.scrollBy(scrollToViewPos, 0);
                }
            });
        }
    }

    /**
     * Callback interface to interact with the HSV.
     */
    public interface SizeCallback {
        /**
         * Used to allow clients to measure Views before re-adding them.
         */
        public void onGlobalLayout();

        /**
         * Used by clients to specify the View dimensions.
         * 
         * @param idx
         *            Index of the View.
         * @param w
         *            Width of the parent View.
         * @param h
         *            Height of the parent View.
         * @param dims
         *            dims[0] should be set to View width. dims[1] should be set to View height.
         */
        public void getViewSize(int idx, int w, int h, int[] dims);
	}

	// /////////////////////////mycode//////////////////////////////////////////////
    
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    static class MyGestureDetector extends SimpleOnGestureListener{
    	private void Invalidate() {
			// TODO Auto-generated method stub
    		
		}
    	View menu;  	
    	HorizontalScrollView scrollView;
		ListView listView;
    	
    	public MyGestureDetector(View menu, HorizontalScrollView scrollView, ListView listView) {
            super();
            this.menu = menu;
            this.scrollView = scrollView;
            this.listView = listView;
            menu.invalidate();
    		scrollView.invalidate();
    		listView.invalidate();
    		Invalidate();
        }
    	
        boolean menuOut = false;
        
        private OnTouchListener mTouchEvent = new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				
				switch (event.getAction()) {
		        case MotionEvent.ACTION_UP :
		            // invalidate()을 호출하면 화면을 갱신한다.
		            menu.invalidate();
		            listView.invalidate();
		            scrollView.invalidate();
		            break;
		        }
				return false;
			}
		};
    	@Override
    	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
    			float velocityY) {
    		// TODO Auto-generated method stub
    		menu.invalidate();
    		scrollView.invalidate();
    		listView.invalidate();
    		Context context = menu.getContext();
            int menuWidth = menu.getMeasuredWidth();
            
            final Toast rt = Toast.makeText(context, "Right Swipe", Toast.LENGTH_SHORT);
            final Toast lt = Toast.makeText(context, "Left Swipe", Toast.LENGTH_SHORT);
            
    		try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;
     
                // right to left swipe
                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                	int left = menuWidth;
                    scrollView.smoothScrollTo(left, 0);
                    rt.show();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                    	@Override
                    	public void run() {
                    		// TODO Auto-generated method stub
                    		rt.cancel();
                    	}
                    }, 1);
                }
                // left to right swipe
                else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    int left = 0;
                    scrollView.smoothScrollTo(left, 0);
                    lt.show();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                    	@Override
                    	public void run() {
                    		// TODO Auto-generated method stub
                    		lt.cancel();
                    	}
                    }, 1);
                }
            } catch (Exception e) {
            	
			}

			return true;
		}
	}
	///////////////////////////mycode END//////////////////////////////////////////
}
