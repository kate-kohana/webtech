package com.kate;

import com.kate.dao.UserDaoImpl;
import com.kate.service.UserService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.rendering.template.JavalinThymeleaf;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.Objects;

import model.Role;
import model.User;
import org.mindrot.jbcrypt.BCrypt;

import static org.eclipse.jetty.util.component.LifeCycle.start;

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
                .get("/", ctx -> {
                    ctx.render("/templates/login.htm", Map.of());
                })
                .get("/login", ctx -> ctx.render("/templates/login.htm", Map.of()))
                .get("/registration", ctx -> ctx.render("/templates/registration.htm", Map.of()))
                .post("/register_user", context -> {
                    String login = context.formParam("login");
                    String password = sha256(context.formParam("password"));
                    Role role = userService.getAll().isEmpty() ? Role.ADMIN : Role.USER;
                    User authenticatedUser = authenticatedUser(context, userService);
                    if (authenticatedUser != null && Role.ADMIN.equals(authenticatedUser.getRole())) {
                        String rawRole = context.formParam("role");
                        if (rawRole != null) {
                            role = Role.mapCode(Integer.parseInt(rawRole));
                        }
                    }
                    userService.create(new User(login, password, role));
                    context.render("/templates/login.htm", Map.of("status", "registration successful"));
                })
                .post("/login_user", context -> {
                    String login = context.formParam("login");
                    String password = sha256(context.formParam("password"));
                    User user = userService.get(new User(login, null));
                    if (user == null) {
                        context.render("/templates/login.htm", Map.of("status", "user not exists"));
                        return;
                    }
                    if (Objects.equals(password, user.getPassword())) {
                        context.cookie("login", login);
                        context.cookie("password", password);
                        context.redirect("/competitors");
                        return;
                    }
                    context.render("/templates/login.htm", Map.of("status", "login failed"));
                })
                .start(8080);
    }
    public static User authenticatedUser(Context context, UserService userService) {
        String login = context.cookie("login");
        if (login == null) {
            return null;
        }
        String password = context.cookie("password");
        User user = userService.get(new User(login, null));
        if (user == null) {
            return null;
        }
        if (!Objects.equals(password, user.getPassword())) {
            throw new UnauthorizedResponse("access denied");
        }
        return user;
    }

    public static void checkAdmin(Context context, UserService userService) {
        String login = context.cookie("login");
        String password = context.cookie("password");
        User user = userService.get(new User(login, null));
        if (!Objects.equals(password, user.getPassword())) {
            throw new UnauthorizedResponse("access denied");
        }
        if (!Role.ADMIN.equals(user.getRole())) {
            throw new UnauthorizedResponse("access denied");
        }
    }

    public static void checkAuthentication(Context context, UserService userService) {
        String login = context.cookie("login");
        String password = context.cookie("password");
        User user = userService.get(new User(login, null));
        if (!Objects.equals(password, user.getPassword())) {
            context.redirect("/login");
            //throw new UnauthorizedResponse("access denied");

        }
    }

    public static String sha256(String in) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(in.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}