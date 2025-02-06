import java.io.* ;
import java.net.* ;
import java.util.* ;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public final class ServidorWeb {
        public static void main(String argv[]) throws Exception
        {
        // Establece el número de puerto.
        int puerto = 6789;

        // Estableciendo el socket de escucha.
        ServerSocket socketdeEscucha = new ServerSocket(puerto);
        System.out.println("Servidor iniciado"); 

        //ThreadPool
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

       // Procesando las solicitudes HTTP en un ciclo infinito.
       while (true) {
        
                // Escuchando las solicitudes de conexión TCP.
                Socket socketdeConexion = socketdeEscucha.accept(); 
                

                // Construye un objeto para procesar el mensaje de solicitud HTTP.
                SolicitudHttp solicitud = new SolicitudHttp(socketdeConexion);
                System.out.println("Nueva conexión...");

                // Crea un nuevo hilo para procesar la solicitud.
                Thread hilo = new Thread(solicitud);

                executor.execute(hilo);


                // Inicia el hilo ya no es necesario.
                // hilo.start();
        }
}
}

final class SolicitudHttp implements Runnable {
        final static String CRLF = "\r\n";
        Socket socket;

        // Constructor
        public SolicitudHttp(Socket socket) throws Exception 
        {
                this.socket = socket;
        }

        // Implementa el método run() de la interface Runnable.
        public void run()
        {
                try {
                        proceseSolicitud();
                } catch (Exception e) {
                        System.out.println(e);
                }
        }

        private void proceseSolicitud() throws Exception
        {
                // Referencia al stream de salida del socket.
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());

                // Referencia y filtros (InputStreamReader y BufferedReader)para el stream de entrada.
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                // Recoge la línea de solicitud HTTP del mensaje.
                String lineaDeSolicitud = br.readLine();

                // Muestra la línea de solicitud en la pantalla.
                System.out.println();
                System.out.println("La linea de Solicitud fue: " + lineaDeSolicitud);

                // recoge y muestra las líneas de header.
                System.out.println();
                System.out.println("Lineas del Header: ");
                String lineaDelHeader = null;
                while ((lineaDelHeader = br.readLine()).length() != 0) {
                        System.out.println(lineaDelHeader);
                }

                // Extrae el nombre del archivo de la línea de solicitud.
                StringTokenizer partesLinea = new StringTokenizer(lineaDeSolicitud);
                partesLinea.nextToken();  // "salta" sobre el método, se supone que debe ser "GET"
                String nombreArchivo = partesLinea.nextToken();

                // Anexa un ".", de tal forma que el archivo solicitado debe estar en el directorio actual.
                nombreArchivo = "." + nombreArchivo;

                // Abre el archivo seleccionado.
                FileInputStream fis = null;
                boolean existeArchivo = true;
                try {
                        fis = new FileInputStream(nombreArchivo);
                } catch (FileNotFoundException e) {
                        existeArchivo = false;
                }

                // Construye el mensaje de respuesta.
                String lineaDeEstado = null;
                String lineaDeTipoContenido = null;
                String cuerpoMensaje = null;
                if (existeArchivo) {
                        lineaDeEstado = "HTTP/1.0 200 Ok" + CRLF;
                        lineaDeTipoContenido = "Content-type: " + 
                                contentType(nombreArchivo) + CRLF;
                } else {
                        lineaDeEstado = "HTTP/1.0 404 Not Found" + CRLF;
                        lineaDeTipoContenido = "Content-type: text/html" + CRLF;
                        cuerpoMensaje = "<HTML>" + 
                                "<HEAD><TITLE>404 Not Found</TITLE></HEAD>" +
                                "<BODY><b>404</b> Not Found</BODY></HTML>";
                }

                // Envia la línea de estado.
                os.writeBytes(lineaDeEstado);

                // Envía el contenido de la línea content-type.
                os.writeBytes(lineaDeTipoContenido);

                // Envía una línea en blanco para indicar el final de las líneas de header.
                os.writeBytes(CRLF);

                // Envía el cuerpo del mensaje.
                if (existeArchivo) {
                        enviarBytes(fis, os);
                        fis.close();
                } else {
                        os.writeBytes(cuerpoMensaje);
                }

                // Cierra los streams y el socket.
                os.close();
                br.close();
                socket.close();
        }

private static void enviarBytes(FileInputStream fis, OutputStream os) throws Exception {

        // Construye un buffer de 1KB para guardar los bytes cuando van hacia el socket.
        byte[] buffer = new byte[1024];
        int bytes = 0;

        // Copia el archivo solicitado hacia el output stream del socket.
        while((bytes = fis.read(buffer)) != -1 ) {
        os.write(buffer, 0, bytes);
        }

}

private static String contentType(String nombreArchivo) {

        if(nombreArchivo.endsWith(".htm") || nombreArchivo.endsWith(".html")) {
                return "text/html";
        }
        if(nombreArchivo.endsWith(".gif")) {
                return "image/gif";
        }
        if(nombreArchivo.endsWith(".jpeg")) {
                return "image/jpeg";
        }
        return "application/octet-stream";

}

}
