package cs321.search;

import cs321.common.ParseArgumentException;

import java.sql.*;

/**
 * The driver class for searching a Database of a B-Tree.
 */
public class SSHSearchDatabase {

    /**
     * String holding the static part of the {@code type} argument used in running this program. Used for parsing
     * command-line arguments.
     */
    private static final String TYPE_FLAG = "--type=";

    /**
     * String holding the static part of the {@code database} argument used in running this program. Used for parsing
     * command-line arguments.
     */
    static final String DATABASE_FLAG = "--database=";

    /**
     * String holding the static part of the {@code top-frequency} argument used in running this program. Used for
     * parsing command-line arguments.
     */
    static final String TOP_FREQUENCY_FLAG = "--top-frequency=";

    /**
     * The test data to insert into the database when the user specifies `--type=test`.
     */
    static final String[] TEST_DATA = {
            "('Accepted-111.222.107.90', 25)",
            "('Accepted-112.96.173.55', 3)"  ,
            "('Accepted-112.96.33.40', 3)"   ,
            "('Accepted-113.116.236.34', 6)" ,
            "('Accepted-113.118.187.34', 2)" ,
            "('Accepted-113.99.127.215', 2)" ,
            "('Accepted-119.137.60.156', 1)" ,
            "('Accepted-119.137.62.123', 9)" ,
            "('Accepted-119.137.62.142', 1)" ,
            "('Accepted-119.137.63.195', 14)",
            "('Accepted-123.255.103.142', 5)",
            "('Accepted-123.255.103.215', 5)",
            "('Accepted-137.189.204.138', 1)",
            "('Accepted-137.189.204.155', 1)",
            "('Accepted-137.189.204.220', 1)",
            "('Accepted-137.189.204.236', 1)",
            "('Accepted-137.189.204.246', 1)",
            "('Accepted-137.189.204.253', 3)",
            "('Accepted-137.189.205.44', 2)" ,
            "('Accepted-137.189.206.152', 1)",
            "('Accepted-137.189.206.243', 1)",
            "('Accepted-137.189.207.18', 1)" ,
            "('Accepted-137.189.207.28', 1)" ,
            "('Accepted-137.189.240.159', 1)",
            "('Accepted-137.189.241.19', 2)"
    };

    /**
     * Based on user-supplied arguments,
     * @param args the command-line arguments
     */
    public static void main(String[] args){
        String[] parsedArgs = parseArguments(args);
        try {
            validateArgs(parsedArgs);
        } catch (ParseArgumentException pae) {
            System.err.println(pae.getMessage());
            printUsage();
        }
        // Command-line arguments should now be usable

        //// Map arguments to variables \\\\
        String         type = parsedArgs[0];
        String databasePath = parsedArgs[1];
        int    topFrequency = Integer.parseInt(parsedArgs[2]);

        //// Set up SQLite connection \\\\
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath)) {
            if ("test".equals(type)) {
                createTestTable(connection);
                System.out.println("Test database created: " + databasePath);
            } else {
                type = type.replace("-", ""); // Remove hyphens as they are invalid in SQL table names
                fetchTopFrequencies(connection, type, topFrequency);
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    /**
     * Parses command-line arguments. The arguments should begin with the flags "--type=", "--database=", and
     * "--top-frequency=", in any order. Returns the user-supplied values in the order {@code type, database,
     * top-frequency}, without the flags attached.
     * @param args the command-line arguments obtained from {@code main()}
     * @return the parsed arguments
     */
    private static String[] parseArguments(String[] args) {
        String type = null;
        String databasePath = null;
        String topFrequency = null;

        for (String arg : args) {
            if (arg.startsWith(TYPE_FLAG)) {
                type = arg.substring(TYPE_FLAG.length()); // Extract value after "--type="
            } else if (arg.startsWith(DATABASE_FLAG)) {
                databasePath = arg.substring(DATABASE_FLAG.length()); // Extract value after "--database="
            } else if (arg.startsWith(TOP_FREQUENCY_FLAG)) {
                topFrequency = arg.substring(TOP_FREQUENCY_FLAG.length()); // Extract value after "--top-frequency="
            }
        }

        // First argument should only contain letters, numbers, underscores, and hyphens (hyphens are removed later)
        // Use regex to remove other characters
        String regexPattern = "[^a-zA-Z0-9_-]";
        if (type != null) {
            type = type.replaceAll(regexPattern, "");
        }

        return new String[]{type, databasePath, topFrequency};
    }

    /**
     * Validates command-line arguments which have already been parsed (such that they only contain the values, without
     * the flags). Throws a {@code ParseArgumentException} if the args are not valid.
     * @param args the command-line arguments
     * @throws ParseArgumentException on invalid args
     */
    private static void validateArgs(String[] args) throws ParseArgumentException {
        // First two arguments must be non-null (i.e. they must exist)
        if (args[0] == null) {
            throw new ParseArgumentException("Missing required argument: type");
        }
        if (args[1] == null) {
            throw new ParseArgumentException("Missing required argument: databasePath");
        }

        // Third argument must be an integer
        try {
            Integer.parseInt(args[2]);
        } catch (NumberFormatException nfe) {
            throw new ParseArgumentException("top-frequency must be an integer");
        }
    }

    /**
     * Prints the command-line usage of this program to stderr.
     */
    private static void printUsage() {
        System.err.println("Usage: java -jar build/libs/SSHSearchDatabase.jar --type=<tree-type>" +
                "--database=<sqlite-database-path> --top-frequency=<10/25/50>");
    }

    /**
     * Creates a test table named {@code acceptedip} in the SQLite database associated with {@code connection}.
     * @param connection a {@code Connection} object associated with an SQLite database
     * @throws SQLException on database connection failure
     */
    private static void createTestTable(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        statement.setQueryTimeout(30);

        // Create table
        statement.executeUpdate("DROP TABLE IF EXISTS acceptedip");
        statement.executeUpdate("CREATE TABLE acceptedip (ip TEXT PRIMARY KEY, frequency INTEGER)");

        for (String entry : TEST_DATA) {
            statement.executeUpdate("INSERT INTO acceptedip VALUES " + entry);
        }
    }

    /**
     * Fetches the top {@code num} most frequent entries from the table named {@code tableName} in the SQLite database
     * associated with {@code connection}.
     * @param connection a {@code Connection} object associated with an SQLite database
     * @param tableName the name of the table from which to fetch data
     * @param num the number of entries to fetch
     * @throws SQLException - on database connection failure
     */
    private static void fetchTopFrequencies(Connection connection, String tableName, int num) throws SQLException {
        Statement statement = connection.createStatement();
        String query = "SELECT ip, frequency" +
                       " FROM " + tableName +
                       " ORDER BY frequency DESC" +
                       " LIMIT " + num; // who cares about sanitizing input?

        ResultSet rs = statement.executeQuery(query);

        // Print the result set
//        System.out.printf("%-23s %s%n", "Key", "Frequency");
//        System.out.println("----------------------- ---------");
        // The above was not included in the output example so it is commented out. Maybe we'd want to implement
        // something like it later; to make the header labels dynamic, we would need to make it into its own function.
        while (rs.next()) {
            System.out.printf("%-20s %d%n", rs.getString("ip"), rs.getInt("frequency"));
        }
    }
}
