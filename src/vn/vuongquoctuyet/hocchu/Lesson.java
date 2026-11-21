package vn.vuongquoctuyet.hocchu;

import java.text.Normalizer;
import java.util.Locale;

final class Lesson {
    static final Locale VI = Locale.forLanguageTag("vi-VN");
    // Keep the original six positions so saved lessons from version 0.1 still open.
    static final Lesson[] ALL = {
        new Lesson("a", "a", "a", ex("quả |táo|", "🍎", 35), ex("quả |cam|", "🍊", 0), "o"),
        new Lesson("o", "o", "o", ex("con |ong|", "🐝", 1), ex("con |bò|", "🐄", 36), "oo"),
        new Lesson("oo", "ô", "ô", ex("cái |ô|", "☂️", 2), ex("con |ốc|", "🐌", 38), "o"),
        new Lesson("e", "e", "e", ex("|xe| đạp", "🚲", 3), ex("cái |kẹo|", "🍬", 37), "ee"),
        new Lesson("ee", "ê", "ê", ex("con |dê|", "🐐", 4), ex("cái |ghế|", "🪑"), "e"),
        new Lesson("m", "m", "mờ", ex("con |mèo|", "🐱", 5), ex("con |mực|", "🦑"), "n"),

        new Lesson("aw", "ă", "á", ex("mặt |trăng|", "🌙", 6), ex("cái |khăn|", "🧣"), "aa"),
        new Lesson("aa", "â", "ớ", ex("cái |cân|", "⚖️", 7), ex("|sân| chơi", "🛝"), "aw"),
        new Lesson("b", "b", "bờ", ex("|bánh| mì", "🍞", 8), ex("quả |bóng|", "⚽"), "d"),
        new Lesson("c", "c", "cờ", ex("con |cá|", "🐟", 9), ex("con |cáo|", "🦊", 11), "k"),
        new Lesson("d", "d", "dờ", ex("quả |dừa|", "🥥", 10), ex("con |dê|", "🐐", 4), "dd"),
        new Lesson("dd", "đ", "đờ", ex("cái |đèn|", "💡", 30), ex("đôi |đũa|", "🥢"), "d"),
        new Lesson("g", "g", "gờ", ex("con |gà|", "🐔", 12), ex("con |gấu|", "🐻"), "q"),
        new Lesson("h", "h", "hờ", ex("bông |hoa|", "🌼", 13), ex("cái |hộp|", "📦"), "n"),

        new Lesson("i", "i", "i", ex("cái |kim|", "🪡", 31), ex("bút |chì|", "✏️"), "y"),
        new Lesson("k", "k", "ca", ex("thước |kẻ|", "📏", 14), ex("cái |kéo|", "✂️"), "h"),
        new Lesson("l", "l", "lờ", ex("quả |lê|", "🍐", 15), ex("con |lợn|", "🐷", 17), "i"),
        new Lesson("n", "n", "nờ", ex("cái |nón|", "👒", 16), ex("quả |na|", "🍈"), "m"),
        new Lesson("ow", "ơ", "ơ", ex("quả |mơ|", "🍑", 18), ex("quả |bơ|", "🥑"), "oo"),
        new Lesson("p", "p", "pê", ex("viên |pin|", "🔋", 32), ex("đèn |pin|", "🔦", 34), "q"),
        new Lesson("q", "q", "quy", ex("cái |quạt|", "🪭", 19), ex("quả |quýt|", "🍊"), "p"),
        new Lesson("r", "r", "rờ", ex("con |rắn|", "🐍", 20), ex("củ cà |rốt|", "🥕"), "d"),

        new Lesson("s", "s", "sờ", ex("ngôi |sao|", "⭐", 21), ex("hoa |sen|", "🪷"), "x"),
        new Lesson("t", "t", "tờ", ex("con |tôm|", "🦐", 22), ex("con |thỏ|", "🐇"), "l"),
        new Lesson("u", "u", "u", ex("ống |hút|", "🥤", 23), ex("cái |lu|", "🏺"), "uw"),
        new Lesson("uw", "ư", "ư", ex("con |cừu|", "🐑", 33), ex("|sư| tử", "🦁", 39), "u"),
        new Lesson("v", "v", "vờ", ex("cuộn |vải|", "🧵", 25), ex("con |vịt|", "🦆", 26), "u"),
        new Lesson("x", "x", "xờ", ex("máy |xúc|", "🚜", 27), ex("|xích| đu", "🛝", 40), "s"),
        new Lesson("y", "y", "i dài", ex("|y| tá", "🧑‍⚕️", 28), ex("chim |yến|", "🐦"), "i")
    };

    private static final String ALPHABET = "aăâbcdđeêghiklmnoôơpqrstuưvxy";

    static int atAlphabetPosition(int position) {
        String letter = ALPHABET.substring(position, position + 1);
        for (int index = 0; index < ALL.length; index++) {
            if (ALL[index].letter.equals(letter)) return index;
        }
        throw new IllegalStateException("Missing letter " + letter);
    }

    static int alphabetPosition(int index) {
        return ALPHABET.indexOf(ALL[index].letter);
    }

    final String key;
    final String letter;
    final String sound;
    final Example[] examples;
    final String neighbor;

    private Lesson(String key, String letter, String sound, Example first, Example second, String neighbor) {
        this.key = key;
        this.letter = letter;
        this.sound = sound;
        this.examples = new Example[]{first, second};
        this.neighbor = neighbor;
    }

    boolean isTarget(String grapheme) {
        // Preserve the breve, circumflex and horn while removing Vietnamese tone marks only.
        String plain = Normalizer.normalize(grapheme, Normalizer.Form.NFD)
            .replaceAll("[\u0300\u0301\u0303\u0309\u0323]", "");
        return letter.equals(Normalizer.normalize(plain, Normalizer.Form.NFC).toLowerCase(VI));
    }

    boolean matchesReading(String word) {
        String normalized = Normalizer.normalize(word, Normalizer.Form.NFC).toLowerCase(VI).trim();
        return sound.equals(normalized) || (!sound.contains(" ") && letter.equals(normalized));
    }

    int neighborIndex() {
        for (int i = 0; i < ALL.length; i++) if (ALL[i].key.equals(neighbor)) return i;
        throw new IllegalStateException("Unknown neighboring letter " + neighbor);
    }

    private static Example ex(String phrase, String picture) { return ex(phrase, picture, -1); }

    private static Example ex(String phrase, String picture, int art) {
        String[] parts = phrase.split("\\|", -1);
        return new Example(parts[0].trim(), parts[1], parts[2].trim(), picture, art);
    }

    static final class Example {
        final String before;
        final String word;
        final String after;
        final String picture;
        final int art;

        Example(String before, String word, String after, String picture, int art) {
            this.before = before;
            this.word = word;
            this.after = after;
            this.picture = picture;
            this.art = art;
        }

        String phrase() {
            return (before.isEmpty() ? "" : before + " ") + word + (after.isEmpty() ? "" : " " + after);
        }
    }
}
