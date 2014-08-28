package wikisearch;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;
import org.apache.lucene.util.Version;

import java.io.Reader;

/**
 * A custom analyzer which works with WikipediaTokenizer.
 */
public class WikipediaAnalyzer extends Analyzer {

    protected Version matchVersion;

    public WikipediaAnalyzer(Version matchVersion) {
      this.matchVersion = matchVersion;
    }

    /**
     * Creates a new {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents} instance for this analyzer.
     *
     * @param fieldName the name of the fields content passed to the
     *                  {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents} sink as a reader
     * @param reader    the reader passed to the {@link org.apache.lucene.analysis.Tokenizer} constructor
     * @return the {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents} for this analyzer.
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        final Tokenizer src = new WikipediaTokenizer(reader);
        TokenStream tok = new StandardFilter(matchVersion, src);
        return new TokenStreamComponents(src, tok);
    }
}
