package iuh.fit.airsky.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class MembershipCodeGenerator {

    private static final String PREFIX = "AK";
    private static final int CODE_LENGTH = 10;
    private static final SecureRandom random = new SecureRandom();

    /**
     * Sinh mã hội viên duy nhất với format AK + 10 số ngẫu nhiên
     * @return membership code unique
     */
    public static String generateMembershipCode() {
        StringBuilder code = new StringBuilder(PREFIX);

        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10)); // 0-9
        }

        return code.toString();
    }

    /**
     * Validate format của membership code
     * @param code mã cần validate
     * @return true nếu format hợp lệ
     */
    public static boolean isValidFormat(String code) {
        if (code == null || code.length() != PREFIX.length() + CODE_LENGTH) {
            return false;
        }

        if (!code.startsWith(PREFIX)) {
            return false;
        }

        // Kiểm tra phần số có phải là digits không
        String numberPart = code.substring(PREFIX.length());
        return numberPart.matches("\\d{" + CODE_LENGTH + "}");
    }
}