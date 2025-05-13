package model;

public class User {

    private String nickname;
    private String password;
    private Role role;

    public User(String login, String password, Role role) {
        this.nickname = login;
        this.password = password;
        this.role = role;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public User(String nickname, String password) {
        this.nickname = nickname;
        this.password = password;
    }

    public User() {
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "User{" +
                "nickname='" + nickname + '\'' +
                ", password='" + password + '\'' +
                ", role=" + role +
                '}';
    }
}
