package com.bk.cloud.server;

import java.sql.*;

public class SQLHandler {
    private static Connection connection;
    private static PreparedStatement psGetNickname;
    private static PreparedStatement psRegistration;
    private static PreparedStatement psGetLogin;

    private static PreparedStatement psAddMessage;
    private static PreparedStatement psGetRootDir;


    public static boolean connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:MyCloud.db");
            prepareAllStatements();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void prepareAllStatements() throws SQLException {
        psGetNickname = connection.prepareStatement("SELECT nickname FROM users WHERE login = ? AND pswd = ?;");
        psRegistration = connection.prepareStatement("INSERT INTO users(login, pswd, nickname) VALUES (? ,? ,? );");
        psGetRootDir = connection.prepareStatement("SELECT dir FROM users WHERE login = ? AND pswd = ?;");
    }

    public static String getNicknameByLoginAndPassword(String login, String password) {
        String nick = null;
        try {
            psGetNickname.setString(1, login);
            psGetNickname.setString(2, password);
            ResultSet rs = psGetNickname.executeQuery();
            if (rs.next()) {
                nick = rs.getString(1);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nick;
    }

    public static String getRootDirByLoginAndPassword(String login, String password) {
        String rootDir = null;
        try {
            psGetRootDir.setString(1, login);
            psGetRootDir.setString(2, password);
            ResultSet rs = psGetRootDir.executeQuery();
            if (rs.next()) {
                rootDir = rs.getString(1);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rootDir;
    }

    public static boolean registration(String login, String password, String nickname) {
        try {
            psRegistration.setString(1, login);
            psRegistration.setString(2, password);
            psRegistration.setString(3, nickname);
            psRegistration.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void disconnect() {
        try {
            psRegistration.close();
            psGetNickname.close();
            psAddMessage.close();
            psGetRootDir.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

}
