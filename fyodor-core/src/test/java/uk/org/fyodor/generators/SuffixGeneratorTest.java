package uk.org.fyodor.generators;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.junit.Test;
import uk.org.fyodor.BaseTestWithRule;

import static org.assertj.core.api.Assertions.assertThat;

public class SuffixGeneratorTest extends BaseTestWithRule {

    SuffixGenerator generator = new SuffixGenerator();
    Multiset<String> suffixes = HashMultiset.create();

    @Test
    public void generateSuffixes() {
        for (int i = 0; i < 1000000; i++) {
            suffixes.add(generator.next());
        }
        assertThat(suffixes.elementSet()).hasSize(generator.getSuffixes().length);
    }
}
