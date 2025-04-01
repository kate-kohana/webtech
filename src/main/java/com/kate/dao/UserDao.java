package com.kate.dao;

import model.User;

import java.util.List;

public interface UserDao {
    void create(User user);

    User get(User user);

    void delete(User user);

    User update(User user);

    List<User> getAll();

    void changeNickname(String oldNickname, String nickname);
}