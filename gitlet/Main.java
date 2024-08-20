package gitlet;

import java.io.*;
import java.util.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main implements Serializable {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */

    public Commit initcommit = null;
    public final static String folderpath = System.getProperty("user.dir");
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Commit initcommit = null;
        HashMap<String, Commit.Node> chains = new HashMap<>();
        String branch = null;
        HashMap<String, Set<String>> tracked = new HashMap<>();
        HashMap<String, Commit.Node> last = new HashMap<>();


        if (args.length == 0) {
            exitWithError("Please enter a command.");
        }

        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                initHelper();
                initcommit = new Commit();

                //copying object to commits/commits.txt
                initcommit.add("initial commit");
                savingObjects("commits.txt", initcommit);

                branch = "main";
                savingObjects("branch.txt", branch);

                chains.put(branch, initcommit.HEAD);
                savingObjects( "chains.txt", chains);

                last.put(branch, initcommit.LAST);
                savingObjects( "last.txt", last);

                tracked.put(branch, new HashSet<>());
                savingObjects("tracked.txt", tracked);
                break;

            case "add":
                addHelper(args[1], getCommit());

                tracked = getTracked();
                tracked.get(getBranch()).add(args[1]);
                savingObjects( "tracked.txt", tracked);
                break;
            case "commit":
                if (args[1].length() == 0) {
                    exitWithError("Please enter a commit message.");
                } else if (new File(".gitlet/staging_add").listFiles().length == 0 &&
                        new File(".gitlet/staging_remove").listFiles().length == 0) {
                    exitWithError("No changes added to the commit.");
                } else {
                    initcommit = getCommit();


                    if (initcommit == null) {
                        exitWithError("You need to initialize a repository first using 'init' command.");
                    }else {
                        String[] restOfArgs = Arrays.copyOfRange(args, 1, args.length);
                        String restOfString = String.join(" ", restOfArgs);

                        initcommit.add(restOfString);

                    }

                    savingObjects( "commits.txt", initcommit);

                    chains = getChain();
                    chains.put(getBranch(), initcommit.HEAD);
                    savingObjects( "chains.txt", chains);

                    last = getLast();
                    last.put(getBranch(), initcommit.LAST);
                    savingObjects( "last.txt", last);



                }
                break;
            case "log":

                initcommit = getCommit();

                if (initcommit == null) {
                    exitWithError("You need to initialize a repository first using 'init' command.");
                } else {
                    initcommit.log();
                }
                break;
            case "restore":
                initcommit = getCommit();

                if(args.length == 3) {
                    if (!args[1].equals("--")) {
                        exitWithError("You need to have --");
                    } else if (initcommit == null) {
                        exitWithError("You need to initialize a repository first using 'init' command.");
                    } else {
                        initcommit.restore(args[2]);
                    }
                } else if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        exitWithError("Incorrect operands."); }
                    initcommit.restore(args[1], args[3], getChain());
                }
                break;
            case "rm":
                if (args.length != 2) {
                    exitWithError("You need to have rm [file name]");
                }
                //case 1 if in stagging_add
                else if(new File(".gitlet/staging_add/" + args[1]).exists()) {
                    new File(".gitlet/staging_add/" + args[1]).delete();
                }
                //case 2 if it is tracked in commit
                else{
                    initcommit = getCommit();
                    if (initcommit.checkFilescurr_commit(args[1])){
                        File myFile = new File(args[1]);
                        byte[] bytes1 = initcommit.bytefromHEAD(args[1]);

                        if (myFile.exists()){ myFile.delete(); }

                        File newFile = new File(folderpath + "/.gitlet/staging_remove/" + args[1]);
                        newFile.createNewFile();
                        Utils.writeObject(newFile, bytes1);
                    }
                    else {
                        exitWithError("No reason to remove the file.");
                    }
                }
                break;
            case "status":
                initcommit = getCommit();

                if (initcommit == null) {
                    exitWithError("Not in an initialized Gitlet directory.");
                } else {
                    statusHelper();
                }
                break;
            case "global-log":
                initcommit = getCommit();
                initcommit.globalLog(getLast());
                break;
            case "reset":
                initcommit = getCommit();
                chains = getChain();

                if (args.length != 2) {
                    exitWithError("You need to have reset [commit id]");
                } else if (initcommit == null) {
                    exitWithError("You need to initialize a repository first using 'init' command.");
                } else {
                    initcommit.reset(args[1], getLast(), getTracked().get(getBranch()));
                    chains.put(getBranch(), initcommit.HEAD);


                }

                savingObjects( "commits.txt", initcommit);
                //chains.put(getBranch(), initcommit.HEAD);
                savingObjects( "chains.txt", chains);

                break;
            case "find":
                initcommit = getCommit();

                if (args.length != 2) {
                    exitWithError("You need to have find [commit message]");
                } else if (initcommit == null) {
                    exitWithError("You need to initialize a repository first using 'init' command.");
                } else {
                    String[] restOfArgs = Arrays.copyOfRange(args, 1, args.length);
                    String restOfString = String.join(" ", restOfArgs);

                    initcommit.find(restOfString, getLast());
                }

                break;
            case "branch":
                initcommit = getCommit();

                if (args.length != 2) {
                    exitWithError("You need to have branch [name]");
                } else if (initcommit == null) {
                    exitWithError("You need to initialize a repository first using 'init' command.");
                } else {
                    chains = getChain();
                    tracked = getTracked();

                    //System.out.println(chains.keySet());

                    if(!chains.keySet().contains(args[1])) { //no duplicates name
                        chains.put(args[1], initcommit.HEAD); //pointer to the current commit
                        savingObjects("chains.txt", chains); //saving chains

                        tracked.put(args[1], new HashSet<>());
                        savingObjects( "tracked.txt", tracked);

                    }
                    else{
                        System.out.print("A branch with that name already exists.");
                    }

                }
                break;
            case "rm-branch":
                initcommit = getCommit();
                branch = getBranch();

                if (args.length != 2) {
                    exitWithError("You need to have rm-branch [branch]");
                } else if (initcommit == null) {
                    exitWithError("You need to initialize a repository first using 'init' command.");
                } else if(branch.equals(args[1])) {
                    exitWithError("Cannot remove the current branch.");
                } else {
                    chains = getChain();

                    if(chains.keySet().contains(args[1])) {
                        chains.remove(args[1]);
                        savingObjects( "chains.txt", chains);
                    }
                    else{
                        System.out.print("A branch with that name does not exist.");
                    }
                }
                break;
            case "switch":
                initcommit = getCommit();
                chains = getChain();

                if (args.length != 2) {
                    exitWithError("You need to have switch [name]");
                }
                else if (initcommit == null) {
                    exitWithError("You need to initialize a repository first using 'init' command.");
                }
                else {
                    if (!chains.containsKey(args[1])){
                        System.out.println("No such branch exists.");
                    } else {
                        initcommit.reset(chains.get(args[1]).address, getLast(), getTracked().get(getBranch()));
                        branch = args[1];
                        chains.put(branch, initcommit.HEAD);
                    }
                }

                savingObjects( "commits.txt", initcommit);
                savingObjects( "chains.txt", chains);
                savingObjects( "branch.txt", branch);

                break;
            case "merge":
                initcommit = getCommit();
                chains = getChain();
                branch = getBranch();

                if (args.length != 2) {
                    exitWithError("You need to have rm-branch [branch]");
                } else if (initcommit == null) {
                    exitWithError("You need to initialize a repository first using 'init' command.");
                } else if (new File(".gitlet/staging_add").listFiles().length != 0 ||
                        new File(".gitlet/staging_remove").listFiles().length != 0){
                    exitWithError("You have uncommitted changes.");
                } else if (!chains.containsKey(args[1])) {
                    exitWithError("A branch with that name does not exist.");
                } else if (getBranch().equals(args[1])) {
                    exitWithError("Cannot merge a branch with itself.");
                } else {
                    String curr_branch = getBranch();
                    String given_branch = args[1];
                    Commit.Node curr_node = chains.get(curr_branch); //main head
                    Commit.Node given_node = chains.get(given_branch); //other head
                    Commit.Node splitpoint = null;

                    outerloop:
                    while(curr_node != null){ //starting from main, go backwards
                        given_node = chains.get(given_branch);
                        while(given_node != null){ //starting from other, go backwards
                            if(given_node.address.equals(curr_node.address)){ //until same address found
                                splitpoint = given_node;
                                break outerloop;
                            }
                            given_node = given_node.prev;
                        }
                        curr_node = curr_node.prev;
                    }

                    if(splitpoint.equals(chains.get(args[1]))) { //same with branch head
                        exitWithError("Given branch is an ancestor of the current branch.");
                    } else if (splitpoint.equals(chains.get(getBranch()))) { //if same with main head
                        initcommit.reset(chains.get(given_branch).address, getLast(), getTracked().get(getBranch())); //reset to the other branch
                        chains.put(getBranch(), initcommit.HEAD);
                        System.out.println("Current branch fast-forwarded.");
                    } else{
                        Map<String, byte[]> fileinSP = splitpoint.listed_byte;
                        Map<String, byte[]> fileincurr = chains.get(curr_branch).listed_byte;
                        Map<String, byte[]> fileinbranch = chains.get(given_branch).listed_byte;

                        for (String filename: fileinSP.keySet()) {
                            boolean existinmain = fileincurr.keySet().contains(filename);
                            boolean existinbranch = fileinbranch.keySet().contains(filename);

                            boolean sameinmain = fileincurr.get(filename) == (fileinSP.get(filename));
                            boolean sameinbranch = fileinbranch.get(filename) == (fileinSP.get(filename));

                            boolean mainandbranch = fileincurr.get(filename) == (fileinbranch.get(filename));

                            boolean existinCWD = gitlet.Utils.plainFilenamesIn(folderpath).contains(filename);

                            if (existinmain && existinbranch && !sameinbranch && sameinmain){
                                File newFile = Utils.join(folderpath, "/.gitlet/staging_add/" + filename);
                                Utils.writeContents(newFile, fileinbranch.get(filename));
                            }
                            if (existinbranch){
                                File newFile = Utils.join(folderpath, "/.gitlet/staging_add/" + filename);
                                Utils.writeContents(newFile, fileinbranch.get(filename));
                            }
                            if(sameinmain && !existinbranch){
                                new File(filename).delete();
                            }
                        }

                        for(String filename: fileinbranch.keySet()) {
                            if(!fileinSP.keySet().contains(filename)){
                                File newFile = Utils.join(folderpath, "/.gitlet/staging_add/" + filename);
                                Utils.writeContents(newFile, fileinbranch.get(filename));

                                File file = new File(filename);
                                file.createNewFile();
                                Utils.writeContents(file,  fileinbranch.get(filename));
                            }
                        }

                        initcommit.add("Merged " + given_branch + " into " + curr_branch + ".");
                        branch = curr_branch;
                    }

                    savingObjects( "commits.txt", initcommit);
                    savingObjects( "chains.txt", chains);
                    savingObjects( "branch.txt", branch);
                    savingObjects("branch.txt", branch);
                }

                break;
            case "see":
                chains = getChain();
                String curr_branch = "main";
                String given_branch = "other";
                Commit.Node curr_node = chains.get("main"); //main head
                Commit.Node given_node = chains.get("other"); //other head

                while(curr_node != null) { //starting from other, go backwards
                    System.out.println(curr_node.comment + curr_node.address);
                    curr_node = curr_node.prev;
                }

                while(given_node != null) { //starting from other, go backwards
                    System.out.println(given_node.comment + given_node.address);
                    given_node = given_node.prev;
                }


