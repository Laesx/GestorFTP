package gestorftp;

import java.util.Scanner;
public class MainBackup {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Introduce el nombre de la carpeta que quieres comprimir (puede ser una ruta relativa o absoluta): ");
        String folderName = scanner.nextLine();
        folderName = folderName.replace("\"", ""); // Elimina las comillas dobles
        folderName = folderName.replace("\'", ""); // Elimina las comillas simples

        GestorFTP gestorFTP = new GestorFTP(folderName);
        gestorFTP.start();
    }


}
