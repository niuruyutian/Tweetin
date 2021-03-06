package io.github.mthli.Tweetin.Tweet;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.widget.Toast;
import com.twitter.Extractor;
import io.github.mthli.Tweetin.Data.DataRecord;
import io.github.mthli.Tweetin.R;
import io.github.mthli.Tweetin.Twitter.TwitterUnit;
import twitter4j.*;

import java.util.List;

public class TweetUnit {
    private Activity activity;

    private String useScreenName;
    private String me;

    public TweetUnit(Activity activity) {
        this.activity = activity;

        this.useScreenName = TwitterUnit.getUseScreenNameFromSharedPreferences(activity);
        this.me = activity.getString(R.string.tweet_info_retweeted_by_me);
    }

    public String getPictureURLFromStatus(Status status) {
        String[] suffixes = activity.getResources().getStringArray(R.array.picture_suffixes);
        URLEntity[] urlEntities;
        MediaEntity[] mediaEntities;

        if (status.isRetweet()) {
            urlEntities = status.getRetweetedStatus().getURLEntities();
            mediaEntities = status.getRetweetedStatus().getMediaEntities();
        } else {
            urlEntities = status.getURLEntities();
            mediaEntities = status.getMediaEntities();
        }

        /* Support for *.png and *.jpg */
        for (MediaEntity mediaEntity : mediaEntities) {
            if (mediaEntity.getType().equals(activity.getString(R.string.picture_media_type))) {
                return mediaEntity.getMediaURL();
            }
        }
        for (URLEntity urlEntity : urlEntities) {
            String expandedURL = urlEntity.getExpandedURL();
            for (String suffix : suffixes) {
                if (expandedURL.endsWith(suffix)) {
                    return expandedURL;
                }
            }
        }

        /* Support for Instagram */
        for (URLEntity urlEntity : urlEntities) {
            String expandedURL = urlEntity.getExpandedURL();
            if (expandedURL.startsWith(activity.getString(R.string.picture_instagram_prefix))) {
                return expandedURL + activity.getString(R.string.picture_instagram_suffix);
            }
        }

        return null;
    }

    public String getDetailTextFromStatus(Status status) {
        URLEntity[] urlEntities;
        MediaEntity[] mediaEntities;

        String text;

        if (status.isRetweet()) {
            urlEntities = status.getRetweetedStatus().getURLEntities();
            mediaEntities = status.getRetweetedStatus().getMediaEntities();
            text = status.getRetweetedStatus().getText();
        } else {
            urlEntities = status.getURLEntities();
            mediaEntities = status.getMediaEntities();
            text = status.getText();
        }

        for (URLEntity urlEntity : urlEntities) {
            text = text.replace(urlEntity.getURL(), urlEntity.getExpandedURL());
        }

        for (MediaEntity mediaEntity : mediaEntities) {
            text = text.replace(mediaEntity.getURL(), mediaEntity.getMediaURL());
        }

        return text;
    }

    public String getDescriptionFromUser(User user) {
        URLEntity[] urlEntities = user.getDescriptionURLEntities();
        String description = user.getDescription();

        for (URLEntity urlEntity : urlEntities) {
            description = description.replace(urlEntity.getURL(), urlEntity.getExpandedURL());
        }

        return description;
    }

