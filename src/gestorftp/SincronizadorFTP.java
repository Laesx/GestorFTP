package gestorftp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

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
    private final int tiempoRefresco; // Tiempo en milisegundos

    public SincronizadorFTP(String carpetaLocal, String carpetaRemota, int tiempoRefresco) throws IOException {
        this.carpetaLocal = carpetaLocal;
        this.carpetaRemota = carpetaRemota;
        this.tiempoRefresco = tiempoRefresco;
        this.gestorFTP = new GestorFTP(carpetaLocal);
    }

    @Override
    public void run() {
        try {
            // Conectar al servidor FTP
            gestorFTP.conectar();
            while (true) {
                System.out.println("Sincronizando...");

                // Obtener la lista de archivos locales y remotos
                Set<String> archivosLocales = obtenerArchivosLocales();
                Set<String> archivosRemotos2 = gestorFTP.listarArchivos(carpetaRemota);

                Set<String> archivosRemotos = new HashSet<>();

                for (String archivo : archivosRemotos2) {
                    archivosRemotos.add(archivo.replace(carpetaRemota + "/", ""));
                }

                System.out.println("Archivos locales: " + archivosLocales);
                System.out.println("Archivos remotos: " + archivosRemotos);

                // Identificar los archivos que no cambian, que han sido modificados o creados, y que han sido borrados
                Set<String> archivosNoCambiados = new HashSet<>(archivosLocales);
                archivosNoCambiados.retainAll(archivosRemotos);

                System.out.println("Archivos no cambiados: " + archivosNoCambiados);

                Set<String> archivosModificadosOCreados = new HashSet<>(archivosLocales);
                archivosModificadosOCreados.removeAll(archivosNoCambiados);
                // Añadimos los archivos locales que se han actualizado
                archivosModificadosOCreados.addAll(obtenerArchivosModificados(archivosLocales, archivosRemotos));

                System.out.println("Archivos modificados o creados: " + archivosModificadosOCreados);

                Set<String> archivosBorrados = new HashSet<>(archivosRemotos);
                archivosBorrados.removeAll(archivosNoCambiados);

                System.out.println("Archivos borrados: " + archivosBorrados);

                // TODO Manejar carpetas

                // Ejecutar las acciones correspondientes concurrentemente
                // Aunque no tiene sentido ya que FTP solo acepta una operación a la vez
                ExecutorService executor = Executors.newFixedThreadPool(1);
                for (String archivo : archivosModificadosOCreados) {
                    executor.submit(() -> {
                        try {
                            System.out.println("Subiendo archivo local: " + carpetaLocal + "/" + archivo + "...");
                            //System.out.println("a remoto: " + carpetaRemota + "/" + archivo + "...");

                            gestorFTP.subirFichero(carpetaLocal + "/" + archivo, carpetaRemota + "/" + archivo);
                        } catch (IOException e) {
                            System.err.println("Ha ocurrido un error al intentar subir el archivo " + archivo + ": " + e.getMessage());
                        }
                    });
                }
                for (String archivo : archivosBorrados) {
                    executor.submit(() -> {
                        try {
                            System.out.println("Borrando archivo " + carpetaRemota + "/" + archivo + "...");
                            gestorFTP.borrarFichero(carpetaRemota + "/" + archivo);
                        } catch (IOException e) {
                            System.err.println("Ha ocurrido un error al intentar borrar el archivo " + archivo + ": " + e.getMessage());
                        }
                    });
                }
                executor.shutdown();
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

                System.out.println("Sincronización completada");
                // Esperar el tiempo de refresco
                Thread.sleep(tiempoRefresco);
            }
        } catch (Exception e) {
            System.err.println("Ha ocurrido un error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Desconectar del servidor FTP
            try {
                gestorFTP.desconectar();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }



    }

    private Set<String> obtenerArchivosModificados(Set<String> archivosLocales, Set<String> archivosRemotos) throws IOException {

        Set<String> archivosModificados = new HashSet<>();
        for (String archivoLocal : archivosLocales) {
            if (archivosRemotos.contains(archivoLocal)) {
                // Aquí comprobar si el archivo ha sido modificado
                if (gestorFTP.archivoActualizado(carpetaLocal + "/" + archivoLocal, carpetaRemota + "/" + archivoLocal)) {
                    archivosModificados.add(archivoLocal);
                    System.out.println("El archivo " + archivoLocal + " ha sido modificado");
                }
            }
        }
        return archivosModificados;
    }


    private Set<String> obtenerArchivosLocalesOld() throws IOException {
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

    private Set<String> obtenerArchivosLocales() throws IOException {
        Set<String> archivos = new HashSet<>();
        archivosEnDirectorio(Paths.get(carpetaLocal), archivos);
        //System.out.println("Archivos locales: " + archivos);
        Set<String> archivosArreglados = new HashSet<>();
        for (String archivo : archivos) {
            archivosArreglados.add(archivo.replace("\\", "/"));
        }
        return archivosArreglados;
    }

    private void archivosEnDirectorio(Path dir, Set<String> archivos) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    archivosEnDirectorio(path, archivos);
                } else {
                    Path relativePath = Paths.get(carpetaLocal).relativize(path);
                    archivos.add(relativePath.toString());
                }
            }
        }
    }

}
