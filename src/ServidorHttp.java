import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ServidorHttp {



    public static void main(String[] args) throws Exception {

        final String[] usuarioCadastro = new String[1];
        final String[] senhaCadastro = new String[1];
        final String[] cargoCadastro = new String[1];

        // Cria servidor HTTP escutando na porta 8080
        HttpServer servidor = HttpServer.create(new InetSocketAddress(8081), 0);



        /* INICIO DO CÓDIGO */
        servidor.createContext("/", troca->{
            enviarArquivo(troca, "index.html", "text/html");
        });


        servidor.createContext("/cadastro", troca->{
            enviarArquivo(troca, "cadastro.html", "text/html");
        });

        servidor.createContext("/login", troca->{
            String query = troca.getRequestURI().getQuery();

            String[] partes;
            partes = query.split("&");

            String usuario;
            String senha;
            String cargo;


            usuario = partes[0].replace("usuario=", "");
            senha = partes[1].replace("senha=", "");
            cargo = partes[2].replace("cargo=", "");

            if(usuario.equals("cadastroA") && senha.equals("cadastroB")){
                System.out.println("Acesso autorizado");
                System.out.println(usuario);
                System.out.println(senha);
                System.out.println(cargo);

                if(cargo.equals("professor")){
                    troca.getResponseHeaders().set("Location","/sobre");
                    troca.sendResponseHeaders(302, -1);
                }

                else if(cargo.equals("aluno")){
                    troca.getResponseHeaders().set("Location","/sobre");
                    troca.sendResponseHeaders(302, -1);
                }
            }

            else{
                System.out.println("Acesso negado");
            }

        });








        servidor.createContext("/criarcadastro", troca->{
            String query = troca.getRequestURI().getQuery();

            String[] partes;
            partes = query.split("&");



            usuarioCadastro[0] = partes[0].replace("usuario=", "");
            senhaCadastro[0] = partes[1].replace("senha=", "");
            cargoCadastro[0] = partes[2].replace("cargo=", "");

        });





        servidor.createContext("/estilo.css", troca->{
            enviarArquivo(troca, "estilo.css", "text/css");
        });


        /* FIM DO CÓDIGO */

        servidor.start();
        System.out.println("Servidor rodando em http://localhost:8081/");
    }

    // Envia um arquivo (HTML ou CSS)
    private static void enviarArquivo(HttpExchange troca, String caminho, String tipo) throws IOException {
        File arquivo = new File(caminho);
        if (!arquivo.exists()) {
            System.out.println("Arquivo não encontrado: " + arquivo.getAbsolutePath());
        }
        byte[] bytes = Files.readAllBytes(arquivo.toPath());
        troca.getResponseHeaders().set("Content-Type", tipo + "; charset=UTF-8");
        troca.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = troca.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Envia resposta HTML gerada no código
    private static void enviarTexto(HttpExchange troca, String texto) throws IOException {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        troca.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = troca.getResponseBody()) {
            os.write(bytes);
        }
    }
}
