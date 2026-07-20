package com.example.mybatis;

/**
 * 用户实体类
 * 对应数据库表 users
 */
public class User {
    private Integer id;
    private String username;
    private String email;
    private Integer age;
    private String city;

    public User() {}

    public User(String username, String email, Integer age, String city) {
        this.username = username;
        this.email = email;
        this.age = age;
        this.city = city;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + '\'' +
               ", email='" + email + '\'' + ", age=" + age +
               ", city='" + city + '\'' + '}';
    }
}
