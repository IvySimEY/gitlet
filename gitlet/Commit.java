package gitlet;
import org.w3c.dom.Attr;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;

// TODO: any imports you need here



/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    public Node HEAD = null;

    public Node LAST = null;

    public final static  SimpleDateFormat dateFormat = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy Z");

    public final static String folderpath = System.getProperty("user.dir");



    /*---------------------------------------------------------------------------*/
    public class Node implements Serializable{
        public String address;
        public Node prev;

        public Map<String, byte[]> listed_byte;

        public String comment;

        private String timeStamp;

        public Node(String cmt, Node prev, Map<String, byte[]> listed_byte, String timeStamp) {
            this.address = Utils.sha1(cmt);
            this.prev = prev;
            this.listed_byte = listed_byte;
            this.comment = cmt;
            this.timeStamp = timeStamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            Node that = (Node) o;
            return address.equals(that.address);
        }

        @Override
        public String toString() {
            return address + "";
        }

    }
    /*---------------------------------------------------------------------------*/

    public Commit() {
    }

    public void add(String cmt){
        Date currentDate = new Date();
        dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT-08:00"));
        String formattedDate = dateFormat.format(currentDate);

        Node curr_commit = new Node(cmt, HEAD, filesbyte(), formattedDate);
        HEAD = curr_commit;
        LAST = curr_commit;

        clear_staging_area();
    }

    private void clear_staging_area(){
        File folder = new File(".gitlet/staging_add");
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }

        folder = new File(".gitlet/staging_remove");
        files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    public void reset(String address, HashMap<String, Node> chains, Set<String> trackedfiles) throws IOException {
        Node temp = null;
        String[] filenames = null;
        String tempaddress = null;


        outerloop: //go into the target node
        for(Node commits: chains.values()){
            temp = commits;

            while(temp != null){
                if(temp.address.equals(address) ){
                    tempaddress = temp.address;
                    filenames = temp.listed_byte.keySet().toArray(new String[0]);
                    break outerloop;
                }
                temp  = temp.prev;
            }

        }

        if(tempaddress == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        } else {
            //check for untracked files
            for(String filename: gitlet.Utils.plainFilenamesIn(folderpath)){
                if (filename.endsWith(".txt")) {  // Check if the filename ends with ".txt"
                    if(temp.listed_byte.containsKey(filename) && !trackedfiles.contains(filename)){
                        System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                        System.exit(0);
                    }
                }
            }

            for(String filename: gitlet.Utils.plainFilenamesIn(folderpath)){
                if (filename.endsWith(".txt")) {  // Check if the filename ends with ".txt"
                    new File(filename).delete();
                }
            }

            for(String filename: filenames){
                //System.out.println(filename);
                restore(address, filename, chains);
            }

            clear_staging_area();
            HEAD = temp; //move HEAD to the node with address
        }





    }

    public void find(String cmt, HashMap<String, Node> chains) {
        Node temp = null;
        String toprint = null;

        for(Node commits: chains.values()){
            temp = commits;

            while(temp != null){
                if(temp.comment.equals(cmt)){
                    toprint = temp.address;
                    System.out.println(toprint);
                }
                temp  = temp.prev;
            }
        }

        if (toprint == null) {
            System.out.println("Found no commit with that message.");
        }
    }



    public void log() {
        Node temp = HEAD;

        if (HEAD == null) {
            System.out.print("head is null");
        }
        while(temp != null) {
            System.out.println("===");
            System.out.println("commit " + temp.address);
            System.out.println("Date: " + temp.timeStamp);
            System.out.println(temp.comment);
            System.out.println();
            temp = temp.prev;
        }
    }

    public void globalLog(HashMap<String, Node> chain) {
        Set<String> visitedCommits = new HashSet<>();

        for (Node branch: chain.values()) {
            Node temp = branch;

            while (temp != null) {
                if (!visitedCommits.contains(temp.address)) {
                    visitedCommits.add(temp.address);

                    System.out.println("===");
                    System.out.println("commit " + temp.address);
                    System.out.println("Date: " + temp.timeStamp);
                    System.out.println(temp.comment);
                    System.out.println();
                }

                temp = temp.prev;
            }
        }
    }


    private Map<String, byte[]> filesbyte(){
        Map<String, byte[]> fileContentsMap = new HashMap<>();

        File folder = new File(".gitlet/staging_add");
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                byte[] fileContent = Utils.readContents(file);
                fileContentsMap.put(file.getName(), fileContent);
                }
            return fileContentsMap;
            }
        return null;
    }

    public void restore(String filename) throws IOException {
        //get file byte from HEAD

        copybyte(filename, bytefromHEAD(filename));
    }

    public byte[] bytefromHEAD(String filename){
        byte[] filebyte = null;

        if( HEAD.listed_byte.size() != 0) {
            filebyte = HEAD.listed_byte.get(filename);}
        return filebyte;
    }

    public void restore(String address, String filename, HashMap<String, Node> chains) throws IOException {
        //find node
        Node temp = null;
        byte[] filebyte = null;
        String fileaddress = null;

        outerloop:
        for(Node commits: chains.values()){
            temp = commits;

            while(temp != null){
                if(temp.address.equals(address) || temp.address.substring(0,8).equals(address.substring(0,8))){
                    fileaddress = address;
                    filebyte = temp.listed_byte.get(filename);
                    break outerloop;
                }
                temp  = temp.prev;
            }

        }


        if (fileaddress == null) {
           System.out.println("No commit with that id exists.");
        } else {
            copybyte(filename, filebyte);
        }
    }

    private void copybyte(String filename, byte[] filebyte) throws IOException {
        if (filebyte == null) {
            System.out.print("File does not exist in that commit.");
        } else {
            File outFile = new File(filename);
            outFile.createNewFile();

            try {
                FileOutputStream fos = new FileOutputStream(outFile);
                fos.write(filebyte);
            } catch (IOException excp) {
                excp.printStackTrace();
            }
        }
    }

    public boolean checkFilescurr_commit(String filename){
        Node temp = HEAD;

        //make sure its not initial commit
        //make sure filename exists in curr commit
        if(temp.listed_byte != null && temp.listed_byte.get(filename) != null ){
            return true;
        }
        else{
            return false;
        }
    }

    public boolean ifsameincurr_commit(String filename) {
        Node temp = HEAD;

        byte[] byte1 = Utils.readContents(new File(filename));

        if(checkFilescurr_commit(filename)){
            byte[] byte2 = temp.listed_byte.get(filename);
            return Arrays.equals(byte1, byte2);
        }

        return false;
    }



}
