package Server;

public interface AuthService {
    /**
     *
     * @return - nickname если пользователь есть, null если пользователья нет
     */
    String getNicknameByLoginAndPassword(String login, String password);

    boolean registration(String login, String password, String nickname);

    boolean changeNick(String oldNickname, String newNickname);

}
