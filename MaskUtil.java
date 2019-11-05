import java.util.regex.Pattern;

/**
 * This is an Utility class for Mask Object
 * @author helen ding on 25/10/2019.
 */
public class MaskUtil {
    private static final Pattern digits = Pattern.compile("\\d");
    private static final Pattern capitalLetters = Pattern.compile("[A-Z]");
    private static final Pattern nonSpecialCharacters = Pattern.compile("[^X\\s!-/:-@\\[-`{-~]");

    public static String maskString(String value) {
        String tmpMasked = digits.matcher(value).replaceAll("*");
        tmpMasked = capitalLetters.matcher(tmpMasked).replaceAll("X");
        return nonSpecialCharacters.matcher(tmpMasked).replaceAll("x");
    }

}
