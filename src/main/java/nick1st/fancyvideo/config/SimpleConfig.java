package nick1st.fancyvideo.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.function.Predicate;

public class SimpleConfig {

    public final File configFile;
    public Properties properties = new Properties();
    Properties defaultProperties = new Properties();
    ArrayList<SimpleConfigObj> simpleConfigObj = new ArrayList<>();

    public SimpleConfig(File configFile) {
        this.configFile = configFile;
    }

    public void read() {
        try (FileReader reader = new FileReader(configFile)) {
            properties.load(reader);
        } catch (IOException ex) {
            // I/O error
        }
    }

    public void write() {
        try (FileWriter writer = new FileWriter(configFile);) {
            simpleConfigObj.forEach(obj -> {
                try {
                    writer.write(obj.toString(this));
                    read();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException ex) {
            // I/O error
        }
    }

    public String get(String key) {
        return properties.getProperty(key, defaultProperties.getProperty(key));
    }

    public boolean getAsBool(String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }

    public int getAsInt(String key) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void setProperty(String key, String value, String description, String range, Predicate<String> validator) {
        defaultProperties.setProperty(key, value);
        simpleConfigObj.add(new SimpleConfigObj(key, description, range, validator));
    }
}
