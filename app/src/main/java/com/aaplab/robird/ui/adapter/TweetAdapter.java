package com.aaplab.robird.ui.adapter;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.aaplab.robird.Analytics;
import com.aaplab.robird.R;
import com.aaplab.robird.data.entity.Account;
import com.aaplab.robird.data.entity.Tweet;
import com.aaplab.robird.data.model.PrefsModel;
import com.aaplab.robird.ui.activity.ImagesActivity;
import com.aaplab.robird.ui.activity.TweetDetailsActivity;
import com.aaplab.robird.ui.activity.UserProfileActivity;
import com.aaplab.robird.ui.activity.VideoActivity;
import com.aaplab.robird.util.LinkUtils;
import com.aaplab.robird.util.NetworkUtils;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by majid on 19.01.15.
 */
public class TweetAdapter extends RecyclerView.Adapter<TweetAdapter.TweetHolder> {
    protected final PrefsModel mPrefsModel = new PrefsModel();

    protected List<Tweet> mTweets;
    protected Activity mActivity;
    protected Account mAccount;

    protected boolean mShowMediaPreview;
    protected boolean mCompactTimeline;
    protected boolean mAbsoluteTime;
    protected boolean mShowClientName;
    protected boolean mIsMediaHiddenOnMobile;
    protected boolean mIsAvatarHiddenOnMobile;
    protected boolean mHighlightLinks;
    protected int mFontSize;

    public TweetAdapter(Activity activity, Account account, List<Tweet> tweets) {
        mActivity = activity;
        mAccount = account;
        mTweets = tweets;
        readPrefs();
    }

