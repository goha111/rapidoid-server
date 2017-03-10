package cc.rapidoid;

import org.rapidoid.setup.App;
import org.rapidoid.setup.On;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Server {

    private String private_key = "12389084059184098308123098579283204880956800909293831223134798257496372124879237412193918239183928140";
    private int len_private_key = private_key.length();
    private String team_id = "CC don't play me";
    private String aws_id = "207238930590";
    private String template = team_id + ',' + aws_id + "\n" + "%s\n%s\n";
    private int[] prefix = new int[len_private_key];
    private static ZoneId zone = ZoneId.of("-05:00");
    private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    private int validate(String message) {
        int i = 1;
        int len = message.length();
        while (len > 0) {
            len -= i;
            i += 1;
            if (len == 0) {
                for (int j = 0; j < message.length(); j ++) {
                    if (message.charAt(j) < 'A' || message.charAt(j) > 'Z') {
                        return -1;
                    }
                }
                return i - 1;
            }
        }
        return -1;
    }

    private int getKey(String key, int offset) {
        int tens = (prefix[len_private_key - 2] - prefix[len_private_key - 2 - offset] + key.charAt(0) - '0' + 10) % 10;
        int ones = (prefix[len_private_key - 1] - prefix[len_private_key - 1 - offset] + key.charAt(1) - '0' + 10) % 10;
        return (tens * 10 + ones) % 25;
    }

    private void init() {
        prefix[0] = 1;
        for (int i = 1; i < len_private_key; i ++) {
            prefix[i] = (private_key.charAt(i) - '0' + prefix[i - 1]) % 10;
        }
    }

    private String decrypt( String message, int offset, int len_edge) {
        int len = message.length();
        char[][] matrix = new char[len][len];
        boolean[] set = new boolean[len_edge * len_edge];
        int count = 0;
        int direction = 0;
        int i = len_edge - 1;
        int j = -1;
        while (count < len) {
            if (direction == 0) {
                while (j + 1 <= i && !set[i * len_edge + j + 1]) {
                    j ++;
                    set[i * len_edge + j] = true;
                    matrix[i][j] = (char) ('A' + ((message.charAt(count) - 'A' - offset + 26) % 26));
                    count ++;
                }
                direction = 1;
            } else if (direction == 1) {
                while (j - 1 >= 0 && i - 1 >= 0 && !set[(i - 1) * len_edge + j - 1]) {
                    i -= 1;
                    j -= 1;
                    set[i * len_edge + j] = true;
                    matrix[i][j] = (char) ('A' + ((message.charAt(count) - 'A' - offset + 26) % 26));
                    count ++;
                }
                direction = 2;
            } else if (direction == 2) {
                while (i + 1 < len_edge && !set[(i + 1) * len_edge + j]) {
                    i += 1;
                    set[i * len_edge + j] = true;
                    matrix[i][j] = (char) ('A' + ((message.charAt(count) - 'A' - offset + 26) % 26));
                    count ++;
                }
                direction = 0;
            }
        }
        StringBuilder rtn = new StringBuilder();
        for (i = 0; i < len_edge; i ++) {
            for (j = 0; j <= i; j ++) {
                rtn.append(matrix[i][j]);
            }
        }
        return rtn.toString();
    }

    private void run(String[] args) {
        App.bootstrap(args).jpa(); // bootstrap JPA
        On.port(80);
        On.get("/q1").plain((String key, String message) -> {
            int len_edge = validate(message);
            if (key.length() > len_private_key || len_edge < 0) {
                message = "INVALID";
            } else {
                int len_key = key.length();
                int offset = len_private_key - len_key + 1;
                int z = getKey(key.substring(len_key - 2), offset);
                message = decrypt(message, 1 + z, len_edge);
            }
            return String.format(template, ZonedDateTime.now(zone).format(timeFormatter), message);
        });
    }

    public static void main(String[] args) {
        Server s = new Server();
        s.init();
        s.run(args);
    }
}
