package main.java;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class Server {

    private static Connection con;

    public static void main(String[] args) throws Exception{

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
        s.createContext("/", t -> enviar(t,"index.html"));
        s.createContext("/cadastro", t -> enviar(t,"cadastro.html"));
        s.createContext("/sobre", t -> enviar(t,"sobre.html"));
        s.createContext("/style.css", t -> enviarCSS(t,"style.css"));
        s.createContext("/global.css", t -> enviarCSS(t,"global.css"));

        s.createContext("/login", Server::login);
        s.createContext("/criarcadastro", Server::criarcadastro);
        s.createContext("/envio", Server::envio);
        s.createContext("/aluno", Server::aluno);
        s.createContext("/participar", Server::participar);
        s.createContext("/deletar", Server::deletar);


        s.start();
        System.out.println("Servidor em http://localhost:8080/");

        //Login------------------------------------------------

        private static void login(HttpExchange t) throws IOException {
            if (!t.getResquestMethod().equalsIgnoreCase("POST")){
                enviar(t, "index.html");
                return;
            }

            String corpo = ler(t);
            corpo = URLDecoder.decode(corpo, StandardCharsets.UTF_8);

            if (corpo.contains("envio")){
                redirecionar(t, "/envio");
            } else {
                redirecionar(t, "/aluno");
            }
        }

        //Envio---------------------------------------------

        private static void envio(httpExchange t) throws IOException {

            if (!t.getRequestMethod().esqualsIgnoreCase("POST")) {
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

            } catch (SQLException e) {
                e.printStackTrace();
            }

            redirecionar(t, "/envio");
        }

        //Aluno--------------------------------------------------------------------------------------------

        private static void aluno(HttpExchange t) throws IOException {
            StringBuilder html = new StringBuilder();

            html.append("<!DOCTYPE html>");
            html.append("<html><head>");
            html.append("<meta charset=\"UTF-8\">");
            html.append("<title>AcademiFlow | Aluno</title>");
            html.append("link rel=\"stylesheet\" href=\"/style.css\">");
            html.append("</head><body>");

            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, materia, descricao, data, participacao FROM dados ORDER BY id DESC")) {

                boolean vazio = true;

                while (rs.next()) {
                    vazio = false;

                    int id = rs.getInt("id");
                    String materia = rs.getString("materia");
                    String desc = rs.getString("descricao");
                    String data = rs.getString("data");
                    String participacao = rs.getString("participacao");

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
                }
            }

        }
    }
}
