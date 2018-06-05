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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import org.jsoup.HttpStatusException;
import twitter4j.TwitterException;

import java.io.IOException;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AlbertoBot implements IChatBot {

    private static final Collection<String> BLACK_LIST = new HashSet<>(
            Arrays.asList("red", "goedemorgen", "groen", "p", "knack", "mie",
                    "duitsland", "rusland", "student", "test",
                    "speciaal", "toets", "verwijderen", "kat", "papegaai", "pearl", "it", "but", "fire", "fantasy",
                    "love", "i love it", "alexander", "baby","blue eyes", "france", "niks"));

    private final SmulwebScraper smulwebScraper = new SmulwebScraper();
    private final ITextGenerator templatedGenerator;

    public AlbertoBot(ITextGenerator generator) {
        this.templatedGenerator = generator;
    }

    public static void main(String[] args) throws TwitterException, IOException {
        new TwitterBotExecutor(new AlbertoBotBuilder().build()).run(args);
    }

    private Optional<String> getRelatedFood(String message) {
        String lowerCaseMessage = message.toLowerCase();

        ImmutableMultiset.Builder<String> b = ImmutableMultiset.builder();
        SentenceUtil.getWordsStream(message)
                .map(this::getRecipes)
                .flatMap(Collection::stream)
                .map(SmulwebRecipeCard::getTitle)
                .map(String::toLowerCase)
                .map(String::trim)
                .filter(title -> title.length() > 1)
                .filter(title -> !BLACK_LIST.contains(title))
                .filter(lowerCaseMessage::contains)
                .sorted(Comparator.comparingInt(String::length))
                .forEach(b::add);
        ImmutableMultiset<String> set = b.build();

        return set.entrySet()
                .stream()
                .filter(e -> e.getCount() >= 3)
                .map(Multiset.Entry::getElement)
                .max(Comparator.comparingInt(String::length));
    }

    private List<SmulwebRecipeCard> getRecipes(String searchWord) {
        try {
            return smulwebScraper.search(searchWord);
        } catch (HttpStatusException httpStatusE) {
            if (httpStatusE.getStatusCode() == 503) {
                try {
                    System.out.println("Smulweb down: sleeping for a minute");
                    TimeUnit.MINUTES.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    return smulwebScraper.search(searchWord);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException(httpStatusE);
            }
        } catch (SocketException e) {
            System.out.println("ERROR: " + e.getMessage() +"\nJust retrying connection...");
            System.out.println("Sleeping for a minute");
            try {
                TimeUnit.MINUTES.sleep(1);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            try {
                return smulwebScraper.search(searchWord);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<String> generateReply(IChatMessage message) {
        System.out.println("Checking message: " + message);
        if (SentenceUtil.getWordsStream(message.getText()).map(String::toLowerCase).anyMatch(e -> e.equals("albert"))) {
            return Optional.of("Ten eerste is het AL-BER-TOOOOOOO. En ten tweede: ik heb honger!");
        }


        Optional<String> foundRecipe = getRelatedFood(message.getText());
        if (foundRecipe.isPresent()) {
            NamedGeneratorRegister register = new NamedGeneratorRegister();
            register.createGenerator("voedsel", foundRecipe.get());
            return Optional.of(templatedGenerator.generate(new TextGeneratorContext(register, true)));
        }
        return Optional.empty();
    }
}
