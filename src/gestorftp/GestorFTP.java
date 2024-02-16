/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/MainBackup.java to edit this template
 */
package gestorftp;

/**
 * @author losgu
 */

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTP;

public class GestorFTP extends Thread{
    private final FTPClient clienteFTP;
    private static final String SERVIDOR = "localhost";
    private static final int PUERTO = 21;
    private static final String USUARIO = "eric";
    private static final String PASSWORD = "admin1234";
    // Carpeta que se va a enviar con esta transacción
    private final String carpetaLocal;

    public GestorFTP(String carpetaLocal) {
        clienteFTP = new FTPClient();
        this.carpetaLocal = carpetaLocal;
    }

    @Override
    public void run() {
        try {
            if (carpetaLocal == null || carpetaLocal.isEmpty()) {
                throw new IOException("La carpeta no puede estar vacía");
            }
            if (!new File(carpetaLocal).isDirectory()) {
                throw new IOException("La carpeta no existe");
            }
            conectar();
            // Comprimir la carpeta
            String archivoTar = comprimirCarpeta(carpetaLocal);

            // Subir el archivo zip al servidor FTP
            boolean enviado = subirFichero(archivoTar);
            System.out.println("Enviando archivo tar...");

            if (enviado) {
                System.out.println("Archivo tar subido correctamente");
            } else {
                System.err.println("Ha ocurrido un error al intentar subir el archivo tar");
            }
            desconectar();
        } catch (Exception e) {
            System.err.println("Ha ocurrido un error: " + e.getMessage());
        }
    }

    /** Comprime una carpeta en un archivo tar.gz, lo hace en un proceso pero tiene que esperar a que termine igualmente
     * @param folderName Nombre de la carpeta que se va a comprimir
     * @return Nombre del archivo tar.gz que se ha creado
     * @throws IOException
     * @throws InterruptedException
     */
    private static String comprimirCarpeta(String folderName) throws IOException, InterruptedException {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        Path path = Path.of(folderName);
        String tarFileName = "temp/" + path.getFileName() + "_" + timestamp + ".tar.gz";
        System.out.println("Comprimiendo carpeta...");

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("cmd.exe", "/c", "tar", "-czf", tarFileName, folderName);
        Process process = processBuilder.start();
        process.waitFor();
        System.out.println("Carpeta comprimida correctamente");

        return tarFileName;
    }

    /** Conecta al servidor FTP
     * @throws IOException No se puede conectar al servidor FTP
     */
    public void conectar() throws IOException {
        clienteFTP.connect(SERVIDOR, PUERTO);
        int respuesta = clienteFTP.getReplyCode();
        if (!FTPReply.isPositiveCompletion(respuesta)) {
            clienteFTP.disconnect();
            throw new IOException("Error al conectar con el servidor FTP");
        }
        boolean credencialesOK = clienteFTP.login(USUARIO, PASSWORD);
        if (!credencialesOK) {
            throw new IOException("Error al conectar con el servidor FTP. Credenciales incorrectas.");
        }
        clienteFTP.setFileType(FTP.BINARY_FILE_TYPE);
    }

    /** Desconecta del servidor FTP
     * @throws IOException No se puede desconectar del servidor FTP
     */
    public void desconectar() throws IOException {
        clienteFTP.disconnect();
    }

    /** Sube un fichero al servidor FTP
     * @param pathLocal Ruta del fichero local
     * @return true si el fichero se ha subido correctamente, false en caso contrario
     * @throws IOException No se puede subir el fichero
     */
    public boolean subirFichero(String pathLocal) throws IOException {
        File ficheroLocal = new File(pathLocal);
        InputStream is = new FileInputStream(ficheroLocal);
        boolean enviado = clienteFTP.storeFile(ficheroLocal.getName(), is);
        is.close();
        return enviado;
    }

    public boolean subirFichero(String pathLocal, String pathRemoto) throws IOException {
        String remoteDirectory = Paths.get(pathRemoto).getParent().toString();
        remoteDirectory = remoteDirectory.replace("\\", "/");
        System.out.println("Comprobando directorio remoto: " + remoteDirectory);
        comprobarDirectorioRemoto(remoteDirectory);

        File ficheroLocal = new File(pathLocal);
        InputStream is = new FileInputStream(ficheroLocal);
        boolean enviado = clienteFTP.storeFile(pathRemoto, is);
        is.close();
        return enviado;
    }


