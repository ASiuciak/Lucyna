import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class FirstIndexing{
    //Creates an index and add catalogue "src\\main\\resources" to it:
    public static void main (String [] args){
        //Index directory:
        final String indexPath = System.getProperty("user.home") + "\\.index";
        String docPath = "src\\main\\resources";
        try{
            System.out.println("Indexing to directory '" + indexPath + "'...");
            Directory pom = FSDirectory.open(Paths.get(indexPath));
            final Directory dir = pom;
            Map<String,Analyzer> analyzers = new HashMap<String,Analyzer>();
            analyzers.put("pl",new PolishAnalyzer());
            analyzers.put("en",new EnglishAnalyzer());
            PerFieldAnalyzerWrapper wrapper = new PerFieldAnalyzerWrapper(new StandardAnalyzer(),analyzers);
            IndexWriterConfig iwc = new IndexWriterConfig(wrapper);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter writer = new IndexWriter(pom,iwc);
            IndexOperations.addOrUpdateDocs(docPath, writer);
            writer.close();
        }catch (FileNotReadable ignore){
            System.out.println("File " + Paths.get(docPath).getFileName() + " is not readable");
        }catch(IOException e){
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }
}
