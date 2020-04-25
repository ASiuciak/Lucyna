import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;

class FileNotReadable extends Exception{}

public class IndexOperations {


    //Adds or updates docs in index - responsible for creating the index and adding files to it (by "--add"):
    public static void addOrUpdateDocs(String path, IndexWriter writer) throws FileNotReadable{
        String usage = "java org.apache.lucene.demo.IndexFiles"
                + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
                + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                + "in INDEX_PATH that can be searched with SearchFiles";
        String docsPath = path;
        if (docsPath == null) {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        final Path docDir = Paths.get(docsPath);
        if (!Files.isReadable(docDir)) {
            System.out.println("Document directory '" + docDir.toAbsolutePath() + "' does not exist or is not readable, please check the path");
            throw new FileNotReadable();
        }

        Date start = new Date();
        try {
            IndexFiles.indexTree(writer, Paths.get(docsPath));
            Date end = new Date();
            System.out.println(end.getTime() - start.getTime() + " total milliseconds");

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }



    //Returns absolute paths to all documents from index:
    public static ArrayList<String> getAllPaths(IndexReader reader){
        ArrayList<String> paths = new ArrayList<String>();
        try{
            for(int i=0; i<reader.numDocs(); i++){
                Document doc = reader.document(i);
                String path = doc.get("path");
                Path absolute = Paths.get(path).toAbsolutePath();
                paths.add(absolute.toString());
            }
        }catch (IOException e){
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
        return paths;
    }



    //Deletes all documents belonging to path's tree.
    //Deletes all at the beginning and adds documents not matching deleting criterium (IndexWriter.delete(Term term) didn't work in my program.
    //(I don't know why)
    public static void deleteDocs(String path, IndexWriter writer, IndexReader reader){
        try{
            path = Paths.get(path).toString();
            ArrayList<String> paths = getAllPaths(reader);
            writer.deleteAll();
            for(int i=0; i<paths.size(); i++){
                if(!paths.get(i).contains(path)){
                    IndexFiles.indexTree(writer, Paths.get(paths.get(i)));
                }
                else{
                    System.out.println("deleting document: " + paths.get(i));
                }
            }
        }catch (IOException e){
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }


    //Deletes all documents from index:
    public static void deleteAll(IndexWriter writer){
        try{
            writer.deleteAll();
        }catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }



    //Deletes all documents, then adds them once again:
    public static void reindex(IndexWriter writer, IndexReader reader){
        try{
            ArrayList<String> paths = getAllPaths(reader);
            writer.deleteAll();
            for(int i=0; i<paths.size(); i++){
                IndexFiles.indexDocument(writer, Paths.get(paths.get(i)), System.currentTimeMillis() );
            }
        }catch (IOException e){
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }


    //Shows absolute paths of all documents in the index:
    public static void list(IndexReader reader){
        try{
            for(int i=0; i<reader.numDocs(); i++){
                Document doc = reader.document(i);
                Path path = Paths.get(doc.get("path"));
                System.out.println(path.toAbsolutePath());
            }
        }catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }
}
