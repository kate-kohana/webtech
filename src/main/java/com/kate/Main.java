package com.kate;

import com.kate.dao.UserDaoImpl;
import com.kate.service.UserService;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) {

        Connection c = null;
        Statement stmt = null;

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:usersdb.db");
            c.setAutoCommit(false);

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }

        UserService userService = new UserService(new UserDaoImpl(c));
        var app = Javalin.create(
                config -> {
                    config.fileRenderer(new JavalinThymeleaf());
                    config.staticFiles.add("static");
                }
        )
                .start(8080);
    }
}