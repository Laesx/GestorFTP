package gestorftp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Main {
    private static final String SERVIDOR = "localhost";
    private static final int PUERTO = 21;
    private static final String USUARIO = "eric";
    private static final String PASSWORD = "admin1234";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Introduce el nombre de la carpeta que quieres comprimir:");
        String folderName = scanner.nextLine();

        try {
            String zipFileName = compressFolder(folderName);
            sendFile(zipFileName);
        } catch (IOException | InterruptedException e) {
            System.err.println("Ha ocurrido un error: " + e.getMessage());
        }
    }

    private static String compressFolder(String folderName) throws IOException, InterruptedException {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String tarFileName = folderName + "_" + timestamp + ".tar.gz";
        System.out.println("Comprimiendo carpeta...");

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("cmd.exe", "/c", "tar", "-czf", tarFileName, folderName);
        Process process = processBuilder.start();
        process.waitFor();
        System.out.println("Carpeta comprimida correctamente");

        return tarFileName;
    }

    private static void sendFile(String fileName) throws IOException {
        //Path path = Paths.get(fileName);
        GestorFTP gestorFTP = new GestorFTP();
        try {
            gestorFTP.conectar();
            System.out.println("Conectado");

            System.out.println("Subiendo fichero al servidor FTP...");
            boolean subido = gestorFTP.subirFichero(fileName);
            if (subido) {
                System.out.println("Fichero subido correctamente");
            } else {
                System.err.println("Ha ocurrido un error al intentar subir el fichero");
            }
            /*
            boolean descargado = gestorFTP.descargarFichero(fileName, "D:/" + fileName);
            if (descargado) {
                System.out.println("Fichero descargado correctamente");
            } else {
                System.err.println("Ha ocurrido un error al intentar descargar el fichero.");
            }*/
            gestorFTP.desconectar();
            System.out.println("Desconectado");
        } catch (Exception e) {
            System.err.println("Ha ocurrido un error:" + e.getMessage());
        }
        //System.out.println("Fichero subido correctamente");
    }
}
