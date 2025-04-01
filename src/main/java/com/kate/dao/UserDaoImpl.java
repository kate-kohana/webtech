package com.kate.dao;

import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDaoImpl implements UserDao{

    Connection c;

    public UserDaoImpl(Connection c) {
        this.c = c;
    }

    @Override
    public void create(User user) {
        PreparedStatement stmt;
        try {
            stmt = c.prepareStatement("INSERT INTO users (nickname, password) values (?, ?)");
            stmt.setString(1, user.getNickname());
            stmt.setString(2, user.getPassword());
            stmt.executeUpdate();
            stmt.close();
            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
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
                receivedUser.setNickname( rst.getString("nickname"));
                receivedUser.setPassword( rst.getString("password"));
                return receivedUser;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }

        return null;
    }

    @Override
    public void delete(User user) {
        PreparedStatement stmt;
        try {
            stmt = c.prepareStatement("DELETE FROM users WHERE nickname=?");
            stmt.setString(1, user.getNickname());
            stmt.executeUpdate();
            stmt.close();
            c.commit();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public User update(User user) {
        PreparedStatement stmt;
        try {
            stmt = c.prepareStatement("UPDATE users SET password=? where nickname=?");
            stmt.setString(1, user.getPassword());
            stmt.setString(2, user.getNickname());
            stmt.executeUpdate();
            stmt.close();
            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
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
                receivedUser.setNickname( rst.getString("nickname"));
                receivedUser.setPassword( rst.getString("password"));
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
}
