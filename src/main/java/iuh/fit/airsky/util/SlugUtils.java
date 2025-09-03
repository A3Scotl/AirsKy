package iuh.fit.airsky.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class SlugUtils {
    
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGESDHASHES = Pattern.compile("(^-|-$)");

    public static String generateSlug(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = EDGESDHASHES.matcher(slug).replaceAll("");
        return slug.toLowerCase();
    }
    
    public static String generateUniqueSlug(String title, java.util.function.Function<String, Boolean> existsChecker) {
        String baseSlug = generateSlug(title);
        String finalSlug = baseSlug;
        int counter = 1;
        
        while (existsChecker.apply(finalSlug)) {
            finalSlug = baseSlug + "-" + counter;
            counter++;
        }
        
        return finalSlug;
    }
}
