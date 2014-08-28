package wikisearch;

import org.apache.commons.cli.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;


/**
 * The searcher command line tool.
 */
public final class WikiSearcher {

    public static void main(String[] args) {
        CommandLine line = processCommandLine(args);

        String directory = line.getOptionValue("directory", Constants.DEFAULT_DIR);
        String contributor = line.getOptionValue("contributor");
        String word = line.getOptionValue("word");
        if (contributor != null && word != null) {
            System.out.println("Searching allowed only by contributor or by article word, but not both.");
            return;
        }

        String token = contributor != null ? contributor : word;
        System.out.println("Searching for " + (contributor != null ? "contributor username " : "page word ") + token);

        Directory dir;
        try {
            dir = FSDirectory.open(new File(directory));
            IndexReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            Query q;
            if (contributor != null) {
                q = new PrefixQuery(new Term(Constants.USERNAME_FIELD, token));
            } else {
                q = new FuzzyQuery(new Term(Constants.BODY_FIELD, token));
            }
            TopDocs hits = searcher.search(q, Constants.DEFAULT_PAGE_SIZE);
            for (ScoreDoc scoreDoc : hits.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                System.out.println(doc.get(Constants.TITLE_FIELD));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    private static CommandLine processCommandLine(String[] args) {
        CommandLineParser parser = new GnuParser();
        Options options = new Options();

        options.addOption(OptionBuilder
                .withLongOpt("directory")
                .withDescription("index files directory.")
                .hasArg()
                .withArgName("DIR")
                .create("d"));

        options.addOption(OptionBuilder
                .withLongOpt("contributor")
                .withDescription("search by contributor user name of the latest article revision.")
                .hasArg()
                .withArgName("CONTRIBUTOR")
                .create("c"));

        options.addOption(OptionBuilder
                .withLongOpt("word")
                .withDescription("search by words in the wiki article bodies.")
                .hasArg()
                .withArgName("WORD")
                .create("w"));

        CommandLine line = null;
        try {
            line = parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("WikiSearcher", options);
            System.exit(-1);
        }
        return line;
    }
}
