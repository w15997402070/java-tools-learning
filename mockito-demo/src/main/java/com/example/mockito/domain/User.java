package com.example.mockito.domain;

import java.util.Objects;

/**
 * 用户领域模型。
 */
public class User {

    private final Long id;
    private final String name;
    private final String email;
    private final boolean vip;

    public User(Long id, String name, String email, boolean vip) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.vip = vip;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public boolean isVip() {
        return vip;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return vip == user.vip
                && Objects.equals(id, user.id)
                && Objects.equals(name, user.name)
                && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email, vip);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", vip=" + vip +
                '}';
    }
}
