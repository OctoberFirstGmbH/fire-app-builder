/**
 * This file was modified by Amazon:
 * Copyright 2015-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.amazon.android.tv.tenfoot.ui.fragments;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.DetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.HorizontalGridView;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowHeaderPresenter;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.support.v17.leanback.widget.TenFootActionPresenterSelector;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.model.Action;
import com.amazon.android.model.content.Content;
import com.amazon.android.model.content.ContentContainer;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.constants.Broadcasters;
import com.amazon.android.tv.tenfoot.presenter.CardPresenter;
import com.amazon.android.tv.tenfoot.presenter.DetailsDescriptionPresenter;
import com.amazon.android.tv.tenfoot.ui.activities.ContentDetailsActivity;
import com.amazon.android.tv.tenfoot.utils.LeanbackHelpers;
import com.amazon.android.utils.GlideHelper;
import com.amazon.android.utils.Helpers;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.List;


/**
 * LeanbackDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback
 * content_details_activity_layout screens.
 * It shows a detailed view of video and its meta plus related videos.
 */
public class ContentDetailsFragment extends android.support.v17.leanback.app.DetailsFragment {

    private static final String TAG = ContentDetailsFragment.class.getSimpleName();
    private static final String TV_PROGRAM_VIDEO_URL_DEFAULT = "https://featvre.com/default.mp4";
    private static final int DETAIL_THUMB_WIDTH = 264; //264;
    private static final int DETAIL_THUMB_HEIGHT = 198; //198;
    private static final String VIDEO_UNAVAILABLE_TITLE = "Dieses Video ist leider nicht abspielbar";
    private static final String VIDEO_UNAVAILABLE_MESSAGE
            = "Bitte versuchen Sie, den Beitrag über Ihr Smartphone zu finden und abzuspielen.";

    private Content mSelectedContent;

    private ArrayObjectAdapter mAdapter;
    private ClassPresenterSelector mPresenterSelector;

    private BackgroundManager mBackgroundManager;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private boolean mShowRelatedContent;

    SparseArrayObjectAdapter mActionAdapter = new SparseArrayObjectAdapter();

    // Decides whether the action button should be enabled or not.
    private boolean actionInProgress = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate DetailsFragment");
        super.onCreate(savedInstanceState);

        prepareBackgroundManager();