    /** Descarga un fichero del servidor FTP
     * @param ficheroRemoto Nombre del fichero a descargar
     * @param pathLocal Ruta donde se va a guardar el fichero
     * @return true si el fichero se ha descargado correctamente, false en caso contrario
     * @throws IOException No se puede descargar el fichero
     */
    public boolean descargarFichero(String ficheroRemoto, String pathLocal)
            throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(pathLocal));
        boolean recibido = clienteFTP.retrieveFile(ficheroRemoto, os);
        os.close();
        return recibido;
    }

    public boolean comprobarDirectorioRemoto(String directoryPath) throws IOException {
        String[] files = clienteFTP.listNames(directoryPath);
        if (files != null) {
            return true; // El directorio existe, devuelve true
        } else {
            return clienteFTP.makeDirectory(directoryPath); // Crea el directorio y devuelve true si lo ha creado
        }
    }

    /** Lista los archivos de una carpeta remota
     * @param carpetaRemota Ruta de la carpeta remota
     * @return Lista de archivos de la carpeta remota
     * @throws IOException No se puede listar los archivos de la carpeta remota
     */
    public Set<String> listarArchivosOld(String carpetaRemota) throws IOException{
        boolean directorio = comprobarDirectorioRemoto(carpetaRemota);
        if (!directorio){
            throw new IOException("No se ha podido crear el directorio remoto");
        }

        Set<String> setArchivos = new HashSet<>();
        FTPFile[] directories =  clienteFTP.listDirectories(carpetaRemota);
        String[] archivos = clienteFTP.listNames(carpetaRemota);

        int respuesta = clienteFTP.getReplyCode();
        // Si la carpeta no existe, la creamos
        if (respuesta == FTPReply.FILE_UNAVAILABLE){
            clienteFTP.makeDirectory(carpetaRemota);
        } else if (!FTPReply.isPositiveCompletion(respuesta)) {
            clienteFTP.disconnect();
            throw new IOException("Error al listar los archivos en el servidor FTP");
        }


        if (archivos != null) {
            // Eliminar el nombre de la carpeta remota de los archivos, para solo tener la lista de ellos.
            for (int i = 0; i < archivos.length; i++) {
                archivos[i] = archivos[i].replace(carpetaRemota + "/", "");
            }
            setArchivos.addAll(Arrays.asList(archivos));
        }
        return setArchivos;
    }

    public Set<String> listarArchivos(String carpetaRemota) throws IOException {
        // Comprobamos que el directorio remoto exista, si no lo creamos
        boolean directorio = comprobarDirectorioRemoto(carpetaRemota);
        if (!directorio){
            throw new IOException("No se ha podido crear el directorio remoto");
        }
    
        Set<String> setArchivos = new HashSet<>();
        FTPFile[] files = clienteFTP.listFiles(carpetaRemota);
        // Usar mListDir para listar archivos y directorios para comprobar

        for (FTPFile file : files) {
            String nombreCompleto = carpetaRemota + "/" + file.getName();
            if (file.isFile()) {
                setArchivos.add(nombreCompleto);
            } else if (file.isDirectory()) {
                setArchivos.addAll(listarArchivos(nombreCompleto));
            }
        }

        //System.out.println("Archivos remotos: " + setArchivos);
        return setArchivos;
    }

    /** Comprueba si un archivo local está actualizado en el servidor FTP
     * @param archivoLocal Ruta del archivo local
     * @param archivoRemoto Ruta del archivo remoto
     * @throws IOException No se puede comprobar si el archivo local está actualizado
     */
    public boolean archivoActualizado(String archivoLocal, String archivoRemoto) throws IOException {
        boolean actualizado = false;
        // Comprobamos si el archivo local es más reciente que el remoto
        File fileLocal = new File(archivoLocal);
        long tiempoLocal = fileLocal.lastModified();
        //Files.getLastModifiedTime(Paths.get(archivoLocal)).toMillis();
        //System.out.println("DEBUG: REMOTO:" + archivoRemoto);
        FTPFile fileRemoto = clienteFTP.mlistFile(archivoRemoto);
        long tiempoRemoto = fileRemoto.getTimestamp().getTimeInMillis();
        //System.out.println("DEBUG: Local: " + tiempoLocal + " Remoto: " + tiempoRemoto);
        if (tiempoLocal > tiempoRemoto) {
            // Subir el archivo
            //subirFichero(archivoLocal, archivoRemoto);
            actualizado = true;
        }
        /*
        else if (tiempoLocal < tiempoRemoto) {
            // Descargar el archivo
            //descargarFichero(archivoRemoto, archivoLocal);

        }
         */

        return actualizado;
    }

    /** Borra un fichero del servidor FTP
     * @param s Nombre del fichero a borrar
     */
    public void borrarFichero(String s) throws IOException {
        if (s == null || s.isEmpty()){
            throw new IOException("El nombre del fichero no puede estar vacío");
        }
        /*
        List<FTPFile> archivos = List.of(clienteFTP.mlistDir(s));

        if (archivos.isEmpty()){
            throw new IOException("El fichero no existe");
        }

        for (FTPFile archivo: archivos){
            if (archivo.isDirectory()){
                clienteFTP.rmd(s);
            } else if (archivo.isFile()) {
                clienteFTP.dele(s);
            }
        }
        */

        //clienteFTP.dele(s);
        //clienteFTP.rmd(s);
        clienteFTP.deleteFile(s);
    }


}
