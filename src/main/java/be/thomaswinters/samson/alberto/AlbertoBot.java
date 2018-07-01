package be.thomaswinters.samson.alberto;

import be.thomaswinters.chatbot.IChatBot;
import be.thomaswinters.chatbot.data.IChatMessage;
import be.thomaswinters.random.Picker;
import be.thomaswinters.scrapers.smulweb.SmulwebScraper;
import be.thomaswinters.scrapers.smulweb.data.SmulwebRecipeCard;
import be.thomaswinters.sentence.SentenceUtil;
import be.thomaswinters.textgeneration.domain.context.TextGeneratorContext;
import be.thomaswinters.textgeneration.domain.generators.databases.DeclarationFileTextGenerator;
import be.thomaswinters.textgeneration.domain.generators.named.NamedGeneratorRegister;
import be.thomaswinters.textgeneration.domain.parsers.DeclarationsFileParser;
import be.thomaswinters.twitter.util.TwitterUtil;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import org.jsoup.HttpStatusException;

import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class AlbertoBot implements IChatBot {

    private static final Collection<String> BLACK_LIST = new HashSet<>(
            Arrays.asList("red", "goedemorgen", "groen", "p", "knack", "mie",
                    "duitsland", "rusland", "student", "test",
                    "speciaal", "toets", "verwijderen", "kat", "papegaai", "pearl", "it", "but", "fire", "fantasy",
                    "love", "i love it", "alexander", "baby", "blue eyes", "france", "niks", "geen idee", "idee", "lekker",
                    "ferrari", "leeg", "rood", "stoplicht", "baby face"));
    private static final URL templateFile = ClassLoader.getSystemResource("templates/alberto.decl");
    private final SmulwebScraper smulwebScraper = new SmulwebScraper();
    private final DeclarationFileTextGenerator templatedGenerator;


    public AlbertoBot() throws IOException {
        this.templatedGenerator =
                DeclarationsFileParser.createTemplatedGenerator(templateFile, new ArrayList<>());
    }


    private Optional<String> getRelatedFood(String message) {
        String lowerCaseMessage = message.toLowerCase()
                .replaceAll("meneer spaghetti", "")
                .replaceAll("mr spaghetti", "")
                .replaceAll("mr. spaghetti", "")
                .replaceAll("vermicelli", "");

        ImmutableMultiset.Builder<String> b = ImmutableMultiset.builder();
        SentenceUtil.getWordsStream(message)
                .filter(e -> !TwitterUtil.isTwitterWord(e))
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

    private Optional<String> getRandomFood() {
        try {
            return Picker.pickOptional(smulwebScraper.scrapeSomeTitles());
        } catch (IOException e) {
            return Optional.empty();
        }
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
            System.out.println("ERROR: " + e.getMessage() + "\nJust retrying connection...");
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
        System.out.println("GENERATING FOR " + message);
        // Find food in tweet
        Optional<String> foundRecipe = getRelatedFood(message.getText());
        NamedGeneratorRegister register = new NamedGeneratorRegister();
        foundRecipe.ifPresent(s -> register.createGenerator("voedsel", s));
        getRandomFood().ifPresent(f -> register.createGenerator("randomVoedsel", f));

        if (SentenceUtil.getWordsStream(message.getText()).map(String::toLowerCase).anyMatch(e -> e.equals("albert"))) {
            return Optional.of(templatedGenerator.generate("naamVerbetering", new TextGeneratorContext(register, true)));
        }

        if (foundRecipe.isPresent()) {
            return Optional.of(templatedGenerator.generate(new TextGeneratorContext(register, true)));
        }
        return Optional.empty();
    }

}
