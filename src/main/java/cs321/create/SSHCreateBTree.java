package cs321.create;

import cs321.btree.BTree;
import cs321.btree.BTreeException;
import cs321.btree.TreeObject;
import cs321.common.ParseArgumentException;

import java.io.FileNotFoundException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Scanner;

public class SSHCreateBTree {
    private static final String[] allowedTypes = {"accepted-ip", "accepted-time", "invalid-ip",
            "invalid-time", "failed-ip", "failed-time", "reverseaddress-ip", "reverseaddress-time", "user-ip"};
    private static final String CACHE_FLAG = "--cache=";
    private static final String DEGREE_FLAG = "--degree=";
    private static final String SSH_FLAG = "--sshFile=";
    private static final String TYPE_FLAG = "--type=";
    private static final String CACHE_SIZE_FLAG = "--cacheSize=";
    private static final String DATABASE_FLAG = "--database=";
    private static final String DEBUG_FLAG = "--debug=";
    private static final String DATABASE_PATH = "SSHLogDB.db";
    private static final int DISK_BLOCK_SIZE = 4096;

    private static int cacheArg = -1;
    private static int degreeArg = -1;
    private static String sshFile = null;
    private static String typeArg = null;
    private static int cacheSizeArg = -1;
    private static Boolean databaseArg = null;
    private static int debugArg = 0;

    //java -jar build/libs/SSHCreateBTree.jar --cache=<0/1> --degree=<btree-degree> \
    //      --sshFile=<ssh-File> --type=<tree-type> [--cache-size=<n>] \
    //      --database=<yes/no> [--debug=<0|1>]

