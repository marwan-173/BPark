package server;

public class EmailTest {
    public static void main(String[] args) {
        try {
            EmailSender.sendRecoveryEmail("test@example.com", "123456");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
