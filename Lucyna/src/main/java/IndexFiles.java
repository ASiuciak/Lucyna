import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;

import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;


public class IndexFiles{

    //Indexes all files from tree of folders:
    public static void indexTree(final IndexWriter writer, Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        indexDocument(writer, file, attrs.lastModifiedTime().toMillis());
                    } catch (IOException ignore) {
                        //We ignore files we can't index
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            indexDocument(writer, path, Files.getLastModifiedTime(path).toMillis());
        }
    }


    //Indexes single file:
    public static void indexDocument(IndexWriter writer, Path file, long lastModified) throws IOException {
        try {
            Document doc = new Document();
            Field pathField = new TextField("path", file.toString(), Field.Store.YES);
            doc.add(pathField);
            doc.add(new LongPoint("modified", lastModified));
            TikaParser tika = new TikaParser();
            String content = tika.Parse(file);
            LanguageDetector detector = new OptimaizeLangDetector().loadModels();
            LanguageResult result = detector.detect(content);
            String language = result.getLanguage();
            Field contentField;


            //ContentField's name depends on language of the content's language, it is "pl" fo Polish, "en" for English and "other" for other languages.
            //It is needed to let the AnalyzerWrapper choose the correct analyzer:
            if(language.equals("pl")){//polish text
                contentField = new TextField("pl", content, Field.Store.YES);
            }else if(language.equals("en")){//english text
                contentField = new TextField("en", content, Field.Store.YES);
            }else{//other language
                contentField = new TextField("other", content, Field.Store.YES);
            }
            doc.add(contentField);


            if (writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
                System.out.println("adding " + file);
                writer.addDocument(doc);
            } else {
                System.out.println("updating " + file);
                writer.updateDocument(new Term("path", file.toString()), doc);
            }
        }
        catch (TikaParser.Fail fail){
            System.out.println("Problem with extracting text from file" + file.getFileName());
        }
    }
}
