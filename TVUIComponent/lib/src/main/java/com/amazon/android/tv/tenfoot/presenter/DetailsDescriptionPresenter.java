/**
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
package com.amazon.android.tv.tenfoot.presenter;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazon.android.configuration.ConfigurationManager;
import com.amazon.android.model.content.Content;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.base.TenFootApp;
import com.amazon.android.tv.tenfoot.utils.ContentHelper;
import com.amazon.android.ui.constants.ConfigurationConstants;
import com.amazon.android.ui.widget.EllipsizedTextView;
import com.amazon.android.utils.GlideHelper;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import uk.co.chrisjenx.calligraphy.CalligraphyUtils;

/**
 * An {@link Presenter} for rendering the detailed description of an item.
 * The description needs to have a title and subtitle.
 */
public class DetailsDescriptionPresenter extends Presenter {

    private final static String TAG = DetailsDescriptionPresenter.class.getSimpleName();

    private Context mContext;

    /**
     * View holder for the details description. It contains title, subtitle, and body text views.
     */
    public static class ViewHolder extends Presenter.ViewHolder {

        private final TextView mCategory;
        private final TextView mTitle;
        private final TextView mSubtitle;
        private final TextView mBody;
        //private final ImageView mBroadcasterImage;
        private final TextView mVideoLength;
        private final TextView mCredits;

        public ViewHolder(final View view) {

            super(view);
            mCategory = (TextView) view.findViewById(R.id.details_description_category);
            mTitle = (TextView) view.findViewById(R.id.details_description_title);
            mSubtitle = (TextView) view.findViewById(R.id.details_description_subtitle);
            mBody = (EllipsizedTextView) view.findViewById(R.id.ellipsized_description_text);
            //mBroadcasterImage = (ImageView) view.findViewById(R.id.details_broadcaster_image);
            mVideoLength = (TextView) view.findViewById(R.id.details_video_length);
            mCredits = (TextView) view.findViewById(R.id.details_credits);
        }

//        public ImageView getBroadcasterImage() {
//
//            return mBroadcasterImage;
//        }

        public TextView getVideoLengthText() {

            return mVideoLength;
        }

        public TextView getCategory() {

            return mCategory;
        }

        public TextView getTitle() {

            return mTitle;
        }

        public TextView getSubtitle() {

            return mSubtitle;
        }

        public TextView getBody() {

            return mBody;
        }

        public TextView getCredits() {

            return mCredits;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ViewHolder onCreateViewHolder(ViewGroup parent) {

        Log.v(TAG, "onCreateViewHolder called.");

        mContext = parent.getContext();

        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.details_description_presenter_layout, parent,
                                           false);
        return new ViewHolder(view);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {

        Log.v(TAG, "onBindViewHolder called.");
        ViewHolder customViewHolder = (ViewHolder) viewHolder;
        onBindDescription(customViewHolder, item);
    }

    private void onBindDescription(ViewHolder viewHolder, Object item) {

        Log.v(TAG, "onBindDescription called.");
        Content content = (Content) item;

        if (content != null) {
            populateViewHolder(viewHolder, content);
        }
        else {
            Log.e(TAG, "Content is null in onBindDescription");
        }
    }

    /**
     * Populate view holder with content model data.
     *
     * @param viewHolder ViewHolder object.
     * @param content    Content model object.
     */
    private void populateViewHolder(ViewHolder viewHolder, Content content) {

        ConfigurationManager config = ConfigurationManager.getInstance(TenFootApp.getInstance());

        // category
        viewHolder.getCategory().setEllipsize(TextUtils.TruncateAt.END);
        viewHolder.getCategory().setSingleLine();
        viewHolder.getCategory().setText(content.getCategory());
        CalligraphyUtils.applyFontToTextView(TenFootApp.getInstance(), viewHolder.getCategory(),
                config.getTypefacePath(ConfigurationConstants.BOLD_ITALIC_FONT));
        viewHolder.getCategory().setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

        // title
        viewHolder.getTitle().setEllipsize(TextUtils.TruncateAt.END);
        viewHolder.getTitle().setSingleLine();
        viewHolder.getTitle().setText(content.getTitle());
        CalligraphyUtils.applyFontToTextView(TenFootApp.getInstance(), viewHolder.getTitle(),
                                             config.getTypefacePath(ConfigurationConstants
                                                                            .BOLD_FONT));

        viewHolder.getSubtitle().setText(ContentHelper.getDescriptiveSubtitle(mContext, content));

        if(content.getReview().trim().length() <= 0) {
            viewHolder.getBody().setText(content.getDescription().trim());
        } else {
            viewHolder.getBody().setText(content.getReview().trim() + "\n\n" + content.getDescription().trim());
        }

        CalligraphyUtils.applyFontToTextView(TenFootApp.getInstance(), viewHolder.getBody(),
                                             config.getTypefacePath(ConfigurationConstants
                                                                            .LIGHT_FONT));
        //updateBroadcasterImage(viewHolder.getBroadcasterImage(), content.getBroadcasterLogo());

        if(content.getDuration() > 0) {
            viewHolder.getVideoLengthText().setText(content.getBroadcaster() + " | " + content.getDuration() + " min");
        }

        viewHolder.getCredits().setText("Bild:© " + content.getImageCredits());
    }

    private void updateBroadcasterImage(final ImageView broadcasterImageView, final String broadcasterLogoUrl) {
        GlideHelper.loadImageIntoView(
                broadcasterImageView, mContext, broadcasterLogoUrl, new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        broadcasterImageView.setImageDrawable(resource);
                        return false;
                    }
                }, android.R.color.transparent,
                new ColorDrawable(ContextCompat.getColor(mContext, android.R.color.transparent)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder holder) {

        Log.v(TAG, "onViewAttachedToWindow called.");
        ViewHolder customViewHolder = (ViewHolder) holder;
        super.onViewAttachedToWindow(customViewHolder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onViewDetachedFromWindow(Presenter.ViewHolder holder) {

        Log.v(TAG, "onViewDetachedFromWindow called.");
        super.onViewDetachedFromWindow(holder);
    }
}
