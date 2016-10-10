package com.adaptiveweb.webapp;

import com.opencsv.CSVReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.CharBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.ClassicFilter;
import org.apache.lucene.analysis.standard.ClassicTokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import static org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer.URL;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;

public class lucenemaven {

    /* Public variables */
    public static String workingDir = System.getProperty("user.dir");// in some function
    public String resources_path = this.getClass().getClassLoader().getResource("data.csv").getPath().toString();
//    public String content_url = this.getClass().getClassLoader().getResource("content/ArrayList.txt").getPath().toString();
    
 /*
    Properties props = new Properties();
    props.load(lucenemaven.class.getResourceAsStream("project.properties"));
    String basedir = props.get("project.basedir");
    */
    
    public static String folder_path = "D:/academics/ASU/Spring '16/Adaptive web/Assignment 2/mavenproject1/webApp/src/main/resources/content";
    public static String csv_path = "D:/academics/ASU/Spring '16/Adaptive web/Assignment 2/mavenproject1/webApp/src/main/resources/data.csv";
    public String conte = this.getClass().getClassLoader().getResource("content").getFile();
    /* posts to save recommended post content from the crawled pages. */
    public static ArrayList<ArrayList<String>> posts = new ArrayList<ArrayList<String>>();
    /* post_title to save Post title of the 10 posts. */
    public static ArrayList<String> post_title = new ArrayList<String>();

    /**
     * Crawls content from wikibooks and writes the page's contents into files.
     *
     * @throws IOException
     */
    public static void crawlContent() throws IOException {
        String url = "https://en.wikibooks.org/wiki/Java_Programming";
        org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
        Elements links = doc.select("#bodyContent #mw-content-text ul li a[href]");
        System.out.println("\n Links:" + links.size());
        int i = 0;
        for (Element link : links) {
            if (link.attr("abs:href").contains("Java_Programming")) {
                System.out.println(" * a: " + link.attr("abs:href") + " ,text:" + trim(link.text(), 35));

                try {
                    i++;
                    org.jsoup.nodes.Document doc2 = Jsoup.connect(link.attr("abs:href")).get();
                    //Elements header = doc2.select("#content #firstHeading span");
                    Elements subheader = doc2.select("div#mw-content-text span.mw-headline");
                    Elements content = doc2.select("div#mw-content-text p,div#mw-content-text table[class!='wikitable']");
                    for(int con=0;con<content.size();con++){
                        System.err.println("Printing p-"+con);
                        //System.out.println(content.get(con).select("p"));
                        Elements ele = content.get(con).select("p");
                        if(!content.get(con).select("p").isEmpty()){
                            String contentStr = ""+content.get(con).select("p");
                            writeFile(folder_path + link.text() + "_"+con+ ".txt", contentStr);
                        }
                    }

                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }

    }

    public static String trim(String s, int width) {
        if (s.length() > width) {
            return s.substring(0, width - 1) + ".";
        } else {
            return s;
        }
    }

    /**
     * Use Lucene functions to remove unwanted symbols & characters from posts
     * to identify keywords for * searching and indexing wikipages. Also
     * includes stemming.
     *
     * @throws IOException
     */
    public static String processText(String input) throws IOException {
        String string = "";

        TokenStream tokenStream = null;
        try {

            // hack to keep dashed words (e.g. "non-specific" rather than "non" and "specific")
            input = input.replaceAll("-+", "-0");
            // replace any punctuation char but apostrophes and dashes by a space
            input = input.replaceAll("[\\p{Punct}&&[^'-]]+", " ");
            // replace most common english contractions
            input = input.replaceAll("(?:'(?:[tdsm]|[vr]e|ll))+\\b", "");

            // tokenize input
            tokenStream = new ClassicTokenizer(Version.LUCENE_40, new StringReader(input));
            // to lowercase
            tokenStream = new LowerCaseFilter(Version.LUCENE_40, tokenStream);
            // remove dots from acronyms (and "'s" but already done manually above)
            tokenStream = new ClassicFilter(tokenStream);
            // convert any char to ASCII
            tokenStream = new ASCIIFoldingFilter(tokenStream);
            // remove english stop words
            tokenStream = new StopFilter(Version.LUCENE_40, tokenStream, EnglishAnalyzer.getDefaultStopSet());
            CharTermAttribute token = tokenStream.getAttribute(CharTermAttribute.class);
            while (tokenStream.incrementToken()) {
                String term = token.toString();
                // stem each term
                String stem = stem(term);
                if (stem != null) {
                    string = string + " " + stem;
                }
            }
        } finally {
            if (tokenStream != null) {
                tokenStream.close();
            }
        }
        return string;
    }

    /**
     * Stemming words by using PorterStemFilter of Lucene
     *
     * @throws IOException
     */
    public static String stem(String term) throws IOException {

        TokenStream tokenStream = null;
        try {

            // tokenize
            tokenStream = new ClassicTokenizer(Version.LUCENE_40, new StringReader(term));
            // stem
            tokenStream = new PorterStemFilter(tokenStream);

            // add each token in a set, so that duplicates are removed
            Set<String> stems = new HashSet<String>();
            CharTermAttribute token = tokenStream.getAttribute(CharTermAttribute.class);
            while (tokenStream.incrementToken()) {
                stems.add(token.toString());
            }

            // if no stem or 2+ stems have been found, return null
            if (stems.size() < 1) {
                return null;
            }
            String stem = stems.iterator().next();
            // if the stem has non-alphanumerical chars, return null
            if (!stem.matches("[a-zA-Z0-9-]+")) {
                return null;
            }
            //System.out.println("Stemmed word:" + stem);
            return stem;

        } finally {
            if (tokenStream != null) {
                tokenStream.close();
            }
        }

    }

    /**
     * Indexing Directory
     *
     * @throws IOException
     */
    public static void indexDirectory(IndexWriter writer, File dir) throws IOException {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                indexDirectory(writer, f); // recurse
            } else if (f.getName().endsWith(".txt")) {
                // call indexFile to add the title of the txt file to your index (you can also index html)
                indexFile(writer, f);
            }
        }
    }

