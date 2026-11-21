package vn.vuongquoctuyet.hocchu;

import java.security.SecureRandom;

final class ParentGate {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String[] WORDS = {"Không", "Một", "Hai", "Ba", "Bốn", "Năm", "Sáu", "Bảy", "Tám", "Chín"};
    private static int previous = -1;
    private String code;
    private boolean unlocked;

    ParentGate() { reset(); }

    String prompt() {
        return WORDS[code.charAt(0) - '0'] + " · " + WORDS[code.charAt(1) - '0'] + " · " + WORDS[code.charAt(2) - '0'];
    }

    boolean submit(String digits) {
        if (unlocked) return false;
        if (code.equals(digits)) {
            unlocked = true;
            return true;
        }
        reset();
        return false;
    }

    boolean unlocked() { return unlocked; }

    private void reset() {
        int next;
        synchronized (RANDOM) {
            do { next = RANDOM.nextInt(1000); } while (next == previous);
            previous = next;
        }
        code = String.valueOf(next + 1000).substring(1);
    }
}
