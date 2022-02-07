package com.bk.cloud.server;

public class DbAuthService implements AuthService {

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        return SQLHandler.getNicknameByLoginAndPassword(login, password);
    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        return SQLHandler.registration(login, password, nickname);
    }

    @Override
    public String getRootDirByLoginAndPassword(String login, String password) {
        return SQLHandler.getRootDirByLoginAndPassword(login, password);
    }
}