//                for (Map.Entry<String, Commit.Node> entry : getChain().entrySet()) {
//                    System.out.println(entry.getKey());
//                    System.out.println(entry.getValue().address);
//                }
//
//                for (Map.Entry<String, Commit.Node> entry : getChain().entrySet()) {
//                    System.out.println(entry.getKey());
//                    System.out.println(entry.getValue().address);
//                }
                break;
            default:
                exitWithError("No command with that name exists.");
        }
        return;
    }

    private static void statusHelper(){
        System.out.println("=== Branches ===");
        System.out.println("*" + getBranch());

        for (String keyvalue : getTracked().keySet()) {
            if (!keyvalue.equals(getBranch())) {
                System.out.println(keyvalue);
            }
        }
        System.out.println("");

        System.out.println("=== Staged Files ===");

        File folder = new File(".gitlet/staging_add");
        File[] files = folder.listFiles();
        Arrays.sort(files);
        if (files != null) {
            for (File file : files) {
                System.out.println(file.getName());
            } }
            System.out.println("");


        System.out.println("=== Removed Files ===");
        folder = new File(".gitlet/staging_remove");
        files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                System.out.println(file.getName());
            } } System.out.println("");

        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println("");

        System.out.println("=== Untracked Files ===");
        System.out.println("");



    }

    private static <T> T getObject(String filename, Class<T> type) {
        T result;
        try {
            File inFile = new File(folderpath + "/.gitlet/commits/" + filename);
            ObjectInputStream inp = new ObjectInputStream(new FileInputStream(inFile));
            result = type.cast(inp.readObject());
            inp.close();
        } catch (IOException | ClassNotFoundException excp) {
            result = null;
        }
        return result;
    }

    private static Commit getCommit() {
        return getObject( "commits.txt", Commit.class);
    }

    private static HashMap<String, Commit.Node> getChain() {
        return getObject( "chains.txt", (Class<HashMap<String, Commit.Node>>)(Class<?>)HashMap.class);
    }

    private static HashMap<String, Commit.Node> getLast() {
        return getObject( "last.txt", (Class<HashMap<String, Commit.Node>>)(Class<?>)HashMap.class);
    }
    private static HashMap<String, Set<String>> getTracked() {
        return getObject( "tracked.txt", (Class<HashMap<String, Set<String>>>)(Class<?>)HashMap.class);
    }

    private static String getBranch() {
        return getObject( "branch.txt", String.class);
    }

    private static void savingObjects( String filename, Object object) throws IOException {
        File newFile = new File(folderpath + "/.gitlet/commits/" + filename);
        newFile.createNewFile();

        try {
            File outFile = new File(folderpath + "/.gitlet/commits/" + filename);
            ObjectOutputStream out =
                    new ObjectOutputStream(new FileOutputStream(outFile));
            out.writeObject(object);
            out.close();
        } catch (IOException excp) {
            excp.printStackTrace();
        }
    }

    public static void initHelper() throws IOException {
        Repository newrepo = new Repository();
        newrepo.createHiddenFile();

        //create commit txt file

        String folderpath = System.getProperty("user.dir");
        File newFile = new File(folderpath + "/.gitlet/commits/commits.txt");
        newFile.createNewFile();

    }

    public static void addHelper(String fileName, Commit mycommit) throws IOException {
        if(!Repository.GITLET_DIR.exists()) {
            System.out.print("not initialised");
            System.exit(-1);
        }

        File myFile = new File(fileName);

        if (!myFile.exists()) {
            exitWithError("File does not exist.");
        } else { //adding to stagging_add
            //check if identical to the version in the current commit,
            if(mycommit.ifsameincurr_commit(fileName)){
                //remove from stagging area
                new File(".gitlet/staging_add/" + fileName).delete();
                new File(".gitlet/staging_remove/" + fileName).delete();
            } else {
                String string1 = Utils.readContentsAsString(myFile);
                //path to staging area
                File newFile = Utils.join(folderpath, "/.gitlet/staging_add/" + fileName);
                //newFile.createNewFile();
                Utils.writeContents(newFile, string1);
            }
        }


    }
    /*---------------------------------------------------------------------------------------------*/
    public static void exitWithError(String message) {
        if (message != null && !message.equals("")) {
            System.out.println(message);
        }
        System.exit(0);
    }
}
