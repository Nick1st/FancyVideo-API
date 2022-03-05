package nick1st.fancyvideo.config;

import java.util.function.Predicate;

public class SimpleConfigObj {

    String key;
    String description;
    String range;

    Predicate<String> validator;

    public SimpleConfigObj(String key, String description, String range, Predicate<String> validator) {
        this.key = key;
        this.description = description;
        this.range = range;
        this.validator = validator;
    }

    public String toString(SimpleConfig config) {
        String value;
        if (validator.test(config.get(key))) {
            value = config.get(key);
        } else {
            value = config.defaultProperties.getProperty(key);
        }
        return "# " + description + "\n" + "# Range: " + range + "\n" + key + "=" + value + "\n";
    }
}
