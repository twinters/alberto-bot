package be.thomaswinters.samson.alberto;

import be.thomaswinters.textgeneration.domain.generators.ITextGenerator;
import be.thomaswinters.textgeneration.domain.parsers.DeclarationsFileParser;
import be.thomaswinters.twitter.bot.AutomaticFollower;
import be.thomaswinters.twitter.bot.GeneratorTwitterBot;
import be.thomaswinters.twitter.bot.TwitterBot;
import be.thomaswinters.twitter.tweetsfetcher.SearchTweetsFetcher;
import be.thomaswinters.twitter.tweetsfetcher.TimelineTweetsFetcher;
import be.thomaswinters.twitter.tweetsfetcher.filter.AlreadyParticipatedFilter;
import be.thomaswinters.twitter.tweetsfetcher.filter.NotFollowingCurrentUserFilter;
import be.thomaswinters.twitter.util.TwitterLogin;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import static be.thomaswinters.twitter.exception.TwitterUnchecker.uncheck;

public class AlbertoBotBuilder {
    private final URL templateFile = ClassLoader.getSystemResource("templates/alberto-honger.decl");

    public TwitterBot build() throws IOException, TwitterException {
        ITextGenerator generator = DeclarationsFileParser.createTemplatedGenerator(templateFile, new ArrayList<>());
        Twitter twitter = TwitterLogin.getTwitterFromEnvironment();

        NotFollowingCurrentUserFilter followingChecker = new NotFollowingCurrentUserFilter(twitter, true);

        return new GeneratorTwitterBot(twitter,
                new AutomaticFollower(twitter),
                new AlbertoBot(generator),
                twit ->
                        TwitterBot.MENTIONS_RETRIEVER
                                .apply(twit)
                                .combineWith(
                                        new TimelineTweetsFetcher(twit)
                                                .filterOutOwnTweets(twit)
                                                .filter(followingChecker)
                                )
                                .combineWith(
                                        new SearchTweetsFetcher(twit, "albert vermeersch")
                                )
                                .combineWith(
                                        new SearchTweetsFetcher(twit, Arrays.asList("samson","koekjes"))
                                )
                                .filter(uncheck(AlreadyParticipatedFilter::new, twitter))
                                .filterOutRetweets());
    }
}
