import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class Encryptor {
    public static String encrypt(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] salt = generateSalt();
            digest.update(salt);

            byte[] hash = digest.digest(password.getBytes());
            String hashBase64 = Base64.getEncoder().encodeToString(hash);
            String saltBase64 = Base64.getEncoder().encodeToString(salt);

            return saltBase64 + ":" + hashBase64;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al generar el hash de la contraseña", e);
        }
    }

    private static byte[] generateSalt() {
        try {
            SecureRandom sr = new SecureRandom();
            byte[] salt = new byte[16];
            sr.nextBytes(salt);
            return salt;
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el salt", e);
        }
    }

    public static boolean verify(String storedPassword, String inputPassword) {
        try {
            String[] parts = storedPassword.split(":");
            String storedSalt = parts[0];
            String storedHash = parts[1];

            byte[] salt = Base64.getDecoder().decode(storedSalt);
            byte[] expectedHash = Base64.getDecoder().decode(storedHash);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            digest.update(salt);

            byte[] inputHash = digest.digest(inputPassword.getBytes());

            return MessageDigest.isEqual(inputHash, expectedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al verificar la contraseña", e);
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }
}
