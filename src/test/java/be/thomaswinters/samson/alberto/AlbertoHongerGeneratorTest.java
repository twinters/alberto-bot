package be.thomaswinters.samson.alberto;

import be.thomaswinters.textgeneration.domain.context.TextGeneratorContext;
import be.thomaswinters.textgeneration.domain.generators.named.NamedGeneratorRegister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlbertoHongerGeneratorTest {

    private AlbertoHongerGenerator albertoHongerGenerator;

    @BeforeEach
    public void setup() throws IOException {
        this.albertoHongerGenerator = new AlbertoHongerGenerator();
    }

    @Test
    public void test_declaration() {
        NamedGeneratorRegister register = new NamedGeneratorRegister();
        register.createGenerator("voedsel", "pizza");
        for (int i = 0; i < 100; i++) {
            String result = this.albertoHongerGenerator.getTemplatedGenerator().generate(
                    new TextGeneratorContext(register, true));
            System.out.println(result);
        }
    }

    @Test
    public void test_simple_search() {
        Optional<String> food = this.albertoHongerGenerator
                .getRelatedFood("lasagne");
        assertEquals(food, Optional.of("lasagne"));
    }

    @Test
    public void test_searching() {
        Optional<String> food = this.albertoHongerGenerator
                .getRelatedFood("Hallo dit is een test waarin ik het woord pizza zeg.");
        assertEquals(food, Optional.of("pizza"));
    }

}