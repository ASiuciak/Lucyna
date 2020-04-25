import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.FSDirectory;


//Useful generic class:
class Pair<T1,T2>{
    T1 first;
    T2 second;
    public Pair(T1 first, T2 second){
        this.first = first;
        this.second = second;
    }
}



//Informs which query is actaully being used:
enum QueryKind{
    TERM,PHRASE,FUZZY
}


public class Searcher {
    public static void main (String [] args){
        // Ścieżka do indeksu:
        String indexPath = System.getProperty("user.home") + "\\.index";

        try{
            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
            IndexSearcher searcher = new IndexSearcher(reader);
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

            //List of possible contentField names:
            ArrayList<String> langs = new ArrayList<>();
            langs.add("pl");
            langs.add("en");
            langs.add("other");

            //Default analyzer is english, default query is termQuery, details are powered off at the start:
            Analyzer actualAnalyzer = new EnglishAnalyzer();
            QueryKind actualKind = QueryKind.TERM;
            boolean details = false;

            //Some default searching limits:
            int limit = Integer.MAX_VALUE;
            int maxDistance = 2;
            int maxPrefix = 2;
            int maxFragmentSize = 10;


            //Informs whether last task is searching task:
            boolean search;

            while(true){
                try{
                    search = true;
                    System.out.println(">");
                    String line = in.readLine();

                    //Settings changing commands, we don't search them in index:
                    if(line.equals("%term")){
                        actualKind = QueryKind.TERM;
                        search = false;
                    }else if(line.equals("%phrase")){
                        actualKind = QueryKind.PHRASE;
                        search = false;
                    }else if(line.equals("%fuzzy")){
                        actualKind = QueryKind.FUZZY;
                        search = false;
                    }else if(line.equals("%lang en")){
                        actualAnalyzer = new EnglishAnalyzer();
                        search = false;
                    }else if(line.equals("%lang pl")){
                        actualAnalyzer = new PolishAnalyzer();
                        search = false;
                    }else if(line.equals("%details on")){
                        details = true;
                        search = false;
                    }else if(line.equals("%details off")){
                        details = false;
                        search = false;
                    }


                    //List of all paths of files matching the search:
                    ArrayList<String> paths = new ArrayList<>();
                    //List of pairs <Path,Context>:
                    ArrayList<Pair<String,String>> listOfContexts = new ArrayList<>();


                    //We have 3 possible names of fields with document's content, "pl', "en" and "other":
                    for(int i=0; i<langs.size() && search; i++){
                        QueryParser parser = new QueryParser(langs.get(i),actualAnalyzer);
                        Query query;

                        //Sets query to one matching actually wanted kind:
                        if(actualKind == QueryKind.TERM){
                            query = new TermQuery(new Term(langs.get(i),line));
                        }
                        else if(actualKind == QueryKind.FUZZY){
                            query = new FuzzyQuery(new Term(langs.get(i),line), maxDistance, maxPrefix);
                        }else if(actualKind == QueryKind.PHRASE){
                            query = new PhraseQuery(maxDistance, langs.get(i),line);
                        }

                        query = parser.parse(line);
                        TopDocs top = searcher.search(query,limit);
                        ScoreDoc[] docs = top.scoreDocs;
                        Formatter formatter = new SimpleHTMLFormatter();
                        QueryScorer scorer = new QueryScorer(query);
                        Highlighter highlighter = new Highlighter(formatter,scorer);
                        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer,10);
                        highlighter.setTextFragmenter(fragmenter);

                        //Loop going through all documents matching the search:
                        for(ScoreDoc doc : docs){
                            Document document = reader.document(doc.doc);
                            String[] frags = highlighter.getBestFragments(actualAnalyzer, langs.get(i), document.get(langs.get(i)), 10);
                            for(int j=0; j<frags.length; j++){
                                listOfContexts.add(new Pair<>(document.get("path"),frags[j]));
                            }
                            paths.add(document.get("path"));
                        }
                    }

                    //Printing the results of searching:
                    if(search){
                        System.out.println("Files count: " + paths.size());
                        if(details){
                            for(int i=0; i<listOfContexts.size(); i++){
                                Pair<String,String> pair = listOfContexts.get(i);
                                System.out.println(pair.first);
                                System.out.println(pair.second);
                            }
                        }
                        else{
                            for(int i=0; i<paths.size(); i++){
                                System.out.println(paths.get(i));
                            }
                        }
                    }

                }catch (IOException e){
                    System.out.println(" caught a " + e.getClass() +
                            "\n with message: " + e.getMessage());
                    System.exit(1);
                }catch(ParseException p){
                    System.out.println("Problem with parsing");
                }catch (InvalidTokenOffsetsException e){
                    System.out.println("Problem with finding contexts.");
                }
            }
        }catch(IOException e){
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }
}
