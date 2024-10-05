public class Cipher {
    // This is used to validate message metadata. This is the vigenere cipher, so it is set to 5, as canvas states.
    public final String CypherId = "5";

    private String encryptOrDecrypt(String plainText, String key, boolean decrypt) {
        if (!isKeyAlphabetical(key)) return "Non alphabetical key was given";
        key = key.toLowerCase();
        String cypherText = "";

        int keyIndex = 0;
        for (char character : plainText.toCharArray()) {
            if (!isCharAlphabetical(character)) {
                cypherText += character;
                continue;
            }

            int keyValue = key.charAt(keyIndex) - 97;
            if (decrypt) keyValue = -keyValue;

            int zeroIndexedCharacter = (int)character - (isCharUppercase(character) ? 65 : 97);
            int zeroIndexedEncryptedCharacter = (zeroIndexedCharacter + keyValue) % 26;
            if (zeroIndexedEncryptedCharacter < 0) zeroIndexedEncryptedCharacter += 26;
            char finalCharacter =  (char)(zeroIndexedEncryptedCharacter + (isCharUppercase(character) ? 65 : 97));
            cypherText += finalCharacter;

            keyIndex = (keyIndex + 1) % key.length();
        }

        return cypherText;
    }

    public String encrypt(String plainText, String key) { return encryptOrDecrypt(plainText, key, false); }
    public String decrypt(String plainText, String key) { return encryptOrDecrypt(plainText, key, true); }

    public boolean isCharUppercase(char character) {
        int value = (int)character;
        return (value >= 65 && value <= 90);
    }

    public boolean isCharLowercase(char character) {
        int value = (int)character;
        return (value >= 97 && value <= 122);
    }

    public boolean isCharAlphabetical(char character) {
        return isCharUppercase(character) || isCharLowercase(character);
    }

    public boolean isKeyAlphabetical(String key) {
        key = key.toLowerCase();

        for (char character : key.toCharArray()) {
            if (!isCharAlphabetical(character)) return false;
        }

        return true;
    }

    public static void main(String[] args) {
        Cipher cypher = new Cipher();

        String key = "Ilikebananasalot";

        String encrypted = cypher.encrypt("Hello, world! Lorem ipsum dolor sis amet", key);
        System.out.println(encrypted);
        System.out.println(cypher.decrypt(encrypted, key));

    }
}