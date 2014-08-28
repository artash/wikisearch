package wikisearch;

/**
 * Common constants go here to avoid duplication between the searcher app and indexer app.
 */
public final class Constants {

    private Constants() {}

    public static final String BODY_FIELD = "body";
    public static final String TITLE_FIELD = "doctitle";
    public static final String ID_FIELD = "docid";
    public static final String USERNAME_FIELD = "docusername";

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final String DEFAULT_DIR = "./enwiki";

}
