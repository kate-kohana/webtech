package com.kate.dao;


import model.Role;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDaoImpl implements UserDao {
    Connection c;

    @Override
    public void create(User user) {
        String sql = "INSERT INTO users (nickname, password, role) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getNickname());
            stmt.setString(2, user.getPassword());
            stmt.setInt(3, user.getRole().getCode());
            stmt.executeUpdate();

            // Получаем сгенерированный ID
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1)); // Устанавливаем ID обратно в объект User
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create user", e);
        }
    }

    @Override
    public User get(User user) {
        System.out.println("user = " + user);
        PreparedStatement stmt;

        try {
            String sql = "SELECT * FROM users WHERE nickname=?";
            stmt = c.prepareStatement(sql);
            stmt.setString(1, user.getNickname());


            ResultSet rst = stmt.executeQuery();
            while (rst.next())
            {
                User receivedUser = new User();
                receivedUser.setId(rst.getInt("id"));
                receivedUser.setNickname( rst.getString("nickname"));
                receivedUser.setPassword( rst.getString("password"));
                receivedUser.setRole(Role.mapCode(rst.getInt("role")));
                return receivedUser;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return null;
    }

    @Override
    public void delete(User user) {
        System.out.println("user = " + user);

        try {
            PreparedStatement stmt;
            stmt = c.prepareStatement("DELETE FROM users WHERE nickname=?");
            stmt.setString(1, user.getNickname());
            stmt.executeUpdate();
            stmt.close();
            c.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public User update(User user) {
        try {
            PreparedStatement stmt;
            stmt = c.prepareStatement("UPDATE users SET password=?, role=? where nickname=?");
            stmt.setString(1, user.getPassword());
            stmt.setInt(2, user.getRole().getCode());
            stmt.setString(3, user.getNickname());
            stmt.executeUpdate();
            stmt.close();
            c.commit();
            return user;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<User> getAll() {
        PreparedStatement stmt;
        List<User> result = new ArrayList<>();
        try {
            String sql = "SELECT * FROM users";
            stmt = c.prepareStatement(sql);

            ResultSet rst = stmt.executeQuery();
            while (rst.next())
            {
                User receivedUser = new User();
                receivedUser.setId(rst.getInt("id"));
                receivedUser.setNickname( rst.getString("nickname"));
                receivedUser.setPassword( rst.getString("password"));
                receivedUser.setRole( Role.mapCode(rst.getInt("role")));
                result.add(receivedUser);
            }
            return result;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void changeNickname(String oldNickname, String nickname) {
        try {
            PreparedStatement stmt;
            stmt = c.prepareStatement("UPDATE users SET nickname=? where nickname=?");
            stmt.setString(1, nickname);
            stmt.setString(2, oldNickname);
            stmt.executeUpdate();
            stmt.close();
            c.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public UserDaoImpl(Connection c) {
        this.c = c;
    }

}