    /**
     * Indexing File
     *
     * @throws IOException
     */
    public static void indexFile(IndexWriter writer, File f) throws IOException {
        System.out.println("Indexing " + f.getName());
        Document doc = new Document();
        doc.add(new TextField("filename", f.getName(), TextField.Store.YES));
        //open each file to index the content
        try {

            FileInputStream is = new FileInputStream(f);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuffer stringBuffer = new StringBuffer();
            String line = null;
            while ((line = reader.readLine()) != null) {
                stringBuffer.append(line).append("\n");
            }
            reader.close();
            doc.add(new TextField("contents", stringBuffer.toString(), TextField.Store.YES));

        } catch (Exception e) {

            System.out.println("something wrong with indexing content of the files");
        }

        writer.addDocument(doc);

    }

    /**
     * Write contents to file - essentially to write wikibooks contents to .txt
     * file
     *
     * @throws IOException
     */
    public static void writeFile(String filename, String content) {
        try {

            // String content = "This is the content to write into file";
            File file = new File(filename);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                System.out.println("creating file-" + filename);
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();

            System.out.println("Done");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if directory exists
     *
     * @throws IOException
     */
    public static void checkDir(File file) {
        //File file = new File(folder_path);
        if (file.isDirectory()) {
            if (file.list().length == 0) {
                System.out.println("dir exists");
            } else {
                System.out.println( "Number of files:" + file.list().length);
            }
        } else {
            System.out.println( "Is not a directory");
        }
    }

    public static void main() throws IOException, ParseException, org.apache.lucene.queryparser.classic.ParseException, URISyntaxException {
        //Object temp_path = System.getProperties().get("basedir");
        //System.out.println("temp_path:"+temp_path);
        File myFolder;
        myFolder = new File(lucenemaven.class.getClassLoader().getResource("content").getPath());
        System.out.println(lucenemaven.class.getClassLoader().getResource("content"));
        //checkDir(myFolder);
        URL pathUrl = lucenemaven.class.getClassLoader().getResource("content/");
        
        if ((pathUrl.equals(null)) && pathUrl.getProtocol().equals("file")) {
            int f = new File(pathUrl.toURI()).list().length;
            System.out.println("Len:"+f);
        } 
        File dataDir = new File(folder_path); //my content file folder path
      
        if (dataDir.isDirectory()) {
            if (dataDir.list().length == 0) {
                crawlContent();
            }
        }
        
        // Check whether the directory to be indexed exists
        if (!dataDir.exists()) {
            throw new IOException(
                    dataDir + " does not exist or is not a directory");
        }
        Directory indexDir = new RAMDirectory();

        // Specify the analyzer for tokenizing text.
        StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer);
        IndexWriter writer = new IndexWriter(indexDir, config);

        // call indexDirectory to add to your index
        // the names of the txt files in dataDir
        indexDirectory(writer, dataDir);
        writer.close();
        // lucenemaven lmobj = null;
        // lmobj.search(analyzer, indexDir);

        /**
         * Read Stackoverflow posts from CSV file
         */
        CSVReader csvreader = new CSVReader(new FileReader(csv_path));
        String[] nextLine;
        int post_no = 0;
        if (csvreader.readNext() == null) {
            System.err.println("empty");
        } else {
            System.err.println("Not empty");
        }
        while ((nextLine = csvreader.readNext()) != null) {
            post_no++;
            // nextLine[] is an array of values from the line
            //System.out.println(processText(nextLine[1]) + "\n");
            //}
            ArrayList<String> al = new ArrayList<String>();
            String querystr = "contents:" + processText(nextLine[1]);
            post_title.add(nextLine[1]);
            /*
		Scanner console = new Scanner(System.in);
		String querystr = "contents:"+console.nextLine();
		System.out.println(querystr);
             */
            Query q = new QueryParser(Version.LUCENE_40, "contents", analyzer).parse(querystr);
            int hitsPerPage = 10;
            IndexReader reader = null;

            TopScoreDocCollector collector = null;
            IndexSearcher searcher = null;
            reader = DirectoryReader.open(indexDir);
            searcher = new IndexSearcher(reader);
            collector = TopScoreDocCollector.create(hitsPerPage, true);
            searcher.search(q, collector);

            ScoreDoc[] hits = collector.topDocs().scoreDocs;
            System.out.println("Found " + hits.length + " hits.");
            //System.out.println();

            for (int i = 0; i < hits.length; ++i) {
                int docId = hits[i].doc;
                Document d;
                d = searcher.doc(docId);
                al.add(d.get("contents"));
                //System.out.println((i + 1) + ". " + d.get("filename"));
            }
            reader.close();
            posts.add(al);
        }
        for (int i = 0; i < posts.size(); i++) {
            //System.out.print("post-" + posts.get(i) + ":");
            ArrayList<String> temp = posts.get(i);
            for (int j = 0; j < temp.size(); j++) {
               // System.out.println(i + "," + j);
            }
        }
    }

    public void search(StandardAnalyzer analyzer, Directory indexDir) throws FileNotFoundException, IOException, org.apache.lucene.queryparser.classic.ParseException {
        //Query string!  
        CSVReader csvreader = new CSVReader(new FileReader(csv_path));
        String[] nextLine;
        if (csvreader.readNext() == null) {
            System.err.println("empty");
        } else {
            System.err.println("Not empty");
        }
        /* while ((nextLine = csvreader.readNext()) != null) {
            // nextLine[] is an array of values from the line
            System.out.println(stemText(nextLine[0]) + "\n");
            //}

            String querystr = "contents:" + nextLine[0];

            /*
		Scanner console = new Scanner(System.in);
		String querystr = "contents:"+console.nextLine();
		System.out.println(querystr);
             
            Query q = new QueryParser(Version.LUCENE_40, "contents", analyzer).parse(querystr);
            int hitsPerPage = 10;
            IndexReader reader = null;

            TopScoreDocCollector collector = null;
            IndexSearcher searcher = null;
            reader = DirectoryReader.open(indexDir);
            searcher = new IndexSearcher(reader);
            collector = TopScoreDocCollector.create(hitsPerPage, true);
            searcher.search(q, collector);

            ScoreDoc[] hits = collector.topDocs().scoreDocs;
            System.out.println("Found " + hits.length + " hits.");
            System.out.println();

            for (int i = 0; i < hits.length; ++i) {
                int docId = hits[i].doc;
                Document d;
                d = searcher.doc(docId);

                System.out.println((i + 1) + ". " + d.get("filename"));
            }
            reader.close();
        }*/
    }
}
