package io.github.mthli.Tweetin.Task.Timeline;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import io.github.mthli.Tweetin.Database.Timeline.TimelineAction;
import io.github.mthli.Tweetin.Fragment.Timeline.TimelineFragment;
import io.github.mthli.Tweetin.R;
import io.github.mthli.Tweetin.Unit.Flag.Flag;
import io.github.mthli.Tweetin.Unit.Tweet.Tweet;
import io.github.mthli.Tweetin.Unit.Tweet.TweetAdapter;
import twitter4j.Twitter;

import java.util.List;

public class TimelineRetweetTask extends AsyncTask<Void, Integer, Boolean> {
    private TimelineFragment timelineFragment;
    private Context context;
    private Twitter twitter;

    private TweetAdapter tweetAdapter;
    private List<Tweet> tweetList;
    private Tweet oldTweet;
    private Tweet newTweet;
    private int position;

    private NotificationManager notificationManager;
    private Notification.Builder builder;

    public TimelineRetweetTask(
            TimelineFragment timelineFragment,
            int position
    ) {
        this.timelineFragment = timelineFragment;
        this.position = position;
    }

    @Override
    protected void onPreExecute() {
        context = timelineFragment.getContentView().getContext();
        twitter = timelineFragment.getTwitter();

        tweetAdapter = timelineFragment.getTweetAdapter();
        tweetList = timelineFragment.getTweetList();
        oldTweet = tweetList.get(position);

        notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_tweet_notification);
        builder.setTicker(
                context.getString(R.string.tweet_notification_rewteet_ing)
        );
        builder.setContentTitle(
                context.getString(R.string.tweet_notification_rewteet_ing)
        );
        builder.setContentText(oldTweet.getText());
        Notification notification = builder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(Flag.POST_NOTIFICATION_ID, notification);
    }

    private twitter4j.Status status;
    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            status = twitter.retweetStatus(oldTweet.getOriginalStatusId());

            newTweet = new Tweet();
            newTweet.setOriginalStatusId(oldTweet.getOriginalStatusId());
            newTweet.setAfterRetweetStatusId(status.getId()); //
            newTweet.setAfterFavoriteStatusId(oldTweet.getAfterFavoriteStatusId());
            newTweet.setReplyToStatusId(oldTweet.getReplyToStatusId());
            newTweet.setUserId(oldTweet.getUserId());
            newTweet.setRetweetedByUserId(timelineFragment.getUseId()); //
            newTweet.setAvatarURL(oldTweet.getAvatarURL());
            newTweet.setCreatedAt(oldTweet.getCreatedAt());
            newTweet.setName(oldTweet.getName());
            newTweet.setScreenName(oldTweet.getScreenName());
            newTweet.setProtect(oldTweet.isProtect());
            newTweet.setCheckIn(oldTweet.getCheckIn());
            newTweet.setText(oldTweet.getText());
            newTweet.setRetweet(true); //
            newTweet.setRetweetedByUserName(
                    context.getString(R.string.tweet_info_retweeted_by_me) //
            );
            newTweet.setFavorite(oldTweet.isFavorite());

            TimelineAction action = new TimelineAction(context);
            action.openDatabase(true);
            action.updatedByRetweet(newTweet);
            action.closeDatabase();

            /* Do something */
        } catch (Exception e) {
            /* Do something */
            return false;
        }

        if (isCancelled()) {
            return false;
        }
        return true;
    }

    @Override
    protected void onCancelled() {
        /* Do nothing */
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        /* Do nothing */
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            /* Do something */
        }
    }
}
