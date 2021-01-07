package be.thomaswinters.samson.alberto;

import be.thomaswinters.twitter.bot.BehaviourCreator;
import be.thomaswinters.twitter.bot.TwitterBot;
import be.thomaswinters.twitter.tweetsfetcher.TimelineTweetsFetcher;
import be.thomaswinters.twitter.tweetsfetcher.filter.AlreadyParticipatedFilter;
import be.thomaswinters.twitter.tweetsfetcher.filter.NotFollowingCurrentUserFilter;
import be.thomaswinters.twitter.util.TwitterLogin;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import java.io.IOException;

import static be.thomaswinters.twitter.exception.TwitterUnchecker.uncheck;

public class AlbertoTwitterBot {

    public static void main(String[] args) throws TwitterException, IOException {
        build().createExecutor().run(args);
    }

    public static TwitterBot build() throws IOException, TwitterException {
        Twitter twitter = TwitterLogin.getTwitterFromEnvironment();

        NotFollowingCurrentUserFilter followingChecker =
                new NotFollowingCurrentUserFilter(twitter, true);

        return new TwitterBot(twitter,
                BehaviourCreator.automaticFollower(twitter),
                BehaviourCreator.fromMessageReactor(new AlbertoHongerGenerator(), 2),
                TwitterBot.MENTIONS_RETRIEVER
                        .apply(twitter)
                        .combineWith(
                                new TimelineTweetsFetcher(twitter)
                                        .filterOutOwnTweets(twitter)
                                        .filter(followingChecker)
                                        .filterRandomly(twitter, 4, 5)
                        )
//                        .combineWith(
//                                Arrays.asList(
//                                        new SearchTweetsFetcher(twitter, "albert vermeersch"),
//                                        new SearchTweetsFetcher(twitter, Arrays.asList("samson", "koekjes")),
//                                        new SearchTweetsFetcher(twitter, Arrays.asList("samson", "gert", "albert"))
//                                )
//                        )
                        .filterOutRetweets()
                        .distinct()
                        .filter(uncheck(AlreadyParticipatedFilter::new, twitter, 3))
                        .filterOutRetweets());
    }
}
