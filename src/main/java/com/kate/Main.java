package com.kate;

import com.kate.dao.EquipmentDaoImpl;
import com.kate.dao.EquipmentLogDao;
import com.kate.dao.UserDaoImpl;
import com.kate.service.EquipmentLogService;
import com.kate.service.EquipmentService;
import com.kate.service.UserService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.http.UploadedFile;
import io.javalin.rendering.template.JavalinThymeleaf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.ServletOutputStream;
import model.*;
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
        EquipmentService equipmentService = new EquipmentService(new EquipmentDaoImpl(c));
        EquipmentLogService equipmentLogService = new EquipmentLogService(new EquipmentLogDao(c));
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
                        context.redirect("/equipments");
                        return;
                    }
                    context.render("/templates/login.htm", Map.of("status", "login failed"));
                })
                .get("/index", context -> {
                    checkAuthentication(context, userService);
                    context.render("/templates/index.htm", Map.of());
                })
                .get("/users", ctx -> {
                    checkAdmin(ctx, userService);
                    ctx.render("/templates/users.htm", Map.of("users", userService.getAll()));
                })
                .get("/edit_user_nickname", ctx -> {
                    checkAdmin(ctx, userService);
                    String nickname = ctx.queryParam("nickname");
                    ctx.render("/templates/edit_user_nickname.htm", Map.of("user", userService.get(new User(nickname, null, null))));
                })
                .post("/edit_user_nickname", ctx -> {
                    checkAdmin(ctx, userService);
                    String nickname = ctx.formParam("nickname");
                    String oldNickname = ctx.formParam("old_nickname");
                    userService.changeNickname(oldNickname, nickname);
                    ctx.redirect("/users");
                })

                .get("/edit_user_role", ctx -> {
                    checkAdmin(ctx, userService);
                    String nickname = ctx.queryParam("nickname");
                    User user = userService.get(new User(nickname, null, null));
                    ctx.render("/templates/edit_user_role.htm", Map.of("user", user));
                })
                .post("/edit_user_role", ctx -> {
                    checkAdmin(ctx, userService);
                    String nickname = ctx.formParam("nickname");
                    String role = ctx.formParam("role");
                    User user = userService.get(new User(nickname, null, null));
                    user.setRole(Role.mapCode(Integer.parseInt(role)));
                    userService.update(user);
                    ctx.redirect("/users");
                })

                .get("/edit_user_password", ctx -> {
                    checkAdmin(ctx, userService);
                    String nickname = ctx.queryParam("nickname");
                    ctx.render("/templates/edit_user_password.htm", Map.of("user", userService.get(new User(nickname, null, null))));
                })
                .post("/edit_user_password", ctx -> {
                    checkAdmin(ctx, userService);
                    String nickname = ctx.formParam("nickname");
                    String password = ctx.formParam("password");
                    User user = userService.get(new User(nickname, null, null));
                    user.setPassword(sha256(password));
                    userService.update(user);
                    ctx.redirect("/users");
                })

                .get("/delete_user", ctx -> {
                    checkAdmin(ctx, userService);
                    String nickname = ctx.queryParam("nickname");
                    ctx.render("/templates/delete_user.htm", Map.of("user", userService.get(new User(nickname, null, null))));
                })
                .post("/delete_user", ctx -> {
                    checkAdmin(ctx, userService);
                    String nickname = ctx.formParam("nickname");
                    userService.delete(new User(nickname, null, null));
                    ctx.redirect("/users");
                })

                .get("/add_user", ctx -> {
                    checkAdmin(ctx, userService);
                    ctx.render("/templates/add_user.htm", Map.of());
                })
                .post("/add_user", context -> {
                    checkAdmin(context, userService);
                    String login = context.formParam("login");
                    String password = sha256(context.formParam("password"));
                    Role role = Role.mapCode(Integer.parseInt(context.formParam("role")));
                    userService.create(new User(login, password, role));
                    context.redirect("/templates/users");
                })

                .get("/equipments", ctx -> {
                    User user = authenticatedUser(ctx, userService);
                    String rawUser = ctx.queryParam("user");
                    List<EquipmentDto> all;
                    if (rawUser != null) {
                        all = equipmentService.getAllForUser(rawUser);
                    } else {
                        all = equipmentService.getAll();
                    }
                    ctx.render("/templates/equipments.htm", Map.of("equipments", all, "admin", Role.ADMIN.equals(user.getRole())));
                })
                .get("/add_equipment", ctx -> {
                    checkAuthentication(ctx, userService);
                    ctx.render("/templates/add_equipment.htm");
                })
                .post("/equipments", context -> {
                    checkAuthentication(context, userService);
                    String name = context.formParam("name");
                    String description = context.formParam("description");
                    String imagePath = context.formParam("image_path");
                    equipmentService.create(new Equipment(null, name, description, imagePath));
                    context.redirect("/equipments");
                })
                .get("/edit_equipment", context -> {
                    checkAuthentication(context, userService);
                    String id = context.queryParam("id");
                    Equipment equipment = equipmentService.get(new Equipment(Integer.valueOf(id)));
                    context.render("/templates/edit_equipment.htm", Map.of("equipment", equipment));
                })
                .post("/edit_equipment", context -> {
                    checkAuthentication(context, userService);
                    Integer id = Integer.valueOf(context.formParam("id"));
                    String name = context.formParam("name");
                    String description = context.formParam("description");
                    UploadedFile file = context.uploadedFile("file");
                    String imagePath = equipmentService.get(new Equipment(id)).getImagePath(); // Оставляем старый imagePath
                    if (file != null) {
                        Path uploadDir = Path.of("uploads/equipment_images/");
                        Files.createDirectories(uploadDir);
                        String filename = id + "_" + file.filename();
                        Path uploadPath = uploadDir.resolve(filename);
                        Files.copy(file.content(), uploadPath, StandardCopyOption.REPLACE_EXISTING);
                        imagePath = filename; // Обновляем imagePath
                        equipmentService.updateImagePath(id, filename);
                    }
                    Equipment equipment = equipmentService.update(new Equipment(id, name, description, imagePath));
                    context.redirect("/equipments");
                })
                .get("/delete_equipment", context -> {
                    checkAuthentication(context, userService);
                    String id = context.queryParam("id");
                    Equipment equipment = equipmentService.get(new Equipment(Integer.valueOf(id)));
                    context.render("/templates/delete_equipment.htm", Map.of("equipment", equipment));
                })
                .post("/delete_equipment", context -> {
                    checkAuthentication(context, userService);
                    String id = context.formParam("id");
                    equipmentService.delete(new Equipment(Integer.valueOf(id)));
                    context.redirect("/equipments");
                })
                .get("/give_equipment", context -> {
                    checkAdmin(context, userService);
                    String id = context.queryParam("id");
                    Equipment equipment = equipmentService.get(new Equipment(Integer.valueOf(id)));
                    context.render("/templates/give_equipment.htm", Map.of("equipment", equipment));
                })
                .post("/give_equipment", context -> {
                    checkAdmin(context, userService);
                    String userName = context.formParam("user");
                    String formIds = context.formParam("equipment_ids");
                    String[] rawIds = formIds.split(",");
                    List<Equipment> equipment = new ArrayList<>();
                    for (String rawId : rawIds) {
                        equipment.add(equipmentService.get(new Equipment(Integer.valueOf(rawId))));
                    }
                    User user = userService.get(new User(userName, null));
                    if (user == null) {
                        throw new IllegalArgumentException("user " + userName + " not found");
                    }
                    equipmentLogService.give(equipment, user);
                    context.redirect("/equipments");
                })
                .get("/release_equipment", context -> {
                    checkAdmin(context, userService);
                    String rawId = context.queryParam("id");
                    Integer id = Integer.valueOf(rawId);
                    Equipment equipment = equipmentService.get(new Equipment(id));
                    EquipmentLogRecord state = equipmentLogService.getLast(id);
                    context.render("/templates/release_equipment.htm", Map.of("equipment", equipment, "state", state));
                })
                .post("/release_equipment", context -> {
                    checkAdmin(context, userService);
                    String user = context.formParam("user");
                    String id = context.formParam("equipment_id");
                    Equipment equipment = equipmentService.get(new Equipment(Integer.parseInt(id)));
                    equipmentLogService.release(equipment, userService.get(new User(user, null)));
                    context.redirect("/equipments");
                })

                .get("/equipment_logs", ctx -> {
                    User user = authenticatedUser(ctx, userService);
                    String rawId = ctx.queryParam("equipment_id");
                    String rawUser = ctx.queryParam("user");
                    List<EquipmentLogDto> all;
                    if (rawUser != null && rawId != null) {
                        throw new IllegalArgumentException("user and equipment_id params specified, required only one of them");
                    }
                    if (rawUser != null) {
                        all = equipmentLogService.getLogForUser(rawUser);
                    } else if (rawId != null) {
                        Integer id = Integer.valueOf(rawId);
                        all = equipmentLogService.getLogForEquipment(id);
                    } else {
                        all = equipmentLogService.getAll();
                    }
                    ctx.render("/templates/equipment_logs.htm", Map.of("equipment_logs", all, "admin", Role.ADMIN.equals(user.getRole())));
                })
                .get("/uploads/equipment_images/{file}", ctx -> {
                    String id = ctx.pathParam("file");
                    Path filePath = Path.of("uploads/equipment_images/" + id);
                    ServletOutputStream stream = ctx.outputStream();
                    Files.copy(filePath, stream);
                    stream.flush();
                })
        .post("/upload_image", ctx -> {
            checkAuthentication(ctx, userService);
            UploadedFile file = ctx.uploadedFile("file");
            String equipmentId = ctx.formParam("equipment_id");
            if (file != null && equipmentId != null) {
                try {
                    Path uploadDir = Path.of("resources/uploads/equipment_images/");

                    // Генерируем уникальное имя файла
                    String filename = equipmentId + "_" + file.filename();
                    Path uploadPath = uploadDir.resolve(filename);

                    // Сохраняем файл
                    Files.copy(file.content(), uploadPath, StandardCopyOption.REPLACE_EXISTING);

                    // Обновляем image_path в базе данных
                    equipmentService.updateImagePath(Integer.parseInt(equipmentId), filename);

                    ctx.redirect("/equipments");
                } catch (IOException e) {
                    ctx.status(500).result("Ошибка загрузки файла: " + e.getMessage());
                }
            } else {
                ctx.status(400).result("Ошибка: не указан файл или ID оборудования.");
            }
        })

                .exception(Exception.class, (e, ctx) -> {
                    ctx.render("/templates/error.htm", Map.of("message", e.getMessage()));
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