    public SpannableString getSpanFromText(String text) {
        Extractor extractor = new Extractor();
        List<String> urlList = extractor.extractURLs(text);
        List<String> userList = extractor.extractMentionedScreennames(text);
        List<String> tagList = extractor.extractHashtags(text);

        SpannableString span = new SpannableString(text);

        for (String url : urlList) {
            span.setSpan(
                    new TweetURLSpan(activity, url),
                    text.indexOf(url),
                    text.indexOf(url) + url.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        for (String user : userList) {
            span.setSpan(
                    new TweetUserSpan(activity, user),
                    text.indexOf(user),
                    text.indexOf(user) + user.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        for (String tag : tagList) {
            span.setSpan(
                    new TweetTagSpan(activity, tag),
                    text.indexOf(tag),
                    text.indexOf(tag) + tag.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        return span;
    }

    public SpannableString getSpanFromTweet(Tweet tweet) {
        return getSpanFromText(tweet.getText());
    }

    public Tweet getTweetFromStatus(Status status) {
        Tweet tweet = new Tweet();

        if (status.isRetweet()) {
            tweet.setAvatarURL(status.getRetweetedStatus().getUser().getOriginalProfileImageURL());
            tweet.setName(status.getRetweetedStatus().getUser().getName());
            tweet.setScreenName(status.getRetweetedStatus().getUser().getScreenName());
            tweet.setCreatedAt(status.getRetweetedStatus().getCreatedAt().getTime());
            Place place = status.getRetweetedStatus().getPlace();
            if (place != null) {
                tweet.setCheckIn(place.getFullName());
            } else {
                tweet.setCheckIn(null);
            }
            tweet.setProtect(status.getRetweetedStatus().getUser().isProtected());
            tweet.setPictureURL(getPictureURLFromStatus(status.getRetweetedStatus()));
            tweet.setText(getDetailTextFromStatus(status.getRetweetedStatus()));
            tweet.setRetweetedByName(status.getUser().getName());
            tweet.setFavorite(status.getRetweetedStatus().isFavorited());

            tweet.setStatusId(status.getRetweetedStatus().getId());
            tweet.setInReplyToStatusId(status.getRetweetedStatus().getInReplyToStatusId());
            tweet.setRetweetedByScreenName(status.getUser().getScreenName());
        } else {
            tweet.setAvatarURL(status.getUser().getOriginalProfileImageURL());
            tweet.setName(status.getUser().getName());
            tweet.setScreenName(status.getUser().getScreenName());
            tweet.setCreatedAt(status.getCreatedAt().getTime());
            Place place = status.getPlace();
            if (place != null) {
                tweet.setCheckIn(place.getFullName());
            } else {
                tweet.setCheckIn(null);
            }
            tweet.setProtect(status.getUser().isProtected());
            tweet.setPictureURL(getPictureURLFromStatus(status));
            tweet.setText(getDetailTextFromStatus(status));
            tweet.setRetweetedByName(null);
            tweet.setFavorite(status.isFavorited());

            tweet.setStatusId(status.getId());
            tweet.setInReplyToStatusId(status.getInReplyToStatusId());
            tweet.setRetweetedByScreenName(null);
        }

        if (status.isRetweetedByMe() || status.isRetweeted()) {
            tweet.setRetweetedByName(me);
            tweet.setRetweetedByScreenName(useScreenName);
        }

        tweet.setDetail(false);

        return tweet;
    }

    public Tweet getTweetFromDataRecord(DataRecord record) {
        Tweet tweet = new Tweet();

        tweet.setAvatarURL(record.getAvatarURL());
        tweet.setName(record.getName());
        tweet.setScreenName(record.getScreenName());
        tweet.setCreatedAt(record.getCreatedAt());
        tweet.setCheckIn(record.getCheckIn());
        tweet.setProtect(record.isProtect());
        tweet.setPictureURL(record.getPictureURL());
        tweet.setText(record.getText());
        tweet.setRetweetedByName(record.getRetweetedByName());
        tweet.setFavorite(record.isFavorite());

        tweet.setStatusId(record.getStatusId());
        tweet.setInReplyToStatusId(record.getInReplyToStatusId());
        tweet.setRetweetedByScreenName(record.getRetweetedByScreenName());

        tweet.setDetail(false);

        return tweet;
    }

    public DataRecord getDataRecordFromTweet(Tweet tweet) {
        DataRecord record = new DataRecord();

        record.setAvatarURL(tweet.getAvatarURL());
        record.setName(tweet.getName());
        record.setScreenName(tweet.getScreenName());
        record.setCreatedAt(tweet.getCreatedAt());
        record.setCheckIn(tweet.getCheckIn());
        record.setProtect(tweet.isProtect());
        record.setPictureURL(tweet.getPictureURL());
        record.setText(tweet.getText());
        record.setRetweetedByName(tweet.getRetweetedByName());
        record.setFavorite(tweet.isFavorite());

        record.setStatusId(tweet.getStatusId());
        record.setInReplyToStatusId(tweet.getInReplyToStatusId());
        record.setRetweetedByScreenName(tweet.getRetweetedByScreenName());

        return record;
    }

    public DataRecord getDataRecordFromStatus(Status status) {
        DataRecord record = new DataRecord();

        if (status.isRetweet()) {
            record.setAvatarURL(status.getRetweetedStatus().getUser().getOriginalProfileImageURL());
            record.setName(status.getRetweetedStatus().getUser().getName());
            record.setScreenName(status.getRetweetedStatus().getUser().getScreenName());
            record.setCreatedAt(status.getRetweetedStatus().getCreatedAt().getTime());
            Place place = status.getRetweetedStatus().getPlace();
            if (place != null) {
                record.setCheckIn(place.getFullName());
            } else {
                record.setCheckIn(null);
            }
            record.setProtect(status.getRetweetedStatus().getUser().isProtected());
            record.setPictureURL(getPictureURLFromStatus(status.getRetweetedStatus()));
            record.setText(getDetailTextFromStatus(status.getRetweetedStatus()));
            record.setRetweetedByName(status.getUser().getName());
            record.setFavorite(status.getRetweetedStatus().isFavorited());

            record.setStatusId(status.getRetweetedStatus().getId());
            record.setInReplyToStatusId(status.getRetweetedStatus().getInReplyToStatusId());
            record.setRetweetedByScreenName(status.getUser().getScreenName());
        } else {
            record.setAvatarURL(status.getUser().getOriginalProfileImageURL());
            record.setName(status.getUser().getName());
            record.setScreenName(status.getUser().getScreenName());
            record.setCreatedAt(status.getCreatedAt().getTime());
            Place place = status.getPlace();
            if (place != null) {
                record.setCheckIn(place.getFullName());
            } else {
                record.setCheckIn(null);
            }
            record.setProtect(status.getUser().isProtected());
            record.setPictureURL(getPictureURLFromStatus(status));
            record.setText(getDetailTextFromStatus(status));
            record.setRetweetedByName(null);
            record.setFavorite(status.isFavorited());

            record.setStatusId(status.getId());
            record.setInReplyToStatusId(status.getInReplyToStatusId());
            record.setRetweetedByScreenName(null);
        }

        if (status.isRetweetedByMe() || status.isRetweeted()) {
            record.setRetweetedByName(me);
            record.setRetweetedByScreenName(useScreenName);
        }

        return record;
    }

    public Intent getIntentFromTweet(Tweet tweet, Class<?> cls) {
        Intent intent = new Intent(activity, cls);
        intent.putExtra(activity.getString(R.string.tweet_intent_avatar_url), tweet.getAvatarURL());
        intent.putExtra(activity.getString(R.string.tweet_intent_name), tweet.getName());
        intent.putExtra(activity.getString(R.string.tweet_intent_screen_name), tweet.getScreenName());
        intent.putExtra(activity.getString(R.string.tweet_intent_created_at), tweet.getCreatedAt());
        intent.putExtra(activity.getString(R.string.tweet_intent_check_in), tweet.getCheckIn());
        intent.putExtra(activity.getString(R.string.tweet_intent_protect), tweet.isProtect());
        intent.putExtra(activity.getString(R.string.tweet_intent_picture_url), tweet.getPictureURL());
        intent.putExtra(activity.getString(R.string.tweet_intent_text), tweet.getText());
        intent.putExtra(activity.getString(R.string.tweet_intent_retweeted_by_name), tweet.getRetweetedByName());
        intent.putExtra(activity.getString(R.string.tweet_intent_favorite), tweet.isFavorite());
        intent.putExtra(activity.getString(R.string.tweet_intent_status_id), tweet.getStatusId());
        intent.putExtra(activity.getString(R.string.tweet_intent_in_reply_to_status_id), tweet.getInReplyToStatusId());
        intent.putExtra(activity.getString(R.string.tweet_intent_retweeted_by_screen_name), tweet.getRetweetedByScreenName());
        return intent;
    }

    public Tweet getTweetFromIntent(Intent intent) {
        Tweet tweet = new Tweet();
        tweet.setAvatarURL(intent.getStringExtra(activity.getString(R.string.tweet_intent_avatar_url)));
        tweet.setName(intent.getStringExtra(activity.getString(R.string.tweet_intent_name)));
        tweet.setScreenName(intent.getStringExtra(activity.getString(R.string.tweet_intent_screen_name)));
        tweet.setCreatedAt(intent.getLongExtra(activity.getString(R.string.tweet_intent_created_at), 0));
        tweet.setCheckIn(intent.getStringExtra(activity.getString(R.string.tweet_intent_check_in)));
        tweet.setProtect(intent.getBooleanExtra(activity.getString(R.string.tweet_intent_protect), false));
        tweet.setPictureURL(intent.getStringExtra(activity.getString(R.string.tweet_intent_picture_url)));
        tweet.setText(intent.getStringExtra(activity.getString(R.string.tweet_intent_text)));
        tweet.setRetweetedByName(intent.getStringExtra(activity.getString(R.string.tweet_intent_retweeted_by_name)));
        tweet.setFavorite(intent.getBooleanExtra(activity.getString(R.string.tweet_intent_favorite), false));
        tweet.setStatusId(intent.getLongExtra(activity.getString(R.string.tweet_intent_status_id), -1));
        tweet.setInReplyToStatusId(intent.getLongExtra(activity.getString(R.string.tweet_intent_in_reply_to_status_id), -1));
        tweet.setRetweetedByScreenName(intent.getStringExtra(activity.getString(R.string.tweet_intent_retweeted_by_screen_name)));
        return tweet;
    }

    public void share(Tweet tweet) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "@" + tweet.getScreenName() + ": " + tweet.getText());
        activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.tweet_share_label)));
    }

    public void copy(Tweet tweet) {
        ClipboardManager clipboardManager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(activity.getString(R.string.tweet_copy_label), tweet.getText());
        clipboardManager.setPrimaryClip(clipData);
        Toast.makeText(activity, R.string.tweet_toast_copy_successful, Toast.LENGTH_SHORT).show();
    }
}
