package gcc.demo.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.type.AnnotationMetadata;

public class ConfigImportSelector {

    public static String[] selectImports(AnnotationMetadata importclssMetadata) {
        List<String> result = new ArrayList<>();
        InputStream in = ConfigImportSelector.class.getClassLoader().getResourceAsStream("Common.Imports");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block

        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
            }
        }
        return result.toArray(String[]::new);
    }
}
