package wikisearch;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.benchmark.byTask.feeds.ContentSource;
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

public class WikiIndexer {
    private File outputDir;

    static public int count = 0;

    static final int BASE = 10;
    protected DocMaker docMaker;

    public WikiIndexer(DocMaker docMaker, File outputDir) {
        this.outputDir = outputDir;
        this.docMaker = docMaker;
        System.out.println("Deleting all files in " + outputDir);
        File[] files = outputDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            files[i].delete();
        }
    }

    public void index() throws Exception {
        Document doc;
        System.out.println("Starting indexing");
        long start = System.currentTimeMillis();

        Directory dir = FSDirectory.open(outputDir);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_48, new StandardAnalyzer(Version.LUCENE_48));
        IndexWriter writer = new IndexWriter(dir, config);

        try {
            int count = 0;
            while ((doc = docMaker.makeDocument()) != null) {
                if (++count % 1000 == 0) {
                    System.out.println(count + " " + doc.get("doctitle"));
                }
                writer.addDocument(doc);
            }
        } catch (NoMoreDataException e) {
            //continue
        }

        writer.close();
        long finish = System.currentTimeMillis();
        System.out.println("Extraction took " + (finish - start) + " ms");
    }

    public static void main(String[] args) throws Exception {

        File wikipedia = null;
        File outputDir = new File("./enwiki");
        boolean keepImageOnlyDocs = true;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--input") || arg.equals("-i")) {
                wikipedia = new File(args[i + 1]);
                i++;
            } else if (arg.equals("--output") || arg.equals("-o")) {
                outputDir = new File(args[i + 1]);
                i++;
            } else if (arg.equals("--discardImageOnlyDocs") || arg.equals("-d")) {
                keepImageOnlyDocs = false;
            }
        }

        Properties properties = new Properties();
        properties.setProperty("docs.file", wikipedia.getAbsolutePath());
        properties.setProperty("content.source.forever", "false");
        properties.setProperty("keep.image.only.docs", String.valueOf(keepImageOnlyDocs));
        Config config = new Config(properties);

        ContentSource source = new EnwikiContentSource();
        source.setConfig(config);

        DocMaker docMaker = new DocMaker();
        docMaker.setConfig(config, source);
        docMaker.resetInputs();
        if (wikipedia.exists()) {
            System.out.println("Extracting Wikipedia to: " + outputDir + " using EnwikiContentSource");
            outputDir.mkdirs();
            WikiIndexer indexer = new WikiIndexer(docMaker, outputDir);
            indexer.index();
        } else {
            printUsage();
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java -cp <...> wikisearch.WikiIndexer --input|-i <Path to Wikipedia XML file> " +
                "[--output|-o <Output Path>] [--discardImageOnlyDocs|-d]");
        System.err.println("--discardImageOnlyDocs tells the extractor to skip Wiki docs that contain only images");
    }

}
