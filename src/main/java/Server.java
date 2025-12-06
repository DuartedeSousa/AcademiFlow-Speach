import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class Server {

    private static Connection con;

    public static void main(String[] args) throws Exception {

        //Conexão com o SQLite
        con = DriverManager.getConnection("jdbc:sqlite:content.db");

        //Criar tabela
        String sql = "CREATE TABLE IF NOT EXISTS dados (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "materia TEXT," +
                "descricao TEXT," +
                "data TEXT," +
                "participacao TEXT" +
                ")";
        con.createStatement().execute(sql);

        //Criar o servidor
        HttpServer s = HttpServer.create(new InetSocketAddress(8080), 0);

        //Rotas
        s.createContext("/", t -> enviar(t, "index.html"));
        s.createContext("/cadastro", t -> enviar(t, "cadastro.html"));
        s.createContext("/sobre", t -> enviar(t, "sobre.html"));
        s.createContext("/errosenha", t -> enviar(t, "errosenha.html"));
        s.createContext("/errousuario", t -> enviar(t, "errousuario.html"));
        s.createContext("/style.css", t -> enviarCSS(t, "style.css"));
        s.createContext("/global.css", t -> enviarCSS(t, "global.css"));

        s.createContext("/login", Server::login);
        //s.createContext("/criarcadastro", Server::criarcadastro);
        s.createContext("/envio", Server::envio);
        s.createContext("/aluno", Server::aluno);
        s.createContext("/professor", Server::professor);
        s.createContext("/participar", Server::participar);
        s.createContext("/deletar", Server::deletar);
        s.createContext("/editar", Server::editar);


        s.start();
        System.out.println("Servidor em http://localhost:8080/");
    }

        //Login------------------------------------------------

        private static void login(HttpExchange t) throws IOException {
            if (!t.getRequestMethod().equalsIgnoreCase("POST")){
                enviar(t, "index.html");
                return;
            }

            System.out.println("testsad");

            String corpo = ler(t);
            corpo = URLDecoder.decode(corpo, StandardCharsets.UTF_8);

            String query = corpo;
            String[] partes;
            partes = query.split("&");

            String usuario;
            String senha;


            usuario = partes[0].replace("usuario=", "");
            senha = partes[1].replace("senha=", "");

            System.out.println("Usuario ss  " + usuario + senha);

            if(usuario.equals("Duarte")) {
                if (senha.equals("123")) {
                    if (corpo.contains("envio")){
                        redirecionar(t, "/envio");
                    } else {
                        redirecionar(t, "/aluno");
                    }
                } else {
                    redirecionar(t, "/errosenha");
                }
            } else {
                redirecionar(t, "/errousuario");
            }

        }

        //Envio---------------------------------------------

        private static void envio(HttpExchange t) throws IOException {

            if (!t.getRequestMethod().equalsIgnoreCase("POST")) {
                enviar(t, "envio.html");
                return;
            }

            String c = URLDecoder.decode(ler(t), StandardCharsets.UTF_8);

            String materia = pega(c, "materia");
            String desc = pega(c, "descricao");
            String data = pega(c, "data");

            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO dados (materia, descricao, data, participacao) VALUES (?,?,?,?)")){

                ps.setString(1,materia);
                ps.setString(2, desc);
                ps.setString(3, data);
                ps.setString(4, "nenhuma");
                ps.executeUpdate();



            } catch (SQLException e) {
                e.printStackTrace();
            }

            redirecionar(t, "/envio");
        }

        //Aluno--------------------------------------------------------------------------------------------

        private static void aluno(HttpExchange t) throws IOException {
            StringBuilder html = new StringBuilder();

            System.out.println("aqui 1");

            html.append("<!DOCTYPE html>");
            html.append("<html><head>");
            html.append("<meta charset=\"UTF-8\">");
            html.append("<link rel=\"stylesheet\" href=\"/style.css\">");
            html.append("<title>AcademiFlow | Aluno</title>");
            html.append("</head><body>");

            try (Statement st = con.createStatement();

                 ResultSet rs = st.executeQuery("SELECT id, materia, descricao, data, participacao FROM dados ORDER BY id DESC")) {
                System.out.println("aqui 2");

                boolean vazio = true;
                System.out.println("aqui 3");

                while (rs.next()) {
                    vazio = false;

                    int id = rs.getInt("id");
                    String materia = rs.getString("materia");
                    String desc = rs.getString("descricao");
                    String data = rs.getString("data");
                    String participacao = rs.getString("participacao");


                    System.out.println("SELE " + materia);


                    //Mudar cor do card
                    String classeExtra = "";
                    if ("participar".equals(participacao)) {
                        classeExtra = " aluno-participando";
                    } else if ("nao-participar".equals(participacao)) {
                        classeExtra = " aluno-nao-participando";
                    }

                    html.append("<div class=\"card").append(classeExtra).append("\">");
                    html.append("<p><strong>ID:</strong> ").append(id).append("</p>");
                    html.append("<p><strong>Matéria:</strong> ").append(materia).append("</p>");
                    html.append("<p><strong>Descrição:</strong> ").append(desc).append("</p>");
                    html.append("<p><strong>Data:</strong> ").append(data).append("</p>");
                    html.append("<p><strong>Paticipação:</strong> ").append(participacao).append("</p>");

                    //Botão de Participar
                    html.append("<form method=\"POST\" action=\"/participar\">");
                    html.append("<input type=\"hidden\" name=\"id\" value=\"").append(id).append("\">");
                    html.append("<input type=\"hidden\" name=\"acao\" value=\"participar\">");
                    html.append("<button type=\"submit\">Participar</button>");
                    html.append("</form>");

                    //Botão de Não Participar
                    html.append("<form method=\"POST\" action=\"/participar\">");
                    html.append("<input type=\"hidden\" name=\"id\" value=\"").append(id).append("\">");
                    html.append("<input type=\"hidden\" name=\"acao\" value=\"nao-participar\">");
                    html.append("<button type=\"submit\">Não Participar</button>");
                    html.append("</form>");


                    html.append("</div>");
                }

                if (vazio) {
                    html.append("<p>Nenhuma atividade enviada ainda.</p>");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                html.append("<p>Erro ao carregar atividades.</p>");
            }

            html.append("</body><html>");

            //Enviar HTML criado
            byte[] b = html.toString().getBytes(StandardCharsets.UTF_8);
            t.getResponseHeaders().add("Contet-Type", "text/html; charset=UTF-8");
            t.sendResponseHeaders(200, b.length);
            t.getResponseBody().write(b);
            t.close();
        }


    //Professor--------------------------------------------------------------------------------------------

    private static void professor(HttpExchange t) throws IOException {
        StringBuilder html = new StringBuilder();

        System.out.println("aqui 1");

        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<link rel=\"stylesheet\" href=\"./style.css\">");
        html.append("<title>AcademiFlow | Aluno</title>");
        html.append("</head><body>");

        try (Statement st = con.createStatement();

             ResultSet rs = st.executeQuery("SELECT id, materia, descricao, data, participacao FROM dados ORDER BY id DESC")) {
            System.out.println("aqui 2");

            boolean vazio = true;
            System.out.println("aqui 3");

            while (rs.next()) {
                vazio = false;

                int id = rs.getInt("id");
                String materia = rs.getString("materia");
                String desc = rs.getString("descricao");
                String data = rs.getString("data");
                String participacao = rs.getString("participacao");


                System.out.println("SELE " + materia);


                //Mudar cor do card
                String classeExtra = "";
                if ("participar".equals(participacao)) {
                    classeExtra = " aluno-participando";
                } else if ("nao-participar".equals(participacao)) {
                    classeExtra = " aluno-nao-participando";
                }

                html.append("<div class=\"card").append(classeExtra).append("\">");
                html.append("<p><strong>ID:</strong> ").append(id).append("</p>");
                html.append("<p><strong>Matéria:</strong> ").append("</p>");

                //html.append("<p><strong>Descrição:</strong> ").append(desc).append("</p>");
                //html.append("<p><strong>Data:</strong> ").append(data).append("</p>");
                html.append("<p><strong>Paticipação:</strong> ").append(participacao).append("</p>");

                //Botão para Editar
                html.append("<form method=\"POST\" action=\"/editar\">");
                html.append("<input type=\"hidden\" name=\"id\" value=\"").append(id).append("\">");


                html.append("<input type=\"text\" name=\"materia\" value=\"").append(materia).append("\">");
                html.append("<input type=\"text\" name=\"descricao\" value=\"").append(desc).append("\">");
                html.append("<input type=\"date\" name=\"data\" value=\"").append(data).append("\">");
                html.append("<button type=\"submit\">Editar</button>");
                html.append("</form>");

                //Botão para Deletar
                html.append("<form method=\"POST\" action=\"/deletar\">");
                html.append("<input type=\"hidden\" name=\"id\" value=\"").append(id).append("\">");
                html.append("<input type=\"hidden\" name=\"acao\" value=\"nao\">");
                html.append("<button type=\"submit\">Deletar</button>");
                html.append("</form>");

                html.append("</div>");
            }

            if (vazio) {
                html.append("<p>Nenhuma atividade enviada ainda.</p>");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            html.append("<p>Erro ao carregar atividades.</p>");
        }

        html.append("</body><html>");

        //Enviar HTML criado
        byte[] b = html.toString().getBytes(StandardCharsets.UTF_8);
        t.getResponseHeaders().add("Contet-Type", "text/html; charset=UTF-8");
        t.sendResponseHeaders(200, b.length);
        t.getResponseBody().write(b);
        t.close();
    }

        //Participar ou não um card específico----------------------------------------------------------------------

        private static void participar(HttpExchange t) throws IOException {

            if (!t.getRequestMethod().equalsIgnoreCase("POST")) {
                redirecionar(t, "/aluno");
                return;
            }

            String corpo = URLDecoder.decode(ler(t), StandardCharsets.UTF_8);
            String acao = pega(corpo, "acao"); //Participar ou não
            String idStr = pega(corpo, "id");

            try {
                int id = Integer.parseInt(idStr);

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE dados SET participacao = ? WHERE id = ?")) {
                    ps.setString(1, acao);
                    ps.setInt(2, id);
                    ps.executeUpdate();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            redirecionar(t, "/aluno");
        }

        //Deletar--------------------------------------------------------------------------

        private static void deletar(HttpExchange t) throws IOException {

            if (!t.getRequestMethod().equalsIgnoreCase("POST")) {
                redirecionar(t, "/professor");
                return;
            }

            String corpo = URLDecoder.decode(ler(t), StandardCharsets.UTF_8);
            String idStr = pega(corpo, "id");

            try {
                int id = Integer.parseInt(idStr);

                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM dados WHERE id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            redirecionar(t, "/professor");
        }

        //Editar--------------------------------------------------------------------------

        private static void editar(HttpExchange t) throws IOException {

            if (!t.getRequestMethod().equalsIgnoreCase("POST")) {
                redirecionar(t, "/professor");
                return;
            }

            String corpo = URLDecoder.decode(ler(t), StandardCharsets.UTF_8);
            String materia = pega(corpo, "materia");
            String desc = pega(corpo, "descricao");
            String data = pega(corpo, "data");
            String idStr = pega(corpo, "id");


            try {
                int id = Integer.parseInt(idStr);

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE dados SET materia = ? WHERE id = ?")) {

                    ps.setString(1, materia);
                    ps.setInt(2, id);
                    ps.executeUpdate();

                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE dados SET descricao = ? WHERE id = ?")) {

                    ps.setString(1, desc);
                    ps.setInt(2, id);
                    ps.executeUpdate();

                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE dados SET data = ? WHERE id = ?")) {

                    ps.setString(1, data);
                    ps.setInt(2, id);
                    ps.executeUpdate();

                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            redirecionar(t, "/professor");
        }


        //Enviar Imagens-------------------------------------------------------------------------------

        private static void enviarImagem(HttpExchange t, String arquivo) throws IOException {
            File f = new File("src/main/java" + arquivo);

            byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
            t.getResponseHeaders().add("Content-Type", "imagem/png");
            t.sendResponseHeaders(200, bytes.length);
            t.getResponseBody().write(bytes);
            t.close();
        }

        //Funções auxiliares--------------------------------------------------------------

        private static String pega(String corpo, String campo) {

            //corpo no formato: campo1=valor1&campo2=valor2...
            for (String s : corpo.split("&")) {
                String[] p = s.split("=");
                if (p.length == 2 && p[0].equals(campo)) return p[1];
            }
            return "";
        }

        private static String ler(HttpExchange t) throws IOException {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(t.getRequestBody(), StandardCharsets.UTF_8)
            );
            String linha = br.readLine();
            return (linha == null) ? "" : linha;
        }

        private static void enviar(HttpExchange t, String arq) throws IOException {
            File f = new File("src/main/java/" + arq);
            System.out.println("Arq " + f);
            byte[] b = java.nio.file.Files.readAllBytes(f.toPath());
            t.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            t.sendResponseHeaders(200, b.length);
            t.getResponseBody().write(b);
            t.close();
        }

        private static void enviarCSS(HttpExchange t, String arq) throws IOException {
            File f = new File("src/main/java/styles/" + arq);
            System.out.println("CSS " + f);
            byte[] b = java.nio.file.Files.readAllBytes(f.toPath());
            t.getResponseHeaders().add("Content-Type", "text/css; charset=UTF-8");
            t.sendResponseHeaders(200, b.length);
            t.getResponseBody().write(b);
            t.close();
        }

        private static void redirecionar(HttpExchange t, String rota) throws IOException {
            t.getResponseHeaders().add("Location", rota);
            t.sendResponseHeaders(302, -1);
            t.close();
        }

}

