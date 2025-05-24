package model;

public class User {

    private Integer id;
    private String nickname;
    private String password;
    private Role role;

    public User(String login, String password, Role role) {
        this.nickname = login;
        this.password = password;
        this.role = role;
    }

    public User(String nickname, String password){
        this.nickname = nickname;
        this.password = password;
    }

    public  Integer getId() { return id;};

    public void setId(Integer id) { this.id=id;};

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

    public User(Integer id, String nickname, String password, Role role) {
        this.id = id;
        this.nickname = nickname;
        this.password = password;
        this.role = role;
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
                "id='" + id + '\'' +
                "nickname='" + nickname + '\'' +
                ", password='" + password + '\'' +
                ", role=" + role +
                '}';
    }
}
