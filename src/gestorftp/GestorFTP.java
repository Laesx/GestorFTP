/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
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
import java.net.SocketException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTP;

public class GestorFTP {
    private final FTPClient clienteFTP;
    private static final String SERVIDOR = "localhost";
    private static final int PUERTO = 21;
    private static final String USUARIO = "eric";
    private static final String PASSWORD = "admin1234";

    public GestorFTP() {
        clienteFTP = new FTPClient();
    }

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

    public void desconectar() throws IOException {
        clienteFTP.disconnect();
    }

    public boolean subirFichero(String path) throws IOException {
        File ficheroLocal = new File(path);
        InputStream is = new FileInputStream(ficheroLocal);
        boolean enviado = clienteFTP.storeFile(ficheroLocal.getName(), is);
        is.close();
        return enviado;
    }

    public boolean descargarFichero(String ficheroRemoto, String pathLocal)
            throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(pathLocal));
        boolean recibido = clienteFTP.retrieveFile(ficheroRemoto, os);
        os.close();
        return recibido;
    }

    public static void main(String[] args) {
        String fichero_descarga = "xdServer.html";
        GestorFTP gestorFTP = new GestorFTP();
        try {
            gestorFTP.conectar();
            System.out.println("Conectado");
            boolean subido = gestorFTP.subirFichero("D:\\Descargas\\xd.html");
            if (subido) {
                System.out.println("Fichero subido correctamente");
            } else {
                System.err.println("Ha ocurrido un error al intentar subir el fichero");
            }
            boolean descargado = gestorFTP.descargarFichero(fichero_descarga, "D:/" + fichero_descarga);
            if (descargado) {
                System.out.println("Fichero descargado correctamente");
            } else {
                System.err.println("Ha ocurrido un error al intentar descargar el fichero.");
            }
            gestorFTP.desconectar();
            System.out.println("Desconectado");
        } catch (Exception e) {
            System.err.println("Ha ocurrido un error:" + e.getMessage());
        }
    }
}
