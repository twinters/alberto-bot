package be.thomaswinters.samson.alberto;

import be.thomaswinters.chatbot.IChatBot;
import be.thomaswinters.chatbot.data.IChatMessage;
import be.thomaswinters.scrapers.smulweb.SmulwebScraper;
import be.thomaswinters.scrapers.smulweb.data.SmulwebRecipeCard;
import be.thomaswinters.sentence.SentenceUtil;
import be.thomaswinters.textgeneration.domain.context.TextGeneratorContext;
import be.thomaswinters.textgeneration.domain.generators.ITextGenerator;
import be.thomaswinters.textgeneration.domain.generators.named.NamedGeneratorRegister;
import be.thomaswinters.twitter.bot.TwitterBotExecutor;
import twitter4j.TwitterException;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AlbertoBot implements IChatBot {

    private final SmulwebScraper smulwebScraper = new SmulwebScraper();
    private final ITextGenerator templatedGenerator;

    public AlbertoBot(ITextGenerator generator) {
        this.templatedGenerator = generator;
    }

    private Optional<String> getRelatedFood(String message) {
        String lowerCaseMessage = message.toLowerCase();
        return SentenceUtil.getWordsStream(message)
                .map(this::getRecipes)
                .flatMap(Collection::stream)
                .map(SmulwebRecipeCard::getTitle)
                .map(String::toLowerCase)
                .map(String::trim)
                .filter(title -> title.length() > 0)
                .filter(lowerCaseMessage::contains)
                .max(Comparator.comparingInt(String::length));
    }

    private List<SmulwebRecipeCard> getRecipes(String searchWord) {
        try {
            return smulwebScraper.search(searchWord);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<String> generateReply(IChatMessage message) {
        System.out.println("Replying to: " + message.getMessage());
        if (SentenceUtil.getWordsStream(message.getMessage()).map(String::toLowerCase).anyMatch(e -> e.equals("albert"))) {
            return Optional.of("Ten eerste is het AL-BER-TOOOOOOO. En ten tweede: Ik heb honger!");
        }


        Optional<String> foundRecipe = getRelatedFood(message.getMessage());
        if (foundRecipe.isPresent()) {
            NamedGeneratorRegister register = new NamedGeneratorRegister();
            register.createGenerator("voedsel", foundRecipe.get());
            return Optional.of(templatedGenerator.generate(new TextGeneratorContext(register, true)));
        }
        return Optional.empty();
    }

    public static void main(String[] args) throws TwitterException, IOException {
        new TwitterBotExecutor(new AlbertoBotBuilder().build()).run(args);
    }
}
