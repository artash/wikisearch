package wikisearch;

/**
 * This file is ripped from org.apache.lucene.benchmark.byTask.feeds package and amended to add latest revision
 * contributor to the lucene document + removed some non-relevant fields and restructured the code.
 * [artash]
 */

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.document.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Creates {@link org.apache.lucene.document.Document} objects. Uses a {@link org.apache.lucene.benchmark.byTask.feeds.ContentSource} to generate
 * {@link org.apache.lucene.benchmark.byTask.feeds.DocData} objects. Supports the following parameters:
 * <ul>
 * <li><b>content.source</b> - specifies the {@link org.apache.lucene.benchmark.byTask.feeds.ContentSource} class to use
 * (default <b>SingleDocSource</b>).
 * <li><b>doc.stored</b> - specifies whether fields should be stored (default
 * <b>false</b>).
 * <li><b>doc.body.stored</b> - specifies whether the body field should be stored (default
 * = <b>doc.stored</b>).
 * {@link org.apache.lucene.benchmark.byTask.feeds.DocData#getProps()} will be indexed. (default <b>false</b>).
 * </ul>
 */
public class DocMaker implements Closeable {

    protected Config config;
    protected FieldType valType;
    protected FieldType bodyValType;
    protected ContentSource source;
    protected boolean reuseFields;
    private ThreadLocal<DocState> docState = new ThreadLocal<>();

    public DocMaker() {
    }

    // create a doc
    private Document createDocument(DocData docData) throws UnsupportedEncodingException {

        final DocState ds = getDocState();
        final Document doc = reuseFields ? ds.doc : new Document();
        doc.getFields().clear();

        // Set ID_FIELD
        Field idField = ds.getField(Constants.ID_FIELD, valType);
        int id = docData.getID();
        idField.setStringValue(Integer.toString(id));
        doc.add(idField);

        // Set USERNAME_FIELD
        String username = docData.getUsername();
        if (username == null) username = "";
        Field usernameField = ds.getField(Constants.USERNAME_FIELD, valType);
        usernameField.setStringValue(username);
        doc.add(usernameField);

        // Set TITLE_FIELD
        String title = docData.getTitle();
        Field titleField = ds.getField(Constants.TITLE_FIELD, valType);
        titleField.setStringValue(title == null ? "" : title);
        doc.add(titleField);

        String body = docData.getBody();
        if (body != null && body.length() > 0) {
            docData.setBody(""); // nothing left
            Field bodyField = ds.getField(Constants.BODY_FIELD, bodyValType);
            bodyField.setStringValue(body);
            doc.add(bodyField);
        }
        //System.out.println("============== Created doc "+numDocsCreated+" :\n"+doc+"\n==========");
        return doc;
    }

    protected DocState getDocState() {
        DocState ds = docState.get();
        if (ds == null) {
            ds = new DocState(reuseFields, valType, bodyValType);
            docState.set(ds);
        }
        return ds;
    }

    /**
     * Closes the {@link DocMaker}. The base implementation closes the
     * {@link ContentSource}, and it can be overridden to do more work (but make
     * sure to call super.close()).
     */
    @Override
    public void close() throws IOException {
        source.close();
    }

    /**
     * Creates a {@link Document} object ready for indexing. This method uses the
     * {@link ContentSource} to get the next document from the source, and creates
     * a {@link Document} object from the returned fields. If
     * <code>reuseFields</code> was set to true, it will reuse {@link Document}
     * and {@link Field} instances.
     */
    public Document makeDocument() throws Exception {
        DocData docData = source.getNextDocData(getDocState().docData);
        Document doc = createDocument(docData);
        return doc;
    }

    /**
     * Reset inputs so that the test run would behave, input wise, as if it just started.
     */
    public synchronized void resetInputs() throws IOException {
        source.printStatistics("docs");
        // re-initiate since properties by round may have changed.
        setConfig(config, source);
        source.resetInputs();
    }

    /**
     * Set the configuration parameters of this doc maker.
     */
    public void setConfig(Config config, ContentSource source) {
        this.config = config;
        this.source = source;

        boolean stored = config.get("doc.stored", true);
        boolean indexed = config.get("doc.indexed", true);
        boolean bodyStored = config.get("doc.body.stored", false);
        boolean bodyIndexed = config.get("doc.body.indexed", true);

        valType = new FieldType(TextField.TYPE_NOT_STORED);
        valType.setStored(stored);
        valType.setIndexed(indexed);
        valType.freeze();

        bodyValType = new FieldType(TextField.TYPE_NOT_STORED);
        bodyValType.setStored(bodyStored);
        bodyValType.setIndexed(bodyIndexed);
        bodyValType.freeze();

        reuseFields = config.get("doc.reuse.fields", true);

        // In a multi-rounds run, it is important to reset DocState since settings
        // of fields may change between rounds, and this is the only way to reset
        // the cache of all threads.
        docState = new ThreadLocal<>();
    }

    /**
     * Document state, supports reuse of field instances
     * across documents (see <code>reuseFields</code> parameter).
     */
    protected static class DocState {

        final Document doc;
        private final Map<String, Field> fields;
        private final boolean reuseFields;
        DocData docData = new DocData();

        public DocState(boolean reuseFields, FieldType ft, FieldType bodyFt) {

            this.reuseFields = reuseFields;

            if (reuseFields) {
                fields = new HashMap<>();

                // Initialize the map with the default fields.
                fields.put(Constants.BODY_FIELD, new Field(Constants.BODY_FIELD, "", bodyFt));
                fields.put(Constants.TITLE_FIELD, new StringField(Constants.TITLE_FIELD, "", Field.Store.YES));
                fields.put(Constants.ID_FIELD, new StringField(Constants.ID_FIELD, "", Field.Store.YES));
                fields.put(Constants.USERNAME_FIELD, new Field(Constants.USERNAME_FIELD, "", ft));

                doc = new Document();
            } else {
                fields = null;
                doc = null;
            }
        }

        /**
         * Returns a field corresponding to the field name. If
         * <code>reuseFields</code> was set to true, then it attempts to reuse a
         * Field instance. If such a field does not exist, it creates a new one.
         */
        Field getField(String name, FieldType ft) {
            if (!reuseFields) {
                return new Field(name, "", ft);
            }

            Field f = fields.get(name);
            if (f == null) {
                f = new Field(name, "", ft);
                fields.put(name, f);
            }
            return f;
        }
    }

}