        mSelectedContent = ContentBrowser.getInstance(getActivity()).getLastSelectedContent();
        mShowRelatedContent = ContentBrowser.getInstance(getActivity()).isShowRelatedContent();
    }

    @Override
    public void onStart() {

        Log.v(TAG, "onStart called.");
        super.onStart();
        if (mSelectedContent != null || checkGlobalSearchIntent()) {

            setupAdapter();
            setupDetailsOverviewRow();
            setupDetailsOverviewRowPresenter();
            if (mShowRelatedContent) {
                setupRelatedContentRow();
            }
            setupContentListRowPresenter();
            updateBackground(mSelectedContent.getBackgroundImageUrl());
            setOnItemViewClickedListener(new ItemViewClickedListener());
        }
        else {
            Log.v(TAG, "Start CONTENT_HOME_SCREEN.");
            ContentBrowser.getInstance(getActivity())
                          .switchToScreen(ContentBrowser.CONTENT_HOME_SCREEN);
        }
    }

    /**
     * Overriding this method to return null since we do not want the title view to be available
     * in ContentDetails page.
     * {@inheritDoc}
     */
    protected View inflateTitle(LayoutInflater inflater, ViewGroup parent,
                                Bundle savedInstanceState) {

        return null;
    }

    /**
     * Check if there is a global search intent.
     */
    private boolean checkGlobalSearchIntent() {

        Log.v(TAG, "checkGlobalSearchIntent called.");
        Intent intent = getActivity().getIntent();
        String intentAction = intent.getAction();
        String globalSearch = getString(R.string.global_search);
        if (globalSearch.equalsIgnoreCase(intentAction)) {
            Uri intentData = intent.getData();
            Log.d(TAG, "action: " + intentAction + " intentData:" + intentData);
            int selectedIndex = Integer.parseInt(intentData.getLastPathSegment());

            ContentContainer contentContainer = ContentBrowser.getInstance(getActivity())
                                                              .getRootContentContainer();

            int contentTally = 0;
            if (contentContainer == null) {
                return false;
            }

            for (Content content : contentContainer) {
                ++contentTally;
                if (selectedIndex == contentTally) {
                    mSelectedContent = content;
                    return true;
                }
            }
        }
        return false;
    }

    private void prepareBackgroundManager() {

        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = ContextCompat.getDrawable(getActivity(), android.R.color.transparent);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void updateBackground(String uri) {

        Log.v(TAG, "updateBackground called");
        if (Helpers.DEBUG) {
            Log.v(TAG, "updateBackground called: " + uri);
        }

        SimpleTarget<Bitmap> bitmapTarget = new SimpleTarget<Bitmap>(mMetrics.widthPixels,
                                                                     mMetrics.heightPixels) {
            @Override
            public void onResourceReady(Bitmap resource,
                                        GlideAnimation<? super Bitmap> glideAnimation) {

                Bitmap bitmap = Helpers.adjustOpacity(resource, getResources().getInteger(
                        R.integer.content_details_fragment_bg_opacity));

                bitmap = blur(bitmap, BITMAP_SCALE, BLUR_RADIUS);

                mBackgroundManager.setBitmap(bitmap);
            }
        };

        GlideHelper.loadImageIntoSimpleTargetBitmap(getActivity(), uri,
                                                    new GlideHelper.LoggingListener(),
                                                    android.R.color.transparent, bitmapTarget);
    }

    private static final float BITMAP_SCALE = 0.8f;
    private static final int BLUR_RADIUS = 70;
    public Bitmap blur(Bitmap sentBitmap, float scale, int radius) {

        int width = Math.round(sentBitmap.getWidth() * scale);
        int height = Math.round(sentBitmap.getHeight() * scale);
        sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false);

        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        Log.e("pix", w + " " + h + " " + pix.length);
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = ( 0xff000000 & pix[yi] ) | ( dv[rsum] << 16 ) | ( dv[gsum] << 8 ) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        Log.e("pix", w + " " + h + " " + pix.length);
        bitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return (bitmap);
    }

    private void setupAdapter() {

        Log.v(TAG, "setupAdapter called.");
        mPresenterSelector = new ClassPresenterSelector();
        mAdapter = new ArrayObjectAdapter(mPresenterSelector);
        setAdapter(mAdapter);
    }

    public void updateActions() {

        List<Action> contentActionList = ContentBrowser.getInstance(getActivity())
                                                       .getContentActionList(mSelectedContent);

        int i = 0;
        mActionAdapter.clear();
        for (Action action : contentActionList) {
            mActionAdapter.set(i++, LeanbackHelpers.translateActionToLeanBackAction(action));
        }

        actionInProgress = false;
    }

    private void setupDetailsOverviewRow() {

        Log.d(TAG, "doInBackground");
        if (Helpers.DEBUG) {
            Log.d(TAG, "Selected content is: " + mSelectedContent.toString());
        }
        final DetailsOverviewRow row = new DetailsOverviewRow(mSelectedContent);
        row.setActionsAdapter(new ArrayObjectAdapter(new TenFootActionPresenterSelector()));
        row.setImageDrawable(ContextCompat.getDrawable(getActivity(),
                                                       android.R.color.transparent));
        int width = Helpers.convertDpToPixel(getActivity().getApplicationContext(),
                                             DETAIL_THUMB_WIDTH);
        int height = Helpers.convertDpToPixel(getActivity().getApplicationContext(),
                                              DETAIL_THUMB_HEIGHT);


        SimpleTarget<Bitmap> bitmapTarget = new SimpleTarget<Bitmap>(width, height) {
            @Override
            public void onResourceReady(Bitmap resource,
                                        GlideAnimation<? super Bitmap> glideAnimation) {

                Log.d(TAG,
                      "content_details_activity_layout overview card image url ready: " + resource);

                int cornerRadius =
                        getResources().getInteger(R.integer.details_overview_image_corner_radius);

                row.setImageBitmap(getActivity(),
                                   Helpers.roundCornerImage(getActivity(), resource, cornerRadius));

                mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
            }
        };

        GlideHelper.loadImageIntoSimpleTargetBitmap(getActivity(),
                                                    mSelectedContent.getCardImageUrl(),
                                                    new GlideHelper.LoggingListener<>(),
                                                    android.R.color.transparent,
                                                    bitmapTarget);

        updateActions();
        row.setActionsAdapter(mActionAdapter);

        mAdapter.add(row);
    }

    private void setupDetailsOverviewRowPresenter() {

        DetailsDescriptionPresenter detailsDescPresenter = new DetailsDescriptionPresenter();

        // Set detail background and style.
        DetailsOverviewRowPresenter detailsPresenter =
                new DetailsOverviewRowPresenter(detailsDescPresenter) {
                    @Override
                    protected void initializeRowViewHolder(RowPresenter.ViewHolder vh) {

                        super.initializeRowViewHolder(vh);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            vh.view.findViewById(R.id.details_overview_image)
                                   .setTransitionName(ContentDetailsActivity.SHARED_ELEMENT_NAME);
                        }

                        //R.layout.lb_details_overview
                        //R.id.lb_details_description_title

                        View details = vh.view.findViewById(R.id.details_frame);
                        //details.setBackgroundColor(Color.parseColor("#FF0000"));
                        ViewGroup.LayoutParams layoutParams2 = details.getLayoutParams();
                        layoutParams2.height = 1000;
                        //details.setLayoutParams(layoutParams2);

                        View detailsRightPanel = vh.view.findViewById(R.id.details_overview_description);
                        //detailsRightPanel.setBackgroundColor(Color.parseColor("#FFFF00"));
                        ViewGroup.LayoutParams layoutParams1 = detailsRightPanel.getLayoutParams();
                        //layoutParams1.height = 1100;
                        //detailsRightPanel.setLayoutParams(layoutParams1);

                        View detailsActions = vh.view.findViewById(R.id.details_overview_actions);
                        //detailsActions.setBackgroundColor(Color.parseColor("#0000FF"));
                        ViewGroup.LayoutParams layoutParams3 = detailsActions.getLayoutParams();
                        //layoutParams3.height = 300;
                        //detailsActions.setLayoutParams(layoutParams3);

                    }
                };
        detailsPresenter.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        detailsPresenter.setStyleLarge(true);


        // Hook up transition element.
        detailsPresenter.setSharedElementEnterTransition(getActivity(),
                                                         ContentDetailsActivity
                                                                 .SHARED_ELEMENT_NAME);

        detailsPresenter.setOnActionClickedListener(action -> {
            try {
                if (actionInProgress) {
                    return;
                }
                actionInProgress = true;

                int actionId = (int) action.getId();
                Log.v(TAG, "detailsPresenter.setOnActionClicked:" + actionId);

                switch (mSelectedContent.getPlayMode()) {
                    case DirectPlay:
                        // have the real video url, play it
                        if(!mSelectedContent.getUrl().equals(TV_PROGRAM_VIDEO_URL_DEFAULT)) {
                            ContentBrowser.getInstance(getActivity())
                                    .actionTriggered(getActivity(), mSelectedContent, actionId);
                        } // have the deep link
                        else if (mSelectedContent.getDeepLinkUrl() != null && !mSelectedContent.getDeepLinkUrl().isEmpty()) {
                            boolean hasDeepLinkWorked = tryDeepLink(mSelectedContent.getBroadcaster());
                            if(!hasDeepLinkWorked) {
                                showDialog(VIDEO_UNAVAILABLE_TITLE, VIDEO_UNAVAILABLE_MESSAGE, "OK", null);
                            }
                        } // default: unavailable message
                        else {
                            actionInProgress = false;
                            showDialog(VIDEO_UNAVAILABLE_TITLE, VIDEO_UNAVAILABLE_MESSAGE, "OK", null);
                        }
                        break;
                    case Deeplink:
                        // have the deep link
                        if (mSelectedContent.getDeepLinkUrl() != null && !mSelectedContent.getDeepLinkUrl().isEmpty()) {
                            boolean hasDeepLinkWorked = tryDeepLink(mSelectedContent.getBroadcaster());
                            if(!hasDeepLinkWorked) {
                                showDialog(VIDEO_UNAVAILABLE_TITLE, VIDEO_UNAVAILABLE_MESSAGE, "OK", null);
                            }
                        } // default: unavailable message
                        else {
                            actionInProgress = false;
                            showDialog(VIDEO_UNAVAILABLE_TITLE, VIDEO_UNAVAILABLE_MESSAGE, "OK", null);
                        }
                        break;
                    default:
                        break;
                }

                /*
                // if we have the deep link
                if(mSelectedContent.getDeepLinkUrl() != null && !mSelectedContent.getDeepLinkUrl().isEmpty()) {

                    boolean hasDeepLinkWorked = tryDeepLink(mSelectedContent.getBroadcaster());

                    // todo: get the information from the server if broadcaster can be played in case of deep link fail
                    boolean canPlayDirectly = true;
                    if(!hasDeepLinkWorked) {
                        if(canPlayDirectly) {
                            ContentBrowser.getInstance(getActivity())
                                    .actionTriggered(getActivity(), mSelectedContent, actionId);
                        } else {
                            showDialog("Dieses Video ist leider nicht abspielbar",
                                    "Bitte versuchen Sie, den Beitrag über Ihr Smartphone zu finden und abzuspielen.",
                                    "OK", null);
                        }
                    }
                } // if we have the default video url
                else if(mSelectedContent.getUrl().equals(TV_PROGRAM_VIDEO_URL_DEFAULT)) {
                    actionInProgress = false;
                    showDialog("Dieses Video ist leider nicht abspielbar",
                            "Bitte versuchen Sie, den Beitrag über Ihr Smartphone zu finden und abzuspielen.",
                            "OK", null);
                } // just play video url
                else {
                    ContentBrowser.getInstance(getActivity())
                            .actionTriggered(getActivity(), mSelectedContent, actionId);
                }
                */
            }
            catch (Exception e) {
                Log.e(TAG, "caught exception while clicking action", e);
                actionInProgress = false;
            }
        });
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
    }

    private boolean tryDeepLink(final String broadcaster) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mSelectedContent.getDeepLinkUrl()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setComponent(new ComponentName(Broadcasters.getPackageName(broadcaster),
                    Broadcasters.getClassName(broadcaster)));
            startActivity(intent);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    /**
     * Builds the related content row. Uses contents from the selected content's category.
     */
    private void setupRelatedContentRow() {

        ContentContainer recommended =
                ContentBrowser.getInstance(getActivity())
                              .getRecommendedListOfAContentAsAContainer(mSelectedContent);
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());

        for (Content c : recommended) {
            listRowAdapter.add(c);
        }
        // Only add the header and row for recommendations if there are any recommended content.
        if (listRowAdapter.size() > 0) {
            HeaderItem header = new HeaderItem(0, recommended.getName());
            mAdapter.add(new ListRow(header, listRowAdapter));
        }
    }

    private void setupContentListRowPresenter() {

        ListRowPresenter presenter = new ListRowPresenter();
        presenter.setHeaderPresenter(new RowHeaderPresenter());
        mPresenterSelector.addClassPresenter(ListRow.class, presenter);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {

        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Content) {
                Content content = (Content) item;
                if (Helpers.DEBUG) {
                    Log.d(TAG, "Item: " + content.getId());
                }
                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        ContentDetailsActivity.SHARED_ELEMENT_NAME).toBundle();

                ContentBrowser.getInstance(getActivity())
                              .setLastSelectedContent(content)
                              .switchToScreen(ContentBrowser.CONTENT_DETAILS_SCREEN, bundle);
            }
        }
    }

    private void showDialog(String title, String message, String positiveButtonText, String negativeButtonText) {

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(title)
                    .setMessage(message);
            if(positiveButtonText != null && !positiveButtonText.isEmpty()){
                builder.setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //if (dialogFragmentListener != null) {
                        //  dialogFragmentListener.onDialogFragmentConfirm();
                        //}
                    }
                });
            }
            if(negativeButtonText != null && !negativeButtonText.isEmpty()){
                builder.setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //if (dialogFragmentListener != null) {
                        //  dialogFragmentListener.onDialogFragmentCancel();
                        //}
                    }
                });
            }

            AlertDialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            alertDialog.show();

        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    @Override
    public void onResume() {

        Log.v(TAG, "onResume called.");
        super.onResume();
        updateActionsProperties();
        actionInProgress = false;
    }

    /**
     * Since we do not have direct access to the details overview actions row, we are adding a
     * delayed handler that waits for some time, searches for the row and then updates the
     * properties. This is not a fool-proof method,
     * > In slow devices its possible that this does not succeed in achieving the desired result.
     * > In fast devices its possible that the update is clearly visible to the user.
     * TODO: Find a better approach to update action properties
     */
    private void updateActionsProperties() {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            View view = getView();
            if (view != null) {
                HorizontalGridView horizontalGridView =
                        (HorizontalGridView) view.findViewById(R.id.details_overview_actions);

                if (horizontalGridView != null) {
                    // This is required to make sure this button gets the focus whenever
                    // detailsFragment is resumed.
                    horizontalGridView.requestFocus();
                    for (int i = 0; i < horizontalGridView.getChildCount(); i++) {
                        final Button button = (Button) horizontalGridView.getChildAt(i);
                        if (button != null) {
                            // Button objects are recreated every time MovieDetailsFragment is
                            // created or restored, so we have to bind OnKeyListener to them on
                            // resuming the Fragment.
                            button.setOnKeyListener((v, keyCode, keyEvent) -> {
                                if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE &&
                                        keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                                    button.performClick();
                                }
                                return false;
                            });
                        }
                    }
                }
            }
        }, 400);
    }
}
