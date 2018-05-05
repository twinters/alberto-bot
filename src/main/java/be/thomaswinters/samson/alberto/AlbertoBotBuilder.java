package be.thomaswinters.samson.alberto;

import be.thomaswinters.textgeneration.domain.generators.ITextGenerator;
import be.thomaswinters.textgeneration.domain.parsers.DeclarationsFileParser;
import be.thomaswinters.twitter.GeneratorTwitterBot;
import be.thomaswinters.twitter.bot.TwitterBot;
import be.thomaswinters.twitter.bot.util.TwitterLoginUtil;
import be.thomaswinters.twitter.util.retriever.TwitterHomeRetriever;
import twitter4j.Twitter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;

public class AlbertoBotBuilder {
    private final URL templateFile = ClassLoader.getSystemResource("templates/alberto-honger.decl");

    public TwitterBot build() throws IOException {
        ITextGenerator generator = DeclarationsFileParser.createTemplatedGenerator(templateFile, new ArrayList<>());


        Twitter twitter = TwitterLoginUtil.getTwitterFromEnvironment();

        return new GeneratorTwitterBot(twitter,
                Optional::empty,
                new AlbertoBot(generator),
                TwitterBot.MENTIONS_RETRIEVER, TwitterHomeRetriever::new);
    }
}
