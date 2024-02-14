package gestorftp;

import java.io.IOException;

public class MainSincro {
    public static void main(String[] args) {
        SincronizadorFTP sincro = null;
        try {
            sincro = new SincronizadorFTP("D:\\Test", "Test", 5000);
            sincro.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