    @Override
    public TweetHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId = mCompactTimeline ? R.layout.tweet_compact_item : R.layout.tweet_item;
        return new TweetHolder(LayoutInflater.from(mActivity).inflate(layoutId, parent, false));
    }

    @Override
    public int getItemCount() {
        return mTweets.size();
    }

    @Override
    public void onBindViewHolder(TweetHolder holder, int position) {
        final Tweet tweet = mTweets.get(position);
        bindFonts(holder, position);

        holder.usernameTextView.setText(TextUtils.concat("@", tweet.username()));
        holder.fullNameTextView.setText(tweet.fullname());
        holder.textView.setText(tweet.text());

        StringBuilder information = new StringBuilder();

        if (!TextUtils.isEmpty(tweet.retweetedBy()))
            information.append(tweet.retweetedBy()).append(" ");

        if (mAbsoluteTime)
            information.append(DateUtils.formatDateTime(mActivity, tweet.createdAt(),
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
        else
            information.append(DateUtils.getRelativeTimeSpanString(tweet.createdAt()));

        if (mShowClientName)
            information.append(" via ").append(tweet.source());

        holder.infoTextView.setText(information.toString());
        holder.retweetImageView.setVisibility(TextUtils.isEmpty(tweet.retweetedBy()) ? View.GONE : View.VISIBLE);

        if (mHighlightLinks) {
            LinkUtils.activate(mActivity, holder.textView);
            holder.textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Analytics.event(Analytics.TWEET_DETAILS);
                    TweetDetailsActivity.start(mActivity, mAccount, tweet);
                }
            });
        }

        if (!TextUtils.isEmpty(tweet.media()) && !mIsMediaHiddenOnMobile && mShowMediaPreview) {
            final String[] media = tweet.media().split("\\+\\+\\+\\+\\+");

            holder.playImageView.setVisibility(TextUtils.isEmpty(tweet.video()) ? View.GONE : View.VISIBLE);
            holder.mediaCountTextView.setVisibility(media.length > 1 ? View.VISIBLE : View.GONE);
            holder.mediaCountTextView.setText("" + media.length);
            holder.mediaLayout.setVisibility(View.VISIBLE);

            Picasso.with(mActivity)
                    .load(media[0])
                    .noFade()
                    .into(holder.mediaImageView);

            holder.mediaImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (TextUtils.isEmpty(tweet.video()))
                        ImagesActivity.start(mActivity, media);
                    else
                        VideoActivity.start(mActivity, tweet.video());
                }
            });
        } else {
            holder.mediaCountTextView.setVisibility(View.GONE);
            holder.mediaLayout.setVisibility(View.GONE);
        }

        if (!mIsAvatarHiddenOnMobile) {
            holder.avatarImageView.setVisibility(View.VISIBLE);
            Picasso.with(mActivity)
                    .load(tweet.avatar())
                    .noFade()
                    .into(holder.avatarImageView);
        } else {
            holder.avatarImageView.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(tweet.quotedText())) {
            holder.quotedScreenNameTextView.setText(TextUtils.concat("@", tweet.quotedScreenName()));
            holder.quotedNameTextView.setText(tweet.quotedName());
            holder.quotedTextView.setText(tweet.quotedText());

            if (mHighlightLinks) {
                LinkUtils.activate(mActivity, holder.quotedTextView);
                holder.quotedTextView.setOnClickListener(new QuoteClickListener(tweet));
            }

            if (TextUtils.isEmpty(tweet.quotedMedia())) {
                holder.quotedImageView.setVisibility(View.GONE);
            } else {
                final String[] media = tweet.quotedMedia().split("\\+\\+\\+\\+\\+");
                Picasso.with(mActivity)
                        .load(media[0])
                        .noFade()
                        .into(holder.quotedImageView);
                holder.quotedImageView.setVisibility(View.VISIBLE);
            }

            holder.quotedCardView.setVisibility(View.VISIBLE);
            holder.quotedCardView.setOnClickListener(new QuoteClickListener(tweet));
        } else {
            holder.quotedCardView.setVisibility(View.GONE);
        }

        holder.avatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Analytics.event(Analytics.USER_PROFILE);
                UserProfileActivity.start(mActivity, mAccount, tweet.username());
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Analytics.event(Analytics.TWEET_DETAILS);
                TweetDetailsActivity.start(mActivity, mAccount, tweet);
            }
        });
    }

    protected void bindFonts(TweetHolder holder, int position) {
        holder.usernameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSize);
        holder.fullNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSize);
        holder.textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSize);
    }

    protected void readPrefs() {
        mFontSize = mPrefsModel.fontSize();
        mAbsoluteTime = mPrefsModel.showAbsoluteTime();
        mCompactTimeline = mPrefsModel.compactTimeline();
        mHighlightLinks = mPrefsModel.highlightTimelineLinks();
        mShowMediaPreview = mPrefsModel.isMediaPreviewEnabled();
        mShowClientName = mPrefsModel.showClientNameInTimeline();
        mIsAvatarHiddenOnMobile = mPrefsModel.hideAvatarOnMobileConnection() && NetworkUtils.isMobile(mActivity);
        mIsMediaHiddenOnMobile = mPrefsModel.hideMediaOnMobileConnection() && NetworkUtils.isMobile(mActivity);
    }

    private final class QuoteClickListener implements View.OnClickListener {
        private final Tweet tweet;

        private QuoteClickListener(Tweet tweet) {
            this.tweet = tweet;
        }

        @Override
        public void onClick(View v) {
            final String url = String.format("https://twitter.com/%s/status/%d",
                    tweet.quotedScreenName(), tweet.quotedId());
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            ActivityCompat.startActivity(mActivity, intent, null);
        }
    }

    static class TweetHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.avatar)
        ImageView avatarImageView;

        @Bind(R.id.screen_name)
        TextView usernameTextView;

        @Bind(R.id.full_name)
        TextView fullNameTextView;

        @Bind(R.id.media_layout)
        FrameLayout mediaLayout;

        @Bind(R.id.media)
        ImageView mediaImageView;

        @Bind(R.id.play_button)
        ImageView playImageView;

        @Bind(R.id.count)
        TextView mediaCountTextView;

        @Bind(R.id.retweet)
        ImageView retweetImageView;

        @Bind(R.id.info)
        TextView infoTextView;

        @Bind(R.id.text)
        TextView textView;

        @Bind(R.id.quoted)
        CardView quotedCardView;

        @Bind(R.id.quoted_media)
        ImageView quotedImageView;

        @Bind(R.id.quoted_text)
        TextView quotedTextView;

        @Bind(R.id.quoted_name)
        TextView quotedNameTextView;

        @Bind(R.id.quoted_screen_name)
        TextView quotedScreenNameTextView;

        public TweetHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
