package cs321.create;

import cs321.btree.BTree;
import cs321.btree.BTreeException;
import cs321.common.ParseArgumentException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Scanner;

public class SSHCreateBTree {
    private static final String[] allowedTypes = {"accepted-ip", "accepted-time", "invalid-ip",
            "invalid-time", "failed-ip", "failed-time", "reverseaddress-ip", "reverseaddress-time", "user-ip"};
    private static final String CACHE_FLAG = "--cache=";
    private static final String DEGREE_FLAG = "--degree=";
    private static final String SSH_FLAG = "--sshFile=";
    private static final String TYPE_FLAG = "--type=";
    private static final String CACHE_SIZE_FLAG = "--cache-size=";
    private static final String DATABASE_FLAG = "--database=";
    private static final String DEBUG_FLAG = "--debug=";
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

        String fileName = "SSH_log.txt.ssh.btree." + typeArg + "." + degreeArg;
        BTree bTree;
        try {
            bTree = new BTree(degreeArg, fileName);
            File logs = new File(sshFile);
            Scanner scan = new Scanner(logs);
            while(scan.hasNextLine()){
                String line = scan.nextLine();
                ArrayList<String> keys = getTokens(line, typeArg);
                for(int i = 0; i <= keys.size(); i++){
                    TreeObject obj = new TreeObject();

                }

            }
        } catch (BTreeException | FileNotFoundException e)  {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

//        fileScanner.nextLine();
    }

    private static ArrayList<String> getTokens(String line, String type){
        ArrayList<String> output = new ArrayList<>();
        String[] parts = line.split("\\s+");
        if(parts.length < 4 || parts.length > 5){
            return output; // or add to the file
        }
        String date = parts[0];
        String time = parts[1];
        String status = parts[2];
        String ip = parts[3];

        // Make sure to change the strings to the variables when adding to the output list.
        switch (type){
            case "accepted-ip":
                output.add("Accepted-" + ip);
                break;
            case "accepted-time":
                output.add("Accepted-" + time);
                break;
            case "invalid-ip":
                output.add("Invalid-" + ip);
                break;
            case "invalid-time":
                output.add("Invalid-" + time);
                break;
            case "failed-ip":
                output.add("Failed-" + ip);
                break;
            case "failed-time":
                output.add("Failed-" + time);
                break;
            case "reverseaddress-ip":
                output.add("ReverseORAddress" + ip);
                break;
            case "reverseaddress-time":
                output.add("ReverseORAddress" + time);
                break;
            case "user-ip":
                output.add("User-" + ip);
                break;
        }
        return output;
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
                    throw new ParseArgumentException("Must be either yes or no.");
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
        if (cacheArg == 1)
        {
            if (cacheSizeArg == -1)
            {
                throw new ParseArgumentException("No cacheSize arg provided.");
            }
        }

        boolean flag = false;
        for (int i = 0; i < allowedTypes.length; i++)
        {
            if (typeArg.equals(allowedTypes[i]))
            {
                flag = true;
            }
        }
        if (flag == false)
        {
            throw new ParseArgumentException("Not one of the allowed types.");
        }
    }
}