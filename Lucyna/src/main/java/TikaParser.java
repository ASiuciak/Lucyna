import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;


public class TikaParser {
    class Fail extends Exception{}
    public String Parse(Path file) throws Fail {
        Tika tika = new Tika();
        String path=file.toString();
        String title = file.getFileName().toString();
        String content = new String();
        try{
            InputStream stream = new FileInputStream(path);
            content = tika.parseToString(stream);
        }catch (FileNotFoundException e){
            throw new Fail();
        }catch (TikaException e){//pusty plik
            return title;
        }catch(IOException e){
            throw new Fail();
        }
        return title+" "+content;
    }
}

