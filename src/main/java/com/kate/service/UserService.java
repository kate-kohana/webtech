package com.kate.service;

import com.kate.dao.UserDao;
import model.User;

import java.util.List;

public class UserService {

    private UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public void create(User user) {
        userDao.create(user);
    }

    public User get(User user) {
        return userDao.get(user);
    }

    public void delete(User user) {
        userDao.delete(user);
    }

    public User update(User user) {
        return userDao.update(user);
    }

    public List<User> getAll() {
        return userDao.getAll();
    }

    public void changeNickname(String oldNickname, String nickname) {
        userDao.changeNickname(oldNickname, nickname);
    }
}
