import java.util.HashSet;
import java.util.Arrays;

public class Cipher {
    /** This is used to validate message metadata. */
    public final int CypherId;

    // A better way to handle all of this would be enums.
    public static final HashSet<Integer> AllowedIds = new HashSet<Integer>(Arrays.asList(1, 2, 3, 5, 8, 9));

    public Cipher(int cypherId) {
        CypherId = cypherId;
    }

    /** Checks whether a character is in A-Z. */ 
    private boolean isCharUppercase(char character) {
        int value = (int)character;
        return (value >= 65 && value <= 90);
    }

    /** Checks whether a character is in a-z. */
    private boolean isCharLowercase(char character) {
        int value = (int)character;
        return (value >= 97 && value <= 122);
    }

    /** Checks whether a character is in A-Z or in a-z. */
    private boolean isCharAlphabetical(char character) {
        return isCharUppercase(character) || isCharLowercase(character);
    }

    /** Checks whether the entire key is made of alphabetical characters */
    private boolean isKeyAlphabetical(String key) {
        key = key.toLowerCase();

        for (char character : key.toCharArray()) {
            if (!isCharAlphabetical(character)) return false;
        }

        return true;
    }

    /** Turns alphabetical characters into ints where a=0, b=1 ... Non alphabetical characters return -1 */
    private int normalizeChar(char character) {
        if (!isCharAlphabetical(character)) return -1;

        return (int)character - (isCharUppercase(character) ? 65 : 97);
    }

    /** Turns ints where a=0, b=1 ... into characters. Is undefined for incorrect ints.*/
    private char denormalizeChar(int character, boolean upperCase) {
        return (char)(character + (upperCase ? 65 : 97));
    }

    private String encryptOrDecryptVigenere(String input, String key, boolean decrypt, boolean autokey) {
        if (!isKeyAlphabetical(key)) return "Non alphabetical key was given!";
        if (!decrypt && autokey) key += input.replaceAll("[^A-Za-z]", "");

        String cypherText = "";
        int keyIndex = 0;
        for (char character : input.toCharArray()) {
            // If the character is not a letter then we just pass it through as is.
            if (!isCharAlphabetical(character)) {
                cypherText += character;
                continue;
            }

            int keyValue = normalizeChar(key.charAt(keyIndex));
            if (decrypt) keyValue = -keyValue;
            int zeroIndexedCharacter = normalizeChar(character);

            int zeroIndexedEncryptedCharacter = (zeroIndexedCharacter + keyValue) % 26;
            if (zeroIndexedEncryptedCharacter < 0) zeroIndexedEncryptedCharacter += 26;

            char finalCharacter = denormalizeChar(zeroIndexedEncryptedCharacter, isCharUppercase(character));
            cypherText += finalCharacter;
            if (decrypt && autokey) key += finalCharacter;

            keyIndex = (keyIndex + 1) % key.length();
        }

        return cypherText;
    }

    private String atBash(String plainText) {
        String cypherText = "";
        
        for (char character : plainText.toCharArray()) {
            if (!isCharAlphabetical(character)) {
                cypherText += character;
                continue;
            }

            cypherText += denormalizeChar(25 - normalizeChar(character), isCharUppercase(character));
        }

        return cypherText;
    }

    public String encrypt(String plainText, String key) { 
        switch (CypherId) {
            case 1: return encryptOrDecryptVigenere(plainText, key, false, false);
            case 2: return atBash(plainText);
            case 3: return encryptOrDecryptVigenere(plainText, "N", false, false);
            case 5: return encryptOrDecryptVigenere(plainText, key, false, false);
            case 8: return encryptOrDecryptVigenere(plainText, key, false, true);
            case 9: return encryptOrDecryptVigenere(atBash(plainText), atBash(key), true, false); 
            default: return plainText;
        }
    }

    public String decrypt(String plainText, String key) { 
        switch (CypherId) {
            case 1: return encryptOrDecryptVigenere(plainText, key, true, false);
            case 2: return atBash(plainText);
            case 3: return encryptOrDecryptVigenere(plainText, "N", true, false);
            case 5: return encryptOrDecryptVigenere(plainText, key, true, false);
            case 8: return encryptOrDecryptVigenere(plainText, key, true, true);
            case 9: return encryptOrDecryptVigenere(atBash(plainText), atBash(key), true, false); 
            default: return plainText;
        }
     }
}

class CipherTest {
    public static void main(String[] args) {
        Cipher cypher = new Cipher(9);

        String key = "GRUMINIONBaNANA";

        String encrypted = cypher.encrypt("Hello, world! Lorem ipsum dolor sis amet", key);
        System.out.println(encrypted);
        System.out.println(cypher.decrypt(encrypted, key));

    }
}