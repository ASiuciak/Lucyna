import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import static java.nio.file.StandardWatchEventKinds.*;

public class Indexer{

    public static void main(String [] args) {

        // Path to index:
        String indexPath = System.getProperty("user.home") + "\\.index";

        try {

            //Creating necessary writer and reader (option APPEND because I assume index has been already initialized by FirstIndexing):
            Directory pom = FSDirectory.open(Paths.get(indexPath));
            final Directory dir = pom;
            Map<String,Analyzer> analyzers = new HashMap<String,Analyzer>();
            analyzers.put("pl",new PolishAnalyzer());
            analyzers.put("en",new EnglishAnalyzer());
            PerFieldAnalyzerWrapper wrapper = new PerFieldAnalyzerWrapper(new StandardAnalyzer(),analyzers);
            IndexWriterConfig iwc = new IndexWriterConfig(wrapper);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
            IndexWriter writer = new IndexWriter(pom,iwc);
            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));

            //Indexer with argument:
            if (args.length > 0) {

                if (args[0].equals("--add")) {
                    if (args.length > 1) {
                        try {
                            IndexOperations.addOrUpdateDocs(args[1], writer);
                        } catch (FileNotReadable e) {
                            System.out.println("File " + Paths.get(args[1]).getFileName() + " is not readable");
                            writer.close();
                            reader.close();
                            System.exit(1);
                        }
                    } else {
                        System.out.println("No argument for --add");
                        writer.close();
                        reader.close();
                        System.exit(1);
                    }
                } else if (args[0].equals("--purge")) {
                    IndexOperations.deleteAll(writer);
                } else if (args[0].equals("--rm")) {
                    if (args.length > 1) {
                        IndexOperations.deleteDocs(args[1], writer, reader);
                    } else {
                        System.out.println("No argument for --rm");
                        writer.close();
                        reader.close();
                        System.exit(1);
                    }
                } else if (args[0].equals("--list")) {
                    IndexOperations.list(reader);
                }else if (args[0].equals("--reindex")) {
                    IndexOperations.reindex(writer,reader);
                }

                writer.close();
                reader.close();
                return;
            }


            //Indexer without arguments - observing src\\main\\resources catalogue:
            String docsPath = "src\\main\\resources";
            WatchService watcher = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(docsPath);
            WatchKey key = path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            while(true){
                try{
                    key = watcher.take();
                }catch (InterruptedException e){
                    System.out.println("Watching the catalogue " + docsPath + " interrupted");
                    System.exit(1);
                }
                for(WatchEvent<?> event: key.pollEvents()){
                    WatchEvent.Kind<?> kind = event.kind();

                    if(kind == OVERFLOW){
                        continue;
                    }

                    WatchEvent<Path> ev = (WatchEvent<Path>)event;
                    Path filename = ev.context();
                    Path child = path.resolve(filename);
                    if(kind == ENTRY_CREATE){
                        try{
                            IndexOperations.addOrUpdateDocs(child.toString(), writer);
                        }catch (FileNotReadable f){
                            System.out.println("File " + filename + " can't be read");
                        }
                    }
                    else if(kind == ENTRY_DELETE){
                        IndexOperations.deleteDocs(child.toString(), writer, reader);
                    }
                    else if(kind == ENTRY_MODIFY){
                        try{
                            IndexOperations.addOrUpdateDocs(child.toString(), writer);
                        }catch(FileNotReadable f){
                            System.out.println("File " + filename + " can't be read");
                        }
                    }
                }
                boolean valid = key.reset();
                if(!valid){
                    break;
                }
            }
            writer.close();
            reader.close();
            return;
        }catch (IOException e){
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
            System.exit(1);
        }
    }
}