    public static void main(String[] args)
    {
        try {
            parseArguments(args);
        } catch (ParseArgumentException e)
        {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

        int degree = (degreeArg == 0) ? BTree.calculateOptimalMinimumDegree(DISK_BLOCK_SIZE) : degreeArg;
        String fileName = "SSH_log.txt.ssh.btree." + typeArg + "." + degreeArg;
        BTree bTree;
        try {
            bTree = new BTree(degree, fileName);
            File logs = new File(sshFile);
            Scanner scan = new Scanner(logs);
            while(scan.hasNextLine()) {
                // Here, we are inside one line of the log file
                // 1. Get key from line & add to BTree
                    // Each line should correspond to 1 TreeObject
                // 2. Add key to dumpfile (if debug == 1)

                // Tokenize the line
                // [date], [time], [status], [ip], [user]
                String[] lineTokens = getTokens(scan.nextLine());


                // Now we have tokenized the line

                // Now we need to add the key to the BTree
                // And to the dump file if debug == 1

                // Adding key to BTree:
                // Here we need to create the right type of TreeObject based on
                // typeArg
                // e.g., if typeArg=="accepted-ip", then key will be "Accepted-###.###.##.##"
                //       if typeArg=="invalid-ip", key will be        "Invalid-###.###.##.##"


                addNewTreeObject(lineTokens, bTree, typeArg);

                // Entire line has been scanned, so move to next line
//                String line = scan.nextLine();
                //TODO close scanner
            }
            scan.close();

            if(debugArg == 1){
                String fileDumpName = "dump-" + typeArg + "." + degreeArg + ".txt";
                PrintWriter write = new PrintWriter(fileDumpName);
                bTree.dumpToFile(write);
                write.close();
            }
            if(databaseArg) {
                String tableName = typeArg.replace("-","");
                bTree.dumpToDatabase(DATABASE_PATH, tableName);
            }
            bTree.close();
        } catch (BTreeException | FileNotFoundException e)  {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * blah blah blah [date], [time], [status], [ip], [user] blah blah blah TODO
     * @param lineTokens
     * @param bTree
     * @return whether an object was inserted
     */
    private static boolean addNewTreeObject(String[] lineTokens, BTree bTree, String type) throws IOException {
        boolean inserted = false;
        String time   = lineTokens[1];
        String status = lineTokens[2];
        String ip     = lineTokens[3];
        String user   = lineTokens[4];

        // time should be HH:MM, so extract first 5 characters of time string
        time = time.substring(0, 5 - 1);

        switch(type) {
            case "accepted-ip":
                if (status.equalsIgnoreCase("accepted")) {
                    bTree.insert(new TreeObject("Accepted-" + ip));
                    inserted = true;
                }
                break;
            case "accepted-time":
                if (status.equalsIgnoreCase("accepted")) {
                    bTree.insert(new TreeObject("Accepted-" + time));
                    inserted = true;
                }
                break;
            case "invalid-ip":
                if (status.equalsIgnoreCase("invalid")) {
                    bTree.insert(new TreeObject("Invalid-" + ip));
                    inserted = true;
                }
                break;
            case "invalid-time":
                if (status.equalsIgnoreCase("invalid")) {
                    bTree.insert(new TreeObject("Invalid-" + time));
                    inserted = true;
                }
                break;
            case "failed-ip":
                if (status.equalsIgnoreCase("failed")) {
                    bTree.insert(new TreeObject("Failed-" + ip));
                    inserted = true;
                }
                break;
            case "failed-time":
                if (status.equalsIgnoreCase("failed")) {
                    bTree.insert(new TreeObject("Failed-" + time));
                    inserted = true;
                }
                break;
            case "reverseaddress-ip":
                if (status.equalsIgnoreCase("reverse") || status.equalsIgnoreCase("address")) {
                    bTree.insert(new TreeObject(status + "-" + ip));
                    inserted = true;
                }
                break;
            case "reverseaddress-time":
                if (status.equalsIgnoreCase("reverse") || status.equalsIgnoreCase("address")) {
                    bTree.insert(new TreeObject(status + "-" + time));
                    inserted = true;
                }
                break;
            case "user-ip":
                if (user != null) {
                    bTree.insert(new TreeObject(user + "-" + ip));
                    inserted = true;
                }
                break;
        }

        return inserted;
    }

    /**
     * [date], [time], [status], [ip], [user]
     * @param line
     * @return
     */
    private static String[] getTokens(String line) {
        Scanner lineScan = new Scanner(line);
        String[] tokens = new String[5];

        tokens[0] = lineScan.next(); // First token: date
        tokens[1] = lineScan.next(); // Second token: time
        tokens[2] = lineScan.next(); // Third token: status
        if ("Address".equals(tokens[2])) {
            tokens[3] = lineScan.next(); // Fourth token: ip
            if (lineScan.hasNext()) { // If user token exists
                tokens[4] = lineScan.next();
            }
        }
        else if ("reverse".equals(tokens[2])) {
            // If status = reverse, next two tokens are either user followed by ip,
            // or just ip
            String dummy = lineScan.next(); // Either username or ip
            if (lineScan.hasNext()) {
                // Fifth token, if it exists, must be ip
                tokens[3] = lineScan.next(); // ip
                tokens[4] = dummy; // Username
            }
            else {
                tokens[3] = dummy; // ip
            }
        }
        else { // status = Accepted, Invalid, or Failed: standard line (all 5 fields)
            tokens[4] = lineScan.next(); // Username
            tokens[3] = lineScan.next(); // IP
        }
        lineScan.close();
        return tokens;
    }

    private static void parseArguments(String[] args) throws ParseArgumentException
    {
        for (String s : args)
        {
            if (s.startsWith(CACHE_FLAG))
            {
                cacheArg = Integer.parseInt(s.substring(CACHE_FLAG.length()));
            }
            if (s.startsWith(DEGREE_FLAG))
            {
                degreeArg = Integer.parseInt(s.substring(DEGREE_FLAG.length()));
            }
            if (s.startsWith(SSH_FLAG))
            {
                sshFile = s.substring(SSH_FLAG.length());
            }
            if (s.startsWith(TYPE_FLAG))
            {
                typeArg = s.substring(TYPE_FLAG.length());
            }
            if (s.startsWith(CACHE_SIZE_FLAG))
            {
                cacheSizeArg = Integer.parseInt(s.substring(CACHE_SIZE_FLAG.length()));
            }
            if (s.startsWith(DATABASE_FLAG))
            {
                if (s.substring(DATABASE_FLAG.length()).equals("yes"))
                {
                    databaseArg = true;
                }
                else if (s.substring(DATABASE_FLAG.length()).equals("no"))
                {
                    databaseArg = false;
                }
                else
                {
                    throw new ParseArgumentException("<database> must be either yes or no.");
                }
            }
            if (s.startsWith(DEBUG_FLAG))
            {
                debugArg = Integer.parseInt(s.substring(DEBUG_FLAG.length()));
            }
        }

        if (cacheArg == -1)
        {
            throw new ParseArgumentException("No cache arg provided.");
        }
        if (degreeArg == -1)
        {
            throw new ParseArgumentException("No degree arg provided.");
        }
        if (sshFile == null)
        {
            throw new ParseArgumentException("No sshFile arg provided.");
        }
        if (typeArg == null)
        {
            throw new ParseArgumentException("No type arg provided.");
        }
        if (databaseArg == null)
        {
            throw new ParseArgumentException("No database arg provided.");
        }
        if (cacheArg == 1 && cacheSizeArg == -1)
        {
            throw new ParseArgumentException("No cacheSize arg provided.");
        }

        boolean flag = false;
        for (int i = 0; i < allowedTypes.length; i++)
        {
            if (typeArg.equals(allowedTypes[i]))
            {
                flag = true;
            }
        }
        if (!flag)
        {
            throw new ParseArgumentException("Not one of the allowed types.");
        }
    }
}