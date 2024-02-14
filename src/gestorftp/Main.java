package gestorftp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Introduce el nombre de la carpeta que quieres comprimir (puede ser una dirección relativa o absoluta): ");
        String folderName = scanner.nextLine();
        folderName = folderName.replace("\"", ""); // Elimina las comillas dobles
        folderName = folderName.replace("\'", ""); // Elimina las comillas simples

        GestorFTP gestorFTP = new GestorFTP(folderName);
        gestorFTP.start();
    }


}
