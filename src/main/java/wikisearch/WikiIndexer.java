package wikisearch;

import org.apache.commons.cli.*;
import org.apache.lucene.benchmark.byTask.feeds.NoMoreDataException;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.*;
import java.util.Properties;

public final class WikiIndexer {

    public static void main(String[] args) throws Exception {
        CommandLine line = processCommandLine(args);

        String directory = line.getOptionValue("directory", Constants.DEFAULT_DIR);
        String inputFile = line.getOptionValue("input");

        File wikipedia = new File(inputFile);
        File outputDir = new File(directory);

        Properties properties = new Properties();
        properties.setProperty("docs.file", wikipedia.getAbsolutePath());
        Config config = new Config(properties);

        ContentSource source = new EnwikiContentSource();
        source.setConfig(config);

        DocMaker docMaker = new DocMaker();
        docMaker.setConfig(config, source);
        docMaker.resetInputs();
        if (wikipedia.exists()) {
            System.out.println("Extracting Wikipedia to: " + outputDir + " using EnwikiContentSource");
            outputDir.mkdirs();
            System.out.println("Deleting all files in " + outputDir);
            File[] files = outputDir.listFiles();
            for (int i = 0; i < files.length; i++) {
                files[i].delete();
            }
            index(outputDir, docMaker);
        } else {
            System.out.println("Input file not found: " + inputFile);
            System.exit(-1);
        }
    }

    public static void index(File outputDir, DocMaker docMaker) throws Exception {
        Document doc;
        System.out.println("Starting indexing");
        long start = System.currentTimeMillis();

        Directory dir = FSDirectory.open(outputDir);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_48, new WikipediaAnalyzer(Version.LUCENE_48));
        IndexWriter writer = new IndexWriter(dir, config);

        try {
            int count = 0;
            while ((doc = docMaker.makeDocument()) != null) {
                if (++count % 1000 == 0) {
                    System.out.println(count + " " + doc.get("doctitle") + "| contributor " + doc.get("docusername"));
                    writer.commit();
                }
                try {
                    writer.addDocument(doc);
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                }
            }
        } catch (NoMoreDataException e) {
            //continue
        }

        writer.close();
        long finish = System.currentTimeMillis();
        System.out.println("Indexing took " + (finish - start) + " ms");
    }

    private static CommandLine processCommandLine(String[] args) {
        CommandLineParser parser = new GnuParser();
        Options options = new Options();

        options.addOption(OptionBuilder
                .withLongOpt("directory")
                .withDescription("specifies the output directory for index files.")
                .hasArg()
                .withArgName("DIR")
                .create("d"));

        options.addOption(OptionBuilder
                .withLongOpt("input")
                .withDescription("wikipedia dump xml file location.")
                .hasArg()
                .withArgName("INPUT_FILE")
                .create("i"));

        CommandLine line = null;
        try {
            line = parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("WikiIndexer", options);
            System.exit(-1);
        }
        return line;
    }


}
