import dao.DatabaseConnection;
import dao.UserDao;
import model.User;
import ui.MainMenu;

/** Application entry point. */
public class Main {

    public static void main(String[] args) {
        DatabaseConnection.initialize();
        UserDao userDao = new UserDao();

        // Default manager so the first pending accounts can be approved.
        if (!userDao.usernameExists("admin")) {
            userDao.registerWithStatus("admin", "admin123", User.ROLE_MANAGER, User.STATUS_ACTIVE);
            System.out.println("Default manager created -> username: admin | password: admin123");
        }

        new MainMenu(userDao).run();
    }
}