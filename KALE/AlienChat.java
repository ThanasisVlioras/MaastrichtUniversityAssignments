import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * This is a program implementing an alien chat protocol. The entry point is `main()`.
 *
 * The protocol is as follows:
 *  - clients connect to the server on port 5999 (by default)
 *  - initially, each client must send `AlienChat.PASSWORD` followed by newline to the server
 *  - then, lines of input from each user should be sent to the server
 *  - simultaneously, the server describes activity in the chat room line-by-line; these
 *    lines should be printed at the client for the user to read.
 *
 * The input sent to and the output received from the server is line based;
 * that is, newline "\n" separates each record. A limitation of the protocol is thus
 * that newlines cannot appear in messages!
 *
 * Lines from the server have one of the following formats:
 *  - "* USERNAME connected" -- a user known as USERNAME has joined the chat
 *  - "* USERNAME disconnected" -- a user known as USERNAME has left the chat
 *  - "- USERNAME >>> MESSAGE" -- a user known as USERNAME has sent the message MESSAGE into the chat.
 *
 * You may rely on USERNAME not containing any whitespace characters (space, newline, tab, etc.)
 *
 * However, each MESSAGE may contain arbitrary text except for newlines.
 */
public class AlienChat {
    public static final String PASSWORD = "F4EF9A36-5FCD-4D27-8A0A-FC7C77D3DBB2";
    public static final String HOSTNAME = getenv("ALIENCHAT_SERVER", "alienchat.demo.leastfixedpoint.com");
    public static final short PORT = Short.parseShort(getenv("ALIENCHAT_PORT", "5999"));

    public static final Cipher DEFAULTCIPHER = new Cipher(9);
    public static String VERYSECRETKEY = "GRUMINIONBANANA";

    /**
     * Connects to the server at `HOSTNAME`:`PORT`. Then, sends the `PASSWORD` and runs two
     * asynchronous tasks, one for relaying lines of input from the client to the server, and one
     * for relaying lines of output from the server for the client's user to read.
     *
     * Whichever task completes first cancels the other. Once both have completed (or been
     * cancelled), the socket is closed and the program terminates.
     */
    public static void main(String[] args) {
        var recognitionCode = args.length > 0 ? args[0] : "wwwwvvwxw";
        try {
            var socket = new Socket(HOSTNAME, PORT);
            InputStream fromServer = socket.getInputStream();
            PrintStream toServer = new PrintStream(socket.getOutputStream());

            loginToServer(toServer);

            var task1 = CompletableFuture.runAsync(() -> relayFromServer(fromServer));
            var task2 = CompletableFuture.runAsync(() -> relayToServer(recognitionCode, toServer));
            task1.thenApply((Void ignored) -> { task2.cancel(true); return null; });
            task2.thenApply((Void ignored) -> { task1.cancel(true); return null; });

            CompletableFuture.allOf(task1, task2).exceptionally((Throwable t) -> null).join();
            socket.close();
        } catch (Throwable e) {
            System.err.println(e);
            System.exit(1);
        }
    }

    /**
     * Retrieve an environment variable, returning a default value if the environment
     * variable is not defined. */
    static String getenv(String environmentVariable, String defaultValue) {
        var v = System.getenv(environmentVariable);
        return v == null ? defaultValue : v;
    }

    /** Sends the `PASSWORD` to the server (or whatever is listening to the other end of `toServer`). */
    static void loginToServer(PrintStream toServer) {
        toServer.println(PASSWORD);
    }

    /**
     * Accepts lines from `fromServer`, printing each to standard output.
     */
    static void relayFromServer(InputStream fromServer) {
        forEachLine("server", fromServer, (line) -> {
            String maybeMessage = parseMessageLine(line);
            System.out.println(maybeMessage != null ? maybeMessage : line);
        });
    }

    /**
     * Parses an incoming line of text from the server. If it is a "message" line,
     * according to the format description in the class comment, returns the
     * text of the message (only). Otherwise, returns null.
     */
    static String parseMessageLine(String line) {
        if (!line.startsWith("- ")) return null;

        // We start looking for our separator starting at index two, because
        // we want to skip the space following the dash at the beginning of the line:
        final var SEPARATOR = " >>> ";
        var separatorPos = line.indexOf(SEPARATOR, 2);
        if (separatorPos == -1) return null;

        // Remove the first `separatorPos` characters, plus as many as are in the separator itself:
        String messageWithMetadata = line.substring(separatorPos + SEPARATOR.length());
        String[] metadataPieces = messageWithMetadata.split(",", 3);

        if (metadataPieces.length != 3) return "% The following message has an incorect metadata format:\n" + messageWithMetadata;

        int cipherId = Integer.parseInt(metadataPieces[0]);
        if (!Cipher.AllowedIds.contains(cipherId)) return "% The following message uses an unknown cipher:\n" + messageWithMetadata;

        String decryptedMessage = new Cipher(cipherId).decrypt(metadataPieces[2], metadataPieces[1]);
        return "% The following message IS" + (validateMessage(decryptedMessage) ? "" : " NOT") + " from our clan:\n" + decryptedMessage;
    }

    /**
     * Accepts lines from standard input, forwarding them on to `toServer`.
     */
    static void relayToServer(String recognitionCode, PrintStream toServer) {
        forEachLine("user", System.in, (line) -> {
            /*
             * TODO: Modify this block to prepend the recognitionCode, followed by a space,
             * to each message the program sends to the server.
             */

            String encodedLine;
            if (line.startsWith("%%")) {
                encodedLine = line.substring(2);
            } else {
                encodedLine = DEFAULTCIPHER.CypherId + "," + VERYSECRETKEY + "," + DEFAULTCIPHER.encrypt(recognitionCode + " " + line, VERYSECRETKEY);
            }

            toServer.println(encodedLine);
        });
    }

    /**
     * Reads lines of input from `source`, sending each one to `sink` until there are
     * no more (when `readLine()` returns `null`) or an exception is thrown.
     */
    static void forEachLine(String sourceDescription, InputStream source, Consumer<String> sink) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(source));
            while (true) {
                var line = in.readLine();
                if (line == null) {
                    System.out.println("(End-of-stream from " + sourceDescription + ")");
                    break;
                }
                sink.accept(line);
            }
        } catch (Throwable e) {
            System.err.println("Error processing input from " + sourceDescription);
            System.err.println(e);
        }
    }

    /**
     * Validates the recognitionCode (if any) at the front of a received message.
     * Returns true if the recognitionCode is valid according to the pattern for our clan,
     * or false if the recognitionCode is invalid or missing.
     */
    static boolean validateMessage(String message) {
        // Expect "somerecognitioncode restofthemessage whichmaycontain spaces",
        // so use the `limit` parameter to `String.split()` to just split *once*:
        String[] pieces = message.split(" ", 2);

        if (pieces.length < 2) {
            // No space at all in the input! Clearly not valid.
            return false;
        }

        String recognitionCode = pieces[0];

        /*
         * TODO: Add code to validate `recognitionCode` according to your DFA/NFA.
         */
        return DFA.getSecretCodeDFA().checkString(recognitionCode);
    }
}