package gestorftp;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

public class SincronizadorFTP extends Thread {
    private final GestorFTP gestorFTP;
    private final String carpetaLocal;
    private final String carpetaRemota;
    private final int tiempoRefresco;

    public SincronizadorFTP(String carpetaLocal, String carpetaRemota, int tiempoRefresco) throws IOException {
        this.carpetaLocal = carpetaLocal;
        this.carpetaRemota = carpetaRemota;
        this.tiempoRefresco = tiempoRefresco;
        this.gestorFTP = new GestorFTP(carpetaLocal);
    }

    @Override
    public void run() {
        try {
            while (true) {
                // Conectar al servidor FTP
                gestorFTP.conectar();

                // Obtener la lista de archivos locales y remotos
                Set<String> archivosLocales = obtenerArchivosLocales();
                String[] archivosRemotos = gestorFTP.listarArchivos(carpetaRemota);

                // Identificar los archivos que no cambian, que han sido modificados o creados, y que han sido borrados
                Set<String> archivosNoCambiados = new HashSet<>(archivosLocales);
                archivosNoCambiados.retainAll(List.of(archivosRemotos));

                Set<String> archivosModificadosOCreados = new HashSet<>(archivosLocales);
                archivosModificadosOCreados.removeAll(archivosNoCambiados);

                Set<String> archivosBorrados = new HashSet<>(List.of(archivosRemotos));
                archivosBorrados.removeAll(archivosNoCambiados);

                // Ejecutar las acciones correspondientes concurrentemente
                ExecutorService executor = Executors.newFixedThreadPool(5);
                for (String archivo : archivosModificadosOCreados) {
                    executor.submit(() -> {
                        try {
                            gestorFTP.subirFichero(carpetaLocal + "/" + archivo);
                        } catch (IOException e) {
                            System.err.println("Ha ocurrido un error al intentar subir el archivo " + archivo + ": " + e.getMessage());
                        }
                    });
                }
                for (String archivo : archivosBorrados) {
                    executor.submit(() -> {
                        try {
                            gestorFTP.borrarFichero(carpetaRemota + "/" + archivo);
                        } catch (IOException e) {
                            System.err.println("Ha ocurrido un error al intentar borrar el archivo " + archivo + ": " + e.getMessage());
                        }
                    });
                }
                executor.shutdown();
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

                // Desconectar del servidor FTP
                gestorFTP.desconectar();

                // Esperar el tiempo de refresco
                Thread.sleep(tiempoRefresco);
            }
        } catch (Exception e) {
            System.err.println("Ha ocurrido un error:" + e.getMessage());
        }
    }

    private Set<String> obtenerArchivosLocales() throws IOException {
        Set<String> archivos = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(carpetaLocal))) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    archivos.add(path.getFileName().toString());
                }
            }
        }
        return archivos;
    }
}
