/*
 * Copyright 2013 Chris Banes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockExpandableListActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.app.SherlockPreferenceActivity;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.util.WeakHashMap;

public class PullToRefreshAttacher extends
        uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher {

    private static final int DEFAULT_ANIM_HEADER_IN = R.anim.fade_in;
    private static final int DEFAULT_ANIM_HEADER_OUT = R.anim.fade_out;

    private static final WeakHashMap<Activity, PullToRefreshAttacher> ATTACHERS
            = new WeakHashMap<Activity, PullToRefreshAttacher>();

    public static PullToRefreshAttacher get(Activity activity) {
        return get(activity, new AbsOptions());
    }

    public static PullToRefreshAttacher get(Activity activity,
            uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher.Options options) {
        PullToRefreshAttacher attacher = ATTACHERS.get(activity);
        if (attacher == null) {
            attacher = new PullToRefreshAttacher(activity, options);
            ATTACHERS.put(activity, attacher);
        }
        return attacher;
    }

    protected PullToRefreshAttacher(Activity activity,
            uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher.Options options) {
        super(activity, options);
    }

    @Override
    protected EnvironmentDelegate createDefaultEnvironmentDelegate() {
        return new AbsEnvironmentDelegate();
    }

    @Override
    protected HeaderTransformer createDefaultHeaderTransformer() {
        return new AbsDefaultHeaderTransformer();
    }

    public static class AbsOptions extends Options {

        /**
         * The anim resource ID which should be started when the header is being hidden.
         */
        public int headerOutAnimationPreHoneycomb = DEFAULT_ANIM_HEADER_OUT;

        /**
         * The anim resource ID which should be started when the header is being shown.
         */
        public int headerInAnimationPreHoneycomb = DEFAULT_ANIM_HEADER_IN;
    }

    public static class AbsEnvironmentDelegate extends EnvironmentDelegate {
        /**
         * @return Context which should be used for inflating the header layout
         */
        public Context getContextForInflater(Activity activity) {
            if (activity instanceof SherlockActivity) {
                return ((SherlockActivity) activity).getSupportActionBar().getThemedContext();
            } else if (activity instanceof SherlockListActivity) {
                return ((SherlockListActivity) activity).getSupportActionBar().getThemedContext();
            } else if (activity instanceof SherlockFragmentActivity) {
                return ((SherlockFragmentActivity) activity).getSupportActionBar()
                        .getThemedContext();
            } else if (activity instanceof SherlockExpandableListActivity) {
                return ((SherlockExpandableListActivity) activity).getSupportActionBar()
                        .getThemedContext();
            } else if (activity instanceof SherlockPreferenceActivity) {
                return ((SherlockPreferenceActivity) activity).getSupportActionBar()
                        .getThemedContext();
            }
            return super.getContextForInflater(activity);
        }
    }

    public static class AbsDefaultHeaderTransformer extends DefaultHeaderTransformer {

        private Animation mHeaderInAnimation, mHeaderOutAnimation;

        @Override
        public void onViewCreated(Activity activity, View headerView, Options options) {
            super.onViewCreated(activity, headerView, options);

            int animIn = DEFAULT_ANIM_HEADER_IN;
            int animOut = DEFAULT_ANIM_HEADER_OUT;

            // If our AbsOptions instance is a Abs version, get the values out
            if (options instanceof AbsOptions) {
                animIn = ((AbsOptions) options).headerInAnimationPreHoneycomb;
                animOut = ((AbsOptions) options).headerOutAnimationPreHoneycomb;
            }

            mHeaderInAnimation = AnimationUtils.loadAnimation(activity, animIn);
            mHeaderOutAnimation = AnimationUtils.loadAnimation(activity, animOut);
        }

        @Override
        public void showHeaderView() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                super.showHeaderView();
                return;
            }

            if (mHeaderView.getVisibility() != View.VISIBLE) {
                if (mHeaderInAnimation != null) {
                    mHeaderView.startAnimation(mHeaderInAnimation);
                }
                mHeaderView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void hideHeaderView() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                super.hideHeaderView();
                return;
            }

            // Hide Header
            if (mHeaderView.getVisibility() == View.VISIBLE) {
                if (mHeaderOutAnimation != null) {
                    mHeaderOutAnimation
                            .setAnimationListener(new AnimationCallback(mHeaderView, this));
                    mHeaderView.startAnimation(mHeaderOutAnimation);
                    // onReset() is called once the animation has finished
                } else {
                    // As we're not animating, hide the header + call the header transformer now
                    mHeaderView.setVisibility(View.GONE);
                    onReset();
                }
            }
        }

        @Override
        public void onRefreshMinimized() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                super.onRefreshMinimized();
                return;
            }

            // Here we fade out most of the header, leaving just the progress bar
            if (mContentLayout != null) {
                mContentLayout.startAnimation(AnimationUtils
                        .loadAnimation(mContentLayout.getContext(), R.anim.fade_out));
                mContentLayout.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        protected Drawable getActionBarBackground(Context context) {
            // Super handles ICS+ anyway...
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                return super.getActionBarBackground(context);
            }

            // Need to get resource id of style pointed to from actionBarStyle
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.actionBarStyle, outValue, true);
            // Now get action bar style values...
            TypedArray abStyle = context.getTheme().obtainStyledAttributes(outValue.resourceId,
                    R.styleable.SherlockActionBar);
            try {
                return abStyle.getDrawable(R.styleable.SherlockActionBar_background);
            } finally {
                abStyle.recycle();
            }
        }

        @Override
        protected int getActionBarSize(Context context) {
            // Super handles ICS+ anyway...
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                return super.getActionBarSize(context);
            }

            TypedArray values = context.getTheme()
                    .obtainStyledAttributes(R.styleable.SherlockTheme);
            try {
                return values.getDimensionPixelSize(R.styleable.SherlockTheme_actionBarSize, 0);
            } finally {
                values.recycle();
            }
        }
    }

    static class AnimationCallback implements Animation.AnimationListener {
        private final View mHeaderView;
        private final HeaderTransformer mHeaderTransformer;

        AnimationCallback(View headerView, HeaderTransformer headerTransformer) {
            mHeaderTransformer = headerTransformer;
            mHeaderView = headerView;
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
           mHeaderView.setVisibility(View.GONE);
           mHeaderTransformer.onReset();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }
}
